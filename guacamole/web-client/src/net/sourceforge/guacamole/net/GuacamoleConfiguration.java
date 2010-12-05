
package net.sourceforge.guacamole.net;

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

import net.sourceforge.guacamole.net.authentication.GuacamoleSessionProvider;
import java.lang.reflect.InvocationTargetException;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleException;

public class GuacamoleConfiguration extends Configuration {

    private String guacd_hostname;
    private int guacd_port;
    private GuacamoleSessionProvider sessionProvider;

    public GuacamoleConfiguration() throws GuacamoleException {

        guacd_hostname       = readParameter("guacd-hostname");
        guacd_port           = readIntParameter("guacd-port", null);

        // Get session provider instance
        try {
            String sessionProviderClassName = readParameter("session-provider");
            Object obj = Class.forName(sessionProviderClassName).getConstructor().newInstance();
            if (!(obj instanceof GuacamoleSessionProvider))
                throw new GuacamoleException("Specified session provider class is not a GuacamoleSessionProvider");

            sessionProvider = (GuacamoleSessionProvider) obj;
        }
        catch (ClassNotFoundException e) {
            throw new GuacamoleException("Session provider class not found", e);
        }
        catch (NoSuchMethodException e) {
            throw new GuacamoleException("Default constructor for session provider not present", e);
        }
        catch (SecurityException e) {
            throw new GuacamoleException("Creation of session provider disallowed; check your security settings", e);
        }
        catch (InstantiationException e) {
            throw new GuacamoleException("Unable to instantiate session provider", e);
        }
        catch (IllegalAccessException e) {
            throw new GuacamoleException("Unable to access default constructor of session provider", e);
        }
        catch (InvocationTargetException e) {
            throw new GuacamoleException("Internal error in constructor of session provider", e.getTargetException());
        }

    }

    public int getProxyPort() {
        return guacd_port;
    }

    public String getProxyHostname() {
        return guacd_hostname;
    }

    public GuacamoleSession createSession(HttpSession session) throws GuacamoleException {
        return sessionProvider.createSession(session);
    }

}
