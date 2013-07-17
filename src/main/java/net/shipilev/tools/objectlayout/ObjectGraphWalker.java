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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ObjectGraphWalker {

    private final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());

    private final Multiset<Class<?>> classSizes = new Multiset<Class<?>>();
    private final Multiset<Class<?>> classCounts = new Multiset<Class<?>>();
    private final Object root;
    private boolean walked;

    public ObjectGraphWalker(Object root) {
        this.root = root;
    }

    private void walk() {
        if (walked) return;
        walked = true;

        List<Object> curLayer = new ArrayList<Object>();
        List<Object> newLayer = new ArrayList<Object>();

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
        List<Object> result = new ArrayList<Object>();

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
        try {
            classSizes.add(klass, ObjectLayout.sizeOf(o));
        } catch (Exception e) {
            classSizes.add(klass, 0);
        }
    }

    public Multiset<Class<?>> getClassSizes() {
        walk();
        return classSizes;
    }

    public Multiset<Class<?>> getClassCounts() {
        walk();
        return classCounts;
    }
}
