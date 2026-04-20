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

import com.google.inject.Inject;
import java.net.URI;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.vault.conf.VaultConfigurationService;

/**
 * Service for retrieving OpenBao configuration from guacamole.properties.
 */
public class OpenBaoConfigurationService extends VaultConfigurationService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * Creates a new OpenBaoConfigurationService.
     */
    public OpenBaoConfigurationService() {
        super("openbao-token-mapping.yml", "guacamole.properties.openbao");
    }

    /**
     * Returns the OpenBao server URL as a parsed URI.
     *
     * @return The OpenBao server URI (e.g., "http://localhost:8200").
     * @throws GuacamoleException
     *     If the property is not defined in guacamole.properties or is
     *     not a valid URI.
     */
    public URI getServerUrl() throws GuacamoleException {
        return environment.getRequiredProperty(OpenBaoConfig.OPENBAO_SERVER_URL);
    }

    /**
     * Returns the OpenBao authentication token, or null if AppRole
     * authentication is configured instead (see {@link #getRoleId()} and
     * {@link #getSecretId()}).
     *
     * @return The OpenBao authentication token, or null.
     * @throws GuacamoleException
     *     If an error occurs reading the property.
     */
    public String getToken() throws GuacamoleException {
        return environment.getProperty(OpenBaoConfig.OPENBAO_TOKEN);
    }

    /**
     * Returns the OpenBao AppRole role ID, or null if not configured.
     *
     * @return The configured AppRole role ID, or null.
     * @throws GuacamoleException
     *     If an error occurs reading the property.
     */
    public String getRoleId() throws GuacamoleException {
        return environment.getProperty(OpenBaoConfig.OPENBAO_ROLE_ID);
    }

    /**
     * Returns the OpenBao AppRole secret ID, or null if not configured.
     *
     * @return The configured AppRole secret ID, or null.
     * @throws GuacamoleException
     *     If an error occurs reading the property.
     */
    public String getSecretId() throws GuacamoleException {
        return environment.getProperty(OpenBaoConfig.OPENBAO_SECRET_ID);
    }

    /**
     * Returns the OpenBao AppRole auth backend mount path (default: "approle").
     *
     * @return The AppRole auth mount path.
     * @throws GuacamoleException
     *     If an error occurs reading the property.
     */
    public String getAppRolePath() throws GuacamoleException {
        return environment.getProperty(
            OpenBaoConfig.OPENBAO_APPROLE_PATH,
            "approle"
        );
    }

    /**
     * Returns true if AppRole authentication has been configured (both
     * role ID and secret ID are present).
     *
     * @return true if AppRole should be used, false to use a static token.
     * @throws GuacamoleException
     *     If an error occurs reading the properties.
     */
    public boolean isAppRoleConfigured() throws GuacamoleException {
        String roleId = getRoleId();
        String secretId = getSecretId();
        return roleId != null && !roleId.isEmpty()
            && secretId != null && !secretId.isEmpty();
    }

    /**
     * Returns the OpenBao KV secrets engine mount path.
     *
     * @return The mount path (default: "rdp-creds").
     * @throws GuacamoleException
     *     If an error occurs reading the property.
     */
    public String getMountPath() throws GuacamoleException {
        return environment.getProperty(
            OpenBaoConfig.OPENBAO_MOUNT_PATH,
            "rdp-creds"
        );
    }

    /**
     * Returns the OpenBao KV version.
     * Hardcoded to "2" for KV v2 secrets engine.
     *
     * @return The KV version "2".
     */
    public String getKvVersion() {
        return "2";
    }

    /**
     * Returns the connection timeout in milliseconds.
     * Hardcoded to 5000ms (5 seconds).
     *
     * @return The connection timeout of 5000ms.
     */
    public int getConnectionTimeout() {
        return 5000;
    }

    /**
     * Returns the request timeout in milliseconds.
     * Hardcoded to 10000ms (10 seconds).
     *
     * @return The request timeout of 10000ms.
     */
    public int getRequestTimeout() {
        return 10000;
    }

    @Override
    public boolean getSplitWindowsUsernames() throws GuacamoleException {
        // Not needed for OpenBao - return false
        return false;
    }

    @Override
    public boolean getMatchUserRecordsByDomain() throws GuacamoleException {
        // Not needed for OpenBao - return false
        return false;
    }
}
