/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.auth.ldap;

import com.google.inject.Inject;
import java.io.IOException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleUnsupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for creating and managing connections to LDAP servers.
 */
public class LDAPConnectionService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(LDAPConnectionService.class);

    /**
     * Service for retrieving LDAP server configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Creates a new instance of LdapConnection, configured as required to use
     * whichever encryption method is requested within guacamole.properties.
     *
     * @return
     *     A new LdapConnection instance which has already been configured to
     *     use the encryption method requested within guacamole.properties, and
     *     also has the host and port configured.
     *
     * @throws GuacamoleException
     *     If an error occurs while parsing guacamole.properties, or if the
     *     requested encryption method is actually not implemented (a bug).
     */
    private LdapNetworkConnection createLdapConnection() throws GuacamoleException {

        // Get basic configuration parameters
        String host = confService.getServerHostname();
        int port = confService.getServerPort();

        // Map encryption method to proper connection and socket factory
        EncryptionMethod encryptionMethod = confService.getEncryptionMethod();
        switch (encryptionMethod) {

            // Unencrypted LDAP connection
            case NONE:
                logger.debug("Connection to LDAP server without encryption.");
                return new LdapNetworkConnection(host, port);

            // LDAP over SSL (LDAPS)
            case SSL:
                logger.debug("Connecting to LDAP server using SSL/TLS.");
                return new LdapNetworkConnection(host, port, true);

            // LDAP + STARTTLS
            case STARTTLS:
                logger.debug("Connecting to LDAP server using STARTTLS.");
                return new LdapNetworkConnection(host, port, false);

            // The encryption method, though known, is not actually
            // implemented. If encountered, this would be a bug.
            default:
                throw new GuacamoleUnsupportedException("Unimplemented encryption method: " + encryptionMethod);

        }

    }

    /**
     * Binds to the LDAP server using the provided user DN and password.
     *
     * @param userDN
     *     The DN of the user to bind as, or null to bind anonymously.
     *
     * @param password
     *     The password to use when binding as the specified user, or null to
     *     attempt to bind without a password.
     *
     * @return
     *     A bound LDAP connection, or null if the connection could not be
     *     bound.
     *
     * @throws GuacamoleException
     *     If an error occurs while binding to the LDAP server.
     */
    public LdapConnection bindAs(Dn userDN, String password)
            throws GuacamoleException {

        // Obtain appropriately-configured LdapConnection instance
        LdapNetworkConnection ldapConnection = createLdapConnection();

        try {

            // Connect to LDAP server
            ldapConnection.connect();

            // Explicitly start TLS if requested
            if (confService.getEncryptionMethod() == EncryptionMethod.STARTTLS)
                ldapConnection.startTls();

        }
        catch (LdapException e) {
            logger.error("Unable to connect to LDAP server: {}", e.getMessage());
            logger.debug("Failed to connect to LDAP server.", e);
            return null;
        }

        // Bind using provided credentials
        try {

            // Bind as user
            BindRequest bindRequest = new BindRequestImpl();
            bindRequest.setDn(userDN);
            bindRequest.setCredentials(password);
            ldapConnection.bind(bindRequest);

        }

        // Disconnect if an error occurs during bind
        catch (LdapException e) {
            logger.debug("LDAP bind failed.", e);
            disconnect(ldapConnection);
            return null;
        }

        return ldapConnection;

    }

    /**
     * Disconnects the given LDAP connection, logging any failure to do so
     * appropriately.
     *
     * @param ldapConnection
     *     The LDAP connection to disconnect.
     */
    public void disconnect(LdapConnection ldapConnection) {

        // Attempt disconnect
        try {
            ldapConnection.close();
        }

        // Warn if disconnect unexpectedly fails
        catch (IOException e) {
            logger.warn("Unable to disconnect from LDAP server: {}", e.getMessage());
            logger.debug("LDAP disconnect failed.", e);
        }

    }

}
