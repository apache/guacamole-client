
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
 * Represents a parameter of a protocol.
 * 
 * @author Michael Jumper
 */
public class ProtocolParameter {

    /**
     * All possible types of protocol parameter.
     */
    public enum Type {

        /**
         * A text parameter, accepting arbitrary values.
         */
        TEXT,

        /**
         * A password parameter, whose value is sensitive and must be hidden.
         */
        PASSWORD,

        /**
         * A numeric parameter, whose value must contain only digits.
         */
        NUMERIC,

        /**
         * A boolean parameter, whose value is either blank or "true".
         */
        BOOLEAN,

        /**
         * An enumerated parameter, whose legal values are fully enumerated
         * by a provided, finite list.
         */
        ENUM
    }
    
    /**
     * The unique name that identifies this parameter to the protocol plugin.
     */
    private String name;

    /**
     * A human-readable name to be presented to the user.
     */
    private String title;

    /**
     * The type of this field.
     */
    private Type type;

    /**
     * A collection of all associated parameter options.
     */
    private Collection<ProtocolParameterOption> options =
            new ArrayList<ProtocolParameterOption>();
    
    /**
     * Returns the name associated with this protocol parameter.
     * @return The name associated with this protocol parameter.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name associated with this protocol parameter. This name must
     * uniquely identify this parameter among the others accepted by the
     * corresponding protocol.
     * 
     * @param name The name to assign to this protocol parameter.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the title associated with this protocol parameter.
     * @return The title associated with this protocol parameter.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the title associated with this protocol parameter. The title must
     * be a human-readable string which describes accurately this parameter.
     * 
     * @param title A human-readable string describing this parameter.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Returns the type of this parameter.
     * @return The type of this parameter.
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the type of this parameter.
     * @param type The type of this parameter.
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Returns a mutable collection of protocol parameter options. Changes to
     * this collection directly affect the available options.
     * 
     * @return A mutable collection of parameter options.
     */
    public Collection<ProtocolParameterOption> getOptions() {
        return options;
    }

}
