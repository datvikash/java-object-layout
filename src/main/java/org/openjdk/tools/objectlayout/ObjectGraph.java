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
