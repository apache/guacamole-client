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

package org.apache.guacamole.vault.hv.conf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.net.URI;
import java.util.Objects;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.properties.BooleanGuacamoleProperty;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;
import org.apache.guacamole.properties.URIGuacamoleProperty;
import org.apache.guacamole.vault.conf.VaultConfigurationService;
import org.apache.guacamole.vault.hv.secret.HvSshKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service class to retrieve configuration information for an OpenBao
 * or Hashicorp Vault.
 */
@Singleton
public class HvConfigurationService extends VaultConfigurationService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(VaultConfigurationService.class);

    /**
     * The default cache lifetime in milliseconds.
     */
    public static final int DEFAULT_CACHE_LIFETIME = 5000;

    /**
     * The default request timeout in milliseconds.
     */
    public static final int DEFAULT_REQUEST_TIMEOUT = 5000;

    /**
     * The default connection timeout in milliseconds.
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 10_000;

    /**
     * The default ssh connection tiemout in seconds.
     */
    public static final int DEFAULT_SSH_CONNECTION_TIMEOUT = 1800;

    /**
     * The default vault token renewal delay in milliseconds. Expiring
     * tokens will be renewed at least this delay before expiration
     */
    public static final int DEFAULT_TOKEN_RENEWAL_DELAY = 10_000;

    /**
     * The default ssh certificate type.
     */
    public static final String DEFAULT_SSH_TYPE = HvSshKeys.ED25519;

    /**
     * The default of whether to accept user configurations or not.
     */
    public static final Boolean DEFAULT_ALLOW_USER_CONFIG = false;

    /**
     * The name of the file which contains the YAML mapping of connection
     * parameter token to secrets within Hashicorp Vault.
     */
    private static final String TOKEN_MAPPING_FILENAME = "vault-token-mapping.yml";

    /**
     * The name of the properties file containing Guacamole configuration
     * properties whose values are the names of corresponding secrets within
     * Hashicorp Vault.
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
     * The maximum time that an ssh signed certificate is considered to be valid.
     */
    private static final IntegerGuacamoleProperty VAULT_SSH_CONNECTION_TIMEOUT =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "vault-ssh-connection-timeout"; }
    };

    /**
     * The renewal delay for expiring Vault tokens in ms. Tokens will be renewed
     * prior to expiration by this delay
     */
    private static final IntegerGuacamoleProperty VAULT_TOKEN_RENEWAL_DELAY =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "vault-token-renewal-delay"; }
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
     * Whether users should be able to supply their own HV configurations.
     */
    private static final BooleanGuacamoleProperty VAULT_ALLOW_USER_CONFIG = new BooleanGuacamoleProperty() {
        @Override
        public String getName() {
            return "vault-allow-user-config";
        }
    };

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * Creates a new HvConfigurationService which reads the configuration
     * from "hv-token-mapping.yml" and properties from
     * "guacamole.properties.hv". The token mapping is a YAML file which lists
     * each connection parameter token and the name of the secret from which
     * the value for that token should be read, while the properties file is an
     * alternative to guacamole.properties where each property value is the
     * name of a secret containing the actual value.
     */
    public HvConfigurationService() {
        super(TOKEN_MAPPING_FILENAME, PROPERTIES_FILENAME);
    }

    /**
     * The URI of the hashicorp or OpenBao vault to use.
     *
     * @return
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
     * @return
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
     * @return
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
     * @return
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
     * @return
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
     * @return
     *      The connection timeout in milliseconds.
     *
     * @throws GuacamoleException
     *     If guacamole.properties can not be parsed.
     */
    public int getConnectionTimeout() throws GuacamoleException {
        return environment.getProperty(VAULT_CONNECTION_TIMEOUT, DEFAULT_CONNECTION_TIMEOUT);
    }

    /**
     * The renewal delay, in milliseconds of expiring token. A token will be renewed
     * prior to it expiration by this delay
     *
     * @return
     *      The renewal delay in milliseconds.
     *
     * @throws GuacamoleException
     *     If guacamole.properties can not be parsed.
     */
    public int getTokenRenewalDelay() throws GuacamoleException {
        return environment.getProperty(VAULT_TOKEN_RENEWAL_DELAY, DEFAULT_TOKEN_RENEWAL_DELAY);
    }

    /**
     * The type of SSH certificates are will be generated. Must be either
     * 'rsa' for 4096-bit RSA keys, 'ec' for ECDSA NIST P-256 keys or 'ed25519'.
     *
     * @return
     *      The ssh type to use.
     *
     * @throws GuacamoleException
     *     If guacamole.properties can not be parsed.
     */
    public String getSshType() throws GuacamoleException {
        final String type = environment.getProperty(VAULT_SSH_TYPE, DEFAULT_SSH_TYPE);
        if (! HvSshKeys.RSA.equals(type) && ! HvSshKeys.ED25519.equals(type) && ! HvSshKeys.EC256.equals(type)) {
            throw new GuacamoleException("Only ssh certificate types 'rsa' (4096-bit), 'ec' (256-bit) and 'ed25519' are supported");
        }
        return type;
    }

    /**
     * The maximum time that a signed SSH certificate is considered valid in
     * milliseconds.
     *
     * @return
     *      The ssh connection timeout in milliseconds.
     *
     * @throws GuacamoleException
     *     If guacamole.properties can not be parsed.
     */
    public int getSshConnectionTimeout() throws GuacamoleException {
        return environment.getProperty(VAULT_SSH_CONNECTION_TIMEOUT, DEFAULT_SSH_CONNECTION_TIMEOUT);
    }

    /**
     * Return whether user-level HV configs should be enabled. If this
     * flag is set to true, users vaults can be used to supply secrets.
     * If set to false, no existing user-specific HV configuration
     * will be exposed through the UI or used when looking up secrets.
     *
     * @return
     *     true if user-specific HV configuration is enabled, false otherwise.
     *
     * @throws GuacamoleException
     *     If the value specified within guacamole.properties cannot be
     *     parsed.
     */
    public boolean allowUserConfig() throws GuacamoleException {
        return environment.getProperty(VAULT_ALLOW_USER_CONFIG, DEFAULT_ALLOW_USER_CONFIG);
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
    
    /**
     * Class containing the configuration associated with a client instance
     */
    public class VaultInfo {
        
        /** 
          * Stores value of vault specific 'vault-uri'
          */
        private final URI uri;          

        /** 
         * Stores value of vault specific 'vault-token'
         */
        private final String token;

        /** 
         * Stores value of vault specific 'vault-username'
         */
        private final String username;

        /**
         * Stores value of vault specific 'vault-password'
         */
        public final String password;

        /**
         * Construct a class containing Vault specific configuration options.
         */
        public VaultInfo(final URI uri, final String token, final String username, final String password) {
            this.uri = uri;
            this.token = token;
            this.username = username;
            this.password = password;
        }

        /**
         * The URI of the hashicorp or OpenBao vault to use.
         *
         * @return
         *      The Hashicorp or OpenBao server URI (e.g., "http://localhost:8200").
         *
         * @throws GuacamoleException
         *     If the property is not defined in guacamole.properties or
         *     guacamole.properties can not be parsed.
         */
        public URI getVaultUri() throws GuacamoleException {
            return uri;
        }
     
        /**
         * The authentication token to use to access the vault.
         *
         * @return
         *      The Hashicorp or OpenBao authentication token.
         *
         * @throws GuacamoleException
         *     If the property is not defined in guacamole.properties or
         *     guacamole.properties can not be parsed.
         */
        public String getVaultToken() throws GuacamoleException {
            return token;
        }

        /**
         * The authentication Username to use to access the vault.
         *
         * @return
         *      The Hashicorp or OpenBao authentication Username
         *
         * @throws GuacamoleException
         *     If the property is not defined in guacamole.properties or
         *     guacamole.properties can not be parsed.
         */
        public String getVaultUsername() throws GuacamoleException {
            return username;
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
            return password;
        }
        
        /**
         * The maximum time that the cached data is considered valid in
         * milliseconds.
         *
         * @return
         *      The cache lifetime in milliseconds.
         *
         * @throws GuacamoleException
         *     If guacamole.properties can not be parsed.
         */
        public int getVaultCacheLifetime() throws GuacamoleException {
            return HvConfigurationService.this.getVaultCacheLifetime();
        }

        /**
         * The maximum time that a request to the vault server can take in
         * milliseconds.
         *
         * @return
         *      The request timeout in milliseconds.
         *
         * @throws GuacamoleException
         *     If guacamole.properties can not be parsed.
         */
        public int getRequestTimeout() throws GuacamoleException {
            return HvConfigurationService.this.getRequestTimeout();
        }

        /**
         * The maximum time that a connection to the vault server can take in
         * milliseconds.
         *
         * @return
         *      The connection timeout in milliseconds.
         *
         * @throws GuacamoleException
         *     If guacamole.properties can not be parsed.
         */
        public int getConnectionTimeout() throws GuacamoleException {
            return HvConfigurationService.this.getConnectionTimeout();
        }

        /**
         * The renewal delay, in milliseconds of expiring token. A token will be renewed
         * prior to it expiration by this delay
         *
         * @return
         *      The renewal delay in milliseconds.
         *
         * @throws GuacamoleException
         *     If guacamole.properties can not be parsed.
         */
        public int getTokenRenewalDelay() throws GuacamoleException {
            return HvConfigurationService.this.getTokenRenewalDelay();
        }

        /**
         * The type of SSH certificates are will be generated. Must be either
         * 'rsa' for 4096-bit RSA keys, 'ec' for ECDSA NIST P-256 keys or 'ed25519'.
         *
         * @return
         *      The ssh type to use.
         *
         * @throws GuacamoleException
         *     If guacamole.properties can not be parsed.
         */
        public String getSshType() throws GuacamoleException {
            return HvConfigurationService.this.getSshType();
        }

        /**
         * The maximum time that a signed SSH certificate is considered valid in
         * milliseconds.
         *
         * @return
         *      The ssh connection timeout in milliseconds.
         *
         * @throws GuacamoleException
         *     If guacamole.properties can not be parsed.
         */
        public int getSshConnectionTimeout() throws GuacamoleException {
            return HvConfigurationService.this.getSshConnectionTimeout();
        }

        @Override
        public boolean equals(final Object object) {
            if (this == object) {
                return true;
            }
            if (object == null || getClass() != object.getClass()) {
              return false;
            }
            
            final VaultInfo that = (VaultInfo) object;
            // If Token non null it is used for the connection
            if (token == null || token.isBlank()) {
                return Objects.equals(uri, that.uri) &&
                        Objects.equals(username, that.username) &&
                        Objects.equals(password, that.password);
            }
            else {
                return Objects.equals(uri, that.uri) &&
                        Objects.equals(token, that.token);
            }
        }

        @Override
        public int hashCode() {
            // Only hash the values that can be different
            if (token == null || token.isBlank()) {
                return Objects.hash(uri, username, password);
            }
            else {
                return Objects.hash(uri, token);
            }
        }

        /**
         * Returns true if the VaultInfo configuration seems valid
         *
         * @return
         *      True is the value in non null, URI is set and at least one of
         *      token or username/password is set
         */
        public boolean isVaultInfoInvalid() {
            return uri == null || uri.toString().isBlank() ||
                ((token == null || token.isBlank()) &&
                (username == null || username.isBlank() ||
                 password == null || password.isBlank()));
        }
    }
}
