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
package net.shipilev.tools.objectlayout.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Multiset<T> {

    private final Map<T, Integer> map = new HashMap<T, Integer>();

    public void add(T t) {
        add(t, 1);
    }

    public void add(T key, int count) {
        Integer v = map.get(key);
        if (v == null) {
            v = 0;
        }
        v += count;
        map.put(key, v);
    }

    public int count(T key) {
        Integer v = map.get(key);
        return (v == null) ? 0 : v;
    }

    public Collection<T> keys() {
        return map.keySet();
    }
}
