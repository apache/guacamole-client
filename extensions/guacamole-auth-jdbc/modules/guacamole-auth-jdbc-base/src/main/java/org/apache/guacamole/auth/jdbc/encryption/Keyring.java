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

import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.guacamole.GuacamoleServerException;

/**
 * Parses keyring properties of the form "kid:value[,kid:value...]".
 * Error messages name key identifiers only, never key material.
 */
public final class Keyring {

    /**
     * The allowed form of a key identifier.
     */
    private static final Pattern KEY_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    /**
     * Turns one property value into raw key bytes.
     */
    public interface Decoder {

        /**
         * Decodes the given value into key bytes.
         *
         * @param keyId
         *     The identifier of the key being decoded.
         *
         * @param value
         *     The encoded key.
         *
         * @return
         *     The raw key bytes.
         *
         * @throws GuacamoleServerException
         *     If the value cannot be decoded.
         */
        byte[] decode(String keyId, String value) throws GuacamoleServerException;

    }

    /**
     * This class is a utility class and cannot be instantiated.
     */
    private Keyring() {}

    /**
     * Parses the given keyring specification.
     *
     * @param spec
     *     Comma-separated "keyId:value" pairs.
     *
     * @param decoder
     *     The decoder applied to each value.
     *
     * @return
     *     An unmodifiable map of key identifiers to 32-byte keys.
     *
     * @throws GuacamoleServerException
     *     If the specification is malformed or a key has the wrong length.
     */
    public static Map<String, byte[]> parse(String spec, Decoder decoder)
            throws GuacamoleServerException {

        Map<String, byte[]> keys = new LinkedHashMap<>();
        String[] entries = spec.split(",", -1);
        for (int i = 0; i < entries.length; i++) {

            String trimmed = entries[i].trim();
            int colon = trimmed.indexOf(':');
            if (colon <= 0)
                throw new GuacamoleServerException("Parameter encryption "
                        + "keyring entries must be \"keyId:value\".");

            String keyId = trimmed.substring(0, colon);
            // Never echo an unvalidated ID: a reversed entry puts the key here.
            if (!KEY_ID.matcher(keyId).matches())
                throw new GuacamoleServerException("Invalid parameter "
                        + "encryption key identifier in keyring entry "
                        + (i + 1) + ".");
            if (keys.containsKey(keyId))
                throw new GuacamoleServerException("Duplicate parameter "
                        + "encryption key identifier \"" + keyId + "\".");
            for (String existing : keys.keySet())
                if (existing.equalsIgnoreCase(keyId))
                    throw new GuacamoleServerException("Parameter encryption key "
                            + "identifiers must differ in more than letter case (\""
                            + keyId + "\").");

            byte[] key = decoder.decode(keyId, trimmed.substring(colon + 1).trim());
            if (key == null)
                throw new GuacamoleServerException("Parameter encryption key \""
                        + keyId + "\" could not be decoded.");
            if (key.length != ParameterCipher.KEY_BYTES)
                throw new GuacamoleServerException("Parameter encryption key \""
                        + keyId + "\" must be " + ParameterCipher.KEY_BYTES
                        + " bytes.");
            keys.put(keyId, key);

        }
        return Collections.unmodifiableMap(keys);

    }

    /**
     * Parses a keyring whose values are base64-encoded 32-byte keys.
     *
     * @param spec
     *     Comma-separated "keyId:base64" pairs.
     *
     * @return
     *     An unmodifiable map of key identifiers to 32-byte keys.
     *
     * @throws GuacamoleServerException
     *     If the specification is malformed or a key is invalid.
     */
    public static Map<String, byte[]> parseBase64(String spec)
            throws GuacamoleServerException {
        return parse(spec, (keyId, value) -> {
            try {
                return Base64.getDecoder().decode(value);
            }
            catch (IllegalArgumentException e) {
                throw new GuacamoleServerException("Parameter encryption key \""
                        + keyId + "\" is not valid base64.");
            }
        });
    }

}
