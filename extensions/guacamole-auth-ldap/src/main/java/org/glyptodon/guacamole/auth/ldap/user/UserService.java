/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.auth.ldap.user;

import com.google.inject.Inject;
import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;
import java.util.HashMap;
import java.util.Map;
import org.glyptodon.guacamole.auth.ldap.ConfigurationService;
import org.glyptodon.guacamole.auth.ldap.EscapingService;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleServerException;
import org.glyptodon.guacamole.net.auth.User;
import org.glyptodon.guacamole.net.auth.simple.SimpleUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for queries the users visible to a particular Guacamole user
 * according to an LDAP directory.
 *
 * @author Michael Jumper
 */
public class UserService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
     * Service for escaping parts of LDAP queries.
     */
    @Inject
    private EscapingService escapingService;

    /**
     * Service for retrieving LDAP server configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Returns all Guacamole users accessible to the user currently bound under
     * the given LDAP connection.
     *
     * @param ldapConnection
     *     The current connection to the LDAP server, associated with the
     *     current user.
     *
     * @return
     *     All users accessible to the user currently bound under the given
     *     LDAP connection, as a map of connection identifier to corresponding
     *     user object.
     *
     * @throws GuacamoleException
     *     If an error occurs preventing retrieval of users.
     */
    public Map<String, User> getUsers(LDAPConnection ldapConnection)
            throws GuacamoleException {

        try {

            // Get username attribute
            String usernameAttribute = confService.getUsernameAttribute();

            // Find all Guacamole users underneath base DN
            LDAPSearchResults results = ldapConnection.search(
                confService.getUserBaseDN(),
                LDAPConnection.SCOPE_ONE,
                "(&(objectClass=*)(" + escapingService.escapeLDAPSearchFilter(usernameAttribute) + "=*))",
                null,
                false
            );

            // Read all visible users
            Map<String, User> users = new HashMap<String, User>();
            while (results.hasMore()) {

                LDAPEntry entry = results.next();

                // Get common name (CN)
                LDAPAttribute username = entry.getAttribute(usernameAttribute);
                if (username == null) {
                    logger.warn("Queried user is missing the username attribute \"{}\".", usernameAttribute);
                    continue;
                }

                // Store connection using cn for both identifier and name
                String identifier = username.getStringValue();
                users.put(identifier, new SimpleUser(identifier));

            }

            // Return map of all connections
            return users;

        }
        catch (LDAPException e) {
            throw new GuacamoleServerException("Error while querying users.", e);
        }

    }

}
