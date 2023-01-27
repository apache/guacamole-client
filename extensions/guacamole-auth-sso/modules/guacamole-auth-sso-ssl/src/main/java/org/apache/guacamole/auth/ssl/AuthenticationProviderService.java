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
import com.google.inject.Provider;
import com.google.inject.Singleton;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;
import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.auth.ssl.conf.ConfigurationService;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.auth.sso.NonceService;
import org.apache.guacamole.auth.sso.SSOAuthenticationProviderService;
import org.apache.guacamole.auth.sso.user.SSOAuthenticatedUser;
import org.apache.guacamole.form.Field;
import org.apache.guacamole.form.RedirectField;
import org.apache.guacamole.language.TranslatableMessage;
import org.apache.guacamole.net.auth.Credentials;
import org.apache.guacamole.net.auth.credentials.CredentialsInfo;
import org.apache.guacamole.net.auth.credentials.GuacamoleInsufficientCredentialsException;
import org.apache.guacamole.net.auth.credentials.GuacamoleInvalidCredentialsException;

/**
 * Service that authenticates Guacamole users using SSL/TLS authentication
 * provided by an external SSL termination service.
 */
@Singleton
public class AuthenticationProviderService implements SSOAuthenticationProviderService {

    /**
     * Service for retrieving configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Service for validating and generating unique nonce values. Here, these
     * nonces are used specifically for generating unique domains.
     */
    @Inject
    private NonceService subdomainNonceService;

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
     * Provider for AuthenticatedUser objects.
     */
    @Inject
    private Provider<SSOAuthenticatedUser> authenticatedUserProvider;

    /**
     * The string value that the SSL termination service uses for its client
     * verification header to represent that the client certificate has been
     * verified.
     */
    private static final String CLIENT_VERIFIED_HEADER_SUCCESS_VALUE = "SUCCESS";

    /**
     * The name of the query parameter containing the temporary session token
     * representing the current state of an in-progress authentication attempt.
     */
    private static final String AUTH_SESSION_PARAMETER_NAME = "state";

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
     * @param credentials
     *     The credentials received by Guacamole in the authentication request.
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
    private SSOAuthenticatedUser authenticateUser(Credentials credentials,
            byte[] certificate) throws GuacamoleException {

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
        String username = dn.getRdn(numComponents - 1).getValue().toString();

        SSOAuthenticatedUser authenticatedUser = authenticatedUserProvider.get();
        authenticatedUser.init(username, credentials,
                Collections.emptySet(), Collections.emptyMap());
        return authenticatedUser;

    }

    /**
     * Processes the given HTTP request, returning the identity represented by
     * the auth session token present in that request. If no such token is
     * present, or the token does not represent a valid identity, null is
     * returned.
     *
     * @param request
     *     The HTTP request to process.
     *
     * @return
     *     The identity represented by the auth session token in the request,
     *     or null if there is no such token or the token does not represent a
     *     valid identity.
     */
    private SSOAuthenticatedUser processIdentity(HttpServletRequest request) {
        String state = request.getParameter(AUTH_SESSION_PARAMETER_NAME);
        return sessionManager.getIdentity(state);
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
    private String processCertificate(Credentials credentials,
            HttpServletRequest request) throws GuacamoleException {

        // Verify that SSL termination has already verified the certificate
        String verified = request.getHeader(confService.getClientVerifiedHeader());
        if (!CLIENT_VERIFIED_HEADER_SUCCESS_VALUE.equals(verified))
            return null;

        String certificate = request.getHeader(confService.getClientCertificateHeader());
        if (certificate == null)
            return null;

        SSOAuthenticatedUser authenticatedUser = authenticateUser(credentials, decode(certificate));
        long validityDuration = TimeUnit.MINUTES.toMillis(confService.getMaxTokenValidity());
        return sessionManager.defer(new SSLAuthenticationSession(authenticatedUser, validityDuration));

    }

    /**
     * Redirects the current user back to the main URL of the Guacamole
     * instance to continue the authentication process after having identified
     * themselves using SSL/TLS client authentication.
     *
     * @param token
     *     The authentication session token generated for the current user's
     *     identity.
     *
     * @throws GuacamoleException
     *     To redirect the user to the main URL of the Guacamole instance.
     */
    private void resumeAuthenticationAtRedirectURI(String token)
            throws GuacamoleException {

        URI redirectURI = UriBuilder.fromUri(confService.getRedirectURI())
                .queryParam(AUTH_SESSION_PARAMETER_NAME, token)
                .build();

        // Request that the provided credentials, now tokenized, be
        // resubmitted in that tokenized form to the original host for
        // authentication
        throw new GuacamoleInsufficientCredentialsException("Please "
                + "resubmit your tokenized credentials using the "
                + "following URI.",
            new CredentialsInfo(Arrays.asList(new Field[] {
                new RedirectField(AUTH_SESSION_PARAMETER_NAME, redirectURI,
                        new TranslatableMessage("LOGIN.INFO_IDP_REDIRECT_PENDING"))
            }))
        );

    }

    @Override
    public SSOAuthenticatedUser authenticateUser(Credentials credentials)
            throws GuacamoleException {

        //
        // Overall flow:
        //
        // 1) Unauthenticated user is given a temporary auth session token
        //    and redirected to the SSL termination instance that provides
        //    SSL client auth. This redirect uses a unique and temporary
        //    subdomain to ensure each SSL client auth attempt is fresh and
        //    does not use cached auth details.
        //
        // 2) Unauthenticated user with a temporary auth session token
        //    is validated by SSL termination, with that SSL termination
        //    adding HTTP headers containing the validated certificate to the
        //    user's HTTP request.
        //
        // 3) If valid, the user is assigned a temporary token and redirected
        //    back to the original URL. That temporary token is accepted by
        //    this extension at the original URL as proof of the user's
        //    identity.
        //
        // NOTE: All SSL termination endpoints in front of Guacamole MUST
        // be configured to drop these headers from any inbound requests
        // or users may be able to assert arbitrary identities, since this
        // extension does not validate anything but the certificate timestamps.
        // It relies purely on SSL termination to validate that the certificate
        // was signed by the expected CA.
        //

        // We can't authenticate using SSL/TLS client auth unless there's an
        // associated HTTP request
        HttpServletRequest request = credentials.getRequest();
        if (request == null)
            return null;

        // We MUST have the domain associated with the request to ensure we
        // always get fresh SSL sessions when validating client certificates
        String host = request.getHeader("Host");
        if (host == null)
            return null;

        //
        // Handle only auth session tokens at the main redirect URI, using the
        // pre-verified information from those tokens to determine user
        // identity.
        //

        String redirectHost = confService.getRedirectURI().getHost();
        if (host.equals(redirectHost)) {

            SSOAuthenticatedUser user = processIdentity(request);
            if (user != null)
                return user;

            // Redirect unauthenticated requests to the endpoint requiring
            // SSL client auth to request identity verification
            throw new GuacamoleInvalidCredentialsException("Invalid login.",
                new CredentialsInfo(Arrays.asList(new Field[] {
                    new RedirectField(AUTH_SESSION_PARAMETER_NAME, getLoginURI(), // <-- Each call to getLoginURI() produces a unique subdomain that is valid only for ONE use (see below)
                            new TranslatableMessage("LOGIN.INFO_IDP_REDIRECT_PENDING"))
                }))
            );

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

        else if (subdomainNonceService.isValid(confService.getClientAuthenticationSubdomain(host))) {
            String token = processCertificate(credentials, request);
            if (token != null)
                resumeAuthenticationAtRedirectURI(token);
        }

        // All other requests are not allowed - refuse to authenticate
        throw new GuacamoleClientException("Direct authentication against "
                + "this endpoint is not valid without first requesting to "
                + "authenticate at the primary URL of this Guacamole "
                + "instance.");

    }

    @Override
    public URI getLoginURI() throws GuacamoleException {
        long validityDuration = TimeUnit.MINUTES.toMillis(confService.getMaxDomainValidity());
        String uniqueSubdomain = subdomainNonceService.generate(validityDuration);
        return confService.getClientAuthenticationURI(uniqueSubdomain);
    }

    @Override
    public void shutdown() {
        sessionManager.shutdown();
    }

}
