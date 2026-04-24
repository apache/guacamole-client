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

package org.apache.guacamole.vault.openbao.conf;

import java.net.URI;
import com.google.inject.Inject;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.properties.BooleanGuacamoleProperty;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;
import org.apache.guacamole.properties.URIGuacamoleProperty;
import org.apache.guacamole.vault.conf.VaultConfigurationService;

/**
 * Service for retrieving Hashicorp/OpenBao configuration from guacamole.properties.
 */
public class OpenBaoConfigurationService extends VaultConfigurationService {
    /**
     * The default cache lifetime in milliseconds.
     */
    public static final int DEFAULT_CACHE_LIFETIME = 5000;

    /**
     * The default request timeout in milliseconds.
     */
    public static final int DEFAULT_REQUEST_TIMEOUT = 5000;

    /**
     * The default connection tiemout in milliseconds.
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 10000;

    /**
     * The default ssh connection tiemout in milliseconds.
     */
    private static final int DEFAULT_SSH_CONNECTION_TIMEOUT = 10000;

    /**
     * The default ssh certificate type
     */
    private static final String DEFAULT_SSH_TYPE = "ed25519";
    
    /**
     * The name of the file which contains the YAML mapping of connection
     * parameter token to secrets within Hashicorp/OpenBao Vault.
     */
    private static final String TOKEN_MAPPING_FILENAME = "vault-token-mapping.yml";

    /**
     * The name of the properties file containing Guacamole configuration
     * properties whose values are the names of corresponding secrets within
     * Hashicorp/OpenBao Vault.
     */
    private static final String PROPERTIES_FILENAME = "guacamole.properties.vlt";

    /**
     * The URI of the hashicorp or OpenBao vault to use.
     */
    private static final URIGuacamoleProperty VAULT_URI =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "vault-uri"; }
    };

    /**
     * The authentication token to use to access the vault.
     */
    private static final StringGuacamoleProperty VAULT_TOKEN =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "vault-token"; }
    };

    /**
     * The authentication username to use to access the vault in place of the token
     */
    private static final StringGuacamoleProperty VAULT_USERNAME =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "vault-username"; }
    };

    /**
     * The authentication password to use to access the vault in place of the token
     */
    private static final StringGuacamoleProperty VAULT_PASSWORD =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "vault-password"; }
    };
    /**
     * The maximum time that the cached data is considered valid in ms.
     */
    private static final IntegerGuacamoleProperty VAULT_CACHE_LIFETIME =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "vault-cache-lifetime"; }
    };

    /**
     * The maximum time that a request to the vault server can take in ms.
     */
    private static final IntegerGuacamoleProperty VAULT_REQUEST_TIMEOUT =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "vault-request-timeout"; }
    };

    /**
     * The maximum time that a connection to the vault server can take in ms.
     */
    private static final IntegerGuacamoleProperty VAULT_CONNECTION_TIMEOUT =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "vault-connection-timeout"; }
    };

    /**
     * The maximum time that a connection to the vault server can take in ms.
     */
    private static final IntegerGuacamoleProperty VAULT_SSH_CONNECTION_TIMEOUT =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "vault-ssh-connection-timeout"; }
    };
    
    /**
     * The type of ssh certificates that will be generated
     */
    private static final StringGuacamoleProperty VAULT_SSH_TYPE =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "vault-ssh-type"; }
    };

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * Creates a new OpenBaoConfigurationService.
     */

    /**
     * Creates a new OpenBaoConfigurationService which reads the configuration
     * from "vault-token-mapping.yml" and properties from
     * "guacamole.properties.vlt". The token mapping is a YAML file which lists
     * each connection parameter token and the name of the secret from which
     * the value for that token should be read, while the properties file is an
     * alternative to guacamole.properties where each property value is the
     * name of a secret containing the actual value.
     */

    public OpenBaoConfigurationService() {
        super(TOKEN_MAPPING_FILENAME, PROPERTIES_FILENAME);
    }

    /**
     * The URI of the hashicorp or OpenBao vault to use.
     *
     * @return URI
     *      The Hashicorp or OpenBao server URI (e.g., "http://localhost:8200").
     *
     * @throws GuacamoleException
     *     If the property is not defined in guacamole.properties or
     *     guacamole.properties can not be parsed.
     */
    public URI getVaultUri() throws GuacamoleException {
        return environment.getRequiredProperty(VAULT_URI);
    }

    /**
     * The authentication token to use to access the vault.
     *
     * @return String
     *      The Hashicorp or OpenBao authentication token.
     *
     * @throws GuacamoleException
     *     If the property is not defined in guacamole.properties or
     *     guacamole.properties can not be parsed.
     */
    public String getVaultToken() throws GuacamoleException {
        return environment.getProperty(VAULT_TOKEN);
    }

    /**
     * The authentication Username to use to access the vault.
     *
     * @return String
     *      The Hashicorp or OpenBao authentication Username
     *
     * @throws GuacamoleException
     *     If the property is not defined in guacamole.properties or
     *     guacamole.properties can not be parsed.
     */
    public String getVaultUsername() throws GuacamoleException {
        return environment.getProperty(VAULT_USERNAME);
    }

    /**
     * The authentication Password to use to access the vault.
     *
     * @return String
     *      The Hashicorp or OpenBao authentication Password
     *
     * @throws GuacamoleException
     *     If the property is not defined in guacamole.properties or
     *     guacamole.properties can not be parsed.
     */
    public String getVaultPassword() throws GuacamoleException {
        return environment.getProperty(VAULT_PASSWORD);
    }
    /**
     * The maximum time that the cached data is considered valid in
     * milliseconds.
     *
     * @return int
     *      The cache lifetime in milliseconds.
     *
     * @throws GuacamoleException
     *     If guacamole.properties can not be parsed.
     */
    public int getVaultCacheLifetime() throws GuacamoleException {
        return environment.getProperty(VAULT_CACHE_LIFETIME, DEFAULT_CACHE_LIFETIME);
    }

    /**
     * The maximum time that a request to the vault server can take in
     * milliseconds.
     *
     * @return int
     *      The request timeout in milliseconds.
     *
     * @throws GuacamoleException
     *     If guacamole.properties can not be parsed.
     */
    public int getRequestTimeout() throws GuacamoleException {
        return environment.getProperty(VAULT_REQUEST_TIMEOUT, DEFAULT_REQUEST_TIMEOUT);
    }

    /**
     * The maximum time that a connection to the vault server can take in
     * milliseconds.
     *
     * @return int
     *      The conenction timeout in milliseconds.
     *
     * @throws GuacamoleException
     *     If guacamole.properties can not be parsed.
     */
    public int getConnectionTimeout() throws GuacamoleException {
        return environment.getProperty(VAULT_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
    }


    /**
     * The type of SSH certificates are will be generated. Must be either
     * 'rsa' of 4096-bit RSA keys or 'ed25519'.
     *
     * @return String
     *      The ssh type to use.
     *
     * @throws GuacamoleException
     *     If guacamole.properties can not be parsed.
     */
    public String getSshType() throws GuacamoleException {
        String type = environment.getProperty(VAULT_SSH_TYPE, DEFAULT_SSH_TYPE);
        if (! type.equals("rsa") & ! type.equals("ed25519")) {
            throw new GuacamoleException("Only ssh certicate types 'rsa' (4096-bit) and 'ed25519' are supported");
        }
        return type;
    }
    
    /**
     * The maximum time that a connection to a ssh server can take in
     * milliseconds.
     *
     * @return int
     *      The ssh conenction timeout in milliseconds.
     *
     * @throws GuacamoleException
     *     If guacamole.properties can not be parsed.
     */
    public int getSshConnectionTimeout() throws GuacamoleException {
        return environment.getProperty(VAULT_SSH_CONNECTION_TIMEOUT, DEFAULT_SSH_CONNECTION_TIMEOUT);
    }

    @Override
    public boolean getSplitWindowsUsernames() throws GuacamoleException {
        // Not needed for Hashicorp/OpenBao - return false
        return false;
    }

    @Override
    public boolean getMatchUserRecordsByDomain() throws GuacamoleException {
        // Not needed for Hashicorp/OpenBao - return false
        return false;
    }
}
