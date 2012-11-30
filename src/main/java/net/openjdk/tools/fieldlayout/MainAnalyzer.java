package net.openjdk.tools.fieldlayout;

import java.lang.instrument.Instrumentation;

public class MainAnalyzer {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: field-layout.jar [class-name]");
            System.exit(1);
        }
        FieldLayout.analyze(System.out, Class.forName(args[0]));
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        FieldLayout.storeInstrumentation(inst);
    }

}
