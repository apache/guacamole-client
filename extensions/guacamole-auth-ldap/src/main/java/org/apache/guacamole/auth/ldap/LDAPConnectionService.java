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
import org.apache.directory.api.ldap.model.filter.ExprNode;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.ResultCodeEnum;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.url.LdapUrl;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.GuacamoleUnsupportedException;
import org.apache.guacamole.auth.ldap.conf.ConfigurationService;
import org.apache.guacamole.auth.ldap.conf.EncryptionMethod;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for creating and managing connections to LDAP servers.
 */
public class LDAPConnectionService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(LDAPConnectionService.class);

    /**
     * Service for retrieving LDAP server configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Creates a new instance of LdapNetworkConnection, configured as required
     * to use whichever encryption method is requested within
     * guacamole.properties.
     *
     * @return
     *     A new LdapNetworkConnection instance which has already been 
     *     configured to use the encryption method requested within 
     *     guacamole.properties.
     *
     * @throws GuacamoleException
     *     If an error occurs while parsing guacamole.properties, or if the
     *     requested encryption method is actually not implemented (a bug).
     */
    private LdapNetworkConnection createLDAPConnection() throws GuacamoleException {

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
                return new LdapNetworkConnection(host, port);

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
    public LdapNetworkConnection bindAs(Dn userDN, String password)
            throws GuacamoleException {

        // Obtain appropriately-configured LdapNetworkConnection instance
        LdapNetworkConnection ldapConnection = createLDAPConnection();

        try {

            // Connect to LDAP server
            ldapConnection.connect();

            // Explicitly start TLS if requested
            if (confService.getEncryptionMethod() == EncryptionMethod.STARTTLS)
                ldapConnection.startTls();

        }
        catch (LdapException e) {
            throw new GuacamoleServerException("Error connecting to LDAP server.", e);
        }

        // Bind using provided credentials
        try {

            BindRequest bindRequest = new BindRequestImpl();
            bindRequest.setDn(userDN);
            bindRequest.setCredentials(password);
            BindResponse bindResponse = ldapConnection.bind(bindRequest);
            if (bindResponse.getLdapResult().getResultCode() == ResultCodeEnum.SUCCESS)
                return ldapConnection;
            
            else
                throw new GuacamoleInvalidCredentialsException("Error binding"
                        + " to server: " + bindResponse.toString(),
                        CredentialsInfo.USERNAME_PASSWORD);

        }

        // Disconnect if an error occurs during bind
        catch (LdapException e) {
            logger.debug("Unable to bind to LDAP server.", e);
            disconnect(ldapConnection);
            throw new GuacamoleInvalidCredentialsException(
                    "Unable to bind to the LDAP server.",
                    CredentialsInfo.USERNAME_PASSWORD);
        }

    }
    
    /**
     * Generate a new LdapNetworkConnection object for following a referral
     * with the given LdapUrl, and copy the username and password
     * from the original connection.
     * 
     * @param referralUrl
     *     The LDAP URL to follow.
     * 
     * @param ldapConfig
     *     The connection configuration to use to retrieve username and
     *     password.
     * 
     * @param hop
     *     The current hop number of this referral - once the configured
     *     limit is reached, this method will throw an exception.
     * 
     * @return
     *     A LdapNetworkConnection object that points at the location
     *     specified in the referralUrl.
     *     
     * @throws GuacamoleException
     *     If an error occurs parsing out the LdapUrl object or the
     *     maximum number of referral hops is reached.
     */
    public LdapNetworkConnection referralConnection(LdapUrl referralUrl,
            LdapConnectionConfig ldapConfig, int hop) 
            throws GuacamoleException {
       
        if (hop >= confService.getMaxReferralHops())
            throw new GuacamoleServerException("Maximum number of referrals reached.");
        
        LdapConnectionConfig referralConfig = new LdapConnectionConfig();
        
        // Copy bind name and password from original config
        referralConfig.setName(ldapConfig.getName());
        referralConfig.setCredentials(ldapConfig.getCredentials());        
        
        // Look for host - if not there, bail out.
        String host = referralUrl.getHost();
        if (host == null || host.isEmpty())
            throw new GuacamoleServerException("Referral URL contains no host.");
       
        referralConfig.setLdapHost(host);
       
        // Look for port, or assign a default.
        int port = referralUrl.getPort();
        if (port < 1)
            referralConfig.setLdapPort(389);
        else
            referralConfig.setLdapPort(port);
        
        // Deal with SSL connections
        if (referralUrl.getScheme().equals(LdapUrl.LDAPS_SCHEME))
            referralConfig.setUseSsl(true);
        else
            referralConfig.setUseSsl(false);
        
        return new LdapNetworkConnection(referralConfig);
        
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
    
    /**
     * Generate a SearchRequest object using the given Base DN and filter
     * and retrieving other properties from the LDAP configuration service.
     * 
     * @param baseDn
     *     The LDAP Base DN at which to search the search.
     * 
     * @param filter
     *     A string representation of a LDAP filter to use for the search.
     * 
     * @return
     *     The properly-configured SearchRequest object.
     * 
     * @throws GuacamoleException
     *     If an error occurs retrieving any of the configuration values.
     */
    public SearchRequest getSearchRequest(Dn baseDn, ExprNode filter)
            throws GuacamoleException {
        
        SearchRequest searchRequest = new SearchRequestImpl();
        searchRequest.setBase(baseDn);
        searchRequest.setDerefAliases(confService.getDereferenceAliases());
        searchRequest.setScope(SearchScope.SUBTREE);
        searchRequest.setFilter(filter);
        searchRequest.setSizeLimit(confService.getMaxResults());
        searchRequest.setTimeLimit(confService.getOperationTimeout());
        searchRequest.setTypesOnly(false);
        
        if (confService.getFollowReferrals())
            searchRequest.followReferrals();
        
        return searchRequest;
    }

}
