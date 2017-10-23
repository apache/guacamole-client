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
import org.apache.guacamole.auth.openid.conf.ConfigurationService;
import org.apache.guacamole.GuacamoleException;
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
     * Validates and parses the given ID token, returning the username contained
     * therein, as defined by the username claim type given in
     * guacamole.properties. If the username claim type is missing or the ID
     * token is invalid, null is returned.
     *
     * @param token
     *     The ID token to validate and parse.
     *
     * @return
     *     The username contained within the given ID token, or null if the ID
     *     token is not valid or the username claim type is missing,
     *
     * @throws GuacamoleException
     *     If guacamole.properties could not be parsed.
     */
    public String processUsername(String token) throws GuacamoleException {

        // Validating the token requires a JWKS key resolver
        HttpsJwks jwks = new HttpsJwks(confService.getJWKSEndpoint());
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

            String usernameClaim = confService.getUsernameClaimType();

            // Validate JWT
            JwtClaims claims = jwtConsumer.processToClaims(token);

            // Verify a nonce is present
            String nonce = claims.getStringClaimValue("nonce");
            if (nonce == null) {
                logger.info("Rejected OpenID token without nonce.");
                return null;
            }

            // Verify that we actually generated the nonce, and that it has not
            // already been used
            if (!nonceService.isValid(nonce)) {
                logger.debug("Rejected OpenID token with invalid/old nonce.");
                return null;
            }

            // Pull username from claims
            String username = claims.getStringClaimValue(usernameClaim);
            if (username != null)
                return username;

            // Warn if username was not present in token, as it likely means
            // the system is not set up correctly
            logger.warn("Username claim \"{}\" missing from token. Perhaps the "
                    + "OpenID scope and/or username claim type are "
                    + "misconfigured?", usernameClaim);

        }

        // Log any failures to validate/parse the JWT
        catch (InvalidJwtException e) {
            logger.info("Rejected invalid OpenID token: {}", e.getMessage());
            logger.debug("Invalid JWT received.", e);
        }
        catch (MalformedClaimException e) {
            logger.info("Rejected OpenID token with malformed claim: {}", e.getMessage());
            logger.debug("Malformed claim within received JWT.", e);
        }

        // Could not retrieve username from JWT
        return null;

    }

}
