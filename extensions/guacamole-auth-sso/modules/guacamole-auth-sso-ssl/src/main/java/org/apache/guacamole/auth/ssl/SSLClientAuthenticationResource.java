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
package org.apache.guacamole.auth.ssl;

import com.google.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.ws.rs.GET;
import javax.ws.rs.core.Response;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleResourceNotFoundException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.ssl.conf.ConfigurationService;
import org.apache.guacamole.auth.sso.NonceService;
import org.apache.guacamole.auth.sso.SSOResource;

/**
 * REST API resource that allows the user to retrieve an opaque state value
 * representing their identity as determined by SSL/TLS client authentication.
 * The opaque value may represent a valid identity or an authentication
 * failure, and must be resubmitted within a normal Guacamole authentication
 * request to finalize the authentication process.
 */
public class SSLClientAuthenticationResource extends SSOResource {

    /**
     * The string value that the SSL termination service uses for its client
     * verification header to represent that the client certificate has been
     * verified.
     */
    private static final String CLIENT_VERIFIED_HEADER_SUCCESS_VALUE = "SUCCESS";

    /**
     * Service for retrieving configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Session manager for generating and maintaining unique tokens to
     * represent the authentication flow of a user who has only partially
     * authenticated. Here, these tokens represent a user that has been
     * validated by SSL termination and allow the Guacamole instance that
     * doesn't require SSL/TLS authentication to retrieve the user's identity
     * and complete the authentication process.
     */
    @Inject
    private SSLAuthenticationSessionManager sessionManager;

    /**
     * Service for validating and generating unique nonce values. Here, these
     * nonces are used specifically for generating unique domains.
     */
    @Inject
    private NonceService subdomainNonceService;

    /**
     * Retrieves a single value from the HTTP header having the given name. If
     * there are multiple HTTP headers present with this name, the first
     * matching header in the request is used. If there are no such headers in
     * the request, null is returned.
     *
     * @param headers
     *     The HTTP headers present in the request.
     *
     * @param name
     *     The name of the header to retrieve.
     *
     * @return
     *     The first value of the HTTP header having the given name, or null if
     *     there is no such header.
     */
    private String getHeader(HttpHeaders headers, String name) {

        List<String> values = headers.getRequestHeader(name);
        if (values.isEmpty())
            return null;

        return values.get(0);

    }

    /**
     * Decodes the provided URL-encoded string as UTF-8, returning the result.
     *
     * @param value
     *     The URL-encoded string to decode.
     *
     * @return
     *     The decoded string.
     *
     * @throws GuacamoleException
     *     If the provided value is not a value URL-encoded string.
     */
    private byte[] decode(String value) throws GuacamoleException {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name())
                    .getBytes(StandardCharsets.UTF_8);
        }
        catch (IllegalArgumentException e) {
            throw new GuacamoleClientException("Invalid URL-encoded value.", e);
        }
        catch (UnsupportedEncodingException e) {
            // This should never happen, as UTF-8 is a standard charset that
            // the JVM is required to support
            throw new UnsupportedOperationException("Unexpected lack of UTF-8 support.", e);
        }
    }

    /**
     * Authenticates a user using HTTP headers containing that user's verified
     * X.509 certificate. It is assumed that this certificate is being passed
     * to Guacamole from an SSL termination service that has already verified
     * that this certificate is valid and authorized for access to that
     * Guacamole instance.
     *
     * @param certificate
     *     The raw bytes of the X.509 certificate retrieved from the request.
     *
     * @return
     *     A new SSOAuthenticatedUser representing the identity of the user
     *     asserted by the SSL termination service via that user's X.509
     *     certificate.
     *
     * @throws GuacamoleException
     *     If the provided X.509 certificate is not valid or cannot be parsed.
     *     It is expected that the SSL termination service will already have
     *     validated the certificate; this function validates only the
     *     certificate timestamps.
     */
    public String getUsername(byte[] certificate) throws GuacamoleException {

        // Parse and re-verify certificate is valid with respect to timestamps
        X509Certificate cert;
        try (InputStream input = new ByteArrayInputStream(certificate)) {

            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            cert = (X509Certificate) certFactory.generateCertificate(input);

            // Verify certificate is valid (it should be given pre-validation
            // from SSL termination, but it's worth rechecking for sanity)
            cert.checkValidity();

        }
        catch (CertificateException e) {
            throw new GuacamoleClientException("The X.509 certificate "
                    + "presented is not valid.", e);
        }
        catch (IOException e) {
            throw new GuacamoleServerException("Provided X.509 certificate "
                    + "could not be read.", e);
        }

        // Extract user's DN from their X.509 certificate
        LdapName dn;
        try {
            Principal principal = cert.getSubjectX500Principal();
            dn = new LdapName(principal.getName());
        }
        catch (InvalidNameException e) {
            throw new GuacamoleClientException("The X.509 certificate "
                    + "presented does not contain a valid subject DN.", e);
        }

        // Verify DN actually contains components
        int numComponents = dn.size();
        if (numComponents < 1)
            throw new GuacamoleClientException("The X.509 certificate "
                    + "presented contains an empty subject DN.");

        // Simply use first component of DN as username (TODO: Enforce
        // requirements on the attribute providing the username and the base DN,
        // and consider using components following the username to determine
        // group memberships)
        return dn.getRdn(numComponents - 1).getValue().toString();

    }

    /**
     * Processes the X.509 certificate in the headers of the given HTTP
     * request, returning an authentication session token representing the
     * identity in that certificate. If the certificate is invalid or not
     * present, null is returned.
     *
     * @param credentials
     *     The credentials submitted in the HTTP request being processed.
     *
     * @param request
     *     The HTTP request to process.
     *
     * @return
     *     An authentication session token representing the identity in the
     *     certificate in the given HTTP request, or null if the request does
     *     not contain a valid certificate.
     *
     * @throws GuacamoleException
     *     If any configuration parameters related to retrieving certificates
     *     from HTTP request cannot be parsed.
     */
    private String processCertificate(HttpHeaders headers) throws GuacamoleException {

        //
        // NOTE: A result with an associated state is ALWAYS returned by
        // processCertificate(), even if the request does not actually contain
        // a valid certificate. This is by design and ensures that the nature
        // of a certificate (valid vs. invalid) cannot be determined except
        // via Guacamole's authentication endpoint, thus allowing auth failure
        // hooks to consider attempts to use invalid certificates as auth
        // failures.
        //

        // Verify that SSL termination has already verified the certificate
        String verified = getHeader(headers, confService.getClientVerifiedHeader());
        if (!CLIENT_VERIFIED_HEADER_SUCCESS_VALUE.equals(verified))
            return sessionManager.generateInvalid();

        String certificate = getHeader(headers, confService.getClientCertificateHeader());
        if (certificate == null)
            return sessionManager.generateInvalid();

        String username = getUsername(decode(certificate));
        long validityDuration = TimeUnit.MINUTES.toMillis(confService.getMaxTokenValidity());
        return sessionManager.defer(new SSLAuthenticationSession(username, validityDuration));

    }

    /**
     * Attempts to authenticate the current user using SSL/TLS client
     * authentication, returning an opaque value that represents their
     * authenticated status. If necessary, the user is first redirected to a
     * unique endpoint that supports SSL/TLS client authentication.
     *
     * @param headers
     *     All HTTP headers submitted in the user's authentication request.
     *
     * @param host
     *     The hostname that the user specified in their HTTP request.
     *
     * @return
     *     A Response containing an opaque value representing the user's
     *     authenticated status, or a Response redirecting the user to a
     *     unique endpoint that can provide this.
     *
     * @throws GuacamoleException
     *     If any required configuration information is missing or cannot be
     *     parsed, or if the request was not received at a valid subdomain.
     */
    @GET
    @Path("identity")
    public Response authenticateClient(@Context HttpHeaders headers,
            @HeaderParam("Host") String host) throws GuacamoleException {

        // Redirect any requests to the domain that does NOT require SSL/TLS
        // client authentication to the same endpoint at a domain that does
        // require SSL/TLS authentication
        String subdomain = confService.getClientAuthenticationSubdomain(host);
        if (subdomain == null) {

            long validityDuration = TimeUnit.MINUTES.toMillis(confService.getMaxDomainValidity());
            String uniqueSubdomain = subdomainNonceService.generate(validityDuration);

            URI clientAuthURI = UriBuilder.fromUri(confService.getClientAuthenticationURI(uniqueSubdomain))
                    .path("api/ext/ssl/identity")
                    .build();

            return Response.seeOther(clientAuthURI).build();

        }

        //
        // Process certificates only at valid single-use subdomains dedicated
        // to client authentication, redirecting back to the main redirect URI
        // for final authentication if that processing is successful.
        //
        // NOTE: This is CRITICAL. If unique subdomains are not generated and
        // tied to strictly one authentication attempt, then those subdomains
        // could be reused by a user on a shared machine to assume the cached
        // credentials of another user that used that machine earlier. The
        // browser and/or OS may cache the certificate so that it can be reused
        // for future SSL sessions to that same domain. Here, we ensure each
        // generated domain is unique and only valid for certificate processing
        // ONCE. The domain may still be valid with DNS, but will no longer be
        // usable for certificate authentication.
        //

        if (subdomainNonceService.isValid(subdomain))
            return Response.ok(new OpaqueAuthenticationResult(processCertificate(headers)))
                    .header("Access-Control-Allow-Origin", confService.getPrimaryOrigin().toString())
                    .type(MediaType.APPLICATION_JSON)
                    .build();

        throw new GuacamoleResourceNotFoundException("No such resource.");

    }

}
