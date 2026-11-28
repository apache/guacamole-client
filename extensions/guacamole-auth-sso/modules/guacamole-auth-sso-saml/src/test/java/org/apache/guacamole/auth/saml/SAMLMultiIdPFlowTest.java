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

package org.apache.guacamole.auth.saml;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.onelogin.saml2.util.Constants;
import com.onelogin.saml2.util.Util;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Inflater;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.auth.saml.acs.AssertionConsumerServiceResource;
import org.apache.guacamole.auth.saml.acs.SAMLAuthenticationSession;
import org.apache.guacamole.auth.saml.acs.SAMLAuthenticationSessionManager;
import org.apache.guacamole.auth.saml.acs.SAMLIdentityProviderInfo;
import org.apache.guacamole.auth.saml.acs.SAMLService;
import org.apache.guacamole.auth.saml.conf.ConfigurationService;
import org.apache.guacamole.auth.saml.user.SAMLAuthenticatedUser;
import org.apache.guacamole.auth.sso.SSOAuthenticationProviderService;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.form.RedirectField;
import org.apache.guacamole.net.auth.AbstractAuthenticationProvider;
import org.apache.guacamole.net.auth.AuthenticationProvider;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.w3c.dom.Document;
import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end multi-IdP SAML flow and cryptographic security tests covering
 * strict SAML 2.0 XML signature validation, per-IdP key isolation, per-IdP
 * ACS callbacks, cross-IdP spoofing rejection, per-IdP group attribute
 * extraction, provider discovery, primary-extension behavior, and per-IdP
 * logout routing.
 */
public class SAMLMultiIdPFlowTest {

    /**
     * Ephemeral test IdP signing keypair and certificate generated at runtime.
     */
    private static final class TestIdpKeyMaterial {

        /**
         * Private key used to sign test SAML responses.
         */
        private final PrivateKey privateKey;

        /**
         * Self-signed X.509 certificate published in the test IdP metadata XML.
         */
        private final X509Certificate certificate;

        /**
         * Creates a new TestIdpKeyMaterial holder.
         *
         * @param privateKey
         *     The RSA private key.
         *
         * @param certificate
         *     The matching X.509 certificate.
         */
        private TestIdpKeyMaterial(PrivateKey privateKey, X509Certificate certificate) {
            this.privateKey = privateKey;
            this.certificate = certificate;
        }

    }

    /**
     * Ephemeral RSA keypair for test IdP A.
     */
    private static TestIdpKeyMaterial idpAKey;

    /**
     * Ephemeral RSA keypair for test IdP B (cryptographically distinct from
     * IdP A).
     */
    private static TestIdpKeyMaterial idpBKey;

    /**
     * Installs the test JAX-RS RuntimeDelegate and generates two distinct
     * ephemeral RSA keypairs for test IdPs before running tests.
     *
     * @param keyDir
     *     Temporary directory for ephemeral keystores.
     *
     * @throws Exception
     *     If key generation fails.
     */
    @BeforeAll
    public static void setupTestSuite(@TempDir Path keyDir) throws Exception {
        TestJaxRsRuntimeDelegate.install();
        idpAKey = generateEphemeralIdpKey(keyDir, "idp-a");
        idpBKey = generateEphemeralIdpKey(keyDir, "idp-b");
    }

    /**
     * Generates an ephemeral RSA-2048 self-signed keypair using the JDK's
     * {@code keytool} utility so no static private keys are stored in source
     * control.
     *
     * @param dir
     *     Directory where the temporary keystore should be written.
     *
     * @param alias
     *     Keystore alias and CN suffix.
     *
     * @return
     *     The generated keypair and X.509 certificate.
     *
     * @throws Exception
     *     If keytool invocation or keystore loading fails.
     */
    private static TestIdpKeyMaterial generateEphemeralIdpKey(Path dir, String alias)
            throws Exception {

        File keystoreFile = dir.resolve(alias + ".p12").toFile();
        char[] password = "changeit".toCharArray();

        String keytoolBin = new File(new File(System.getProperty("java.home"), "bin"), "keytool")
                .getAbsolutePath();

        Process process = new ProcessBuilder(
                keytoolBin,
                "-genkeypair",
                "-alias", alias,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-sigalg", "SHA256withRSA",
                "-validity", "365",
                "-dname", "CN=" + alias + ".test.example.com",
                "-storetype", "PKCS12",
                "-keystore", keystoreFile.getAbsolutePath(),
                "-storepass", new String(password),
                "-keypass", new String(password))
                .redirectErrorStream(true)
                .start();

        int exitCode = process.waitFor();
        if (exitCode != 0)
            throw new IllegalStateException("keytool failed with exit code " + exitCode);

        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(keystoreFile)) {
            ks.load(fis, password);
        }

        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, password);
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
        return new TestIdpKeyMaterial(privateKey, cert);

    }

    /**
     * Writes a local SAML 2.0 IdP Metadata XML file containing the given
     * entity ID, SSO URL, and X.509 signing certificate.
     *
     * @param dir
     *     Directory where the metadata file should be written.
     *
     * @param filename
     *     Filename for the metadata XML file.
     *
     * @param entityId
     *     The IdP entity ID.
     *
     * @param ssoUrl
     *     The IdP HTTP-Redirect SingleSignOnService URL.
     *
     * @param keyMaterial
     *     The IdP key material whose X.509 certificate should be published.
     *
     * @return
     *     The {@code file://} URI of the written metadata XML file.
     *
     * @throws Exception
     *     If certificate encoding or file writing fails.
     */
    private URI writeIdpMetadata(Path dir, String filename, String entityId,
            String ssoUrl, TestIdpKeyMaterial keyMaterial) throws Exception {

        String base64Cert = Base64.getEncoder().encodeToString(
                keyMaterial.certificate.getEncoded());

        String xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" "
                + "xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" "
                + "entityID=\"" + entityId + "\">\n"
                + "  <md:IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n"
                + "    <md:KeyDescriptor use=\"signing\">\n"
                + "      <ds:KeyInfo><ds:X509Data><ds:X509Certificate>"
                + base64Cert
                + "</ds:X509Certificate></ds:X509Data></ds:KeyInfo>\n"
                + "    </md:KeyDescriptor>\n"
                + "    <md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" "
                + "Location=\"" + ssoUrl + "\"/>\n"
                + "  </md:IDPSSODescriptor>\n"
                + "</md:EntityDescriptor>\n";

        Path file = dir.resolve(filename);
        Files.write(file, xml.getBytes(StandardCharsets.UTF_8));
        return file.toUri();

    }

    /**
     * Creates a Guice injector wiring all SAML extension components against
     * the given {@link TestEnvironment}.
     *
     * @param env
     *     The test environment.
     *
     * @return
     *     The initialized Guice injector.
     */
    private Injector createInjector(final TestEnvironment env) {
        final AuthenticationProvider dummyProvider = new AbstractAuthenticationProvider() {
            @Override
            public String getIdentifier() {
                return "saml";
            }
        };

        return Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(AuthenticationProvider.class).toInstance(dummyProvider);
                bind(SSOAuthenticationProviderService.class).to(AuthenticationProviderService.class);
                bind(AssertionConsumerServiceResource.class);
                bind(ConfigurationService.class);
                bind(SAMLAuthenticationSessionManager.class);
                bind(SAMLService.class);
                bind(Environment.class).toInstance(env);
            }
        });
    }

    /**
     * Parses query parameters from a URI into a decoded map.
     *
     * @param uri
     *     The URI whose query string should be parsed.
     *
     * @return
     *     A map of query parameter names to decoded values.
     *
     * @throws Exception
     *     If URL decoding fails.
     */
    private Map<String, String> parseQueryParams(URI uri) throws Exception {
        Map<String, String> params = new HashMap<>();
        String query = uri.getRawQuery();
        if (query == null || query.isEmpty())
            return params;
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8.name());
                String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8.name());
                params.put(key, val);
            }
        }
        return params;
    }

    /**
     * Decodes a deflated, base64-encoded SAML AuthnRequest query parameter.
     *
     * @param samlRequestParam
     *     The raw base64 SAMLRequest parameter value.
     *
     * @return
     *     The XML string of the SAML AuthnRequest.
     *
     * @throws Exception
     *     If decoding or inflation fails.
     */
    private String decodeAuthnRequest(String samlRequestParam) throws Exception {
        byte[] compressed = Base64.getDecoder().decode(samlRequestParam);
        Inflater inflater = new Inflater(true);
        inflater.setInput(compressed);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!inflater.finished()) {
            int count = inflater.inflate(buffer);
            if (count == 0)
                break;
            out.write(buffer, 0, count);
        }
        inflater.end();
        return new String(out.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Extracts the XML {@code ID} attribute from an AuthnRequest XML string.
     *
     * @param authnRequestXml
     *     The AuthnRequest XML string.
     *
     * @return
     *     The request ID value.
     */
    private String extractRequestId(String authnRequestXml) {
        Matcher m = Pattern.compile("\\bID=\"([^\"]+)\"").matcher(authnRequestXml);
        assertTrue(m.find(), "AuthnRequest XML should contain an ID attribute");
        return m.group(1);
    }

    /**
     * Builds and RSA-SHA256 signs a SAML 2.0 Response XML using the given IdP
     * key material, returning the base64-encoded SAMLResponse string.
     *
     * @param keyMaterial
     *     The IdP private key and certificate used to sign the SAML response.
     *
     * @param issuer
     *     The IdP Issuer URI.
     *
     * @param destination
     *     The ACS destination URL.
     *
     * @param audience
     *     The SP Entity ID audience.
     *
     * @param inResponseTo
     *     The AuthnRequest ID being answered.
     *
     * @param nameId
     *     The asserted user NameID.
     *
     * @param groupAttrName
     *     The SAML attribute name carrying group memberships.
     *
     * @param groups
     *     The group values to include.
     *
     * @return
     *     The base64-encoded signed SAMLResponse payload.
     *
     * @throws Exception
     *     If XML parsing or digital signing fails.
     */
    private String buildSignedSamlResponse(TestIdpKeyMaterial keyMaterial,
            String issuer, String destination, String audience,
            String inResponseTo, String nameId, String groupAttrName,
            String... groups) throws Exception {

        Instant now = Instant.now();
        String issueInstant = DateTimeFormatter.ISO_INSTANT.format(
                now.atOffset(ZoneOffset.UTC));
        String notBefore = DateTimeFormatter.ISO_INSTANT.format(
                now.minusSeconds(60).atOffset(ZoneOffset.UTC));
        String notOnOrAfter = DateTimeFormatter.ISO_INSTANT.format(
                now.plusSeconds(300).atOffset(ZoneOffset.UTC));

        StringBuilder groupValuesXml = new StringBuilder();
        for (String group : groups) {
            groupValuesXml.append("<saml:AttributeValue>")
                    .append(group)
                    .append("</saml:AttributeValue>");
        }

        String unsignedXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<samlp:Response xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\" "
                + "xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\" "
                + "ID=\"_resp_12345\" Version=\"2.0\" IssueInstant=\"" + issueInstant + "\" "
                + "Destination=\"" + destination + "\" InResponseTo=\"" + inResponseTo + "\">\n"
                + "  <saml:Issuer>" + issuer + "</saml:Issuer>\n"
                + "  <samlp:Status><samlp:StatusCode Value=\"urn:oasis:names:tc:SAML:2.0:status:Success\"/></samlp:Status>\n"
                + "  <saml:Assertion ID=\"_assert_12345\" Version=\"2.0\" IssueInstant=\"" + issueInstant + "\">\n"
                + "    <saml:Issuer>" + issuer + "</saml:Issuer>\n"
                + "    <saml:Subject>\n"
                + "      <saml:NameID Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\">" + nameId + "</saml:NameID>\n"
                + "      <saml:SubjectConfirmation Method=\"urn:oasis:names:tc:SAML:2.0:cm:bearer\">\n"
                + "        <saml:SubjectConfirmationData NotOnOrAfter=\"" + notOnOrAfter + "\" "
                + "Recipient=\"" + destination + "\" InResponseTo=\"" + inResponseTo + "\"/>\n"
                + "      </saml:SubjectConfirmation>\n"
                + "    </saml:Subject>\n"
                + "    <saml:Conditions NotBefore=\"" + notBefore + "\" NotOnOrAfter=\"" + notOnOrAfter + "\">\n"
                + "      <saml:AudienceRestriction><saml:Audience>" + audience + "</saml:Audience></saml:AudienceRestriction>\n"
                + "    </saml:Conditions>\n"
                + "    <saml:AuthnStatement AuthnInstant=\"" + issueInstant + "\" SessionNotOnOrAfter=\"" + notOnOrAfter + "\">\n"
                + "      <saml:AuthnContext><saml:AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport</saml:AuthnContextClassRef></saml:AuthnContext>\n"
                + "    </saml:AuthnStatement>\n"
                + "    <saml:AttributeStatement>\n"
                + "      <saml:Attribute Name=\"" + groupAttrName + "\">" + groupValuesXml + "</saml:Attribute>\n"
                + "      <saml:Attribute Name=\"department\"><saml:AttributeValue>Engineering</saml:AttributeValue></saml:Attribute>\n"
                + "    </saml:AttributeStatement>\n"
                + "  </saml:Assertion>\n"
                + "</samlp:Response>";

        Document doc = Util.convertStringToDocument(unsignedXml);
        String signedXml = Util.addSign(doc, keyMaterial.privateKey,
                keyMaterial.certificate, Constants.RSA_SHA256, Constants.SHA256);

        return Base64.getEncoder().encodeToString(signedXml.getBytes(StandardCharsets.UTF_8));

    }

    /**
     * Creates a minimal {@link HttpServletRequest} dynamic proxy returning the
     * given request URL and parameter map.
     *
     * @param requestUrl
     *     The full request URL to return from {@code getRequestURL()}.
     *
     * @param params
     *     Parameters to expose via {@code getParameter(String)}.
     *
     * @return
     *     A proxied HttpServletRequest.
     */
    private HttpServletRequest mockRequest(final String requestUrl,
            final Map<String, String> params) {
        final Map<String, String[]> paramMap = new HashMap<>();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                paramMap.put(entry.getKey(), new String[] { entry.getValue() });
            }
        }
        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[] { HttpServletRequest.class },
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getRequestURL":
                            return new StringBuffer(requestUrl);
                        case "getParameter":
                            return params != null ? params.get(args[0]) : null;
                        case "getParameterNames":
                            return java.util.Collections.enumeration(paramMap.keySet());
                        case "getParameterValues":
                            return paramMap.get(args[0]);
                        case "getParameterMap":
                            return paramMap;
                        case "getHeader":
                            return null;
                        case "getHeaderNames":
                            return java.util.Collections.emptyEnumeration();
                        case "getRemoteAddr":
                            return "127.0.0.1";
                        case "getRemoteHost":
                            return "localhost";
                        default:
                            return null;
                    }
                });
    }

    /**
     * Verifies full strict-mode end-to-end authentication against two
     * cryptographically distinct SAML IdPs, including per-IdP AuthnRequest
     * generation, per-IdP ACS callback handling, per-IdP group attribute
     * extraction, and per-IdP logout routing.
     *
     * @param tempDir
     *     Temporary GUACAMOLE_HOME directory.
     *
     * @throws Exception
     *     If an unexpected error occurs.
     */
    @Test
    public void testMultiIdpEndToEndAuthenticationAndLogout(@TempDir Path tempDir) throws Exception {

        URI oktaMetadata = writeIdpMetadata(tempDir, "okta-meta.xml",
                "https://corp.okta.com/entity", "https://corp.okta.com/sso", idpAKey);
        URI entraMetadata = writeIdpMetadata(tempDir, "entra-meta.xml",
                "https://sts.windows.net/tenant/", "https://login.microsoftonline.com/tenant/saml2", idpBKey);

        TestEnvironment env = new TestEnvironment(tempDir.toFile())
                .setProperty("saml-callback-url", "https://guac.example.com")
                .setProperty("saml-strict", "true");

        String yaml =
                "- id: okta\n"
                + "  display-name: Okta Corporate\n"
                + "  idp-metadata-url: " + oktaMetadata + "\n"
                + "  entity-id: https://guac.example.com/sp/okta\n"
                + "  group-attribute: groups\n"
                + "  logout-endpoint: https://corp.okta.com/slo\n"
                + "  post-logout-redirect-uri: https://guac.example.com/logged-out\n"
                + "\n"
                + "- id: entra\n"
                + "  display-name: Microsoft Entra ID\n"
                + "  idp-metadata-url: " + entraMetadata + "\n"
                + "  entity-id: https://guac.example.com/sp/entra\n"
                + "  group-attribute: http://schemas.microsoft.com/ws/2008/06/identity/claims/groups\n";

        Files.write(tempDir.resolve(ConfigurationService.SAML_IDPS_YML),
                yaml.getBytes(StandardCharsets.UTF_8));

        Injector injector = createInjector(env);
        AuthenticationProviderService authService = injector.getInstance(AuthenticationProviderService.class);
        AssertionConsumerServiceResource resource = injector.getInstance(AssertionConsumerServiceResource.class);

        try {

            // 1. Provider discovery endpoint returns both IdPs in order
            List<SAMLIdentityProviderInfo> providers = resource.getIdentityProviders();
            assertEquals(2, providers.size());
            assertEquals("okta", providers.get(0).getId());
            assertEquals("Okta Corporate", providers.get(0).getDisplayName());
            assertEquals("api/ext/saml/login/okta", providers.get(0).getLoginUrl());
            assertEquals("entra", providers.get(1).getId());
            assertEquals("Microsoft Entra ID", providers.get(1).getDisplayName());
            assertEquals("api/ext/saml/login/entra", providers.get(1).getLoginUrl());

            // 2. With no default IdP configured, primary authenticateUser()
            // returns null (no auto-redirect trap) and parameterless /login throws 404
            Credentials emptyCreds = new Credentials(null, null,
                    mockRequest("https://guac.example.com/api/tokens", null));
            assertNull(authService.authenticateUser(emptyCreds));
            assertThrows(GuacamoleResourceNotFoundException.class, authService::getLoginURI);
            assertNull(authService.getLogoutURI(null));

            // 3. Authenticate through Okta (strict mode + RSA-SHA256 signature)
            Response oktaLoginResp = resource.redirectToIdentityProvider("okta");
            assertEquals(303, oktaLoginResp.getStatus());
            URI oktaRedirect = (URI) oktaLoginResp.getMetadata().getFirst("Location");
            assertTrue(oktaRedirect.toString().startsWith("https://corp.okta.com/sso?"));

            Map<String, String> oktaParams = parseQueryParams(oktaRedirect);
            String oktaRelayState = oktaParams.get("RelayState");
            String oktaRequestXml = decodeAuthnRequest(oktaParams.get("SAMLRequest"));
            assertTrue(oktaRequestXml.contains("https://guac.example.com/api/ext/saml/callback/okta"));
            assertTrue(oktaRequestXml.contains("https://guac.example.com/sp/okta"));
            String oktaRequestId = extractRequestId(oktaRequestXml);

            String oktaSamlResponse = buildSignedSamlResponse(
                    idpAKey,
                    "https://corp.okta.com/entity",
                    "https://guac.example.com/api/ext/saml/callback/okta",
                    "https://guac.example.com/sp/okta",
                    oktaRequestId,
                    "Alice@Corp.Example.com",
                    "groups",
                    "okta-admins", "dev-team");

            Response oktaCallbackResp = resource.processSamlResponse(
                    "okta", oktaRelayState, oktaSamlResponse,
                    mockRequest("https://guac.example.com/api/ext/saml/callback/okta", null));
            assertEquals(303, oktaCallbackResp.getStatus());

            URI oktaFinalRedirect = (URI) oktaCallbackResp.getMetadata().getFirst("Location");
            Map<String, String> oktaFinalParams = parseQueryParams(oktaFinalRedirect);
            assertEquals("okta", oktaFinalParams.get("idp"));
            assertNotNull(oktaFinalParams.get("state"));

            SAMLAuthenticatedUser oktaUser = authService.authenticateUser(
                    new Credentials(null, null, mockRequest("https://guac.example.com/api/tokens", oktaFinalParams)));
            assertNotNull(oktaUser);
            assertEquals("alice@corp.example.com", oktaUser.getIdentifier());
            Set<String> oktaGroups = oktaUser.getEffectiveUserGroups();
            assertTrue(oktaGroups.contains("okta-admins"));
            assertTrue(oktaGroups.contains("dev-team"));
            assertEquals("Engineering", oktaUser.getTokens().get("SAML_DEPARTMENT"));

            // 4. Authenticate through Microsoft Entra ID (distinct keypair + distinct group attribute)
            Response entraLoginResp = resource.redirectToIdentityProvider("entra");
            assertEquals(303, entraLoginResp.getStatus());
            URI entraRedirect = (URI) entraLoginResp.getMetadata().getFirst("Location");
            assertTrue(entraRedirect.toString().startsWith("https://login.microsoftonline.com/tenant/saml2?"));

            Map<String, String> entraParams = parseQueryParams(entraRedirect);
            String entraRelayState = entraParams.get("RelayState");
            String entraRequestXml = decodeAuthnRequest(entraParams.get("SAMLRequest"));
            assertTrue(entraRequestXml.contains("https://guac.example.com/api/ext/saml/callback/entra"));
            assertTrue(entraRequestXml.contains("https://guac.example.com/sp/entra"));
            String entraRequestId = extractRequestId(entraRequestXml);

            String entraSamlResponse = buildSignedSamlResponse(
                    idpBKey,
                    "https://sts.windows.net/tenant/",
                    "https://guac.example.com/api/ext/saml/callback/entra",
                    "https://guac.example.com/sp/entra",
                    entraRequestId,
                    "Bob@Entra.Example.com",
                    "http://schemas.microsoft.com/ws/2008/06/identity/claims/groups",
                    "entra-ops", "cloud-users");

            Response entraCallbackResp = resource.processSamlResponse(
                    "entra", entraRelayState, entraSamlResponse,
                    mockRequest("https://guac.example.com/api/ext/saml/callback/entra", null));
            assertEquals(303, entraCallbackResp.getStatus());

            URI entraFinalRedirect = (URI) entraCallbackResp.getMetadata().getFirst("Location");
            Map<String, String> entraFinalParams = parseQueryParams(entraFinalRedirect);
            assertEquals("entra", entraFinalParams.get("idp"));

            SAMLAuthenticatedUser entraUser = authService.authenticateUser(
                    new Credentials(null, null, mockRequest("https://guac.example.com/api/tokens", entraFinalParams)));
            assertNotNull(entraUser);
            assertEquals("bob@entra.example.com", entraUser.getIdentifier());
            Set<String> entraGroups = entraUser.getEffectiveUserGroups();
            assertTrue(entraGroups.contains("entra-ops"));
            assertTrue(entraGroups.contains("cloud-users"));

            // 5. Per-IdP logout routing
            Response oktaLogout = resource.getLogoutRedirect("okta", null);
            assertEquals(303, oktaLogout.getStatus());
            URI oktaLogoutUri = (URI) oktaLogout.getMetadata().getFirst("Location");
            assertTrue(oktaLogoutUri.toString().startsWith("https://corp.okta.com/slo?"));
            assertEquals("https://guac.example.com/logged-out",
                    parseQueryParams(oktaLogoutUri).get("RelayState"));

            // Entra has no logout-endpoint -> 204 No Content
            Response entraLogout = resource.getLogoutRedirect("entra", null);
            assertEquals(204, entraLogout.getStatus());

            // Unknown IdP -> 404
            assertThrows(GuacamoleResourceNotFoundException.class,
                    () -> resource.redirectToIdentityProvider("unknown"));
            assertThrows(GuacamoleResourceNotFoundException.class,
                    () -> resource.getLogoutRedirect("unknown", null));

        }
        finally {
            authService.shutdown();
        }

    }

    /**
     * Verifies strict security enforcement:
     * 1) Submitting IdP-A's RelayState to IdP-B's callback path is rejected.
     * 2) Submitting a response signed by IdP-B's key for IdP-A's session is
     *    cryptographically rejected.
     * 3) Replaying or omitting RelayState is rejected.
     *
     * @param tempDir
     *     Temporary GUACAMOLE_HOME directory.
     *
     * @throws Exception
     *     If an unexpected error occurs.
     */
    @Test
    public void testCrossIdpSpoofingWrongKeyAndReplayRejected(@TempDir Path tempDir) throws Exception {

        URI idpAMetadata = writeIdpMetadata(tempDir, "idp-a-meta.xml",
                "https://a.example.com/entity", "https://a.example.com/sso", idpAKey);
        URI idpBMetadata = writeIdpMetadata(tempDir, "idp-b-meta.xml",
                "https://b.example.com/entity", "https://b.example.com/sso", idpBKey);

        TestEnvironment env = new TestEnvironment(tempDir.toFile())
                .setProperty("saml-callback-url", "https://guac.example.com")
                .setProperty("saml-strict", "true");

        String yaml =
                "- id: idp-a\n"
                + "  idp-metadata-url: " + idpAMetadata + "\n"
                + "  entity-id: https://guac.example.com/sp/a\n"
                + "- id: idp-b\n"
                + "  idp-metadata-url: " + idpBMetadata + "\n"
                + "  entity-id: https://guac.example.com/sp/b\n";

        Files.write(tempDir.resolve(ConfigurationService.SAML_IDPS_YML),
                yaml.getBytes(StandardCharsets.UTF_8));

        Injector injector = createInjector(env);
        SAMLService samlService = injector.getInstance(SAMLService.class);
        AuthenticationProviderService authService = injector.getInstance(AuthenticationProviderService.class);

        try {

            // 1. Cross-IdP callback path mismatch rejected before validation
            URI idpALogin1 = samlService.createRequest("idp-a");
            Map<String, String> params1 = parseQueryParams(idpALogin1);
            String relayState1 = params1.get("RelayState");
            String requestId1 = extractRequestId(decodeAuthnRequest(params1.get("SAMLRequest")));

            String payloadSignedByA = buildSignedSamlResponse(
                    idpAKey,
                    "https://a.example.com/entity",
                    "https://guac.example.com/api/ext/saml/callback/idp-b",
                    "https://guac.example.com/sp/a",
                    requestId1,
                    "attacker@example.com",
                    "groups",
                    "admins");

            assertThrows(GuacamoleSecurityException.class, () ->
                    samlService.processResponse("idp-b",
                            "https://guac.example.com/api/ext/saml/callback/idp-b",
                            relayState1, payloadSignedByA));

            // 2. Replaying the same RelayState against idp-a MUST fail
            assertThrows(GuacamoleSecurityException.class, () ->
                    samlService.processResponse("idp-a",
                            "https://guac.example.com/api/ext/saml/callback/idp-a",
                            relayState1, payloadSignedByA));

            // 3. Cryptographic cross-IdP attack: start session on idp-a, but
            //    sign assertion with idp-b's key -> MUST fail signature check!
            URI idpALogin2 = samlService.createRequest("idp-a");
            Map<String, String> params2 = parseQueryParams(idpALogin2);
            String relayState2 = params2.get("RelayState");
            String requestId2 = extractRequestId(decodeAuthnRequest(params2.get("SAMLRequest")));

            String payloadSignedByB = buildSignedSamlResponse(
                    idpBKey,
                    "https://a.example.com/entity",
                    "https://guac.example.com/api/ext/saml/callback/idp-a",
                    "https://guac.example.com/sp/a",
                    requestId2,
                    "forged@example.com",
                    "groups",
                    "admins");

            assertThrows(GuacamoleSecurityException.class, () ->
                    samlService.processResponse("idp-a",
                            "https://guac.example.com/api/ext/saml/callback/idp-a",
                            relayState2, payloadSignedByB));

            // 4. Missing RelayState MUST fail
            assertThrows(GuacamoleSecurityException.class, () ->
                    samlService.processResponse("idp-a",
                            "https://guac.example.com/api/ext/saml/callback/idp-a",
                            null, payloadSignedByA));

        }
        finally {
            authService.shutdown();
        }

    }

    /**
     * Verifies that the shared {@code /api/ext/saml/callback} endpoint resolves
     * the originating IdP from {@code RelayState}, and that explicit
     * {@code default: true} triggers automatic redirect when SAML is primary.
     *
     * @param tempDir
     *     Temporary GUACAMOLE_HOME directory.
     *
     * @throws Exception
     *     If an unexpected error occurs.
     */
    @Test
    public void testSharedCallbackAndExplicitDefaultAutoRedirect(@TempDir Path tempDir) throws Exception {

        URI primaryMetadata = writeIdpMetadata(tempDir, "primary-meta.xml",
                "https://primary.example.com/entity", "https://primary.example.com/sso", idpAKey);
        URI secondaryMetadata = writeIdpMetadata(tempDir, "secondary-meta.xml",
                "https://secondary.example.com/entity", "https://secondary.example.com/sso", idpBKey);

        TestEnvironment env = new TestEnvironment(tempDir.toFile())
                .setProperty("saml-callback-url", "https://guac.example.com")
                .setProperty("saml-strict", "true");

        String yaml =
                "- id: primary-idp\n"
                + "  display-name: Primary IdP\n"
                + "  default: true\n"
                + "  idp-metadata-url: " + primaryMetadata + "\n"
                + "  entity-id: https://guac.example.com/sp/primary\n"
                + "  acs-url: https://guac.example.com/api/ext/saml/callback\n"
                + "- id: secondary-idp\n"
                + "  display-name: Secondary IdP\n"
                + "  idp-metadata-url: " + secondaryMetadata + "\n"
                + "  entity-id: https://guac.example.com/sp/secondary\n"
                + "  acs-url: https://guac.example.com/api/ext/saml/callback\n";

        Files.write(tempDir.resolve(ConfigurationService.SAML_IDPS_YML),
                yaml.getBytes(StandardCharsets.UTF_8));

        Injector injector = createInjector(env);
        AuthenticationProviderService authService = injector.getInstance(AuthenticationProviderService.class);
        SAMLService samlService = injector.getInstance(SAMLService.class);

        try {

            // Auto-redirect targets primary-idp because default: true is set
            Credentials emptyCreds = new Credentials(null, null,
                    mockRequest("https://guac.example.com/api/tokens", null));
            GuacamoleInvalidCredentialsException redirectEx = assertThrows(
                    GuacamoleInvalidCredentialsException.class,
                    () -> authService.authenticateUser(emptyCreds));

            RedirectField field = (RedirectField) redirectEx.getCredentialsInfo().getFields().iterator().next();
            URI redirectUri = URI.create(field.getRedirectUrl());
            assertTrue(redirectUri.toString().startsWith("https://primary.example.com/sso?"));

            // Shared /callback works for secondary-idp using RelayState lookup
            URI secondaryLogin = samlService.createRequest("secondary-idp");
            Map<String, String> params = parseQueryParams(secondaryLogin);
            String relayState = params.get("RelayState");
            String requestId = extractRequestId(decodeAuthnRequest(params.get("SAMLRequest")));

            String payload = buildSignedSamlResponse(
                    idpBKey,
                    "https://secondary.example.com/entity",
                    "https://guac.example.com/api/ext/saml/callback",
                    "https://guac.example.com/sp/secondary",
                    requestId,
                    "carol@secondary.example.com",
                    "groups",
                    "secondary-users");

            SAMLAuthenticationSession session = samlService.processResponse(
                    "https://guac.example.com/api/ext/saml/callback",
                    relayState, payload);

            assertEquals("secondary-idp", session.getIdentityProviderId());
            assertEquals("carol@secondary.example.com", session.getIdentity().getUsername());

        }
        finally {
            authService.shutdown();
        }

    }

    /**
     * Verifies legacy single-IdP behavior (no {@code saml-idps.yml}): provider
     * discovery returns a single entry with {@code displayName == null} and
     * {@code loginUrl == "api/ext/saml/login"}, and callback redirect omits
     * the {@code idp} query parameter.
     *
     * @param tempDir
     *     Temporary GUACAMOLE_HOME directory.
     *
     * @throws Exception
     *     If an unexpected error occurs.
     */
    @Test
    public void testLegacySingleIdpCallbackAndProviderDiscovery(@TempDir Path tempDir) throws Exception {

        URI singleMetadata = writeIdpMetadata(tempDir, "single-meta.xml",
                "https://single.example.com/entity", "https://single.example.com/sso", idpAKey);

        TestEnvironment env = new TestEnvironment(tempDir.toFile())
                .setProperty("saml-idp-metadata-url", singleMetadata.toString())
                .setProperty("saml-entity-id", "https://guac.example.com/sp")
                .setProperty("saml-callback-url", "https://guac.example.com")
                .setProperty("saml-strict", "true");

        Injector injector = createInjector(env);
        AuthenticationProviderService authService = injector.getInstance(AuthenticationProviderService.class);
        AssertionConsumerServiceResource resource = injector.getInstance(AssertionConsumerServiceResource.class);

        try {

            List<SAMLIdentityProviderInfo> providers = resource.getIdentityProviders();
            assertEquals(1, providers.size());
            assertEquals("default", providers.get(0).getId());
            assertNull(providers.get(0).getDisplayName());
            assertEquals("api/ext/saml/login", providers.get(0).getLoginUrl());

            URI loginUri = authService.getLoginURI();
            Map<String, String> params = parseQueryParams(loginUri);
            String relayState = params.get("RelayState");
            String requestId = extractRequestId(decodeAuthnRequest(params.get("SAMLRequest")));

            String payload = buildSignedSamlResponse(
                    idpAKey,
                    "https://single.example.com/entity",
                    "https://guac.example.com/api/ext/saml/callback",
                    "https://guac.example.com/sp",
                    requestId,
                    "legacy@example.com",
                    "groups",
                    "legacy-group");

            Response callbackResp = resource.processSamlResponse(
                    relayState, payload,
                    mockRequest("https://guac.example.com/api/ext/saml/callback", null));
            assertEquals(303, callbackResp.getStatus());

            URI finalRedirect = (URI) callbackResp.getMetadata().getFirst("Location");
            Map<String, String> finalParams = parseQueryParams(finalRedirect);
            assertNotNull(finalParams.get("state"));
            assertNull(finalParams.get("idp"),
                    "Legacy single-IdP mode should not add idp query param");

        }
        finally {
            authService.shutdown();
        }

    }

}
