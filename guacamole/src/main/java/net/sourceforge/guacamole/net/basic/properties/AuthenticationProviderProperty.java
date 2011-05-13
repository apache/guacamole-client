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

import java.lang.reflect.InvocationTargetException;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.basic.AuthenticationProvider;
import net.sourceforge.guacamole.properties.GuacamoleProperty;

public abstract class AuthenticationProviderProperty implements GuacamoleProperty<AuthenticationProvider> {

    @Override
    public AuthenticationProvider parseValue(String authProviderClassName) throws GuacamoleException {

        // Get auth provider instance
        try {

            Object obj = Class.forName(authProviderClassName).getConstructor().newInstance();
            if (!(obj instanceof AuthenticationProvider))
                throw new GuacamoleException("Specified authentication provider class is not a AuthenticationProvider.");

            return (AuthenticationProvider) obj;

        }
        catch (ClassNotFoundException e) {
            throw new GuacamoleException("Authentication provider class not found", e);
        }
        catch (NoSuchMethodException e) {
            throw new GuacamoleException("Default constructor for authentication provider not present", e);
        }
        catch (SecurityException e) {
            throw new GuacamoleException("Creation of authentication provider disallowed; check your security settings", e);
        }
        catch (InstantiationException e) {
            throw new GuacamoleException("Unable to instantiate authentication provider", e);
        }
        catch (IllegalAccessException e) {
            throw new GuacamoleException("Unable to access default constructor of authentication provider", e);
        }
        catch (InvocationTargetException e) {
            throw new GuacamoleException("Internal error in constructor of authentication provider", e.getTargetException());
        }

    }

}

