
package net.sourceforge.guacamole;

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
 * A generic exception thrown when parts of the Guacamole API encounter
 * errors.
 *
 * @author Michael Jumper
 */
public class GuacamoleException extends Exception {

    /**
     * Creates a new GuacamoleException with the given message and cause.
     *
     * @param message A human readable description of the exception that
     *                occurred.
     * @param cause The cause of this exception.
     */
    public GuacamoleException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new GuacamoleException with the given message.
     *
     * @param message A human readable description of the exception that
     *                occurred.
     */
    public GuacamoleException(String message) {
        super(message);
    }

    /**
     * Creates a new GuacamoleException with the given cause.
     *
     * @param cause The cause of this exception.
     */
    public GuacamoleException(Throwable cause) {
        super(cause);
    }

}
