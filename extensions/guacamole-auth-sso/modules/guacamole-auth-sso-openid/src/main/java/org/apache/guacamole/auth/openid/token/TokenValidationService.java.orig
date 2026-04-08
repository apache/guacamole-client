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

package org.apache.guacamole.auth.openid.token;

import com.google.inject.Inject;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.UriBuilder;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.openid.conf.ConfigurationService;
import org.apache.guacamole.auth.sso.NonceService;
import org.apache.guacamole.token.TokenName;
import org.jose4j.json.JsonUtil;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service for validating ID tokens forwarded to us by the client, verifying
 * that they did indeed come from the OpenID service.
 */
public class TokenValidationService {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(TokenValidationService.class);

    /**
     * The prefix to use when generating token names.
     */
    public static final String OIDC_ATTRIBUTE_TOKEN_PREFIX = "OIDC_";

    /**
     * Service for retrieving OpenID configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Service for validating and generating unique nonce values.
     */
    @Inject
    private NonceService nonceService;

    /**
     * Validates the given ID token, using implicit flow, returning the JwtClaims
     * contained therein. If the ID token is invalid, null is returned.
     *
     * @param token
     *     The ID token to validate.
     *
     * @return
     *     The JWT claims contained within the given ID token if it passes tests,
     *     or null if the token is not valid.
     *
     * @throws GuacamoleException
     *     If guacamole.properties could not be parsed.
     */
    public JwtClaims validateToken(String token) throws GuacamoleException {
        // Validating the token requires a JWKS key resolver
        HttpsJwks jwks = new HttpsJwks(confService.getJWKSEndpoint().toString());
        HttpsJwksVerificationKeyResolver resolver = new HttpsJwksVerificationKeyResolver(jwks);

        // Create JWT consumer for validating received token
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setMaxFutureValidityInMinutes(confService.getMaxTokenValidity())
                .setAllowedClockSkewInSeconds(confService.getAllowedClockSkew())
                .setRequireSubject()
                .setExpectedIssuer(confService.getIssuer())
                .setExpectedAudience(confService.getClientID())
                .setVerificationKeyResolver(resolver)
                .build();

        try {
            // Validate JWT
            JwtClaims claims = jwtConsumer.processToClaims(token);

            // Verify a nonce is present
            String nonce = claims.getStringClaimValue("nonce");
            if (nonce != null) {
                // Verify that we actually generated the nonce, and that it has not
                // already been used
                if (nonceService.isValid(nonce)) {
                    // nonce is valid, consider claims valid
                    return claims;
                }
                else {
                    logger.info("Rejected OpenID token with invalid/old nonce.");
                }
            }
            else {
                logger.info("Rejected OpenID token without nonce.");
            }
        }
        // Log any failures to validate/parse the JWT
        catch (MalformedClaimException e) {
            logger.info("Rejected OpenID token with malformed claim: {}", e.getMessage(), e);
        }
        catch (InvalidJwtException e) {
            logger.info("Rejected invalid OpenID token: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * Validates the given ID token, using code flow, returning the JwtClaims
     * contained therein. If the ID token is invalid, null is returned.
     *
     * @param code
     *     The code to validate and receive the id_token.
     *
     * @param verifier
     *     A PKCE verifier or null if not used.
     *
     * @return
     *     The JWT claims contained within the given ID token if it passes tests,
     *     or null if the token is not valid.
     *
     * @throws GuacamoleException
     *     If guacamole.properties could not be parsed.
     */
    public JwtClaims validateCode(String code, String verifier) throws GuacamoleException {
        // Validating the token requires a JWKS key resolver
        HttpsJwks jwks = new HttpsJwks(confService.getJWKSEndpoint().toString());
        HttpsJwksVerificationKeyResolver resolver = new HttpsJwksVerificationKeyResolver(jwks);

        /* Exchange code → token */
        String token = exchangeCode(code, verifier);

        // Create JWT consumer for validating received token
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setMaxFutureValidityInMinutes(confService.getMaxTokenValidity())
                .setAllowedClockSkewInSeconds(confService.getAllowedClockSkew())
                .setRequireSubject()
                .setExpectedIssuer(confService.getIssuer())
                .setExpectedAudience(confService.getClientID())
                .setVerificationKeyResolver(resolver)
                .build();

        try {
            // Validate JWT
            return jwtConsumer.processToClaims(token);
        }
        // Log any failures to validate/parse the JWT
        catch (InvalidJwtException e) {
            logger.info("Rejected invalid OpenID token: {}", e.getMessage(), e);
        }

        return null;
    }

    /**
     * URLEncodes a key/value pair
     *
     * @param key
     *     The key to encode
     *
     * @param value
     *     The value to encode
     *
     * @return
     *     The urlencoded kay/value pair
     */
     private String urlencode(String key, String value) {
         StringBuilder builder = new StringBuilder();
         return builder.append(URLEncoder.encode(key, StandardCharsets.UTF_8))
                       .append("=")
                       .append(URLEncoder.encode(value, StandardCharsets.UTF_8))
                       .toString();
     }

    /**
     * Exchanges the authorization code for tokens.
     *
     * @param code
     *     The authorization code received from the IdP.
     * @param codeVerifier
     *     The PKCE verifier (or null if PKCE is disabled).
     *
     * @return
     *     The token string returned.
     *
     * @throws GuacamoleException
     *     If a valid token is not returned.
     */
    private String exchangeCode(String code, String verifier) throws GuacamoleException {

        try {
            StringBuilder bodyBuilder = new StringBuilder();
            bodyBuilder.append(urlencode("grant_type", "authorization_code")).append("&");
            bodyBuilder.append(urlencode("code", code)).append("&");
            bodyBuilder.append(urlencode("redirect_uri", confService.getRedirectURI().toString())).append("&");
            bodyBuilder.append(urlencode("scope", confService.getScope())).append("&");
            bodyBuilder.append(urlencode("client_id", confService.getClientID()));

            String clientSecret = confService.getClientSecret();
            if (clientSecret != null && !clientSecret.trim().isEmpty()) {
                bodyBuilder.append("&").append(urlencode("client_secret", clientSecret));
            }

            if (confService.isPKCERequired()) {
                bodyBuilder.append("&").append(urlencode("code_verifier", verifier));
            }

            // Build the final URI and convert to a URL
            URL url = confService.getTokenEndpoint().toURL();

            // Open connection, using HttpURLConnection
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"
            );

            try (OutputStream out = conn.getOutputStream()) {
                byte [] body = bodyBuilder.toString().getBytes(StandardCharsets.UTF_8);
                out.write(body, 0, body.length);
            }

            // Read response
            int status = conn.getResponseCode();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            status >= 200 && status < 300
                                    ? conn.getInputStream()
                                    : conn.getErrorStream(),
                            StandardCharsets.UTF_8
                    )
            );

            StringBuilder responseBody = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseBody.append(line);
            }
            reader.close();

            Map<String,Object> json = JsonUtil.parseJson(responseBody.toString());

            if (status < 200 || status >= 300) {
                throw new GuacamoleException("Token endpoint error (" + status + "): " + json.toString());
            }

            return (String) json.get("id_token");

        } catch (Exception e) {
            logger.info("Rejected invalid OpenID code exchange: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Parses the given JwtClaims, returning the username contained
     * therein, as defined by the username claim type given in
     * guacamole.properties. If the username claim type is missing or
     * is invalid, null is returned.
     *
     * @param claims
     *     A valid JwtClaims to extract the username from.
     *
     * @return
     *     The username contained within the given JwtClaims, or null if the
     *     claim is not valid or the username claim type is missing,
     *
     * @throws GuacamoleException
     *     If guacamole.properties could not be parsed.
     */
    public String processUsername(JwtClaims claims) throws GuacamoleException {
        String usernameClaim = confService.getUsernameClaimType();

        if (claims != null) {
            try {
                // Pull username from claims
                String username = claims.getStringClaimValue(usernameClaim);
                if (username != null)
                    return username;
            }
            catch (MalformedClaimException e) {
                logger.info("Rejected OpenID token with malformed claim: {}", e.getMessage(), e);
            }

            // Warn if username was not present in token, as it likely means
            // the system is not set up correctly
            logger.warn("Username claim \"{}\" missing from token. Perhaps the "
                    + "OpenID scope and/or username claim type are "
                    + "misconfigured?", usernameClaim);
        }

        // Could not retrieve username from JWT
        return null;
    }

    /**
     * Parses the given JwtClaims, returning the groups contained
     * therein, as defined by the groups claim type given in
     * guacamole.properties. If the groups claim type is missing or
     * is invalid, an empty set is returned.
     *
     * @param claims
     *     A valid JwtClaims to extract groups from.
     *
     * @return
     *     A Set of String representing the groups the user is member of
     *     from the OpenID provider point of view, or an empty Set if
     *     claim is not valid or the groups claim type is missing,
     *
     * @throws GuacamoleException
     *     If guacamole.properties could not be parsed.
     */
    public Set<String> processGroups(JwtClaims claims) throws GuacamoleException {
        String groupsClaim = confService.getGroupsClaimType();

        if (claims != null) {
            try {
                // Pull groups from claims
                List<String> oidcGroups = claims.getStringListClaimValue(groupsClaim);
                if (oidcGroups != null && !oidcGroups.isEmpty())
                    return Collections.unmodifiableSet(new HashSet<>(oidcGroups));
            }
            catch (MalformedClaimException e) {
                logger.info("Rejected OpenID token with malformed claim: {}", e.getMessage(), e);
            }
        }

        // Could not retrieve groups from JWT
        return Collections.emptySet();
    }

    /**
     * Parses the given JwtClaims, returning the attributes contained
     * therein, as defined by the attributes claim type given in
     * guacamole.properties. If the attributes claim type is missing or
     * is invalid, an empty set is returned.
     *
     * @param claims
     *     A valid JwtClaims to extract attributes from.
     *
     * @return
     *     A Map of String,String representing the attributes and values
     *     from the OpenID provider point of view, or an empty Map if
     *     claim is not valid or the attributes claim type is missing.
     *
     * @throws GuacamoleException
     *     If guacamole.properties could not be parsed.
     */
    public Map<String, String> processAttributes(JwtClaims claims) throws GuacamoleException {
        Collection<String> attributesClaim = confService.getAttributesClaimType();

        if (claims != null && !attributesClaim.isEmpty()) {
            try {
                logger.debug("Iterating over attributes claim list : {}", attributesClaim);

                // We suppose all claims are resolved, so the hashmap is initialised to
                // the size of the configuration list
                Map<String, String> tokens = new HashMap<String, String>(attributesClaim.size());

                // We iterate over the configured attributes
                for (String key: attributesClaim) {
                    // Retrieve the corresponding claim
                    String oidcAttr = claims.getStringClaimValue(key);

                    // We do have a matching claim and it is not empty
                    if (oidcAttr != null && !oidcAttr.isEmpty()) {
                        // append the prefixed claim value to the token map with its value
                        String tokenName = TokenName.canonicalize(key, OIDC_ATTRIBUTE_TOKEN_PREFIX);
                        tokens.put(tokenName, oidcAttr);
                        logger.debug("Claim {} found and set to {}", key, tokenName);
                    }
                    else {
                        // wanted attribute is not found in the claim
                        logger.debug("Claim {} not found in JWT.", key);
                    }
                }

                // We did process all the expected claims
                return Collections.unmodifiableMap(tokens);
            }
            catch (MalformedClaimException e) {
                logger.info("Rejected OpenID token with malformed claim: {}", e.getMessage(), e);
            }
        }

        // Could not retrieve attributes from JWT
        logger.debug("Attributes claim not defined. Returning empty map.");
        return Collections.emptyMap();
    }
}
