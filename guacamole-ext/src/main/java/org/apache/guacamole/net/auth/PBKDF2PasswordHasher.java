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

package org.apache.guacamole.net.auth;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hashes and verifies passwords using PBKDF2 with HMAC-SHA256, as provided by
 * the JDK. Unlike a single pass of a fast digest like SHA-256, PBKDF2 has a
 * configurable iteration count, making stolen hashes expensive to crack.
 *
 * Hashes may also be represented as a self-describing string of the form
 * "pbkdf2-sha256$&lt;iterations&gt;$&lt;salt&gt;$&lt;hash&gt;", where the
 * salt and hash are standard (RFC 4648) base64. The length of the hash
 * determines the length of the derived key.
 */
public final class PBKDF2PasswordHasher {

    /**
     * The JDK name of the key derivation algorithm used by this class.
     */
    public static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    /**
     * The prefix identifying the self-describing string representation of a
     * PBKDF2-HMAC-SHA256 hash.
     */
    public static final String PREFIX = "pbkdf2-sha256";

    /**
     * The default number of iterations, per OWASP's current guidance for
     * PBKDF2-HMAC-SHA256.
     */
    public static final int DEFAULT_ITERATIONS = 600000;

    /**
     * The length of newly-generated hashes and salts, in bytes.
     */
    public static final int DEFAULT_LENGTH = 32;

    /**
     * Source of randomness for newly-generated salts.
     */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * This class is a utility class and may not be instantiated.
     */
    private PBKDF2PasswordHasher() {}

    /**
     * Generates a new random salt of {@link #DEFAULT_LENGTH} bytes.
     *
     * @return
     *     A new random salt.
     */
    public static byte[] generateSalt() {
        byte[] salt = new byte[DEFAULT_LENGTH];
        RANDOM.nextBytes(salt);
        return salt;
    }

    /**
     * Hashes the given password using PBKDF2-HMAC-SHA256.
     *
     * @param password
     *     The password to hash.
     *
     * @param salt
     *     The salt to use, which must not be empty.
     *
     * @param iterations
     *     The number of iterations, which must be positive.
     *
     * @param length
     *     The length of the hash to produce, in bytes, which must be positive.
     *
     * @return
     *     The resulting hash.
     *
     * @throws IllegalArgumentException
     *     If the salt, iteration count, or length is invalid.
     */
    public static byte[] hash(String password, byte[] salt, int iterations,
            int length) throws IllegalArgumentException {

        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt,
                iterations, length * 8);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM)
                    .generateSecret(spec).getEncoded();
        }
        catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("Unexpected lack of " + ALGORITHM + " support.", e);
        }
        catch (InvalidKeySpecException e) {
            throw new IllegalArgumentException("Invalid PBKDF2 parameters.", e);
        }
        finally {
            spec.clearPassword();
        }

    }

    /**
     * Hashes the given password using PBKDF2-HMAC-SHA256, producing a hash of
     * {@link #DEFAULT_LENGTH} bytes.
     *
     * @param password
     *     The password to hash.
     *
     * @param salt
     *     The salt to use, which must not be empty.
     *
     * @param iterations
     *     The number of iterations, which must be positive.
     *
     * @return
     *     The resulting hash.
     */
    public static byte[] hash(String password, byte[] salt, int iterations) {
        return hash(password, salt, iterations, DEFAULT_LENGTH);
    }

    /**
     * Returns whether the given password matches the given hash, using a
     * constant-time comparison. A null password never matches.
     *
     * @param password
     *     The password to verify.
     *
     * @param salt
     *     The salt that was used to produce the hash.
     *
     * @param iterations
     *     The number of iterations that was used to produce the hash.
     *
     * @param expected
     *     The hash to compare against.
     *
     * @return
     *     true if the password matches, false otherwise.
     */
    public static boolean verify(String password, byte[] salt, int iterations,
            byte[] expected) {

        if (password == null)
            return false;

        return MessageDigest.isEqual(
                hash(password, salt, iterations, expected.length), expected);

    }

    /**
     * Hashes the given password with a new random salt, returning the
     * self-describing string representation of the result.
     *
     * @param password
     *     The password to hash.
     *
     * @param iterations
     *     The number of iterations, which must be positive.
     *
     * @return
     *     A string of the form "pbkdf2-sha256$iterations$salt$hash".
     */
    public static String encode(String password, int iterations) {
        Base64.Encoder base64 = Base64.getEncoder();
        byte[] salt = generateSalt();
        return PREFIX + "$" + iterations
                + "$" + base64.encodeToString(salt)
                + "$" + base64.encodeToString(hash(password, salt, iterations));
    }

    /**
     * Returns whether the given password matches the given self-describing
     * hash string, as produced by {@link #encode(String, int)}.
     *
     * @param password
     *     The password to verify.
     *
     * @param encoded
     *     A string of the form "pbkdf2-sha256$iterations$salt$hash".
     *
     * @return
     *     true if the password matches, false otherwise.
     *
     * @throws IllegalArgumentException
     *     If the given string is not a well-formed PBKDF2-HMAC-SHA256 hash.
     */
    public static boolean verifyEncoded(String password, String encoded)
            throws IllegalArgumentException {

        if (encoded == null)
            throw new IllegalArgumentException("No PBKDF2 hash given.");

        String[] parts = encoded.split("\\$", -1);
        if (parts.length != 4 || !PREFIX.equals(parts[0]))
            throw new IllegalArgumentException("PBKDF2 hash is not of the form "
                    + "\"" + PREFIX + "$iterations$salt$hash\".");

        int iterations;
        byte[] salt;
        byte[] hash;
        try {
            iterations = Integer.parseInt(parts[1]);
            salt = Base64.getDecoder().decode(parts[2]);
            hash = Base64.getDecoder().decode(parts[3]);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("PBKDF2 hash contains an invalid "
                    + "iteration count or invalid base64.", e);
        }

        if (iterations <= 0 || salt.length == 0 || hash.length == 0)
            throw new IllegalArgumentException("PBKDF2 hash has an empty salt "
                    + "or hash, or a non-positive iteration count.");

        return verify(password, salt, iterations, hash);

    }

}
