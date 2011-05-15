
package net.sourceforge.guacamole.properties;

import net.sourceforge.guacamole.GuacamoleException;

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

/**
 * An abstract representation of a property in the guacamole.properties file,
 * which parses into a specific type.
 *
 * @author Michael Jumper
 * @param <Type> The type this GuacamoleProperty will parse into.
 */
public interface GuacamoleProperty<Type> {

    /**
     * Returns the name of the property in guacamole.properties that this
     * GuacamoleProperty will parse.
     *
     * @return The name of the property in guacamole.properties that this
     *         GuacamoleProperty will parse.
     */
    public String getName();

    /**
     * Parses the given string value into the type associated with this
     * GuacamoleProperty.
     *
     * @param value The string value to parse.
     * @return The parsed value.
     * @throws GuacamoleException If an error occurs while parsing the
     *                            provided value.
     */
    public Type parseValue(String value) throws GuacamoleException;

}
