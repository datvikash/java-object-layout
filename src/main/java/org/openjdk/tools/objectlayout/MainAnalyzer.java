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

import java.io.PrintStream;

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

}
