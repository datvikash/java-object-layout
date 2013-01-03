/*
 * #%L
 * Java Object Layout Dumper
 * %%
 * Copyright (C) 2012 - 2013 Aleksey Shipilev
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.openjdk.tools.objectlayout;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.sun.management.HotSpotDiagnosticMXBean;
import sun.misc.Unsafe;

import javax.management.MBeanServer;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.Arrays;

public class VMSupport {

    public static Instrumentation INSTRUMENTATION;

    public final static Unsafe U;
    public final static int ADDRESS_SIZE;
    public final static int OOP_SIZE;
    public final static int HEADER_SIZE;
    public final static int OBJECT_ALIGNMENT;

    private static final String HOTSPOT_BEAN_NAME =
            "com.sun.management:type=HotSpotDiagnostic";

    private static HotSpotDiagnosticMXBean getHotspotMBean() {
        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            return ManagementFactory.newPlatformMXBeanProxy(server,
                    HOTSPOT_BEAN_NAME, HotSpotDiagnosticMXBean.class);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception exp) {
            throw new RuntimeException(exp);
        }
    }

    private static boolean autoCompressedOops;
    private static boolean autoObjectAlignment;

    static {
        // steal Unsafe
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            U = (Unsafe) unsafe.get(null);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        int headerSize;
        int oopSize;

        // When running with CompressedOops on 64-bit platform, the address size
        // reported by Unsafe is still 8, while the real reference fields are 4 bytes long.
        // Try to guess the reference field size with this naive trick.
        try {
            long off1 = U.objectFieldOffset(CompressedOopsClass.class.getField("obj1"));
            long off2 = U.objectFieldOffset(CompressedOopsClass.class.getField("obj2"));
            oopSize = (int) Math.abs(off2 - off1);
        } catch (NoSuchFieldException e) {
            oopSize = -1;
        }

        try {
            long off1 = U.objectFieldOffset(HeaderClass.class.getField("b1"));
            headerSize = (int) off1;
        } catch (NoSuchFieldException e) {
            headerSize = -1;
        }

        ADDRESS_SIZE = U.addressSize();
        HEADER_SIZE = headerSize;

        try {
            HotSpotDiagnosticMXBean mBean = getHotspotMBean();
            oopSize = Boolean.valueOf(mBean.getVMOption("UseCompressedOops").getValue()) ? 4 : 8;
        } catch (Exception e) {
            // not the hotspot, or 32-bit version, falling back
            autoCompressedOops = true;
        }
        OOP_SIZE = oopSize;

        int alignment;
        try {
            HotSpotDiagnosticMXBean mBean = getHotspotMBean();
            alignment = Integer.valueOf(mBean.getVMOption("ObjectAlignmentInBytes").getValue());
        } catch (Exception e) {
            // not the hotspot? falling back
            if (VMSupport.ADDRESS_SIZE != VMSupport.OOP_SIZE) {
                alignment = guessAlignment() * 8; // assume compressed oops are << 3
            } else {
                alignment = guessAlignment();
            }
            autoObjectAlignment = true;
        }
        OBJECT_ALIGNMENT = alignment;

    }

    public static void storeInstrumentation(Instrumentation inst) {
        VMSupport.INSTRUMENTATION = inst;
    }

    public static void detect(PrintStream out) {
        out.print("Running " + (VMSupport.ADDRESS_SIZE * 8) + "-bit VM, ");
        if (OOP_SIZE != ADDRESS_SIZE) {
            out.print("using compressed references" +
                    (autoCompressedOops ? "(automatically guessed, can be unreliable), " : ", "));
        }
        out.println("objects are " + VMSupport.OBJECT_ALIGNMENT + " bytes aligned" +
                (autoObjectAlignment ? " (automatically guessed, can be unreliable)." : "."));
        out.println();
    }

    static class CompressedOopsClass {
        public Object obj1;
        public Object obj2;
    }

    static class HeaderClass {
        public boolean b1;
    }

    public static int guessAlignment() {
        final int COUNT = 1000*1000;
        Object[] array = new Object[COUNT];
        long[] offsets = new long[COUNT];

        for (int c = 0; c < COUNT - 3; c += 3) {
            array[c + 0] = new MyObject1();
            array[c + 1] = new MyObject2();
            array[c + 1] = new MyObject3();
        }

        for (int c = 0; c < COUNT; c++) {
            offsets[c] = addressOf(array[c]);
        }

        Arrays.sort(offsets);

        Multiset<Integer> sizes = HashMultiset.create();
        for (int c = 1; c < COUNT; c++) {
            sizes.add((int) (offsets[c] - offsets[c - 1]));
        }

        int min = -1;
        for (int s : sizes.elementSet()) {
            if (s <= 0) continue;
            if (min == -1) {
                min = s;
            } else {
                min = gcd(min, s);
            }
        }

        return min;
    }

    private static int gcd(int a, int b) {
        while (b > 0) {
            int temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    static class MyObject1 {

    }

    static class MyObject2 {
        private boolean b1;
    }

    static class MyObject3 {
        private int i1;
    }

    public static long addressOf(Object o) {
        Object[] array = new Object[]{o};

        long baseOffset = U.arrayBaseOffset(Object[].class);
        long objectAddress;
        switch (OOP_SIZE) {
            case 4:
                objectAddress = U.getInt(array, baseOffset);
                break;
            case 8:
                objectAddress = U.getLong(array, baseOffset);
                break;
            default:
                throw new Error("unsupported address size: " + OOP_SIZE);
        }

        return (objectAddress);
    }

    private static int sizeOfArray(Object o) {
        int base = U.arrayBaseOffset(o.getClass());
        int scale = U.arrayIndexScale(o.getClass());
        Class<?> type = o.getClass().getComponentType();
        if (type == boolean.class) return base + ((boolean[]) o).length * scale;
        if (type == byte.class) return base + ((byte[]) o).length * scale;
        if (type == short.class) return base + ((short[]) o).length * scale;
        if (type == char.class) return base + ((char[]) o).length * scale;
        if (type == int.class) return base + ((int[]) o).length * scale;
        if (type == float.class) return base + ((float[]) o).length * scale;
        if (type == long.class) return base + ((long[]) o).length * scale;
        if (type == double.class) return base + ((double[]) o).length * scale;
        return base + ((Object[]) o).length * scale;
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        VMSupport.storeInstrumentation(inst);
    }

}
