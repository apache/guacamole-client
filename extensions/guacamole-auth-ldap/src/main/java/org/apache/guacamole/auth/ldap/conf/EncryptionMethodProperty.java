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

package org.apache.guacamole.auth.ldap.conf;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.properties.GuacamoleProperty;

/**
 * A GuacamoleProperty whose value is an EncryptionMethod. The string values
 * "none", "ssl", and "starttls" are each parsed to their corresponding values
 * within the EncryptionMethod enum. All other string values result in parse
 * errors.
 */
public abstract class EncryptionMethodProperty implements GuacamoleProperty<EncryptionMethod> {

    @Override
    public EncryptionMethod parseValue(String value) throws GuacamoleException {

        // If no value provided, return null.
        if (value == null)
            return null;

        // Plaintext (no encryption)
        if (value.equals("none"))
            return EncryptionMethod.NONE;

        // SSL
        if (value.equals("ssl"))
            return EncryptionMethod.SSL;

        // STARTTLS
        if (value.equals("starttls"))
            return EncryptionMethod.STARTTLS;

        // The provided value is not legal
        throw new GuacamoleServerException("Encryption method must be one of \"none\", \"ssl\", or \"starttls\".");

    }

}
