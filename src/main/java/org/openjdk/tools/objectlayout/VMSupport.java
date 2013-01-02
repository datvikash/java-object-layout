package org.openjdk.tools.objectlayout;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import sun.misc.Unsafe;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class VMSupport {

    public static Instrumentation INSTRUMENTATION;

    public final static Unsafe U;
    public final static int ADDRESS_SIZE;
    public final static int OOP_SIZE;
    public final static int HEADER_SIZE;
    public final static int OBJECT_ALIGNMENT;

    static {
        // steal Unsafe
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            U = (Unsafe) unsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
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
            headerSize = (int) Math.min(off1, off2);
        } catch (NoSuchFieldException e) {
            oopSize = -1;
            headerSize = -1;
        }

        ADDRESS_SIZE = U.addressSize();
        OOP_SIZE = oopSize;
        HEADER_SIZE = headerSize;

        if (VMSupport.ADDRESS_SIZE != VMSupport.OOP_SIZE) {
            OBJECT_ALIGNMENT = guessAlignment() * 8; // assume compressed oops are << 3, FIXME: figure out!
        } else {
            OBJECT_ALIGNMENT = guessAlignment();
        }
    }

    public static void storeInstrumentation(Instrumentation inst) {
        VMSupport.INSTRUMENTATION = inst;
    }

    static class CompressedOopsClass {
        public Object obj1;
        public Object obj2;
    }

    public static int guessAlignment() {
        final int COUNT = 1_000_000;
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
}
