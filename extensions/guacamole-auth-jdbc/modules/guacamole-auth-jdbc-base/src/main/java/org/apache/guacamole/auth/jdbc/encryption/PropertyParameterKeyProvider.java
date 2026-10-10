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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.guacamole.GuacamoleServerException;

/**
 * Key provider whose keys are given directly in a property.
 */
public class PropertyParameterKeyProvider implements ParameterKeyProvider {

    /**
     * Provider used when no keyring is configured.
     */
    public static final ParameterKeyProvider DISABLED = new ParameterKeyProvider() {
        @Override public boolean isEnabled() { return false; }
        @Override public String currentKeyId() {
            throw new IllegalStateException("Parameter encryption is disabled.");
        }
        @Override public byte[] keyFor(String keyId) { return null; }
    };

    /**
     * The keyring.
     */
    private final Map<String, byte[]> keys;

    /**
     * The identifier of the key used for new encryption.
     */
    private final String currentKeyId;

    /**
     * Creates a provider serving the given keys.
     *
     * @param keys
     *     The keyring, by key identifier.
     *
     * @param currentKeyId
     *     The identifier of the key to use for new encryption.
     *
     * @throws GuacamoleServerException
     *     If the current key is null or not in the keyring.
     */
    public PropertyParameterKeyProvider(Map<String, byte[]> keys,
            String currentKeyId) throws GuacamoleServerException {
        if (currentKeyId == null)
            throw new GuacamoleServerException("The parameter encryption "
                    + "current key must be set when keys are configured.");
        if (!keys.containsKey(currentKeyId))
            throw new GuacamoleServerException("The current parameter "
                    + "encryption key \"" + currentKeyId + "\" is not in the "
                    + "keyring.");
        Map<String, byte[]> copy = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> entry : keys.entrySet())
            copy.put(entry.getKey(), entry.getValue().clone());
        this.keys = Collections.unmodifiableMap(copy);
        this.currentKeyId = currentKeyId;
    }

    @Override public boolean isEnabled() { return true; }
    @Override public String currentKeyId() { return currentKeyId; }
    @Override public byte[] keyFor(String keyId) {
        byte[] key = keys.get(keyId);
        return key == null ? null : key.clone();
    }

}
