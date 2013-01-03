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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ObjectGraphWalker {

    private final Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());

    private final Multiset<Class<?>> classSizes = HashMultiset.create();
    private final Multiset<Class<?>> classCounts = HashMultiset.create();
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
