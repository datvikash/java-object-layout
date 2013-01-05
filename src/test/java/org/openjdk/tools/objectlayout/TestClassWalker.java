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
        Assert.assertTrue(cw.getClassSizes().count(boolean[].class)   > 245* VMSupport.sizeOfType(boolean.class));
        Assert.assertTrue(cw.getClassSizes().count(byte[].class)      > 245* VMSupport.sizeOfType(byte.class));
        Assert.assertTrue(cw.getClassSizes().count(short[].class)     > 245* VMSupport.sizeOfType(short.class));
        Assert.assertTrue(cw.getClassSizes().count(char[].class)      > 245* VMSupport.sizeOfType(char.class));
        Assert.assertTrue(cw.getClassSizes().count(int[].class)       > 245* VMSupport.sizeOfType(int.class));
        Assert.assertTrue(cw.getClassSizes().count(long[].class)      > 245* VMSupport.sizeOfType(long.class));
        Assert.assertTrue(cw.getClassSizes().count(double[].class)    > 245* VMSupport.sizeOfType(double.class));
        Assert.assertTrue(cw.getClassSizes().count(float[].class)     > 245* VMSupport.sizeOfType(float.class));
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
