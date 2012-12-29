package org.openjdk.tools.objectlayout;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ObjectGraphWalker {

    private final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());

    private final Map<Class<?>, Integer> classSizes = new HashMap<>();
    private final Multiset<Class<?>> classCounts = HashMultiset.create();
    private final Object root;
    private boolean walked;

    public ObjectGraphWalker(Object root) {
        this.root = root;
    }

    private void walk() {
        if (walked) return;
        walked = true;

        List<Object> curLayer = new ArrayList<>();
        List<Object> newLayer = new ArrayList<>();

        visitObject(root);
        visited.add(root);
        curLayer.add(root);

        while (!curLayer.isEmpty()) {
            newLayer.clear();
            for (Object next : curLayer) {
                for (Object ref : peelReferences(next)) {
                    if (ref != null) {
                        if (visited.add(ref)) {
                            visitObject(ref);
                            newLayer.add(ref);
                        }
                    }
                }
            }
            curLayer.clear();
            curLayer.addAll(newLayer);
        }
    }

    private List<Object> peelReferences(Object o) {
        List<Object> result = new ArrayList<>();

        if (o.getClass().isArray() && !o.getClass().getComponentType().isPrimitive()) {
            result.addAll(Arrays.asList((Object[])o));
        }

        for (Field f : o.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.getType().isPrimitive()) continue;
            if (Modifier.isStatic(f.getModifiers())) continue;

            try {
                result.add(f.get(o));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        return result;
    }

    private void visitObject(Object o) {
        Class<?> klass = o.getClass();
        classCounts.add(klass);
        if (!classSizes.containsKey(klass)) {
            try {
                classSizes.put(klass, ObjectLayout.sizeOf(o));
            } catch (Exception e) {
                classSizes.put(klass, 0);
            }
        }
    }

    public Map<Class<?>, Integer> getClassSizes() {
        walk();
        return classSizes;
    }

    public Multiset<Class<?>> getClassCounts() {
        walk();
        return classCounts;
    }

    public int getTotalSize() {
        walk();

        int totalSize = 0;
        for (Multiset.Entry<Class<?>> entry : classCounts.entrySet()) {
            Integer size = classSizes.get(entry.getElement());
            totalSize += size * entry.getCount();
        }
        return totalSize;
    }
}
