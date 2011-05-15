
package net.sourceforge.guacamole.protocol;

import java.util.HashMap;

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
 * All information necessary to complete the initial protocol handshake of a
 * Guacamole session.
 *
 * @author Michael Jumper
 */
public class GuacamoleConfiguration {

    private String protocol;
    private HashMap<String, String> parameters = new HashMap<String, String>();

    /**
     * Returns the name of the protocol to be used.
     * @return The name of the protocol to be used.
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Sets the name of the protocol to be used.
     * @param protocol The name of the protocol to be used.
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * Returns the value set for the parameter with the given name, if any.
     * @param name The name of the parameter to return the value for.
     * @return The value of the parameter with the given name, or null if
     *         that parameter has not been set.
     */
    public String getParameter(String name) {
        return parameters.get(name);
    }

    /**
     * Sets the value for the parameter with the given name.
     *
     * @param name The name of the parameter to set the value for.
     * @param value The value to set for the parameter with the given name.
     */
    public void setParameter(String name, String value) {
        parameters.put(name, value);
    }

}
