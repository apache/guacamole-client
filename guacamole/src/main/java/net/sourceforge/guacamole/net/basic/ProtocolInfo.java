
package net.sourceforge.guacamole.net.basic;

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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Describes a protocol and all parameters associated with it, as required by
 * a protocol plugin for guacd. This class allows known parameters for a
 * protocol to be exposed to the user as friendly fields.
 *
 * @author Michael Jumper
 */
public class ProtocolInfo {

    /**
     * The human-readable title associated with this protocol.
     */
    private String title;

    /**
     * The unique name associated with this protocol.
     */
    private String name;

    /**
     * A collection of all associated protocol parameters.
     */
    private Collection<ProtocolParameter> parameters =
            new ArrayList<ProtocolParameter>();

    /**
     * Returns the human-readable title associated with this protocol.
     *
     * @return The human-readable title associated with this protocol.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the human-readable title associated with this protocol.
     *
     * @param title The human-readable title to associate with this protocol.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the unique name of this protocol. The protocol name is the
     * value required by the corresponding protocol plugin for guacd.
     *
     * @return The unique name of this protocol.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the unique name of this protocol. The protocol name is the value
     * required by the corresponding protocol plugin for guacd.
     *
     * @param name The unique name of this protocol.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns a mutable collection of the protocol parameters associated with
     * this protocol. Changes to this collection affect the parameters exposed
     * to the user.
     *
     * @return A mutable collection of protocol parameters.
     */
    public Collection<ProtocolParameter> getParameters() {
        return parameters;
    }

}
