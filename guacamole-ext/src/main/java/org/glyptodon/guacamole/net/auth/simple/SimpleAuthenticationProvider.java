
package org.glyptodon.guacamole.net.auth.simple;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-auth.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

import java.util.Map;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.AuthenticationProvider;
import org.glyptodon.guacamole.net.auth.Credentials;
import org.glyptodon.guacamole.net.auth.UserContext;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;


/**
 * Provides means of retrieving a set of named GuacamoleConfigurations for a
 * given Credentials object. This is a simple AuthenticationProvider
 * implementation intended to be easily extended. It is useful for simple
 * authentication situations where access to web-based administration and
 * complex users and permissions are not required.
 *
 * The interface provided by SimpleAuthenticationProvider is similar to that of
 * the AuthenticationProvider interface of older Guacamole releases.
 *
 * @author Michael Jumper
 */
public abstract class SimpleAuthenticationProvider
    implements AuthenticationProvider {

    /**
     * Given an arbitrary credentials object, returns a Map containing all
     * configurations authorized by those credentials. The keys of this Map
     * are Strings which uniquely identify each configuration.
     *
     * @param credentials The credentials to use to retrieve authorized
     *                    configurations.
     * @return A Map of all configurations authorized by the given credentials,
     *         or null if the credentials given are not authorized.
     * @throws GuacamoleException If an error occurs while retrieving
     *                            configurations.
     */
    public abstract Map<String, GuacamoleConfiguration>
            getAuthorizedConfigurations(Credentials credentials)
            throws GuacamoleException;

    @Override
    public UserContext getUserContext(Credentials credentials)
            throws GuacamoleException {

        // Get configurations
        Map<String, GuacamoleConfiguration> configs =
                getAuthorizedConfigurations(credentials);

        // Return as unauthorized if not authorized to retrieve configs
        if (configs == null)
            return null;

        // Return user context restricted to authorized configs
        return new SimpleUserContext(configs);

    }

    @Override
    public UserContext updateUserContext(UserContext context,
        Credentials credentials) throws GuacamoleException {

        // Simply return the given context, updating nothing
        return context;
        
    }

}
