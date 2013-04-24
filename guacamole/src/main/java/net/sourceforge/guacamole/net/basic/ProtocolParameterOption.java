
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

/**
 * Describes an available legal value for an enumerated protocol parameter.
 *
 * @author Michael Jumper
 */
public class ProtocolParameterOption {

    /**
     * The value that will be sent to the client plugin if this option is
     * chosen.
     */
    private String value;

    /**
     * A human-readable title describing the effect of the value.
     */
    private String title;

    /**
     * Returns the value that will be sent to the client plugin if this option
     * is chosen.
     *
     * @return The value that will be sent if this option is chosen.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value that will be sent to the client plugin if this option is
     * chosen.
     *
     * @param value The value to send if this option is chosen.
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Returns the human-readable title describing the effect of this option.
     * @return The human-readable title describing the effect of this option.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the human-readable title describing the effect of this option.
     * @param title A human-readable title describing the effect of this option.
     */
    public void setTitle(String title) {
        this.title = title;
    }

}
