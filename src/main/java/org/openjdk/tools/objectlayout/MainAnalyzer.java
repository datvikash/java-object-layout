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
