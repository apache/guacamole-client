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

package org.apache.guacamole.auth.saml.conf;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import java.net.URI;
import javax.ws.rs.core.UriBuilder;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.properties.GuacamoleProperty;

/**
 * SAMLConfiguration implementation that is annotated for deserialization by
 * Jackson from YAML ({@code saml-idps.yml}). Shared SP settings omitted from a
 * YAML entry inherit defaults from guacamole.properties (via
 * {@link #setDefaults(SAMLConfiguration)}), whereas IdP-specific endpoints
 * ({@code idp-metadata-url}, {@code idp-url}, {@code logout-endpoint}) are
 * scoped strictly to each entry to avoid cross-IdP leakage.
 */
public class JacksonSAMLConfiguration implements SAMLConfiguration {

    /**
     * The unique URL-safe identifier of this SAML identity provider.
     */
    @JsonProperty("id")
    private String id;

    /**
     * The human-readable display name of this SAML identity provider for the
     * Guacamole login UI. If omitted, {@link #id} is used.
     */
    @JsonProperty("display-name")
    private String displayName;

    /**
     * Whether this SAML identity provider should be treated as the default IdP
     * for automatic redirection and parameterless login/logout endpoints.
     */
    @JsonProperty("default")
    private Boolean defaultProvider;

    /**
     * The raw YAML value of {@link SAMLGuacamoleProperties#SAML_IDP_METADATA_URL}.
     */
    @JsonProperty("idp-metadata-url")
    private String idpMetadataUrl;

    /**
     * The raw YAML value of {@link SAMLGuacamoleProperties#SAML_IDP_URL}.
     */
    @JsonProperty("idp-url")
    private String idpUrl;

    /**
     * The raw YAML value of {@link SAMLGuacamoleProperties#SAML_ENTITY_ID}.
     */
    @JsonProperty("entity-id")
    private String entityId;

    /**
     * The raw YAML value of {@link SAMLGuacamoleProperties#SAML_CALLBACK_URL}.
     */
    @JsonProperty("callback-url")
    private String callbackUrl;

    /**
     * The raw YAML value of {@link SAMLGuacamoleProperties#SAML_ACS_URL}.
     */
    @JsonProperty("acs-url")
    private String acsUrl;

    /**
     * The raw YAML value of {@link SAMLGuacamoleProperties#SAML_DEBUG}.
     */
    @JsonProperty("debug")
    private Boolean debug;

    /**
     * The raw YAML value of {@link SAMLGuacamoleProperties#SAML_COMPRESS_REQUEST}.
     */
    @JsonProperty("compress-request")
    private Boolean compressRequest;

    /**
     * The raw YAML value of {@link SAMLGuacamoleProperties#SAML_COMPRESS_RESPONSE}.
     */
    @JsonProperty("compress-response")
    private Boolean compressResponse;

    /**
     * The raw YAML value of {@link SAMLGuacamoleProperties#SAML_STRICT}.
     */
    @JsonProperty("strict")
    private Boolean strict;

    /**
     * The raw YAML value of {@link SAMLGuacamoleProperties#SAML_GROUP_ATTRIBUTE}.
     */
    @JsonProperty("group-attribute")
    private String groupAttribute;

    /**
     * The raw YAML value of {@link SAMLGuacamoleProperties#SAML_AUTH_TIMEOUT}.
     */
    @JsonProperty("auth-timeout")
    private Integer authTimeout;

    /**
     * The raw YAML value of {@link SAMLGuacamoleProperties#SAML_X509_CERT_PATH}.
     */
    @JsonProperty("x509-cert-path")
    private String x509CertPath;

    /**
     * The raw YAML value of {@link SAMLGuacamoleProperties#SAML_PRIVATE_KEY_PATH}.
     */
    @JsonProperty("private-key-path")
    private String privateKeyPath;

    /**
     * The raw YAML value of {@link SAMLGuacamoleProperties#SAML_LOGOUT_ENDPOINT}.
     */
    @JsonProperty("logout-endpoint")
    private String logoutEndpoint;

    /**
     * The raw YAML value of {@link SAMLGuacamoleProperties#SAML_POST_LOGOUT_REDIRECT_URI}.
     */
    @JsonProperty("post-logout-redirect-uri")
    private String postLogoutRedirectUri;

    /**
     * The default configuration options for shared SP parameters omitted from
     * YAML.
     */
    private SAMLConfiguration defaultConfig = new DefaultSAMLConfiguration();

    /**
     * Supplier of default values for SAML configurations that may throw a
     * {@link GuacamoleException}.
     *
     * @param <T>
     *     The type of value returned by this DefaultSupplier.
     */
    @FunctionalInterface
    private interface DefaultSupplier<T> {

        /**
         * Returns the value supplied by this DefaultSupplier.
         *
         * @return
         *     The supplied default value.
         *
         * @throws GuacamoleException
         *     If an error occurs while producing or retrieving the default
         *     value.
         */
        T get() throws GuacamoleException;

    }

    /**
     * Returns the given value if non-null; otherwise returns the value
     * supplied by {@code defaultValue}.
     *
     * @param <T>
     *     The type of value accepted and returned.
     *
     * @param value
     *     The possibly null value to return if non-null.
     *
     * @param defaultValue
     *     Supplier of the fallback value when {@code value} is null.
     *
     * @return
     *     {@code value} if non-null, otherwise the supplied default value.
     *
     * @throws GuacamoleException
     *     If an error occurs while producing or retrieving the default value.
     */
    private <T> T withDefault(T value, DefaultSupplier<T> defaultValue)
            throws GuacamoleException {
        return value != null ? value : defaultValue.get();
    }

    /**
     * Parses and returns the given raw string value using the specified
     * {@link GuacamoleProperty} if non-null; otherwise returns the value
     * supplied by {@code defaultValue}.
     *
     * @param <T>
     *     The type of value accepted and returned.
     *
     * @param property
     *     The GuacamoleProperty used to parse {@code value}.
     *
     * @param value
     *     The possibly null string value to parse.
     *
     * @param defaultValue
     *     Supplier of the fallback value when {@code value} is null.
     *
     * @return
     *     The parsed value if non-null, otherwise the supplied default value.
     *
     * @throws GuacamoleException
     *     If the value cannot be parsed or an error occurs retrieving the
     *     default value.
     */
    private <T> T withDefault(GuacamoleProperty<T> property, String value,
            DefaultSupplier<T> defaultValue) throws GuacamoleException {
        return withDefault(property.parseValue(value), defaultValue);
    }

    /**
     * Sets the SAMLConfiguration used for default values of shared SP options
     * omitted from this YAML entry. If not set, an instance of
     * {@link DefaultSAMLConfiguration} will be used.
     *
     * @param defaultConfig
     *     The SAMLConfiguration providing default values for omitted SP
     *     options.
     */
    public void setDefaults(SAMLConfiguration defaultConfig) {
        this.defaultConfig = defaultConfig;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        if (displayName != null && !displayName.trim().isEmpty())
            return displayName.trim();
        return id;
    }

    @Override
    public boolean isDefault() {
        return Boolean.TRUE.equals(defaultProvider);
    }

    @Override
    public URI getIdpMetadata() throws GuacamoleException {
        return SAMLGuacamoleProperties.SAML_IDP_METADATA_URL.parseValue(idpMetadataUrl);
    }

    @Override
    public URI getIdpUrl() throws GuacamoleException {
        return SAMLGuacamoleProperties.SAML_IDP_URL.parseValue(idpUrl);
    }

    @Override
    public URI getEntityId() throws GuacamoleException {
        return withDefault(SAMLGuacamoleProperties.SAML_ENTITY_ID,
                entityId, defaultConfig::getEntityId);
    }

    @Override
    public URI getCallbackUrl() throws GuacamoleException {
        return withDefault(SAMLGuacamoleProperties.SAML_CALLBACK_URL,
                callbackUrl, defaultConfig::getCallbackUrl);
    }

    @Override
    public URI getAssertionConsumerServiceUrl() throws GuacamoleException {
        URI explicitAcsUrl = SAMLGuacamoleProperties.SAML_ACS_URL.parseValue(acsUrl);
        if (explicitAcsUrl != null)
            return explicitAcsUrl;

        return UriBuilder.fromUri(getCallbackUrl())
                .path("api/ext/saml/callback")
                .path(getId())
                .build();
    }

    @Override
    public boolean getDebug() throws GuacamoleException {
        return withDefault(debug, defaultConfig::getDebug);
    }

    @Override
    public boolean getCompressRequest() throws GuacamoleException {
        return withDefault(compressRequest, defaultConfig::getCompressRequest);
    }

    @Override
    public boolean getCompressResponse() throws GuacamoleException {
        return withDefault(compressResponse, defaultConfig::getCompressResponse);
    }

    @Override
    public boolean getStrict() throws GuacamoleException {
        return withDefault(strict, defaultConfig::getStrict);
    }

    @Override
    public String getGroupAttribute() throws GuacamoleException {
        return withDefault(groupAttribute, defaultConfig::getGroupAttribute);
    }

    @Override
    public int getAuthenticationTimeout() throws GuacamoleException {
        return withDefault(authTimeout, defaultConfig::getAuthenticationTimeout);
    }

    @Override
    public File getCertificateFile() throws GuacamoleException {
        return withDefault(SAMLGuacamoleProperties.SAML_X509_CERT_PATH,
                x509CertPath, defaultConfig::getCertificateFile);
    }

    @Override
    public File getPrivateKeyFile() throws GuacamoleException {
        return withDefault(SAMLGuacamoleProperties.SAML_PRIVATE_KEY_PATH,
                privateKeyPath, defaultConfig::getPrivateKeyFile);
    }

    @Override
    public URI getLogoutEndpoint() throws GuacamoleException {
        return SAMLGuacamoleProperties.SAML_LOGOUT_ENDPOINT.parseValue(logoutEndpoint);
    }

    @Override
    public URI getPostLogoutRedirectURI() throws GuacamoleException {
        return withDefault(SAMLGuacamoleProperties.SAML_POST_LOGOUT_REDIRECT_URI,
                postLogoutRedirectUri, defaultConfig::getPostLogoutRedirectURI);
    }

}
