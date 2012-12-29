package org.openjdk.tools.objectlayout;

import java.lang.instrument.Instrumentation;

public class MainAnalyzer {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java-object-layout.jar [class-name]");
            System.exit(1);
        }
        ObjectLayout.analyze(System.out, Class.forName(args[0]));
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        ObjectLayout.storeInstrumentation(inst);
    }

}
