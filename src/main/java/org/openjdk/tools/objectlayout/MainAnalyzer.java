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

        out.print("Running " + (VMSupport.ADDRESS_SIZE*8) + "-bit VM, ");
        if (VMSupport.ADDRESS_SIZE != VMSupport.OOP_SIZE) {
            out.println("compressed pointers.");
            out.println("Objects seem to be " + VMSupport.OBJECT_ALIGNMENT + " bytes aligned (speculated).");
        } else {
            out.println("full-width pointers.");
            out.println("Objects seem to be " + VMSupport.OBJECT_ALIGNMENT + " bytes aligned.");
        }
        out.println();

        ObjectLayout.analyze(System.out, Class.forName(args[0]));
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        VMSupport.storeInstrumentation(inst);
    }

}
