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
