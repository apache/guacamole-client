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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.onelogin.saml2.settings.Saml2Settings;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.saml.TestEnvironment;
import org.apache.guacamole.auth.saml.TestJaxRsRuntimeDelegate;
import org.apache.guacamole.environment.Environment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ConfigurationService} covering backward-compatible
 * single-IdP configuration, multi-IdP YAML loading, inheritance, validation,
 * and hot-reloading.
 */
public class ConfigurationServiceTest {

    /**
     * Installs the test JAX-RS RuntimeDelegate before running tests.
     */
    @BeforeAll
    public static void setupJaxRs() {
        TestJaxRsRuntimeDelegate.install();
    }

    /**
     * Creates a Guice-wired {@link ConfigurationService} using the given
     * {@link TestEnvironment}.
     *
     * @param env
     *     The test environment to bind.
     *
     * @return
     *     A configured ConfigurationService instance.
     */
    private ConfigurationService createConfigurationService(final TestEnvironment env) {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Environment.class).toInstance(env);
                bind(ConfigurationService.class);
            }
        });
        return injector.getInstance(ConfigurationService.class);
    }

    /**
     * Writes UTF-8 content to a file inside the given directory.
     *
     * @param dir
     *     The target directory.
     *
     * @param filename
     *     The name of the file to write.
     *
     * @param content
     *     The UTF-8 text content.
     *
     * @return
     *     The written file.
     *
     * @throws IOException
     *     If writing fails.
     */
    private File writeFile(Path dir, String filename, String content) throws IOException {
        File file = dir.resolve(filename).toFile();
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
        return file;
    }

    /**
     * Verifies full backward compatibility when {@code saml-idps.yml} does not
     * exist and only {@code guacamole.properties} is used.
     *
     * @param tempDir
     *     Temporary GUACAMOLE_HOME directory.
     *
     * @throws Exception
     *     If an unexpected error occurs.
     */
    @Test
    public void testLegacySingleIdpProperties(@TempDir Path tempDir) throws Exception {

        TestEnvironment env = new TestEnvironment(tempDir.toFile())
                .setProperty("saml-idp-url", "https://idp.example.com/sso")
                .setProperty("saml-entity-id", "https://guac.example.com/sp")
                .setProperty("saml-callback-url", "https://guac.example.com")
                .setProperty("saml-group-attribute", "memberOf")
                .setProperty("saml-auth-timeout", "10")
                .setProperty("saml-strict", "false")
                .setProperty("saml-debug", "true")
                .setProperty("saml-logout-endpoint", "https://idp.example.com/slo");

        ConfigurationService conf = createConfigurationService(env);

        assertFalse(conf.isMultiIdpConfigured());
        assertEquals(1, conf.getConfigurations().size());

        SAMLConfiguration defaultConfig = conf.getDefaultConfiguration();
        assertNotNull(defaultConfig);
        assertEquals(SAMLConfiguration.DEFAULT_ID, defaultConfig.getId());
        assertNull(defaultConfig.getDisplayName());
        assertEquals(URI.create("https://guac.example.com/api/ext/saml/callback"),
                defaultConfig.getAssertionConsumerServiceUrl());
        assertEquals("memberOf", conf.getGroupAttribute());
        assertEquals(10, conf.getAuthenticationTimeout());
        assertEquals(URI.create("https://idp.example.com/slo"), conf.getLogoutEndpoint());
        assertEquals(URI.create("https://guac.example.com"), conf.getPostLogoutRedirectURI());

        Saml2Settings settings = conf.getSamlSettings();
        assertEquals("https://idp.example.com/sso", settings.getIdpEntityId());
        assertEquals("https://idp.example.com/sso", settings.getIdpSingleSignOnServiceUrl().toString());
        assertEquals("https://guac.example.com/sp", settings.getSpEntityId());
        assertEquals("https://guac.example.com/api/ext/saml/callback",
                settings.getSpAssertionConsumerServiceUrl().toString());
        assertFalse(settings.isStrict());
        assertTrue(settings.isDebugActive());

    }

    /**
     * Verifies multi-IdP YAML parsing, declaration order preservation, shared
     * SP default inheritance from {@code guacamole.properties}, per-IdP
     * overrides, and isolation of IdP-specific endpoints.
     *
     * @param tempDir
     *     Temporary GUACAMOLE_HOME directory.
     *
     * @throws Exception
     *     If an unexpected error occurs.
     */
    @Test
    public void testMultiIdpYamlWithInheritanceAndIsolation(@TempDir Path tempDir) throws Exception {

        TestEnvironment env = new TestEnvironment(tempDir.toFile())
                .setProperty("saml-callback-url", "https://guac.example.com")
                .setProperty("saml-entity-id", "https://guac.example.com/default-sp")
                .setProperty("saml-group-attribute", "groups")
                .setProperty("saml-auth-timeout", "8")
                .setProperty("saml-strict", "true")
                // IdP-specific settings in guacamole.properties must NOT leak into YAML IdPs
                .setProperty("saml-idp-metadata-url", "https://legacy-idp.example.com/metadata")
                .setProperty("saml-logout-endpoint", "https://legacy-idp.example.com/slo");

        String yaml =
                "- id: okta\n"
                + "  display-name: Okta Corporate\n"
                + "  idp-url: https://corp.okta.com/sso\n"
                + "  default: true\n"
                + "  logout-endpoint: https://corp.okta.com/slo\n"
                + "\n"
                + "- id: entra-id\n"
                + "  display-name: Microsoft Entra ID\n"
                + "  idp-url: https://login.microsoftonline.com/tenant/saml2\n"
                + "  entity-id: https://guac.example.com/sp/entra\n"
                + "  group-attribute: http://schemas.microsoft.com/ws/2008/06/identity/claims/groups\n"
                + "  acs-url: https://guac.example.com/api/ext/saml/callback\n"
                + "  strict: false\n"
                + "  auth-timeout: 15\n"
                + "\n"
                + "- id: partner.idp\n"
                + "  idp-url: https://partner.example.org/saml/sso\n"
                + "  callback-url: https://partner-guac.example.com\n";

        writeFile(tempDir, ConfigurationService.SAML_IDPS_YML, yaml);

        ConfigurationService conf = createConfigurationService(env);
        assertTrue(conf.isMultiIdpConfigured());

        List<SAMLConfiguration> list = new ArrayList<>(conf.getConfigurations());
        assertEquals(3, list.size());
        assertEquals("okta", list.get(0).getId());
        assertEquals("entra-id", list.get(1).getId());
        assertEquals("partner.idp", list.get(2).getId());

        // Default IdP is okta
        SAMLConfiguration defaultCfg = conf.getDefaultConfiguration();
        assertNotNull(defaultCfg);
        assertEquals("okta", defaultCfg.getId());

        // Okta inherits shared SP settings from guacamole.properties
        SAMLConfiguration okta = conf.getRequiredConfiguration("okta");
        assertEquals("Okta Corporate", okta.getDisplayName());
        assertNull(okta.getIdpMetadata(), "Legacy saml-idp-metadata-url must not leak into YAML entries");
        assertEquals(URI.create("https://corp.okta.com/sso"), okta.getIdpUrl());
        assertEquals(URI.create("https://guac.example.com/default-sp"), okta.getEntityId());
        assertEquals(URI.create("https://guac.example.com/api/ext/saml/callback/okta"),
                okta.getAssertionConsumerServiceUrl());
        assertEquals("groups", okta.getGroupAttribute());
        assertEquals(8, okta.getAuthenticationTimeout());
        assertTrue(okta.getStrict());
        assertEquals(URI.create("https://corp.okta.com/slo"), okta.getLogoutEndpoint());

        // Entra ID overrides SP settings and does NOT inherit legacy logout-endpoint
        SAMLConfiguration entra = conf.getRequiredConfiguration("entra-id");
        assertEquals("Microsoft Entra ID", entra.getDisplayName());
        assertEquals(URI.create("https://guac.example.com/sp/entra"), entra.getEntityId());
        assertEquals("http://schemas.microsoft.com/ws/2008/06/identity/claims/groups",
                entra.getGroupAttribute());
        assertEquals(URI.create("https://guac.example.com/api/ext/saml/callback"),
                entra.getAssertionConsumerServiceUrl());
        assertFalse(entra.getStrict());
        assertEquals(15, entra.getAuthenticationTimeout());
        assertNull(entra.getLogoutEndpoint(), "Legacy saml-logout-endpoint must not leak into YAML entries");

        // Partner omits display-name (falls back to id) and overrides callback-url
        SAMLConfiguration partner = conf.getRequiredConfiguration("partner.idp");
        assertEquals("partner.idp", partner.getDisplayName());
        assertEquals(URI.create("https://partner-guac.example.com"), partner.getCallbackUrl());
        assertEquals(URI.create("https://partner-guac.example.com/api/ext/saml/callback/partner.idp"),
                partner.getAssertionConsumerServiceUrl());
        assertEquals(URI.create("https://partner-guac.example.com"),
                conf.getPostLogoutRedirectURI(partner));

        // Unknown IdP lookup throws 404 exception
        assertNull(conf.getConfiguration("nonexistent"));
        assertThrows(GuacamoleResourceNotFoundException.class,
                () -> conf.getRequiredConfiguration("nonexistent"));

    }

    /**
     * Verifies default IdP resolution when multiple IdPs are configured with
     * no {@code default: true}, versus when only one IdP is defined in YAML.
     *
     * @param tempDir
     *     Temporary GUACAMOLE_HOME directory.
     *
     * @throws Exception
     *     If an unexpected error occurs.
     */
    @Test
    public void testDefaultIdpResolution(@TempDir Path tempDir) throws Exception {

        TestEnvironment env = new TestEnvironment(tempDir.toFile())
                .setProperty("saml-callback-url", "https://guac.example.com");

        // Multiple IdPs without default: true -> no default
        File yamlFile = writeFile(tempDir, ConfigurationService.SAML_IDPS_YML,
                "- id: idp-a\n"
                + "  idp-url: https://a.example.com/sso\n"
                + "- id: idp-b\n"
                + "  idp-url: https://b.example.com/sso\n");

        ConfigurationService conf = createConfigurationService(env);
        assertNull(conf.getDefaultConfiguration());
        assertThrows(GuacamoleResourceNotFoundException.class,
                conf::getRequiredDefaultConfiguration);
        assertEquals(URI.create("https://guac.example.com"), conf.getFallbackCallbackUrl());

        // Single IdP in YAML without default: true -> implicitly default
        writeFile(tempDir, ConfigurationService.SAML_IDPS_YML,
                "- id: only-idp\n"
                + "  idp-url: https://only.example.com/sso\n");
        assertTrue(yamlFile.setLastModified(yamlFile.lastModified() + 5000L));

        SAMLConfiguration singleDefault = conf.getDefaultConfiguration();
        assertNotNull(singleDefault);
        assertEquals("only-idp", singleDefault.getId());

    }

    /**
     * Verifies validation errors for missing/invalid/duplicate IDs, multiple
     * defaults, and missing IdP endpoints.
     *
     * @param tempDir
     *     Temporary GUACAMOLE_HOME directory.
     *
     * @throws Exception
     *     If an unexpected error occurs.
     */
    @Test
    public void testValidationErrors(@TempDir Path tempDir) throws Exception {

        TestEnvironment env = new TestEnvironment(tempDir.toFile())
                .setProperty("saml-callback-url", "https://guac.example.com")
                .setProperty("saml-entity-id", "https://guac.example.com/sp");

        // Duplicate IDs
        writeFile(tempDir, ConfigurationService.SAML_IDPS_YML,
                "- id: dup\n"
                + "  idp-url: https://a.example.com/sso\n"
                + "- id: dup\n"
                + "  idp-url: https://b.example.com/sso\n");
        ConfigurationService conf1 = createConfigurationService(env);
        assertThrows(GuacamoleServerException.class, conf1::getConfigurations);

        // Multiple defaults
        writeFile(tempDir, ConfigurationService.SAML_IDPS_YML,
                "- id: idp-a\n"
                + "  default: true\n"
                + "  idp-url: https://a.example.com/sso\n"
                + "- id: idp-b\n"
                + "  default: true\n"
                + "  idp-url: https://b.example.com/sso\n");
        ConfigurationService conf2 = createConfigurationService(env);
        assertThrows(GuacamoleServerException.class, conf2::getConfigurations);

        // Path traversal / invalid characters in ID
        writeFile(tempDir, ConfigurationService.SAML_IDPS_YML,
                "- id: ../evil\n"
                + "  idp-url: https://a.example.com/sso\n");
        ConfigurationService conf3 = createConfigurationService(env);
        assertThrows(GuacamoleServerException.class, conf3::getConfigurations);

        // Double dots inside ID
        writeFile(tempDir, ConfigurationService.SAML_IDPS_YML,
                "- id: a..b\n"
                + "  idp-url: https://a.example.com/sso\n");
        ConfigurationService conf4 = createConfigurationService(env);
        assertThrows(GuacamoleServerException.class, conf4::getConfigurations);

        // Missing ID
        writeFile(tempDir, ConfigurationService.SAML_IDPS_YML,
                "- display-name: Missing ID\n"
                + "  idp-url: https://a.example.com/sso\n");
        ConfigurationService conf5 = createConfigurationService(env);
        assertThrows(GuacamoleServerException.class, conf5::getConfigurations);

        // Valid ID but neither idp-metadata-url nor idp-url configured
        writeFile(tempDir, ConfigurationService.SAML_IDPS_YML,
                "- id: incomplete\n"
                + "  display-name: Incomplete IdP\n");
        ConfigurationService conf6 = createConfigurationService(env);
        SAMLConfiguration incomplete = conf6.getRequiredConfiguration("incomplete");
        assertThrows(GuacamoleServerException.class, () -> conf6.getSamlSettings(incomplete));

    }

    /**
     * Verifies that empty YAML falls back to {@code guacamole.properties},
     * modifications hot-reload, and deleting {@code saml-idps.yml} cleanly
     * reverts to {@code guacamole.properties}.
     *
     * @param tempDir
     *     Temporary GUACAMOLE_HOME directory.
     *
     * @throws Exception
     *     If an unexpected error occurs.
     */
    @Test
    public void testHotReloadAndDeletionFallback(@TempDir Path tempDir) throws Exception {

        TestEnvironment env = new TestEnvironment(tempDir.toFile())
                .setProperty("saml-idp-url", "https://fallback.example.com/sso")
                .setProperty("saml-entity-id", "https://guac.example.com/sp")
                .setProperty("saml-callback-url", "https://guac.example.com");

        // Empty (comments-only) YAML falls back to guacamole.properties
        File yamlFile = writeFile(tempDir, ConfigurationService.SAML_IDPS_YML,
                "# No IdPs defined yet\n");

        ConfigurationService conf = createConfigurationService(env);
        assertFalse(conf.isMultiIdpConfigured());
        assertEquals(SAMLConfiguration.DEFAULT_ID, conf.getDefaultConfiguration().getId());

        // Populate YAML and bump mtime -> hot-reloads
        writeFile(tempDir, ConfigurationService.SAML_IDPS_YML,
                "- id: alpha\n"
                + "  idp-url: https://alpha.example.com/sso\n");
        assertTrue(yamlFile.setLastModified(yamlFile.lastModified() + 5000L));

        assertTrue(conf.isMultiIdpConfigured());
        assertNotNull(conf.getConfiguration("alpha"));

        // Update YAML with second IdP -> hot-reloads
        writeFile(tempDir, ConfigurationService.SAML_IDPS_YML,
                "- id: alpha\n"
                + "  idp-url: https://alpha.example.com/sso\n"
                + "- id: beta\n"
                + "  idp-url: https://beta.example.com/sso\n");
        assertTrue(yamlFile.setLastModified(yamlFile.lastModified() + 10000L));

        assertEquals(2, conf.getConfigurations().size());
        assertNotNull(conf.getConfiguration("beta"));

        // Delete YAML -> reverts cleanly to guacamole.properties
        assertTrue(yamlFile.delete());
        assertFalse(conf.isMultiIdpConfigured());
        assertEquals(SAMLConfiguration.DEFAULT_ID, conf.getDefaultConfiguration().getId());
        assertNull(conf.getConfiguration("alpha"));

    }

    /**
     * Verifies that {@link ConfigurationService#getSamlSettings(SAMLConfiguration)}
     * loads local file IdP metadata XML cleanly per IdP.
     *
     * @param tempDir
     *     Temporary GUACAMOLE_HOME directory.
     *
     * @throws Exception
     *     If an unexpected error occurs.
     */
    @Test
    public void testLocalMetadataXmlParsing(@TempDir Path tempDir) throws Exception {

        String metadataXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" "
                + "entityID=\"https://idp-meta.example.com/entity\">\n"
                + "  <md:IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n"
                + "    <md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" "
                + "Location=\"https://idp-meta.example.com/sso/redirect\"/>\n"
                + "  </md:IDPSSODescriptor>\n"
                + "</md:EntityDescriptor>\n";

        File metadataFile = writeFile(tempDir, "idp-meta.xml", metadataXml);

        TestEnvironment env = new TestEnvironment(tempDir.toFile())
                .setProperty("saml-callback-url", "https://guac.example.com");

        writeFile(tempDir, ConfigurationService.SAML_IDPS_YML,
                "- id: meta-idp\n"
                + "  display-name: Metadata IdP\n"
                + "  idp-metadata-url: " + metadataFile.toURI() + "\n"
                + "  entity-id: https://guac.example.com/sp/meta-idp\n");

        ConfigurationService conf = createConfigurationService(env);
        SAMLConfiguration cfg = conf.getRequiredConfiguration("meta-idp");
        Saml2Settings settings = conf.getSamlSettings(cfg);

        assertEquals("https://idp-meta.example.com/entity", settings.getIdpEntityId());
        assertEquals("https://idp-meta.example.com/sso/redirect",
                settings.getIdpSingleSignOnServiceUrl().toString());
        assertEquals("https://guac.example.com/sp/meta-idp", settings.getSpEntityId());
        assertEquals("https://guac.example.com/api/ext/saml/callback/meta-idp",
                settings.getSpAssertionConsumerServiceUrl().toString());

    }

}
