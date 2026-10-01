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
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import java.io.IOException;
import java.net.URI;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.vault.openbao.conf.OpenBaoConfigurationService;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for communicating with OpenBao REST API.
 */
public class OpenBaoClient {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(OpenBaoClient.class);

    /**
     * Gson instance for JSON parsing. Gson is thread-safe, so a single
     * static instance is reused across all calls.
     */
    private static final Gson GSON = new Gson();

    /**
     * Service for retrieving OpenBao configuration.
     */
    @Inject
    private OpenBaoConfigurationService configService;

    /**
     * Shared HTTP client. Lazily created on first use and reused for the
     * lifetime of this instance; Apache HttpClient 5 is thread-safe and
     * designed to be reused across requests.
     */
    private volatile CloseableHttpClient httpClient;

    /**
     * Cached AppRole token. Populated on first successful AppRole login
     * and re-used until invalidated (e.g. on a 403 response).
     */
    private volatile String cachedAppRoleToken;

    /**
     * Returns the shared {@link CloseableHttpClient}, creating it on first
     * access. Thread-safe double-checked initialization.
     *
     * @return
     *     The shared HTTP client instance.
     */
    private CloseableHttpClient getHttpClient() {
        CloseableHttpClient client = httpClient;
        if (client == null) {
            synchronized (this) {
                client = httpClient;
                if (client == null) {
                    client = HttpClients.createDefault();
                    httpClient = client;
                }
            }
        }
        return client;
    }

    /**
     * Builds a {@link RequestConfig} from the configured connection and
     * request timeouts.
     *
     * @return
     *     A request configuration reflecting current timeouts.
     */
    private RequestConfig buildRequestConfig() {
        return RequestConfig.custom()
                .setConnectionRequestTimeout(
                        Timeout.ofMilliseconds(configService.getConnectionTimeout()))
                .setResponseTimeout(
                        Timeout.ofMilliseconds(configService.getRequestTimeout()))
                .build();
    }

    /**
     * Validates that the configured server URL, mount path, and auth
     * credentials are present and non-empty, throwing a
     * {@link GuacamoleServerException} with a clear message otherwise.
     *
     * @return
     *     The validated server URL as a string (without trailing slash).
     *
     * @throws GuacamoleException
     *     If a required property is missing, empty, or invalid.
     */
    private String validatedServerUrl() throws GuacamoleException {

        URI serverUri = configService.getServerUrl();
        if (serverUri == null || serverUri.toString().trim().isEmpty()) {
            throw new GuacamoleServerException(
                    "OpenBao server URL (\"openbao-server-url\") is not configured.");
        }

        String mountPath = configService.getMountPath();
        if (mountPath == null || mountPath.trim().isEmpty()) {
            throw new GuacamoleServerException(
                    "OpenBao mount path (\"openbao-mount-path\") must not be empty.");
        }

        // Strip trailing slash to keep path concatenation predictable
        String url = serverUri.toString();
        if (url.endsWith("/"))
            url = url.substring(0, url.length() - 1);

        return url;
    }

    /**
     * Resolves a usable OpenBao auth token, either from the configured
     * static {@code openbao-token} or, if AppRole is configured, by
     * performing an AppRole login. AppRole tokens are cached for the
     * lifetime of this client and refreshed only when explicitly
     * invalidated via {@link #invalidateCachedToken()}.
     *
     * @return
     *     A non-empty OpenBao auth token.
     *
     * @throws GuacamoleException
     *     If no valid authentication credentials are configured or if
     *     AppRole login fails.
     */
    private String resolveAuthToken() throws GuacamoleException {

        if (configService.isAppRoleConfigured()) {
            String token = cachedAppRoleToken;
            if (token == null || token.isEmpty()) {
                synchronized (this) {
                    token = cachedAppRoleToken;
                    if (token == null || token.isEmpty()) {
                        token = loginWithAppRole();
                        cachedAppRoleToken = token;
                    }
                }
            }
            return token;
        }

        String token = configService.getToken();
        if (token == null || token.trim().isEmpty()) {
            throw new GuacamoleServerException(
                    "OpenBao authentication is not configured. Set "
                    + "\"openbao-token\", or both \"openbao-role-id\" and "
                    + "\"openbao-secret-id\".");
        }

        return token;
    }

    /**
     * Invalidates any cached AppRole token, forcing a fresh login on the
     * next request. Called when a 403 indicates the token has expired or
     * been revoked.
     */
    private synchronized void invalidateCachedToken() {
        cachedAppRoleToken = null;
    }

    /**
     * Performs an AppRole login against OpenBao and returns the issued
     * client token.
     *
     * @return
     *     The client token returned by OpenBao.
     *
     * @throws GuacamoleException
     *     If the login fails or the response cannot be parsed.
     */
    private String loginWithAppRole() throws GuacamoleException {

        String serverUrl = validatedServerUrl();
        String roleId = configService.getRoleId();
        String secretId = configService.getSecretId();
        String approlePath = configService.getAppRolePath();

        String loginUrl = serverUrl + "/v1/auth/" + approlePath + "/login";

        JsonObject payload = new JsonObject();
        payload.addProperty("role_id", roleId);
        payload.addProperty("secret_id", secretId);

        HttpPost httpPost = new HttpPost(loginUrl);
        httpPost.setHeader("Accept", "application/json");
        httpPost.setEntity(new StringEntity(payload.toString(), ContentType.APPLICATION_JSON));
        httpPost.setConfig(buildRequestConfig());

        logger.info("Authenticating to OpenBao using AppRole at {}", loginUrl);

        try (ClassicHttpResponse response = getHttpClient().executeOpen(null, httpPost, null)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            if (statusCode != 200) {
                throw new GuacamoleServerException(
                        "OpenBao AppRole login failed (HTTP " + statusCode + "): "
                        + responseBody);
            }

            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
            if (json == null || !json.has("auth"))
                throw new GuacamoleServerException(
                        "OpenBao AppRole login response missing \"auth\" object.");

            JsonObject auth = json.getAsJsonObject("auth");
            if (!auth.has("client_token"))
                throw new GuacamoleServerException(
                        "OpenBao AppRole login response missing \"auth.client_token\".");

            return auth.get("client_token").getAsString();
        }
        catch (IOException | ParseException | JsonSyntaxException e) {
            throw new GuacamoleServerException(
                    "Failed to communicate with OpenBao during AppRole login", e);
        }
    }

    /**
     * Retrieves the secret at the given path, relative to the configured
     * mount. For a KV v2 engine this resolves to
     * {@code /v1/<mount>/data/<path>}; for KV v1 it resolves to
     * {@code /v1/<mount>/<path>}.
     *
     * @param secretPath
     *     The path (relative to the mount) of the secret to retrieve.
     *
     * @return
     *     The parsed JSON response from OpenBao.
     *
     * @throws GuacamoleException
     *     If the secret cannot be retrieved from OpenBao.
     */
    public JsonObject getSecret(String secretPath) throws GuacamoleException {

        if (secretPath == null || secretPath.trim().isEmpty()) {
            throw new GuacamoleServerException(
                    "OpenBao secret path must not be empty.");
        }

        String serverUrl = validatedServerUrl();
        String mountPath = configService.getMountPath();
        String kvVersion = configService.getKvVersion();

        // Strip any leading slash so URL concatenation stays predictable
        String normalizedPath = secretPath.startsWith("/")
                ? secretPath.substring(1) : secretPath;

        String apiPath;
        if ("2".equals(kvVersion))
            apiPath = String.format("/v1/%s/data/%s", mountPath, normalizedPath);
        else
            apiPath = String.format("/v1/%s/%s", mountPath, normalizedPath);

        String fullUrl = serverUrl + apiPath;
        logger.debug("Fetching secret from OpenBao: {}", fullUrl);

        JsonObject result = executeGet(fullUrl, resolveAuthToken());
        if (result != null)
            return result;

        // A null result from executeGet means we got a 403 with an
        // AppRole token; retry once with a freshly-issued token.
        if (configService.isAppRoleConfigured()) {
            invalidateCachedToken();
            logger.info("OpenBao AppRole token may have expired; retrying with a fresh token.");
            JsonObject retry = executeGet(fullUrl, resolveAuthToken());
            if (retry != null)
                return retry;
        }

        throw new GuacamoleServerException(
                "Permission denied accessing OpenBao. Check token permissions.");
    }

    /**
     * Issues a GET against {@code fullUrl} authenticated with the given
     * token. Returns the parsed JSON response on success, or {@code null}
     * if the response was a 403 (caller may choose to refresh credentials
     * and retry).
     *
     * @param fullUrl
     *     The fully-qualified URL to GET.
     *
     * @param token
     *     The OpenBao auth token to present via {@code X-Vault-Token}.
     *
     * @return
     *     The parsed JSON response, or {@code null} on 403.
     *
     * @throws GuacamoleException
     *     On non-200, non-403 responses or communication failures.
     */
    private JsonObject executeGet(String fullUrl, String token)
            throws GuacamoleException {

        HttpGet httpGet = new HttpGet(fullUrl);
        httpGet.setHeader("X-Vault-Token", token);
        httpGet.setHeader("Accept", "application/json");
        httpGet.setConfig(buildRequestConfig());

        try (ClassicHttpResponse response = getHttpClient().executeOpen(null, httpGet, null)) {
            int statusCode = response.getCode();
            String responseBody = EntityUtils.toString(response.getEntity());

            if (statusCode == 200) {
                logger.debug("OpenBao response 200 for {}", fullUrl);
                return GSON.fromJson(responseBody, JsonObject.class);
            }

            if (statusCode == 404) {
                throw new GuacamoleServerException(
                        "Secret not found in OpenBao at: " + fullUrl);
            }

            if (statusCode == 403) {
                // Signal to caller so it can retry after refreshing auth.
                return null;
            }

            throw new GuacamoleServerException(
                    "OpenBao error (HTTP " + statusCode + "): " + responseBody);
        }
        catch (IOException | ParseException | JsonSyntaxException e) {
            logger.error("Failed to communicate with OpenBao at {}: {}",
                    fullUrl, e.getMessage());
            throw new GuacamoleServerException(
                    "Failed to communicate with OpenBao", e);
        }
    }

    /**
     * Extracts the {@code password} field from an OpenBao secret response.
     *
     * @param response
     *     The JSON response previously returned by {@link #getSecret(String)}.
     *
     * @return
     *     The password string, or null if not present.
     */
    public String extractPassword(JsonObject response) {
        return extractField(response, "password");
    }

    /**
     * Extracts an arbitrary string field from an OpenBao secret response.
     * Supports both KV v2 ({@code data.data.<field>}) and KV v1
     * ({@code data.<field>}) layouts.
     *
     * @param response
     *     The JSON response previously returned by {@link #getSecret(String)}.
     *
     * @param fieldName
     *     The name of the field to extract from the secret's data object.
     *
     * @return
     *     The field value, or null if not present or not a string.
     */
    public String extractField(JsonObject response, String fieldName) {

        if (response == null || fieldName == null)
            return null;

        if (!response.has("data"))
            return null;

        JsonObject data = response.getAsJsonObject("data");

        // KV v2 nests the user-supplied data under an inner "data" object.
        JsonObject values;
        if (data.has("data") && data.get("data").isJsonObject())
            values = data.getAsJsonObject("data");
        else
            values = data;

        if (!values.has(fieldName)) {
            logger.debug("Field \"{}\" not found in OpenBao secret", fieldName);
            return null;
        }

        if (!values.get(fieldName).isJsonPrimitive()) {
            logger.debug("Field \"{}\" in OpenBao secret is not a primitive", fieldName);
            return null;
        }

        return values.get(fieldName).getAsString();
    }
}
