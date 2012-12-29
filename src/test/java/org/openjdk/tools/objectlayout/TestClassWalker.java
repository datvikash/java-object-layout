package org.openjdk.tools.objectlayout;

import junit.framework.Assert;
import org.junit.Test;

public class TestClassWalker {

    @Test
    public void testSelfRecursive() {
        ObjectGraphWalker cw = new ObjectGraphWalker(new SelfRecursiveSample());
        Assert.assertEquals(1, cw.getClassCounts().count(SelfRecursiveSample.class));
    }

    public static class SelfRecursiveSample {
        private SelfRecursiveSample sample = this;
    }

    @Test
    public void testAllTypesSample() {
        ObjectGraphWalker cw = new ObjectGraphWalker(new AllTypesSample());
        Assert.assertEquals(1, cw.getClassCounts().count(AllTypesSample.class));
    }

    public static class AllTypesSample {
        private boolean f1;
        private byte    f2;
        private short   f3;
        private char    f4;
        private int     f5;
        private long    f6;
        private double  f7;
        private float   f8;
        private boolean[] a1;
        private byte[]    a2;
        private short[]   a3;
        private char[]    a4;
        private int[]     a5;
        private long[]    a6;
        private double[]  a7;
        private float[]   a8;
    }

    @Test
    public void testArraySample() {
        ObjectGraphWalker cw = new ObjectGraphWalker(new ArraySample());
        Assert.assertEquals(1, cw.getClassCounts().count(ArraySample.class));
        Assert.assertEquals(1, cw.getClassCounts().count(boolean[].class));
        Assert.assertTrue(cw.getClassSizes().get(boolean[].class)   > 245* ObjectLayout.sizeOfType(boolean.class));
        Assert.assertTrue(cw.getClassSizes().get(byte[].class)      > 245* ObjectLayout.sizeOfType(byte.class));
        Assert.assertTrue(cw.getClassSizes().get(short[].class)     > 245* ObjectLayout.sizeOfType(short.class));
        Assert.assertTrue(cw.getClassSizes().get(char[].class)      > 245* ObjectLayout.sizeOfType(char.class));
        Assert.assertTrue(cw.getClassSizes().get(int[].class)       > 245* ObjectLayout.sizeOfType(int.class));
        Assert.assertTrue(cw.getClassSizes().get(long[].class)      > 245* ObjectLayout.sizeOfType(long.class));
        Assert.assertTrue(cw.getClassSizes().get(double[].class)    > 245* ObjectLayout.sizeOfType(double.class));
        Assert.assertTrue(cw.getClassSizes().get(float[].class)     > 245* ObjectLayout.sizeOfType(float.class));
    }

    public static class ArraySample {
        private boolean[] a1 = new boolean[245];
        private byte[]    a2 = new byte[245];
        private short[]   a3 = new short[245];
        private char[]    a4 = new char[245];
        private int[]     a5 = new int[245];
        private long[]    a6 = new long[245];
        private double[]  a7 = new double[245];
        private float[]   a8 = new float[245];
    }


}
