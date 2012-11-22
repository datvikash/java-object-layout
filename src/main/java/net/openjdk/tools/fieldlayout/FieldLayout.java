package net.openjdk.tools.fieldlayout;

import sun.misc.Unsafe;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.SortedSet;
import java.util.TreeSet;

public class FieldLayout {

    private static final Unsafe U;
    private static int ADDRESS_SIZE;
    private static int HEADER_SIZE;

    static {
        // steal Unsafe
        try {
            Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
            unsafe.setAccessible(true);
            U = (Unsafe) unsafe.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }

        // When running with CompressedOops on 64-bit platform, the address size
        // reported by Unsafe is still 8, while the real reference fields are 4 bytes long.
        // Try to guess the reference field size with this naive trick.
        try {
            long off1 = U.objectFieldOffset(CompressedOopsClass.class.getField("obj1"));
            long off2 = U.objectFieldOffset(CompressedOopsClass.class.getField("obj2"));
            ADDRESS_SIZE = (int) Math.abs(off2 - off1);
            HEADER_SIZE = (int) Math.min(off1, off2);
        } catch (NoSuchFieldException e) {
            ADDRESS_SIZE = -1;
        }
    }

    static class CompressedOopsClass {
        public Object obj1;
        public Object obj2;
    }

    public static void analyze(Class klass) throws Exception {
        analyze(System.out, klass);
    }

    public static void analyze(PrintStream pw, Class klass) throws Exception {
        SortedSet<FieldInfo> set = new TreeSet<>();

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

        int nextFree = 0;
        pw.println(klass.getCanonicalName());
        pw.printf("   (header %d bytes)\n", HEADER_SIZE);
        nextFree += HEADER_SIZE;

        for (FieldInfo f : set) {
            if (f.offset > nextFree) {
                pw.printf("   (gap %d bytes)\n", (f.offset - nextFree));
            }
            pw.printf(" %3d %3d %15s %s\n", f.offset, f.getSize(), f.getType(), f.getHostClass() + "." + f.name);

            nextFree = f.offset + f.getSize();
        }
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
                offset = (int) U.staticFieldOffset(field);
            } else {
                offset = (int) U.objectFieldOffset(field);
            }
        }

        @Override
        public int compareTo(FieldInfo o) {
            return Integer.compare(offset, o.offset);
        }

        public int getSize() {
            if (type == byte.class)    { return 1; }
            if (type == boolean.class) { return 1; }
            if (type == short.class)   { return 2; }
            if (type == char.class)    { return 2; }
            if (type == int.class)     { return 4; }
            if (type == float.class)   { return 4; }
            if (type == long.class)    { return 8; }
            if (type == double.class)  { return 8; }
            return ADDRESS_SIZE;
        }

        public String getType() {
            return type.getSimpleName();
        }

        public String getHostClass() {
            return klass.getSimpleName();
        }
    }

}
