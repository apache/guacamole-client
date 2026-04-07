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

package org.apache.guacamole.auth.openid.util;

import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Utility class for generating PKCE parameters.
 *
 * Supports:
 *   - code_verifier (random Base64URL)
 *   - code_challenge (S256)
 */
public final class PKCEUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PKCEUtil() {}

    /**
     * Generates a high-entropy PKCE code_verifier.
     */
    public static String generateCodeVerifier() {
        byte[] bytes = new byte[64];
        RANDOM.nextBytes(bytes);
        return base64Url(bytes);
    }

    /**
     * Computes the PKCE code_challenge = BASE64URL(SHA256(code_verifier)).
     */
    public static String generateCodeChallenge(String verifier) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] hash = sha.digest(verifier.getBytes("US-ASCII"));
        return base64Url(hash);
    }

    /**
     * Base64URL encoding without padding.
     */
    public static String base64Url(byte[] bytes) {
        return java.util.Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }
}
