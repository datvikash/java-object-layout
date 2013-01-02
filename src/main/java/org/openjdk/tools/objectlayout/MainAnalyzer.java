package org.openjdk.tools.objectlayout;

import java.io.PrintStream;
import java.lang.instrument.Instrumentation;

public class MainAnalyzer {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java-object-layout.jar [class-name]");
            System.exit(1);
        }

        PrintStream out = System.out;

        VMSupport.detect(out);
        ObjectLayout.analyze(out, Class.forName(args[0]));
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        VMSupport.storeInstrumentation(inst);
    }

}
