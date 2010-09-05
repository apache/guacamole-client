
package net.sourceforge.guacamole.instruction;

/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

public abstract class Instruction {


    // All Instructions must provide a toString() implementation
    // which returns the properly formatted instruction:
    // OPCODE:parm1,parm2,...,parmN;

    @Override
    public abstract String toString();

    public String escape(String str) {

        StringBuffer sb = new StringBuffer();

        for (int i=0; i<str.length(); i++) {

            char c = str.charAt(i);

            switch (c) {
                case ',':
                    sb.append("\\c");
                    break;

                case ';':
                    sb.append("\\s");
                    break;

                case '\\':
                    sb.append("\\\\");
                    break;

                default:
                    sb.append(c);
            }

        }

        return sb.toString();

    }

}
