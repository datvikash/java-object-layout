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
package org.openjdk.tools.objectlayout;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ObjectGenerator implements Opcodes {

    public static class ByteClassLoader extends ClassLoader {
        private byte[] data;

        public ByteClassLoader(ClassLoader parent, byte[] data) {
            super(parent);
            this.data = data;
        }

        @Override
        protected Class<?> findClass(final String name) throws ClassNotFoundException {
            return defineClass(name, data, 0, data.length);
        }

    }

    public static void generateClasses() throws Exception {
        final int reportStep = 8192; // should be power of two
        long time = System.nanoTime();

        PrintStream ps = new PrintStream(new File("classes.layout"));
        for (int i = 0; i < 256*256*4*16; i++) {
            Class<?> klass = buildClass("Sample" + i, i);

            ObjectLayout.analyze(ps, klass);
            ps.println();

            if ((i & (reportStep - 1)) == 0) {
                long t = System.nanoTime();
                System.err.printf("%d classes processed in %d msec\n", reportStep, TimeUnit.NANOSECONDS.toMillis(t - time));
                time = t;
            }
        }
        ps.close();
    }

    public static Class<?> buildClass(String name, int signature) throws Exception {
        ClassWriter cw = new ClassWriter(0);

        cw.visit(49,
                ACC_PUBLIC + ACC_SUPER,
                name,
                null,
                "java/lang/Object",
                null);

        genFields(cw, "boolean", Type.BOOLEAN_TYPE.getDescriptor(), signature & 3);
        signature >>= 2;

        genFields(cw, "byte", Type.BYTE_TYPE.getDescriptor(), signature & 3);
        signature >>= 2;

        genFields(cw, "short", Type.SHORT_TYPE.getDescriptor(), signature & 3);
        signature >>= 2;

        genFields(cw, "char", Type.CHAR_TYPE.getDescriptor(), signature & 3);
        signature >>= 2;

        genFields(cw, "int", Type.INT_TYPE.getDescriptor(), signature & 3);
        signature >>= 2;

        genFields(cw, "long", Type.LONG_TYPE.getDescriptor(), signature & 3);
        signature >>= 2;

        genFields(cw, "float", Type.FLOAT_TYPE.getDescriptor(), signature & 3);
        signature >>= 2;

        genFields(cw, "double", Type.DOUBLE_TYPE.getDescriptor(), signature & 3);
        signature >>= 2;

        genFields(cw, "object", Type.getType(Object.class).getDescriptor(), signature & 3);
        signature >>= 2;


        cw.visitSource(name + ".java", null);
        cw.visitEnd();

        return new ByteClassLoader(Thread.currentThread().getContextClassLoader(), cw.toByteArray()).findClass(name);
    }

    private static void genFields(ClassWriter cw, String prefix, String desc, int count) {
        for (int c = 0; c < count; c++) {
            cw.visitField(ACC_PUBLIC, prefix + "Field" + c, desc, "", 0);
        }
    }

}
