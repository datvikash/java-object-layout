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
            int totalTotal = 0;

            pw.println(o.getClass().getName() + " instance:");
            pw.printf(" %5s %5s %5s %s\n", "count", "size", "total", "description");
            for (Multiset.Entry<Class<?>> entry : walker.getClassCounts().entrySet()) {
                Integer size = walker.getClassSizes().get(entry.getElement());
                pw.printf(" %5d %5d %5d %s\n", entry.getCount(), size, entry.getCount()*size, entry.getElement().getName());
                totalCount += entry.getCount();
                totalSize += size;
                totalTotal += entry.getCount()*size;
            }
            pw.printf(" %5d %5d %5d %s\n", totalCount, totalSize, totalTotal, "(total)");


        } catch (InstantiationException e) {
            pw.println("Instantiation exception, does the class have the default constructor?");
        } catch (IllegalAccessException e) {
            pw.println("Illegal access exception, does the class have the public default constructor?");
        }
    }

}
