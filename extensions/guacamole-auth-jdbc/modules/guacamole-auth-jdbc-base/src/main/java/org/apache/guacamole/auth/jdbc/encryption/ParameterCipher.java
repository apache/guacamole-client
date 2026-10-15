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

package org.apache.guacamole.auth.jdbc.encryption;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES-256-GCM encryption of a single parameter value, bound to its owner and
 * parameter name through the associated data. Stored form is
 * base64(IV || ciphertext || tag).
 */
public final class ParameterCipher {

    /** Largest plain text, in UTF-8 bytes, whose stored form fits 8192 chars. */
    public static final int MAX_PLAINTEXT_BYTES = 6000;

    /** Required key length in bytes (AES-256). */
    public static final int KEY_BYTES = 32;

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final SecureRandom RANDOM = new SecureRandom();

    private ParameterCipher() {}

    /**
     * Builds length-prefixed associated data so that no two distinct
     * (owner, id, name) triples share an encoding.
     * The encoding is part of the stored format; never change it.
     */
    private static byte[] associatedData(ParameterOwner owner, String ownerId,
            String name) throws IllegalArgumentException {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeUTF(owner.getType());
            out.writeUTF(ownerId);
            out.writeUTF(name);
            return bytes.toByteArray();
        }
        catch (IOException e) {
            throw new IllegalArgumentException("Owner identifier or parameter name too long.");
        }
    }

    /**
     * Encrypts the given value.
     *
     * @return
     *     The stored form, base64(IV || ciphertext || tag).
     *
     * @throws IllegalArgumentException
     *     If the value exceeds MAX_PLAINTEXT_BYTES or the key is not
     *     KEY_BYTES long.
     */
    public static String encrypt(byte[] key, ParameterOwner owner,
            String ownerId, String name, String plaintext) {

        byte[] data = plaintext.getBytes(StandardCharsets.UTF_8);
        if (data.length > MAX_PLAINTEXT_BYTES)
            throw new IllegalArgumentException("Value exceeds "
                    + MAX_PLAINTEXT_BYTES + " bytes.");
        if (key.length != KEY_BYTES)
            throw new IllegalArgumentException("Key must be "
                    + KEY_BYTES + " bytes.");

        byte[] iv = new byte[IV_BYTES];
        RANDOM.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(associatedData(owner, ownerId, name));
            byte[] sealed = cipher.doFinal(data);
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + sealed.length)
                            .put(iv).put(sealed).array());
        }
        catch (GeneralSecurityException e) {
            throw new IllegalStateException("AES-GCM unavailable.", e);
        }

    }

    /**
     * Decrypts a stored value.
     *
     * @throws GeneralSecurityException
     *     If the value is malformed, was encrypted under another key, or does
     *     not belong to this owner and parameter name.
     */
    public static String decrypt(byte[] key, ParameterOwner owner,
            String ownerId, String name, String stored)
            throws GeneralSecurityException {

        if (key == null || key.length != KEY_BYTES)
            throw new GeneralSecurityException("Key must be " + KEY_BYTES + " bytes.");

        byte[] raw;
        try {
            raw = Base64.getDecoder().decode(stored);
        }
        catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("Stored value is not base64.");
        }
        if (raw.length < IV_BYTES + TAG_BITS / 8)
            throw new GeneralSecurityException("Stored value is too short.");

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, raw, 0, IV_BYTES));
            cipher.updateAAD(associatedData(owner, ownerId, name));
            byte[] data = cipher.doFinal(raw, IV_BYTES, raw.length - IV_BYTES);
            return new String(data, StandardCharsets.UTF_8);
        }
        catch (IllegalArgumentException e) {
            throw new GeneralSecurityException(e.getMessage());
        }

    }

}
