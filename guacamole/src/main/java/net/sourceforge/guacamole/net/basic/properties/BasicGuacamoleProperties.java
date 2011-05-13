
package net.sourceforge.guacamole.net.basic.properties;

import net.sourceforge.guacamole.properties.FileGuacamoleProperty;

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

public class BasicGuacamoleProperties {

    private BasicGuacamoleProperties() {}

    public static final FileGuacamoleProperty BASIC_USER_MAPPING = new FileGuacamoleProperty() {

        @Override
        public String getName() { return "basic-user-mapping"; }

    };

    public static final AuthenticationProviderProperty AUTH_PROVIDER = new AuthenticationProviderProperty() {

        @Override
        public String getName() { return "auth-provider"; }

    };

}
