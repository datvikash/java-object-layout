/*
 * #%L
 * Java Object Layout Dumper
 * %%
 * Copyright (C) 2012 - 2013 Aleksey Shipilev
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package net.shipilev.tools.objectlayout;

import net.shipilev.tools.objectlayout.util.Multiset;
import sun.misc.Unsafe;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.RuntimeMBeanException;
import javax.management.openmbean.CompositeDataSupport;
import java.io.PrintStream;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VMSupport {

    public static Instrumentation INSTRUMENTATION;

    public final static Unsafe U;
    public final static int ADDRESS_SIZE;
    public final static int HEADER_SIZE;
    private final static VMOptions OPTIONS;

    private static class VMOptions {
        private final String name;
        private final boolean compressedRef;
        private final int compressRefShift;
        private final int objectAlignment;
        private final int referenceSize;

        public VMOptions(String name) {
            this.name = name;
            this.referenceSize = U.addressSize();
            this.objectAlignment = guessAlignment(this.referenceSize);
            this.compressedRef = false;
            this.compressRefShift = 1;
        }

        public VMOptions(String name, int shift) {
            this.name = name;
            this.referenceSize = 4;
            this.objectAlignment = guessAlignment(this.referenceSize) << shift;
            this.compressedRef = true;
            this.compressRefShift = shift;
        }

        public long toNativeAddress(long address) {
            if (compressedRef) {
                return address << compressRefShift;
            } else {
                return address;
            }
        }
    }

    public static int align(int addr) {
        int align = OPTIONS.objectAlignment;
        if ((addr % align) == 0) {
            return addr;
        } else {
            return ((addr / align) + 1) * align;
        }
    }

    private static int log2p(int x) {
        int r = 0;
        while ((x >>= 1) != 0)
            r++;
        return r;
    }

    private static VMOptions getOptions() {
        // try Hotspot
        VMOptions hsOpts = getHotspotSpecifics();
        if (hsOpts != null) return hsOpts;

        // try JRockit
        VMOptions jrOpts = getJRockitSpecifics();
        if (jrOpts != null) return jrOpts;

        // When running with CompressedOops on 64-bit platform, the address size
        // reported by Unsafe is still 8, while the real reference fields are 4 bytes long.
        // Try to guess the reference field size with this naive trick.
        int oopSize;
        try {
            long off1 = U.objectFieldOffset(CompressedOopsClass.class.getField("obj1"));
            long off2 = U.objectFieldOffset(CompressedOopsClass.class.getField("obj2"));
            oopSize = (int) Math.abs(off2 - off1);
        } catch (NoSuchFieldException e) {
            oopSize = -1;
        }

        if (oopSize != U.addressSize()) {
            return new VMOptions("Auto-detected", 3); // assume compressed references have << 3 shift
        } else {
            return new VMOptions("Auto-detected");
        }
    }

    private static VMOptions getHotspotSpecifics() {
        String name = System.getProperty("java.vm.name");
        if (!name.contains("HotSpot") && !name.contains("OpenJDK")) {
            return null;
        }

        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();

            try {
                ObjectName mbean = new ObjectName("com.sun.management:type=HotSpotDiagnostic");
                CompositeDataSupport compressedOopsValue = (CompositeDataSupport) server.invoke(mbean, "getVMOption", new Object[]{"UseCompressedOops"}, new String[]{"java.lang.String"});
                boolean compressedOops = Boolean.valueOf(compressedOopsValue.get("value").toString());
                if (compressedOops) {
                    // if compressed oops are enabled, then this option is also accessible
                    CompositeDataSupport alignmentValue = (CompositeDataSupport) server.invoke(mbean, "getVMOption", new Object[]{"ObjectAlignmentInBytes"}, new String[]{"java.lang.String"});
                    int align = Integer.valueOf(alignmentValue.get("value").toString());
                    return new VMOptions("HotSpot", log2p(align));
                } else {
                    return new VMOptions("HotSpot");
                }

            } catch (RuntimeMBeanException iae) {
                return new VMOptions("HotSpot");
            }
        } catch (RuntimeException re) {
            System.err.println("Failed to read HotSpot-specific configuration properly, please report this as the bug");
            re.printStackTrace();
            return null;
        } catch (Exception exp) {
            System.err.println("Failed to read HotSpot-specific configuration properly, please report this as the bug");
            exp.printStackTrace();
            return null;
        }
    }

    private static VMOptions getJRockitSpecifics() {
        String name = System.getProperty("java.vm.name");
        if (!name.contains("JRockit")) {
            return null;
        }

        try {
            MBeanServer server = ManagementFactory.getPlatformMBeanServer();
            String str = (String) server.invoke(new ObjectName("oracle.jrockit.management:type=DiagnosticCommand"), "execute", new Object[]{"print_vm_state"}, new String[]{"java.lang.String"});
            String[] split = str.split("\n");
            for (String s : split) {
                if (s.contains("CompRefs")) {
                    Pattern pattern = Pattern.compile("(.*?)References are compressed, with heap base (.*?) and shift (.*?)\\.");
                    Matcher matcher = pattern.matcher(s);
                    if (matcher.matches()) {
                        return new VMOptions("JRockit", Integer.valueOf(matcher.group(3)));
                    } else {
                        return new VMOptions("JRockit");
                    }
                }
            }
            return null;
        } catch (RuntimeException re) {
            System.err.println("Failed to read JRockit-specific configuration properly, please report this as the bug");
            re.printStackTrace();
            return null;
        } catch (Exception exp) {
            System.err.println("Failed to read JRockit-specific configuration properly, please report this as the bug");
            exp.printStackTrace();
            return null;
        }
    }


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
        try {
            long off1 = U.objectFieldOffset(HeaderClass.class.getField("b1"));
            headerSize = (int) off1;
        } catch (NoSuchFieldException e) {
            headerSize = -1;
        }

        ADDRESS_SIZE = U.addressSize();
        HEADER_SIZE = headerSize;
        OPTIONS = getOptions();
    }

    public static void storeInstrumentation(Instrumentation inst) {
        VMSupport.INSTRUMENTATION = inst;
    }

    public static void detect(PrintStream out) {
        out.println("Running " + (VMSupport.ADDRESS_SIZE * 8) + "-bit " + OPTIONS.name + " VM.");
        if (OPTIONS.compressedRef)
            out.println("Using compressed references with " + OPTIONS.compressRefShift + "-bit shift.");

        out.println("Objects are " + OPTIONS.objectAlignment + " bytes aligned.");
        out.println();
    }

    static class CompressedOopsClass {
        public Object obj1;
        public Object obj2;
    }

    static class HeaderClass {
        public boolean b1;
    }

    public static int guessAlignment(int oopSize) {
        final int COUNT = 1000 * 1000;
        Object[] array = new Object[COUNT];
        long[] offsets = new long[COUNT];

        for (int c = 0; c < COUNT - 3; c += 3) {
            array[c + 0] = new MyObject1();
            array[c + 1] = new MyObject2();
            array[c + 1] = new MyObject3();
        }

        for (int c = 0; c < COUNT; c++) {
            offsets[c] = addressOf(array[c], oopSize);
        }

        Arrays.sort(offsets);

        List<Integer> sizes = new ArrayList<Integer>();
        for (int c = 1; c < COUNT; c++) {
            sizes.add((int) (offsets[c] - offsets[c - 1]));
        }

        int min = -1;
        for (int s : sizes) {
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
        return addressOf(o, OPTIONS.referenceSize);
    }

    public static long addressOf(Object o, int oopSize) {
        Object[] array = new Object[]{o};

        long baseOffset = U.arrayBaseOffset(Object[].class);
        long objectAddress;
        switch (oopSize) {
            case 4:
                objectAddress = U.getInt(array, baseOffset);
                break;
            case 8:
                objectAddress = U.getLong(array, baseOffset);
                break;
            default:
                throw new Error("unsupported address size: " + oopSize);
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

    public static int sizeOfType(Class<?> type) {
        if (type == byte.class)    { return 1; }
        if (type == boolean.class) { return 1; }
        if (type == short.class)   { return 2; }
        if (type == char.class)    { return 2; }
        if (type == int.class)     { return 4; }
        if (type == float.class)   { return 4; }
        if (type == long.class)    { return 8; }
        if (type == double.class)  { return 8; }
        return OPTIONS.referenceSize;
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        VMSupport.storeInstrumentation(inst);
    }

}
