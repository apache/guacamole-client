package org.glyptodon.guacamole.net.basic.auth;

import java.util.HashMap;
import java.util.Map;

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
 * Mapping of all usernames to corresponding authorizations.
 *
 * @author Mike Jumper
 */
public class UserMapping {

    /**
     * All authorizations, indexed by username.
     */
    private Map<String, Authorization> authorizations =
            new HashMap<String, Authorization>();

    /**
     * Adds the given authorization to the user mapping.
     *
     * @param authorization The authorization to add to the user mapping.
     */
    public void addAuthorization(Authorization authorization) {
        authorizations.put(authorization.getUsername(), authorization);
    }

    /**
     * Returns the authorization corresponding to the user having the given
     * username, if any.
     *
     * @param username The username to find the authorization for.
     * @return The authorization corresponding to the user having the given
     *         username, or null if no such authorization exists.
     */
    public Authorization getAuthorization(String username) {
        return authorizations.get(username);
    }

}
