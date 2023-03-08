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

package org.apache.guacamole.auth.ssl.conf;

import com.google.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import javax.naming.ldap.LdapName;
import javax.ws.rs.core.UriBuilder;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;
import org.apache.guacamole.properties.StringListProperty;
import org.apache.guacamole.properties.URIGuacamoleProperty;

/**
 * Service for retrieving configuration information regarding SSO using SSL/TLS
 * authentication.
 */
public class ConfigurationService {

    /**
     * The default name of the header to use to retrieve the URL-encoded client
     * certificate from an HTTP request received from an SSL termination
     * service providing SSL/TLS client authentication.
     */
    private static String DEFAULT_CLIENT_CERTIFICATE_HEADER = "X-Client-Certificate";

    /**
     * The default name of the header to use to retrieve the verification
     * status of the certificate an HTTP request received from an SSL
     * termination service providing SSL/TLS client authentication.
     */
    private static String DEFAULT_CLIENT_VERIFIED_HEADER = "X-Client-Verified";

    /**
     * The default amount of time that a temporary authentication token for
     * SSL/TLS authentication may remain valid, in minutes.
     */
    private static int DEFAULT_MAX_TOKEN_VALIDITY = 5;

    /**
     * The default amount of time that the temporary, unique subdomain
     * generated for SSL/TLS authentication may remain valid, in minutes.
     */
    private static int DEFAULT_MAX_DOMAIN_VALIDITY = 5;

    /**
     * The property representing the URI that should be used to authenticate
     * users with SSL/TLS client authentication. This must be a URI that points
     * to THIS instance of Guacamole, but behind SSL termination that requires
     * SSL/TLS client authentication.
     */
    private static final WildcardURIGuacamoleProperty SSL_CLIENT_AUTH_URI =
            new WildcardURIGuacamoleProperty() {

        @Override
        public String getName() { return "ssl-client-auth-uri"; }

    };

    /**
     * The property representing the URI of this instance without SSL/TLS
     * client authentication required. This must be a URI that points
     * to THIS instance of Guacamole, but behind SSL termination that DOES NOT
     * require or request SSL/TLS client authentication.
     */
    private static final URIGuacamoleProperty SSL_PRIMARY_URI =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "ssl-primary-uri"; }

    };

    /**
     * The property representing the name of the header to use to retrieve the
     * URL-encoded client certificate from an HTTP request received from an
     * SSL termination service providing SSL/TLS client authentication.
     */
    private static final StringGuacamoleProperty SSL_CLIENT_CERTIFICATE_HEADER =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "ssl-client-certificate-header"; }

    };

    /**
     * The property representing the name of the header to use to retrieve the
     * verification status of the certificate an HTTP request received from an
     * SSL termination service providing SSL/TLS client authentication. This
     * value of this header must be "SUCCESS" (all uppercase) if the
     * certificate was successfully verified.
     */
    private static final StringGuacamoleProperty SSL_CLIENT_VERIFIED_HEADER =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "ssl-client-verified-header"; }

    };

    /**
     * The property representing the amount of time that a temporary
     * authentication token for SSL/TLS authentication may remain valid, in
     * minutes. This token is used to represent the user's asserted identity
     * after it has been verified by the SSL termination service. This interval
     * must be long enough to allow for network delays in receiving the token,
     * but short enough that unused tokens do not consume unnecessary server
     * resources and cannot potentially be guessed while the token is still
     * valid. These tokens are 256-bit secure random values.
     */
    private static final IntegerGuacamoleProperty SSL_MAX_TOKEN_VALIDITY =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "ssl-max-token-validity"; }

    };

    /**
     * The property defining the LDAP attribute or attributes that may be used
     * to represent a username within the subject DN of a user's X.509
     * certificate. If the least-significant attribute of the subject DN is not
     * one of these attributes, the certificate will be rejected. By default,
     * any attribute is accepted.
     */
    private static final StringListProperty SSL_SUBJECT_USERNAME_ATTRIBUTE =
            new StringListProperty () {

        @Override
        public String getName() { return "ssl-subject-username-attribute"; }

    };

    /**
     * The property defining the base DN containing all valid subject DNs. If
     * specified, only certificates asserting subject DNs beneath this base DN
     * will be accepted. By default, all DNs are accepted.
     */
    private static final LdapNameGuacamoleProperty SSL_SUBJECT_BASE_DN =
            new LdapNameGuacamoleProperty () {

        @Override
        public String getName() { return "ssl-subject-base-dn"; }

    };

    /**
     * The property representing the amount of time that the temporary, unique
     * subdomain generated for SSL/TLS authentication may remain valid, in
     * minutes. This subdomain is used to ensure each SSL/TLS authentication
     * attempt is fresh and does not potentially reuse a previous
     * authentication attempt that was cached by the browser or OS. This
     * interval must be long enough to allow for network delays in
     * authenticating the user with the SSL termination service that enforces
     * SSL/TLS client authentication, but short enough that an unused domain
     * does not consume unnecessary server resources and cannot potentially be
     * guessed while that subdomain is still valid. These subdomains are
     * 128-bit secure random values.
     */
    private static final IntegerGuacamoleProperty SSL_MAX_DOMAIN_VALIDITY =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "ssl-max-domain-validity"; }

    };

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * Returns a URI that should be used to authenticate users with SSL/TLS
     * client authentication. The returned URI will consist of the configured
     * client authentication URI with the wildcard portion ("*.") replaced with
     * the given subdomain.
     *
     * @param subdomain
     *     The subdomain that should replace the wildcard portion of the
     *     configured client authentication URI.
     *
     * @return
     *     A URI that should be used to authenticate users with SSL/TLS
     *     client authentication.
     *
     * @throws GuacamoleException
     *     If the required property for configuring the client authentication
     *     URI is missing or cannot be parsed.
     */
    public URI getClientAuthenticationURI(String subdomain) throws GuacamoleException {

        URI authURI = environment.getRequiredProperty(SSL_CLIENT_AUTH_URI);
        String baseHostname = authURI.getHost();

        // Add provided subdomain to auth URI
        return UriBuilder.fromUri(authURI)
                .host(subdomain + "." + baseHostname)
                .build();

    }

    /**
     * Given a hostname that was used by a user for SSL/TLS client
     * authentication, returns the subdomain at the beginning of that hostname.
     * If the hostname does not match the pattern of hosts represented by the
     * configured client authentication URI, null is returned.
     *
     * @param hostname
     *     The hostname to extract the subdomain from.
     *
     * @return
     *     The subdomain at the beginning of the provided hostname, if that
     *     hostname matches the pattern of hosts represented by the
     *     configured client authentication URI, or null otherwise.
     *
     * @throws GuacamoleException
     *     If the required property for configuring the client authentication
     *     URI is missing or cannot be parsed.
     */
    public String getClientAuthenticationSubdomain(String hostname) throws GuacamoleException {

        // Any hostname that matches the explicitly-specific primary URI is not
        // a client auth subdomain
        if (isPrimaryHostname(hostname))
            return null;

        URI authURI = environment.getRequiredProperty(SSL_CLIENT_AUTH_URI);
        String baseHostname = authURI.getHost();

        // Verify the first domain component is at least one character in
        // length
        int firstPeriod = hostname.indexOf('.');
        if (firstPeriod <= 0)
            return null;

        // Verify domain matches the configured auth URI except for the leading
        // subdomain
        if (!hostname.regionMatches(true, firstPeriod + 1, baseHostname, 0, baseHostname.length()))
            return null;

        // Extract subdomain
        return hostname.substring(0, firstPeriod);

    }

    /**
     * Returns the URI of this instance without SSL/TLS client authentication
     * required.
     *
     * @return
     *     The URI of this instance without SSL/TLS client authentication
     *     required.
     *
     * @throws GuacamoleException
     *     If the required property for configuring the primary URI is missing
     *     or cannot be parsed.
     */
    public URI getPrimaryURI() throws GuacamoleException {
        return environment.getRequiredProperty(SSL_PRIMARY_URI);
    }

    /**
     * Returns the HTTP request origin for requests originating from this
     * instance via the primary URI (as returned by {@link #getPrimaryURI()}.
     * This value is essentially the same as the primary URI but with only the
     * scheme, host, and port present.
     *
     * @return
     *     The HTTP request origin for requests originating from this instance
     *     via the primary URI.
     *
     * @throws GuacamoleException
     *     If the required property for configuring the primary URI is missing
     *     or cannot be parsed.
     */
    public URI getPrimaryOrigin() throws GuacamoleException {
        URI primaryURI = getPrimaryURI();
        try {
            return new URI(primaryURI.getScheme(), null, primaryURI.getHost(), primaryURI.getPort(), null, null, null);
        }
        catch (URISyntaxException e) {
            throw new GuacamoleServerException("Request origin could not be "
                    + "derived from the configured primary URI.", e);
        }
    }

    /**
     * Returns whether the given hostname is the same as the hostname in the
     * primary URI (as returned by {@link #getPrimaryURI()}. Hostnames are
     * case-insensitive.
     *
     * @param hostname
     *     The hostname to test.
     *
     * @return
     *     true if the hostname is the same as the hostname in the primary URI,
     *     false otherwise.
     *
     * @throws GuacamoleException
     *     If the required property for configuring the primary URI is missing
     *     or cannot be parsed.
     */
    public boolean isPrimaryHostname(String hostname) throws GuacamoleException {
        URI primaryURI = getPrimaryURI();
        return hostname.equalsIgnoreCase(primaryURI.getHost());
    }

    /**
     * Returns the name of the header to use to retrieve the URL-encoded client
     * certificate from an HTTP request received from an SSL termination
     * service providing SSL/TLS client authentication.
     *
     * @return
     *     The name of the header to use to retrieve the URL-encoded client
     *     certificate from an HTTP request received from an SSL termination
     *     service providing SSL/TLS client authentication.
     *
     * @throws GuacamoleException
     *     If the property for configuring the client certificate header cannot
     *     be parsed.
     */
    public String getClientCertificateHeader() throws GuacamoleException {
        return environment.getProperty(SSL_CLIENT_CERTIFICATE_HEADER, DEFAULT_CLIENT_CERTIFICATE_HEADER);
    }

    /**
     * Returns the name of the header to use to retrieve the verification
     * status of the certificate an HTTP request received from an SSL
     * termination service providing SSL/TLS client authentication.
     *
     * @return
     *     The name of the header to use to retrieve the verification
     *     status of the certificate an HTTP request received from an SSL
     *     termination service providing SSL/TLS client authentication.
     *
     * @throws GuacamoleException
     *     If the property for configuring the client verification header
     *     cannot be parsed.
     */
    public String getClientVerifiedHeader() throws GuacamoleException {
        return environment.getProperty(SSL_CLIENT_VERIFIED_HEADER, DEFAULT_CLIENT_VERIFIED_HEADER);
    }

    /**
     * Returns the maximum amount of time that the token generated by the
     * Guacamole server representing current SSL authentication state should
     * remain valid, in minutes. This imposes an upper limit on the amount of
     * time any particular authentication request can result in successful
     * authentication within Guacamole when SSL/TLS client authentication is
     * configured. By default, this will be 5.
     *
     * @return
     *     The maximum amount of time that an SSL authentication token
     *     generated by the Guacamole server should remain valid, in minutes.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public int getMaxTokenValidity() throws GuacamoleException {
        return environment.getProperty(SSL_MAX_TOKEN_VALIDITY, DEFAULT_MAX_TOKEN_VALIDITY);
    }

    /**
     * Returns the maximum amount of time that a unique client authentication
     * subdomain generated by the Guacamole server should remain valid, in
     * minutes. This imposes an upper limit on the amount of time any
     * particular authentication request can result in successful
     * authentication within Guacamole when SSL/TLS client authentication is
     * configured. By default, this will be 5.
     *
     * @return
     *     The maximum amount of time that a unique client authentication
     *     subdomain generated by the Guacamole server should remain valid, in
     *     minutes.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public int getMaxDomainValidity() throws GuacamoleException {
        return environment.getProperty(SSL_MAX_DOMAIN_VALIDITY, DEFAULT_MAX_DOMAIN_VALIDITY);
    }

    /**
     * Returns the base DN that contains all valid subject DNs. If there is no
     * such base DN (and all subject DNs are valid), null is returned.
     *
     * @return
     *     The base DN that contains all valid subject DNs, or null if all
     *     subject DNs are valid.
     *
     * @throws GuacamoleException
     *     If the configured base DN cannot be read or is not a valid LDAP DN.
     */
    public LdapName getSubjectBaseDN() throws GuacamoleException {
        return environment.getProperty(SSL_SUBJECT_BASE_DN);
    }

    /**
     * Returns a list of all attributes that may be used to represent a user's
     * username within their subject DN. If all attributes may be accepted,
     * null is returned.
     *
     * @return
     *     A list of all attributes that may be used to represent a user's
     *     username within their subject DN, or null if any attribute may be
     *     used.
     *
     * @throws GuacamoleException
     *     If the configured set of username attributes cannot be read.
     */
    public List<String> getSubjectUsernameAttributes() throws GuacamoleException {
        return environment.getProperty(SSL_SUBJECT_USERNAME_ATTRIBUTE);
    }

}
