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

package org.glyptodon.guacamole.auth.ldap.connection;

import com.google.inject.Inject;
import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import net.sourceforge.guacamole.net.auth.ldap.LDAPAuthenticationProvider;
import org.glyptodon.guacamole.auth.ldap.ConfigurationService;
import org.glyptodon.guacamole.auth.ldap.EscapingService;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleServerException;
import org.glyptodon.guacamole.net.auth.AuthenticatedUser;
import org.glyptodon.guacamole.net.auth.Connection;
import org.glyptodon.guacamole.net.auth.simple.SimpleConnection;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;
import org.glyptodon.guacamole.token.StandardTokens;
import org.glyptodon.guacamole.token.TokenFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for querying the connections available to a particular Guacamole
 * user according to an LDAP directory.
 *
 * @author Michael Jumper
 */
public class ConnectionService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(ConnectionService.class);

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
     * Returns all Guacamole connections accessible to the user currently bound
     * under the given LDAP connection.
     *
     * @param user
     *     The AuthenticatedUser object associated with the user who is
     *     currently authenticated with Guacamole.
     *
     * @param ldapConnection
     *     The current connection to the LDAP server, associated with the
     *     current user.
     *
     * @return
     *     All connections accessible to the user currently bound under the
     *     given LDAP connection, as a map of connection identifier to
     *     corresponding connection object.
     *
     * @throws GuacamoleException
     *     If an error occurs preventing retrieval of connections.
     */
    public Map<String, Connection> getConnections(AuthenticatedUser user,
            LDAPConnection ldapConnection) throws GuacamoleException {

        // Do not return any connections if base DN is not specified
        String configurationBaseDN = confService.getConfigurationBaseDN();
        if (configurationBaseDN == null)
            return Collections.<String, Connection>emptyMap();

        try {

            // Pull the current user DN from the LDAP connection
            String userDN = ldapConnection.getAuthenticationDN();

            // getConnections() will only be called after a connection has been
            // authenticated (via non-anonymous bind), thus userDN cannot
            // possibly be null
            assert(userDN != null);

            // Find all Guacamole connections for the given user
            LDAPSearchResults results = ldapConnection.search(
                configurationBaseDN,
                LDAPConnection.SCOPE_SUB,
                "(&(objectClass=guacConfigGroup)(member=" + escapingService.escapeLDAPSearchFilter(userDN) + "))",
                null,
                false
            );

            // Build token filter containing credential tokens
            TokenFilter tokenFilter = new TokenFilter();
            StandardTokens.addStandardTokens(tokenFilter, user.getCredentials());

            // Produce connections for each readable configuration
            Map<String, Connection> connections = new HashMap<String, Connection>();
            while (results.hasMore()) {

                LDAPEntry entry = results.next();

                // Get common name (CN)
                LDAPAttribute cn = entry.getAttribute("cn");
                if (cn == null) {
                    logger.warn("guacConfigGroup is missing a cn.");
                    continue;
                }

                // Get associated protocol
                LDAPAttribute protocol = entry.getAttribute("guacConfigProtocol");
                if (protocol == null) {
                    logger.warn("guacConfigGroup \"{}\" is missing the "
                              + "required \"guacConfigProtocol\" attribute.",
                            cn.getStringValue());
                    continue;
                }

                // Set protocol
                GuacamoleConfiguration config = new GuacamoleConfiguration();
                config.setProtocol(protocol.getStringValue());

                // Get parameters, if any
                LDAPAttribute parameterAttribute = entry.getAttribute("guacConfigParameter");
                if (parameterAttribute != null) {

                    // For each parameter
                    Enumeration<?> parameters = parameterAttribute.getStringValues();
                    while (parameters.hasMoreElements()) {

                        String parameter = (String) parameters.nextElement();

                        // Parse parameter
                        int equals = parameter.indexOf('=');
                        if (equals != -1) {

                            // Parse name
                            String name = parameter.substring(0, equals);
                            String value = parameter.substring(equals+1);

                            config.setParameter(name, value);

                        }

                    }

                }

                // Filter the configuration, substituting all defined tokens
                tokenFilter.filterValues(config.getParameters());

                // Store connection using cn for both identifier and name
                String name = cn.getStringValue();
                Connection connection = new SimpleConnection(name, name, config);
                connection.setParentIdentifier(LDAPAuthenticationProvider.ROOT_CONNECTION_GROUP);
                connections.put(name, connection);

            }

            // Return map of all connections
            return connections;

        }
        catch (LDAPException e) {
            throw new GuacamoleServerException("Error while querying for connections.", e);
        }

    }

}
