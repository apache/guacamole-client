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

/**
 * Source of the keys used to encrypt secret parameter values.
 */
public interface ParameterKeyProvider {

    /**
     * Returns whether a keyring is configured. When false, no value is
     * encrypted and encrypted values cannot be read.
     *
     * @return
     *     true if a keyring is configured, false otherwise.
     */
    boolean isEnabled();

    /**
     * Returns the identifier of the key used for new encryption. Valid only
     * if isEnabled() returns true.
     *
     * @return
     *     The identifier of the current key.
     */
    String currentKeyId();

    /**
     * Returns the 32-byte key having the given identifier, or null if no such
     * key is configured.
     *
     * @param keyId
     *     The identifier of the key to retrieve.
     *
     * @return
     *     The key, or null if the identifier is unknown.
     */
    byte[] keyFor(String keyId);

}
