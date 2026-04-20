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

import org.apache.guacamole.properties.StringGuacamoleProperty;
import org.apache.guacamole.properties.URIGuacamoleProperty;

/**
 * Configuration properties for OpenBao vault integration.
 */
public class OpenBaoConfig {

    /**
     * OpenBao server URL (e.g., "http://localhost:8200").
     * This property is REQUIRED and must be configured in guacamole.properties.
     */
    public static final URIGuacamoleProperty OPENBAO_SERVER_URL =
            new URIGuacamoleProperty() {
                @Override
                public String getName() {
                    return "openbao-server-url";
                }
            };

    /**
     * OpenBao authentication token. Required unless AppRole
     * authentication is configured via {@link #OPENBAO_ROLE_ID} and
     * {@link #OPENBAO_SECRET_ID}.
     */
    public static final StringGuacamoleProperty OPENBAO_TOKEN =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "openbao-token";
                }
            };

    /**
     * OpenBao AppRole role ID. Optional. When both this property and
     * {@link #OPENBAO_SECRET_ID} are set, AppRole authentication is used
     * instead of a static token.
     */
    public static final StringGuacamoleProperty OPENBAO_ROLE_ID =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "openbao-role-id";
                }
            };

    /**
     * OpenBao AppRole secret ID. Optional. See {@link #OPENBAO_ROLE_ID}.
     */
    public static final StringGuacamoleProperty OPENBAO_SECRET_ID =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "openbao-secret-id";
                }
            };

    /**
     * OpenBao AppRole auth mount path (default: "approle"). Only relevant
     * when {@link #OPENBAO_ROLE_ID} / {@link #OPENBAO_SECRET_ID} are set.
     */
    public static final StringGuacamoleProperty OPENBAO_APPROLE_PATH =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "openbao-approle-path";
                }
            };

    /**
     * OpenBao KV secrets engine mount path (default: "rdp-creds").
     * This is the mount point where RDP credentials are stored.
     */
    public static final StringGuacamoleProperty OPENBAO_MOUNT_PATH =
            new StringGuacamoleProperty() {
                @Override
                public String getName() {
                    return "openbao-mount-path";
                }
            };
}
