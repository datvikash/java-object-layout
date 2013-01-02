package org.openjdk.tools.objectlayout;

import java.lang.instrument.Instrumentation;

public class MainObjectGraph {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java-object-graph.jar [class-name]");
            System.exit(1);
        }
        VMSupport.detect(System.out);
        ObjectGraph.analyze(System.out, Class.forName(args[0]));
    }

}
