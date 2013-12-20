package org.glyptodon.guacamole.net.basic.rest;

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
 * A simple object to represent an error to be sent from the REST API.
 * @author James Muehlner
 */
public class APIError {
    
    /**
     * The error message.
     */
    private String message;

    /**
     * Get the error message.
     * @return The error message.
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Create a new APIError with the specified error message.
     * @param message The error message.
     */
    public APIError(String message) {
        this.message = message;
    }
}
