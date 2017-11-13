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
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPConstraints;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.novell.ldap.LDAPJSSEStartTLSFactory;
import java.io.UnsupportedEncodingException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleUnsupportedException;
import org.apache.guacamole.auth.ldap.ReferralAuthHandler;
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
     * Creates a new instance of LDAPConnection, configured as required to use
     * whichever encryption method is requested within guacamole.properties.
     *
     * @return
     *     A new LDAPConnection instance which has already been configured to
     *     use the encryption method requested within guacamole.properties.
     *
     * @throws GuacamoleException
     *     If an error occurs while parsing guacamole.properties, or if the
     *     requested encryption method is actually not implemented (a bug).
     */
    private LDAPConnection createLDAPConnection() throws GuacamoleException {

        // Map encryption method to proper connection and socket factory
        EncryptionMethod encryptionMethod = confService.getEncryptionMethod();
        switch (encryptionMethod) {

            // Unencrypted LDAP connection
            case NONE:
                logger.debug("Connection to LDAP server without encryption.");
                return new LDAPConnection();

            // LDAP over SSL (LDAPS)
            case SSL:
                logger.debug("Connecting to LDAP server using SSL/TLS.");
                return new LDAPConnection(new LDAPJSSESecureSocketFactory());

            // LDAP + STARTTLS
            case STARTTLS:
                logger.debug("Connecting to LDAP server using STARTTLS.");
                return new LDAPConnection(new LDAPJSSEStartTLSFactory());

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
    public LDAPConnection bindAs(String userDN, String password)
            throws GuacamoleException {

        // Obtain appropriately-configured LDAPConnection instance
        LDAPConnection ldapConnection = createLDAPConnection();

        // Configure LDAP connection constraints
        LDAPConstraints ldapConstraints = ldapConnection.getConstraints();
        if (ldapConstraints == null)
          ldapConstraints = new LDAPConstraints();

        // Set whether or not we follow referrals
        ldapConstraints.setReferralFollowing(confService.getFollowReferrals());

        // Set referral authentication to use the provided credentials.
        if (userDN != null && !userDN.isEmpty())
            ldapConstraints.setReferralHandler(new ReferralAuthHandler(userDN, password));

        // Set the maximum number of referrals we follow
        ldapConstraints.setHopLimit(confService.getMaxReferralHops());

        // Set timelimit to wait for LDAP operations, converting to ms
        ldapConstraints.setTimeLimit(confService.getOperationTimeout() * 1000);

        // Apply the constraints to the connection
        ldapConnection.setConstraints(ldapConstraints);

        try {

            // Connect to LDAP server
            ldapConnection.connect(
                confService.getServerHostname(),
                confService.getServerPort()
            );

            // Explicitly start TLS if requested
            if (confService.getEncryptionMethod() == EncryptionMethod.STARTTLS)
                ldapConnection.startTLS();

        }
        catch (LDAPException e) {
            logger.error("Unable to connect to LDAP server: {}", e.getMessage());
            logger.debug("Failed to connect to LDAP server.", e);
            return null;
        }

        // Bind using provided credentials
        try {

            byte[] passwordBytes;
            try {

                // Convert password into corresponding byte array
                if (password != null)
                    passwordBytes = password.getBytes("UTF-8");
                else
                    passwordBytes = null;

            }
            catch (UnsupportedEncodingException e) {
                logger.error("Unexpected lack of support for UTF-8: {}", e.getMessage());
                logger.debug("Support for UTF-8 (as required by Java spec) not found.", e);
                disconnect(ldapConnection);
                return null;
            }

            // Bind as user
            ldapConnection.bind(LDAPConnection.LDAP_V3, userDN, passwordBytes);

        }

        // Disconnect if an error occurs during bind
        catch (LDAPException e) {
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
    public void disconnect(LDAPConnection ldapConnection) {

        // Attempt disconnect
        try {
            ldapConnection.disconnect();
        }

        // Warn if disconnect unexpectedly fails
        catch (LDAPException e) {
            logger.warn("Unable to disconnect from LDAP server: {}", e.getMessage());
            logger.debug("LDAP disconnect failed.", e);
        }

    }

}
