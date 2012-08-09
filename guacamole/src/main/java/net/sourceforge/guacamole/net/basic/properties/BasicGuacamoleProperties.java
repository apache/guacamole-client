
package net.sourceforge.guacamole.net.basic.properties;

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

import net.sourceforge.guacamole.properties.FileGuacamoleProperty;

/**
 * Properties used by the default Guacamole web application.
 *
 * @author Michael Jumper
 */
public class BasicGuacamoleProperties {

    /**
     * This class should not be instantiated.
     */
    private BasicGuacamoleProperties() {}

    /**
     * The authentication provider to user when retrieving the authorized
     * configurations of a user.
     */
    public static final AuthenticationProviderProperty AUTH_PROVIDER = new AuthenticationProviderProperty() {

        @Override
        public String getName() { return "auth-provider"; }

    };

    /**
     * The directory to search for authentication provider classes.
     */
    public static final FileGuacamoleProperty LIB_DIRECTORY = new FileGuacamoleProperty() {

        @Override
        public String getName() { return "lib-directory"; }

    };

    /**
     * The comma-separated list of all classes to use as event listeners.
     */
    public static final EventListenersProperty EVENT_LISTENERS = new EventListenersProperty() {

        @Override
        public String getName() { return "event-listeners"; }

    };

}
