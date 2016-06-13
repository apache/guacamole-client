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

package org.apache.guacamole.auth.oauth.token;

import com.google.inject.Inject;
import org.apache.guacamole.auth.oauth.conf.ConfigurationService;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleSecurityException;
import org.apache.guacamole.GuacamoleServerException;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;

/**
 * Service for validating ID tokens forwarded to us by the client, verifying
 * that they did indeed come from the OAuth service.
 */
public class TokenValidationService {

    /**
     * Service for retrieving OAuth configuration information.
     */
    @Inject
    private ConfigurationService confService;

    /**
     * Validates and parses the given ID token, returning the username contained
     * therein, as defined by the username claim type given in
     * guacamole.properties. If the username claim type is missing or the ID
     * token is invalid, an exception is thrown instead.
     *
     * @param token
     *     The ID token to validate and parse.
     *
     * @return
     *     The username contained within the given ID token.
     *
     * @throws GuacamoleException
     *     If the ID token is not valid, the username claim type is missing, or
     *     guacamole.properties could not be parsed.
     */
    public String processUsername(String token) throws GuacamoleException {

        // Validating the token requires a JWKS key resolver
        HttpsJwks jwks = new HttpsJwks(confService.getJWKSEndpoint());
        HttpsJwksVerificationKeyResolver resolver = new HttpsJwksVerificationKeyResolver(jwks);

        // Create JWT consumer for validating received token
        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                .setRequireExpirationTime()
                .setMaxFutureValidityInMinutes(300)
                .setAllowedClockSkewInSeconds(30)
                .setRequireSubject()
                .setExpectedIssuer(confService.getIssuer())
                .setExpectedAudience(confService.getClientID())
                .setVerificationKeyResolver(resolver)
                .build();

        try {

            // Validate JWT
            JwtClaims claims = jwtConsumer.processToClaims(token);

            // Pull username from claims
            String username = claims.getStringClaimValue(confService.getUsernameClaimType());
            if (username == null)
                throw new GuacamoleSecurityException("Username missing from token");

            // Username successfully retrieved from the JWT
            return username;

        }

        // Rethrow any failures to validate/parse the JWT
        catch (InvalidJwtException e) {
            throw new GuacamoleSecurityException("Invalid ID token.", e);
        }
        catch (MalformedClaimException e) {
            throw new GuacamoleServerException("Unable to parse JWT claims.", e);
        }

    }

}
