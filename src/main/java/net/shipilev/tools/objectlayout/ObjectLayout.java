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

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.SortedSet;
import java.util.TreeSet;

public class ObjectLayout {

    public static void analyze(Class klass) throws Exception {
        analyze(System.out, klass);
    }

    public static int sizeOf(Object o) throws Exception {
        if (VMSupport.INSTRUMENTATION != null) {
            return VMSupport.align((int) VMSupport.INSTRUMENTATION.getObjectSize(o));
        }

        if (o.getClass().isArray()) {
            return VMSupport.align(sizeOfArray(o));
        }

        SortedSet<FieldInfo> set = new TreeSet<FieldInfo>();

        Class<?> klass = o.getClass();
        for (Field f : klass.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                set.add(new FieldInfo(klass, f));
            }
        }

        Class<?> superKlass = klass;
        while ((superKlass = superKlass.getSuperclass()) != null) {
            for (Field f : superKlass.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    set.add(new FieldInfo(superKlass, f));
                }
            }
        }

        if (!set.isEmpty()) {
            return VMSupport.align(set.last().offset + set.last().getSize());
        } else {
            return VMSupport.align(VMSupport.HEADER_SIZE);
        }
    }

    private static int sizeOfArray(Object o) {
        int base = VMSupport.U.arrayBaseOffset(o.getClass());
        int scale = VMSupport.U.arrayIndexScale(o.getClass());
        Class<?> type = o.getClass().getComponentType();
        if (type == boolean.class)  return base + ((boolean[])o).length * scale;
        if (type == byte.class)     return base + ((byte[])o).length    * scale;
        if (type == short.class)    return base + ((short[])o).length   * scale;
        if (type == char.class)     return base + ((char[])o).length    * scale;
        if (type == int.class)      return base + ((int[])o).length     * scale;
        if (type == float.class)    return base + ((float[])o).length   * scale;
        if (type == long.class)     return base + ((long[])o).length    * scale;
        if (type == double.class)   return base + ((double[])o).length  * scale;
        return VMSupport.align(base + ((Object[])o).length  * scale);
    }

    public static int analyze(PrintStream pw, Class klass) throws Exception {
        SortedSet<FieldInfo> set = new TreeSet<FieldInfo>();

        for (Field f : klass.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
                set.add(new FieldInfo(klass, f));
            }
        }

        Class<?> superKlass = klass;
        while ((superKlass = superKlass.getSuperclass()) != null) {
            for (Field f : superKlass.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    set.add(new FieldInfo(superKlass, f));
                }
            }
        }

        int maxLength = 1;
        for (FieldInfo f : set) {
            maxLength = Math.max(f.getType().length(), maxLength);
        }

        int nextFree = 0;
        pw.println(klass.getCanonicalName());
        pw.printf(" %6s %5s %" + maxLength + "s %s\n", "offset", "size", "type", "description");
        pw.printf(" %6d %5d %" + maxLength + "s %s\n", 0, VMSupport.HEADER_SIZE, "", "(assumed to be the object header + first field alignment)");
        nextFree += VMSupport.HEADER_SIZE;

        for (FieldInfo f : set) {
            if (f.offset > nextFree) {
                pw.printf(" %6d %5d %" + maxLength + "s %s\n", nextFree, (f.offset - nextFree), "", "(alignment/padding gap)");
            }
            pw.printf(" %6d %5d %" + maxLength + "s %s\n", f.offset, f.getSize(), f.getType(), f.getHostClass() + "." + f.name);

            nextFree = f.offset + f.getSize();
        }
        int aligned = VMSupport.align(nextFree);
        if (aligned != nextFree) {
            pw.printf(" %6d %5s %" + maxLength + "s %s\n", nextFree, aligned - nextFree, "", "(loss due to the next object alignment)");
        }
        pw.printf(" %6d %5s %" + maxLength + "s %s\n", aligned, "", "", "(object boundary, size estimate)");

        if (VMSupport.INSTRUMENTATION != null) {
            try {
                Object i = klass.newInstance();
                pw.println("VM reports " + VMSupport.INSTRUMENTATION.getObjectSize(i) + " bytes per instance");
            } catch (InstantiationException e) {
                pw.println("VM fails to invoke default constructor (does object have one?)");
            }
        } else {
            pw.println("VM agent is not enabled, use -javaagent: to add this JAR as Java agent");
        }

        return aligned;
    }

    public static class FieldInfo implements Comparable<FieldInfo> {

        private final String name;
        private final boolean aStatic;
        private final int offset;
        private final Class<?> type;
        private final Class klass;

        public FieldInfo(Class hostKlass, Field field) {
            klass = hostKlass;
            name = field.getName();
            type = field.getType();
            aStatic = Modifier.isStatic(field.getModifiers());
            if (aStatic) {
                offset = (int) VMSupport.U.staticFieldOffset(field);
            } else {
                offset = (int) VMSupport.U.objectFieldOffset(field);
            }
        }

        @Override
        public int compareTo(FieldInfo o) {
            return (offset < o.offset) ? -1 : ((offset == o.offset) ? 0 : 1);
        }

        public int getSize() {
            return VMSupport.sizeOfType(type);
        }

        public String getType() {
            return type.getSimpleName();
        }

        public String getHostClass() {
            return klass.getSimpleName();
        }
    }

}
