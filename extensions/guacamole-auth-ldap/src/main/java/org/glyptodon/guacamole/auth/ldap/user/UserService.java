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
     * Adds all Guacamole users accessible to the user currently bound under
     * the given LDAP connection to the provided map. Only users with the
     * specified attribute are added. If the same username is encountered
     * multiple times, warnings about possible ambiguity will be logged.
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
    private void putAllUsers(Map<String, User> users, LDAPConnection ldapConnection,
            String usernameAttribute) throws GuacamoleException {

        try {

            // Find all Guacamole users underneath base DN
            LDAPSearchResults results = ldapConnection.search(
                confService.getUserBaseDN(),
                LDAPConnection.SCOPE_SUB,
                "(&(objectClass=*)(" + escapingService.escapeLDAPSearchFilter(usernameAttribute) + "=*))",
                null,
                false
            );

            // Read all visible users
            while (results.hasMore()) {

                LDAPEntry entry = results.next();

                // Get username from record
                LDAPAttribute username = entry.getAttribute(usernameAttribute);
                if (username == null) {
                    logger.warn("Queried user is missing the username attribute \"{}\".", usernameAttribute);
                    continue;
                }

                // Store user using their username as the identifier
                String identifier = username.getStringValue();
                if (users.put(identifier, new SimpleUser(identifier)) != null)
                    logger.warn("Possibly ambiguous user account: \"{}\".", identifier);

            }

        }
        catch (LDAPException e) {
            throw new GuacamoleServerException("Error while querying users.", e);
        }

    }

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

        // Build map of users by querying each username attribute separately
        Map<String, User> users = new HashMap<String, User>();
        for (String usernameAttribute : confService.getUsernameAttributes()) {

            // Attempt to pull all users with given attribute
            try {
                putAllUsers(users, ldapConnection, usernameAttribute);
            }

            // Log any errors non-fatally
            catch (GuacamoleException e) {
                logger.warn("Could not query list of all users for attribute \"{}\": {}",
                        usernameAttribute, e.getMessage());
                logger.debug("Error querying list of all users.", e);
            }

        }

        // Return map of all users
        return users;

    }

}
