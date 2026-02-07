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

package org.apache.guacamole.vault.openbao.secret;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.vault.openbao.conf.OpenBaoConfigurationService;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Client for communicating with OpenBao REST API.
 */
public class OpenBaoClient {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(OpenBaoClient.class);

    /**
     * Service for retrieving OpenBao configuration.
     */
    @Inject
    private OpenBaoConfigurationService configService;

    /**
     * Gson instance for JSON parsing.
     */
    private final Gson gson = new Gson();

    /**
     * Retrieves a secret from OpenBao by username.
     *
     * @param username
     *     The Guacamole username to look up in OpenBao.
     *
     * @return
     *     The JSON response from OpenBao.
     *
     * @throws GuacamoleException
     *     If the secret cannot be retrieved from OpenBao.
     */
    public JsonObject getSecret(String username) throws GuacamoleException {

        String serverUrl = configService.getServerUrl();
        String token = configService.getToken();
        String mountPath = configService.getMountPath();
        String kvVersion = configService.getKvVersion();

        // Build the API path based on KV version
        // KV v2: /v1/{mount-path}/data/{username}
        // KV v1: /v1/{mount-path}/{username}
        String apiPath;
        if ("2".equals(kvVersion)) {
            apiPath = String.format("/v1/%s/data/%s", mountPath, username);
        } else {
            apiPath = String.format("/v1/%s/%s", mountPath, username);
        }

        String fullUrl = serverUrl + apiPath;

        logger.info("Fetching secret from OpenBao: {}", fullUrl);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

            HttpGet httpGet = new HttpGet(fullUrl);
            httpGet.setHeader("X-Vault-Token", token);
            httpGet.setHeader("Accept", "application/json");

            // Set timeouts
            httpGet.setConfig(org.apache.hc.client5.http.config.RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(configService.getConnectionTimeout()))
                    .setResponseTimeout(Timeout.ofMilliseconds(configService.getRequestTimeout()))
                    .build());

            org.apache.hc.core5.http.ClassicHttpResponse response = httpClient.executeOpen(null, httpGet, null);
            try {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                if (statusCode == 200) {
                    logger.info("OpenBao response status: {} - successfully retrieved password for {}", statusCode, username);
                    JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                    return jsonResponse;
                } else if (statusCode == 404) {
                    logger.warn("Secret not found in OpenBao for username: {}", username);
                    throw new GuacamoleServerException("Secret not found in OpenBao for username: " + username);
                } else if (statusCode == 403) {
                    logger.error("Permission denied accessing OpenBao. Check token permissions.");
                    throw new GuacamoleServerException("Permission denied accessing OpenBao. Check token permissions.");
                } else {
                    logger.error("OpenBao returned error status {}: {}", statusCode, responseBody);
                    throw new GuacamoleServerException("OpenBao error (HTTP " + statusCode + "): " + responseBody);
                }
            } finally {
                response.close();
            }

        } catch (IOException | org.apache.hc.core5.http.ParseException e) {
            logger.error("Failed to communicate with OpenBao at {}: {}", fullUrl, e.getMessage());
            throw new GuacamoleServerException("Failed to communicate with OpenBao", e);
        }
    }

    /**
     * Extracts the password field from an OpenBao KV v2 response.
     *
     * @param response
     *     The JSON response from OpenBao.
     *
     * @return
     *     The password string, or null if not found.
     */
    public String extractPassword(JsonObject response) {
        try {
            // For KV v2: response.data.data.password
            if (response.has("data")) {
                JsonObject data = response.getAsJsonObject("data");
                if (data.has("data")) {
                    JsonObject innerData = data.getAsJsonObject("data");
                    if (innerData.has("password")) {
                        return innerData.get("password").getAsString();
                    }
                }
            }

            logger.warn("Password field not found in OpenBao response");
            return null;

        } catch (Exception e) {
            logger.error("Error extracting password from OpenBao response", e);
            return null;
        }
    }
}
