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


package org.apache.guacamole.auth.jdbc.reset;

import com.google.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.language.TranslatableGuacamoleClientTooManyException;

/**
 * Issues, validates and consumes password reset tokens. A token is 32 random
 * bytes, URL-safe base64 encoded; only its SHA-256 hash reaches the database.
 */
public class PasswordResetTokenStore {

    /**
     * The number of random bytes in a token.
     */
    private static final int TOKEN_BYTES = 32;

    /**
     * Length of the rate limit window, in milliseconds.
     */
    private static final long HOUR = 60L * 60L * 1000L;

    /**
     * How long token rows are kept before being deleted, in milliseconds.
     */
    private static final long RETENTION = 24L * HOUR;

    /**
     * Source of token randomness.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Mapper for the token table.
     */
    private final PasswordResetTokenMapper mapper;

    /**
     * Creates a store backed by the given mapper.
     *
     * @param mapper
     *     The mapper for the token table.
     */
    @Inject
    public PasswordResetTokenStore(PasswordResetTokenMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Generates a new random token.
     *
     * @return
     *     A URL-safe base64 token encoding 32 random bytes.
     */
    public static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Returns the SHA-256 hash of the given token, the only form in which a
     * token is stored or looked up.
     *
     * @param token
     *     The token to hash.
     *
     * @return
     *     The 32-byte SHA-256 hash of the token's UTF-8 bytes.
     */
    public static byte[] hash(String token) {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable.", e);
        }
    }

    /**
     * Issues a new token for the given user, invalidating any older unused
     * token of theirs.
     *
     * @param userID
     *     The ID of the user.
     *
     * @param ttlMinutes
     *     How long the token stays valid, in minutes.
     *
     * @param maxPerHour
     *     The maximum number of tokens the user may be issued per hour.
     *
     * @param now
     *     The current time.
     *
     * @return
     *     The new token, in the clear. It must only ever be sent to the user.
     *
     * @throws GuacamoleException
     *     If the user has already been issued maxPerHour tokens within the
     *     last hour.
     */
    public String issue(Integer userID, int ttlMinutes, int maxPerHour, Date now)
            throws GuacamoleException {

        mapper.deleteIssuedBefore(new Date(now.getTime() - RETENTION));

        if (mapper.countIssuedSince(userID, new Date(now.getTime() - HOUR)) >= maxPerHour)
            throw new TranslatableGuacamoleClientTooManyException("Too many "
                    + "password reset requests for this user.",
                    "PASSWORD_RESET.ERROR_TOO_MANY");

        mapper.invalidateUnused(userID, now);

        String token = generateToken();
        PasswordResetTokenModel model = new PasswordResetTokenModel();
        model.setTokenHash(hash(token));
        model.setUserID(userID);
        model.setCreated(now);
        model.setExpires(new Date(now.getTime() + ttlMinutes * 60L * 1000L));
        mapper.insert(model);

        return token;

    }

    /**
     * Returns the username of the user the given token belongs to, if the
     * token is valid. The token is not consumed.
     *
     * @param token
     *     The token presented by the user.
     *
     * @param now
     *     The current time.
     *
     * @return
     *     The username, or null if the token is unknown, used or expired.
     */
    public String findUsername(String token, Date now) {
        PasswordResetTokenModel model = mapper.selectValid(hash(token), now);
        return model == null ? null : model.getUsername();
    }

    /**
     * Consumes the given token. Succeeds at most once per token.
     *
     * @param token
     *     The token presented by the user.
     *
     * @param now
     *     The current time.
     *
     * @return
     *     true if the token was valid and is now used, false otherwise.
     */
    public boolean consume(String token, Date now) {
        return mapper.consume(hash(token), now) == 1;
    }

}
