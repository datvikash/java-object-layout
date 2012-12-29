package net.openjdk.tools.fieldlayout;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.junit.Test;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.TreeSet;

public class TestObjectAlignment {

    private static final Unsafe U;

    static {
        // steal Unsafe
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            U = (Unsafe) unsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    public static long addressOf(Object o)
            throws Exception {
        Object[] array = new Object[]{o};

        long baseOffset = U.arrayBaseOffset(Object[].class);
        int addressSize = U.addressSize();
        long objectAddress;
        switch (addressSize) {
            case 4:
                objectAddress = U.getInt(array, baseOffset);
                break;
            case 8:
                objectAddress = U.getLong(array, baseOffset);
                break;
            default:
                throw new Error("unsupported address size: " + addressSize);
        }

        return (objectAddress);
    }

    @Test
    public void figureOutAlignment() throws Exception {
        final int COUNT = 100000;
        Object[] array = new Object[COUNT];
        long baseOffset = U.arrayBaseOffset(Object[].class);
        int scale = U.arrayIndexScale(Object[].class);


        for (int c = 0; c < COUNT; c++) {
            array[c] = new Object();
        }

        long[] oldOffsets = new long[COUNT];
        for (int c = 0; c < COUNT; c++) {
            oldOffsets[c] = U.getInt(array, baseOffset + scale*c);
        }

        Arrays.sort(oldOffsets);

        Multiset<Long> sizes = HashMultiset.create();
        for (int c = 1; c < COUNT; c++) {
            sizes.add(oldOffsets[c] - oldOffsets[c-1]);
        }

        System.err.println(sizes);
    }

    public static class MyObject {

    }

}
