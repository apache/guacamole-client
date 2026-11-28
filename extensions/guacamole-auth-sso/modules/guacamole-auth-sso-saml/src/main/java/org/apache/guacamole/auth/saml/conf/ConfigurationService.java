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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.onelogin.saml2.settings.IdPMetadataParser;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.settings.SettingsBuilder;
import com.onelogin.saml2.util.Constants;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.environment.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for retrieving configuration information regarding the SAML
 * authentication module, supporting both single-IdP configuration via
 * {@code guacamole.properties} and multi-IdP configuration via
 * {@code GUACAMOLE_HOME/saml-idps.yml}.
 */
@Singleton
public class ConfigurationService {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(ConfigurationService.class);

    /**
     * The name of the file within GUACAMOLE_HOME that defines multiple SAML
     * identity providers (if not configuring a single IdP via
     * guacamole.properties).
     */
    public static final String SAML_IDPS_YML = "saml-idps.yml";

    /**
     * Pattern defining valid URL-safe SAML identity provider identifiers.
     * Identifiers must begin with an alphanumeric character and contain only
     * ASCII letters, digits, dots, underscores, or hyphens.
     */
    private static final Pattern VALID_IDP_ID_PATTERN =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]*$");

    /**
     * ObjectMapper for deserializing YAML IdP definitions.
     */
    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    /**
     * Monitor lock synchronizing reloads of {@link #SAML_IDPS_YML}.
     */
    private final Object reloadLock = new Object();

    /**
     * Immutable snapshot of configurations parsed from {@link #SAML_IDPS_YML}.
     * Null when {@link #SAML_IDPS_YML} does not exist or is empty.
     */
    private volatile ConfigurationSnapshot cachedSnapshot = null;

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * Immutable snapshot of SAML identity provider configurations loaded from
     * {@link #SAML_IDPS_YML}.
     */
    private static final class ConfigurationSnapshot {

        /**
         * The {@link File#lastModified()} timestamp when this snapshot was
         * loaded.
         */
        private final long lastModified;

        /**
         * Ordered map of identity provider IDs to their configurations.
         */
        private final Map<String, SAMLConfiguration> configurationsById;

        /**
         * The default identity provider configuration, or null if multiple
         * identity providers are configured with none marked as default.
         */
        private final SAMLConfiguration defaultConfiguration;

        /**
         * Creates a new immutable ConfigurationSnapshot.
         *
         * @param lastModified
         *     The lastModified timestamp of the YAML file.
         *
         * @param configurationsById
         *     The ordered map of IdP IDs to SAMLConfiguration instances.
         *
         * @param defaultConfiguration
         *     The default SAMLConfiguration, or null if none is designated.
         */
        private ConfigurationSnapshot(long lastModified,
                Map<String, SAMLConfiguration> configurationsById,
                SAMLConfiguration defaultConfiguration) {
            this.lastModified = lastModified;
            this.configurationsById = Collections.unmodifiableMap(configurationsById);
            this.defaultConfiguration = defaultConfiguration;
        }

    }

    /**
     * Validates and builds an immutable {@link ConfigurationSnapshot} from the
     * given list of YAML-deserialized IdP configurations.
     *
     * @param lastModified
     *     The lastModified timestamp of the YAML configuration file.
     *
     * @param configs
     *     The list of deserialized configurations from {@link #SAML_IDPS_YML}.
     *
     * @param yamlFile
     *     The YAML file from which the configurations were loaded, used for
     *     error messages.
     *
     * @return
     *     A validated, immutable {@link ConfigurationSnapshot}.
     *
     * @throws GuacamoleException
     *     If any configuration entry is missing an ID, has an invalid or
     *     duplicate ID, or if more than one entry is marked as default.
     */
    private ConfigurationSnapshot buildSnapshot(long lastModified,
            List<JacksonSAMLConfiguration> configs, File yamlFile)
            throws GuacamoleException {

        SAMLConfiguration envDefaults = new EnvironmentSAMLConfiguration(environment);
        Map<String, SAMLConfiguration> byId = new LinkedHashMap<>();
        SAMLConfiguration explicitDefault = null;

        for (JacksonSAMLConfiguration config : configs) {

            if (config == null)
                throw new GuacamoleServerException("SAML identity provider "
                        + "configuration in \"" + yamlFile + "\" contains a "
                        + "null entry.");

            String id = config.getId();
            if (id == null || !VALID_IDP_ID_PATTERN.matcher(id).matches() || id.contains(".."))
                throw new GuacamoleServerException("Invalid or missing SAML "
                        + "identity provider ID \"" + id + "\" in \"" + yamlFile
                        + "\". IDs must start with an alphanumeric character and "
                        + "contain only letters, digits, '.', '_', or '-'.");

            if (byId.containsKey(id))
                throw new GuacamoleServerException("Duplicate SAML identity "
                        + "provider ID \"" + id + "\" in \"" + yamlFile + "\".");

            config.setDefaults(envDefaults);
            byId.put(id, config);

            if (config.isDefault()) {
                if (explicitDefault != null)
                    throw new GuacamoleServerException("Multiple SAML identity "
                            + "providers are marked as default in \""
                            + yamlFile + "\".");
                explicitDefault = config;
            }

        }

        // If only a single IdP is defined in YAML, treat it as the default
        // automatically so single-entry YAML files behave intuitively
        SAMLConfiguration effectiveDefault = explicitDefault;
        if (effectiveDefault == null && byId.size() == 1)
            effectiveDefault = byId.values().iterator().next();

        return new ConfigurationSnapshot(lastModified, byId, effectiveDefault);

    }

    /**
     * Loads or refreshes the {@link ConfigurationSnapshot} from
     * {@code GUACAMOLE_HOME/saml-idps.yml}, if that file exists and contains
     * one or more identity provider definitions. Returns null when
     * {@code saml-idps.yml} does not exist or is empty, indicating that
     * {@code guacamole.properties} should be used directly.
     *
     * @return
     *     The current {@link ConfigurationSnapshot} loaded from
     *     {@code saml-idps.yml}, or null if {@code guacamole.properties}
     *     should be used instead.
     *
     * @throws GuacamoleException
     *     If {@code saml-idps.yml} exists but cannot be read, parsed, or
     *     validated.
     */
    private ConfigurationSnapshot getYamlSnapshot() throws GuacamoleException {

        File samlIdps = new File(environment.getGuacamoleHome(), SAML_IDPS_YML);
        if (!samlIdps.exists()) {
            if (cachedSnapshot != null) {
                synchronized (reloadLock) {
                    if (cachedSnapshot != null && !samlIdps.exists()) {
                        logger.debug("Clearing cached SAML configuration from \"{}\" (file no longer exists).", samlIdps);
                        cachedSnapshot = null;
                    }
                }
            }
            return null;
        }

        long currentLastModified = samlIdps.lastModified();
        ConfigurationSnapshot snapshot = cachedSnapshot;
        if (snapshot != null && snapshot.lastModified == currentLastModified)
            return snapshot;

        synchronized (reloadLock) {

            // Re-check inside lock to avoid redundant reloads
            if (!samlIdps.exists()) {
                cachedSnapshot = null;
                return null;
            }

            currentLastModified = samlIdps.lastModified();
            snapshot = cachedSnapshot;
            if (snapshot != null && snapshot.lastModified == currentLastModified)
                return snapshot;

            try {
                logger.debug("Reading updated SAML configuration from \"{}\"...", samlIdps);
                JsonNode rootNode = mapper.readTree(samlIdps);

                if (rootNode == null || rootNode.isNull() || rootNode.isMissingNode()
                        || (rootNode.isArray() && rootNode.size() == 0)) {
                    logger.debug("Using only guacamole.properties for SAML "
                            + "identity provider configuration as \"{}\" is "
                            + "empty.", samlIdps);
                    cachedSnapshot = null;
                    return null;
                }

                if (!rootNode.isArray())
                    throw new GuacamoleServerException("SAML identity provider "
                            + "configuration file \"" + samlIdps + "\" must "
                            + "contain a YAML list of identity provider entries.");

                List<JacksonSAMLConfiguration> configs = mapper.convertValue(
                        rootNode, new TypeReference<List<JacksonSAMLConfiguration>>() {});

                ConfigurationSnapshot updated = buildSnapshot(
                        currentLastModified, configs, samlIdps);
                cachedSnapshot = updated;
                return updated;
            }
            catch (IOException | IllegalArgumentException e) {
                logger.error("\"{}\" could not be read/parsed: {}", samlIdps, e.getMessage(), e);
                throw new GuacamoleServerException("SAML identity provider "
                        + "configuration file \"" + samlIdps + "\" could not "
                        + "be read/parsed: " + e.getMessage(), e);
            }

        }

    }

    /**
     * Returns whether SAML identity providers are currently configured via
     * {@link #SAML_IDPS_YML} rather than solely via {@code guacamole.properties}.
     *
     * @return
     *     true if {@link #SAML_IDPS_YML} is active, false if using
     *     {@code guacamole.properties} only.
     *
     * @throws GuacamoleException
     *     If {@link #SAML_IDPS_YML} exists but cannot be read or validated.
     */
    public boolean isMultiIdpConfigured() throws GuacamoleException {
        return getYamlSnapshot() != null;
    }

    /**
     * Returns all configured SAML identity providers in declaration order.
     * When {@link #SAML_IDPS_YML} is not used, a singleton collection
     * representing the configuration from {@code guacamole.properties} is
     * returned.
     *
     * @return
     *     An unmodifiable collection of all configured SAML identity providers.
     *
     * @throws GuacamoleException
     *     If the SAML configuration cannot be read or validated.
     */
    public Collection<SAMLConfiguration> getConfigurations() throws GuacamoleException {
        ConfigurationSnapshot snapshot = getYamlSnapshot();
        if (snapshot != null)
            return snapshot.configurationsById.values();

        return Collections.<SAMLConfiguration>singletonList(
                new EnvironmentSAMLConfiguration(environment));
    }

    /**
     * Returns the configuration of the SAML identity provider with the given
     * identifier, or null if no such identity provider is configured.
     *
     * @param idpId
     *     The unique identifier of the SAML identity provider.
     *
     * @return
     *     The matching {@link SAMLConfiguration}, or null if no identity
     *     provider with the given ID exists.
     *
     * @throws GuacamoleException
     *     If the SAML configuration cannot be read or validated.
     */
    public SAMLConfiguration getConfiguration(String idpId) throws GuacamoleException {

        if (idpId == null)
            return null;

        ConfigurationSnapshot snapshot = getYamlSnapshot();
        if (snapshot != null)
            return snapshot.configurationsById.get(idpId);

        if (SAMLConfiguration.DEFAULT_ID.equals(idpId))
            return new EnvironmentSAMLConfiguration(environment);

        return null;

    }

    /**
     * Returns the configuration of the SAML identity provider with the given
     * identifier, throwing a {@link GuacamoleResourceNotFoundException} if no
     * such identity provider is configured.
     *
     * @param idpId
     *     The unique identifier of the SAML identity provider.
     *
     * @return
     *     The matching {@link SAMLConfiguration}.
     *
     * @throws GuacamoleException
     *     If the SAML configuration cannot be read/validated, or if no
     *     identity provider with the given ID exists.
     */
    public SAMLConfiguration getRequiredConfiguration(String idpId)
            throws GuacamoleException {

        SAMLConfiguration config = getConfiguration(idpId);
        if (config == null)
            throw new GuacamoleResourceNotFoundException("No SAML identity "
                    + "provider configured with ID \"" + idpId + "\".");

        return config;

    }

    /**
     * Returns the default SAML identity provider configuration, if one exists.
     * When configured via {@code guacamole.properties}, or when only a single
     * IdP is defined in {@link #SAML_IDPS_YML}, that IdP is returned. When
     * multiple IdPs are defined in {@link #SAML_IDPS_YML}, the entry marked
     * {@code default: true} is returned, or null if no default is designated.
     *
     * @return
     *     The default {@link SAMLConfiguration}, or null if multiple IdPs are
     *     configured without an explicit default.
     *
     * @throws GuacamoleException
     *     If the SAML configuration cannot be read or validated.
     */
    public SAMLConfiguration getDefaultConfiguration() throws GuacamoleException {
        ConfigurationSnapshot snapshot = getYamlSnapshot();
        if (snapshot != null)
            return snapshot.defaultConfiguration;

        return new EnvironmentSAMLConfiguration(environment);
    }

    /**
     * Returns the default SAML identity provider configuration, throwing a
     * {@link GuacamoleResourceNotFoundException} if multiple IdPs are
     * configured with no default designated.
     *
     * @return
     *     The default {@link SAMLConfiguration}.
     *
     * @throws GuacamoleException
     *     If the SAML configuration cannot be read/validated, or if no default
     *     identity provider is configured.
     */
    public SAMLConfiguration getRequiredDefaultConfiguration()
            throws GuacamoleException {

        SAMLConfiguration config = getDefaultConfiguration();
        if (config == null)
            throw new GuacamoleResourceNotFoundException("No default SAML "
                    + "identity provider is configured.");

        return config;

    }

    /**
     * Returns a fallback Guacamole base callback URL suitable for error
     * redirects when an incoming SAML callback cannot be matched to a specific
     * identity provider. Checks the default IdP configuration first, then
     * {@code saml-callback-url} in {@code guacamole.properties}, then the
     * first configured IdP.
     *
     * @return
     *     A fallback Guacamole base callback URL.
     *
     * @throws GuacamoleException
     *     If no callback URL can be determined from configuration.
     */
    public URI getFallbackCallbackUrl() throws GuacamoleException {

        SAMLConfiguration defaultConfig = getDefaultConfiguration();
        if (defaultConfig != null)
            return defaultConfig.getCallbackUrl();

        URI envCallback = environment.getProperty(SAMLGuacamoleProperties.SAML_CALLBACK_URL);
        if (envCallback != null)
            return envCallback;

        Collection<SAMLConfiguration> configs = getConfigurations();
        if (!configs.isEmpty())
            return configs.iterator().next().getCallbackUrl();

        return environment.getRequiredProperty(SAMLGuacamoleProperties.SAML_CALLBACK_URL);

    }

    /**
     * The callback URL used for the SAML IdP to POST a response
     * to upon completion of authentication, normally the base
     * of the Guacamole install.
     *
     * @return
     *     The callback URL to be sent to the SAML IdP that will
     *     be POSTed to upon completion of SAML authentication.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or the property
     *     is missing.
     */
    public URI getCallbackUrl() throws GuacamoleException {
        return getFallbackCallbackUrl();
    }

    /**
     * Return the name of the attribute that will be supplied by the default
     * identity provider that contains the groups of which this user is a member.
     *
     * @return
     *     The name of the attribute that contains the user groups.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public String getGroupAttribute() throws GuacamoleException {
        SAMLConfiguration config = getDefaultConfiguration();
        if (config != null)
            return config.getGroupAttribute();
        return new EnvironmentSAMLConfiguration(environment).getGroupAttribute();
    }

    /**
     * Returns the maximum amount of time to allow for an in-progress SAML
     * authentication attempt to be completed, in minutes.
     *
     * @return
     *     The maximum amount of time to allow for an in-progress SAML
     *     authentication attempt to be completed, in minutes.
     *
     * @throws GuacamoleException
     *     If the authentication timeout cannot be parsed.
     */
    public int getAuthenticationTimeout() throws GuacamoleException {
        SAMLConfiguration config = getDefaultConfiguration();
        if (config != null)
            return config.getAuthenticationTimeout();
        return new EnvironmentSAMLConfiguration(environment).getAuthenticationTimeout();
    }

    /**
     * Returns the file containing the X.509 certificate to use when signing
     * requests to the default SAML IdP.
     *
     * @return
     *     The file containing the X.509 certificate, or null if not defined.
     *
     * @throws GuacamoleException
     *     If the X.509 certificate cannot be parsed.
     */
    public File getCertificateFile() throws GuacamoleException {
        SAMLConfiguration config = getDefaultConfiguration();
        if (config != null)
            return config.getCertificateFile();
        return new EnvironmentSAMLConfiguration(environment).getCertificateFile();
    }

    /**
     * Returns the file containing the private key to use when signing
     * requests to the default SAML IdP.
     *
     * @return
     *     The file containing the private key, or null if not defined.
     *
     * @throws GuacamoleException
     *     If the private key file cannot be parsed.
     */
    public File getPrivateKeyFile() throws GuacamoleException {
        SAMLConfiguration config = getDefaultConfiguration();
        if (config != null)
            return config.getPrivateKeyFile();
        return new EnvironmentSAMLConfiguration(environment).getPrivateKeyFile();
    }

    /**
     * Returns the logout endpoint (URI) of the default SAML IdP, or null if
     * not configured or no default IdP exists.
     *
     * @return
     *     The logout endpoint of the default SAML IdP, or null if not
     *     configured.
     *
     * @throws GuacamoleException
     *     If configuration cannot be parsed.
     */
    public URI getLogoutEndpoint() throws GuacamoleException {
        SAMLConfiguration config = getDefaultConfiguration();
        return config != null ? config.getLogoutEndpoint() : null;
    }

    /**
     * Returns the effective URI to redirect to after logout completes for the
     * given SAML identity provider configuration. If no post-logout redirect
     * URI is explicitly configured, the identity provider's callback URL is
     * returned.
     *
     * @param config
     *     The SAML identity provider configuration.
     *
     * @return
     *     The effective post-logout redirect URI.
     *
     * @throws GuacamoleException
     *     If configuration cannot be parsed.
     */
    public URI getPostLogoutRedirectURI(SAMLConfiguration config)
            throws GuacamoleException {
        URI postLogoutUri = config.getPostLogoutRedirectURI();
        if (postLogoutUri != null)
            return postLogoutUri;
        return config.getCallbackUrl();
    }

    /**
     * Returns the URI to redirect to after logout is complete for the default
     * SAML IdP. If not configured, the callback URL will be used.
     *
     * @return
     *     The post-logout redirect URI.
     *
     * @throws GuacamoleException
     *     If configuration cannot be parsed.
     */
    public URI getPostLogoutRedirectURI() throws GuacamoleException {
        SAMLConfiguration config = getDefaultConfiguration();
        if (config != null)
            return getPostLogoutRedirectURI(config);
        return getFallbackCallbackUrl();
    }

    /**
     * Returns the contents of a small file, such as a private key or
     * certificate, as a UTF-8 String. If the file does not exist or cannot be
     * read for any reason, an exception is thrown with the details of the
     * failure.
     *
     * @param file
     *     The file to read into a string.
     *
     * @param name
     *     A human-readable name for the file, to be used when formatting error
     *     messages.
     *
     * @return
     *     The contents of the file having the given path.
     *
     * @throws GuacamoleException
     *     If the provided file does not exist, or cannot be read for any reason.
     */
    private String readFileContentsIntoString(File file, String name) throws GuacamoleException {

        // Attempt to read the file directly into a String
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        }
        catch (IOException e) {
            throw new GuacamoleServerException(
                    name + " at \"" + file.getAbsolutePath() + "\" could not be read.", e);
        }

    }

    /**
     * Returns the collection of OneLogin SAML settings used to initialize the
     * SAML client for the given identity provider configuration.
     *
     * @param config
     *     The configuration of the SAML identity provider.
     *
     * @return
     *     The collection of SAML settings for the given identity provider.
     *
     * @throws GuacamoleException
     *     If configuration cannot be parsed, metadata XML cannot be loaded, or
     *     required parameters are missing.
     */
    public Saml2Settings getSamlSettings(SAMLConfiguration config)
            throws GuacamoleException {

        // Try to load XML metadata first
        URI idpMetadata = config.getIdpMetadata();
        Map<String, Object> samlMap;
        if (idpMetadata != null) {
            try {
                samlMap = IdPMetadataParser.parseRemoteXML(idpMetadata.toURL());
            }
            catch (Exception e) {
                throw new GuacamoleServerException(
                        "Could not parse SAML IdP Metadata file for identity "
                        + "provider \"" + config.getId() + "\".", e);
            }
        }

        // If no XML metadata is provided, fall back to individual values
        else {
            URI idpUrl = config.getIdpUrl();
            if (idpUrl == null)
                throw new GuacamoleServerException("Either SAML IdP metadata "
                        + "URL or SAML IdP URL must be configured for identity "
                        + "provider \"" + config.getId() + "\".");

            samlMap = new HashMap<>();
            samlMap.put(SettingsBuilder.IDP_ENTITYID_PROPERTY_KEY,
                    idpUrl.toString());
            samlMap.put(SettingsBuilder.IDP_SINGLE_SIGN_ON_SERVICE_URL_PROPERTY_KEY,
                    idpUrl.toString());
            samlMap.put(SettingsBuilder.IDP_SINGLE_SIGN_ON_SERVICE_BINDING_PROPERTY_KEY,
                    Constants.BINDING_HTTP_REDIRECT);
        }

        // Read entity ID from configuration if not provided within metadata XML
        if (!samlMap.containsKey(SettingsBuilder.SP_ENTITYID_PROPERTY_KEY)) {
            URI entityId = config.getEntityId();
            if (entityId == null)
                throw new GuacamoleServerException("SAML Entity ID was not found "
                        + "in either the metadata XML file or configuration for "
                        + "identity provider \"" + config.getId() + "\".");
            samlMap.put(SettingsBuilder.SP_ENTITYID_PROPERTY_KEY, entityId.toString());
        }

        // Derive ACS URL from configuration if not provided within metadata XML
        if (!samlMap.containsKey(SettingsBuilder.SP_ASSERTION_CONSUMER_SERVICE_URL_PROPERTY_KEY)) {
            samlMap.put(SettingsBuilder.SP_ASSERTION_CONSUMER_SERVICE_URL_PROPERTY_KEY,
                    config.getAssertionConsumerServiceUrl().toString());
        }

        // If a private key file is set, load the value into the builder now
        File privateKeyFile = config.getPrivateKeyFile();
        if (privateKeyFile != null)
            samlMap.put(SettingsBuilder.SP_PRIVATEKEY_PROPERTY_KEY,
                    readFileContentsIntoString(privateKeyFile, "Private Key"));

        // If a certificate file is set, load the value into the builder now
        File certificateFile = config.getCertificateFile();
        if (certificateFile != null)
            samlMap.put(SettingsBuilder.SP_X509CERT_PROPERTY_KEY,
                    readFileContentsIntoString(certificateFile, "X.509 Certificate"));

        SettingsBuilder samlBuilder = new SettingsBuilder();
        Saml2Settings samlSettings = samlBuilder.fromValues(samlMap).build();
        samlSettings.setStrict(config.getStrict());
        samlSettings.setDebug(config.getDebug());
        samlSettings.setCompressRequest(config.getCompressRequest());
        samlSettings.setCompressResponse(config.getCompressResponse());

        // Request that the SAML library sign everything that it can, if
        // both private key and certificate are specified
        if (privateKeyFile != null && certificateFile != null) {
            samlSettings.setAuthnRequestsSigned(true);
            samlSettings.setLogoutRequestSigned(true);
            samlSettings.setLogoutResponseSigned(true);
        }

        return samlSettings;

    }

    /**
     * Returns the collection of SAML settings used to initialize the client
     * for the default SAML identity provider.
     *
     * @return
     *     The collection of SAML settings used to initialize the SAML client.
     *
     * @throws GuacamoleException
     *     If configuration cannot be parsed, required parameters are missing,
     *     or no default identity provider is configured.
     */
    public Saml2Settings getSamlSettings() throws GuacamoleException {
        return getSamlSettings(getRequiredDefaultConfiguration());
    }

}
