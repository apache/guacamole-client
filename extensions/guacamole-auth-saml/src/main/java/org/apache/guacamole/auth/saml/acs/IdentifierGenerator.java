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

package org.apache.guacamole.auth.saml.acs;

import com.google.common.io.BaseEncoding;
import com.google.inject.Singleton;
import java.security.SecureRandom;

/**
 * Generator of unique and unpredictable identifiers. Each generated identifier
 * is an arbitrary, random string produced using a cryptographically-secure
 * random number generator and consists of at least 256 bits.
 */
@Singleton
public class IdentifierGenerator {

    /**
     * Cryptographically-secure random number generator for generating unique
     * identifiers.
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a unique and unpredictable identifier. Each identifier is at
     * least 256-bit and produced using a cryptographically-secure random
     * number generator.
     *
     * @return
     *     A unique and unpredictable identifier.
     */
    public String generateIdentifier() {
        byte[] bytes = new byte[33];
        secureRandom.nextBytes(bytes);
        return BaseEncoding.base64().encode(bytes);
    }

}
