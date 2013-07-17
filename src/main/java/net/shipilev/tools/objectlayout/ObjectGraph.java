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

import com.google.common.collect.Multiset;

import java.io.PrintStream;

public class ObjectGraph {

    public static void analyze(PrintStream pw, Class<?> klass) {
        try {
            Object o = klass.newInstance();
            ObjectGraphWalker walker = new ObjectGraphWalker(o);

            int totalCount = 0;
            int totalSize = 0;

            pw.println(o.getClass().getName() + " instance:");
            pw.printf(" %5s %5s %5s %s\n", "count", "size", "avg", "description");
            for (Multiset.Entry<Class<?>> entry : walker.getClassCounts().entrySet()) {
                int size = walker.getClassSizes().count(entry.getElement());
                pw.printf(" %5d %5d %5d %s\n", entry.getCount(), size, size/entry.getCount(), entry.getElement().getName());
                totalCount += entry.getCount();
                totalSize += size;
            }
            pw.printf(" %5d %5d %5s %s\n", totalCount, totalSize, "", "(total)");


        } catch (InstantiationException e) {
            pw.println("Instantiation exception, does the class have the default constructor?");
        } catch (IllegalAccessException e) {
            pw.println("Illegal access exception, does the class have the public default constructor?");
        }
    }

}
