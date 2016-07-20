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

package org.apache.guacamole.auth.jdbc.sharing;

import java.security.SecureRandom;
import javax.xml.bind.DatatypeConverter;

/**
 * An implementation of the ShareKeyGenerator which uses SecureRandom to
 * generate cryptographically-secure random sharing keys.
 * 
 * @author Michael Jumper
 */
public class SecureRandomShareKeyGenerator implements ShareKeyGenerator {

    /**
     * Instance of SecureRandom for generating sharing keys.
     */
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String getShareKey() {
        byte[] bytes = new byte[33];
        secureRandom.nextBytes(bytes);
        return DatatypeConverter.printBase64Binary(bytes);
    }

}
