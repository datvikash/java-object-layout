package net.openjdk.tools.fieldlayout;

public class MainAnalyzer {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: field-layout.jar [class-name]");
            System.exit(1);
        }
        FieldLayout.analyze(System.out, Class.forName(args[0]));
    }

}
