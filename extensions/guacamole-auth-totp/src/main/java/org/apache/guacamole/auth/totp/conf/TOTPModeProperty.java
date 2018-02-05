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

package org.apache.guacamole.auth.totp.conf;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.properties.GuacamoleProperty;
import org.apache.guacamole.totp.TOTPGenerator;

/**
 * A GuacamoleProperty whose value is a TOTP generation method. The string
 * values "sha1", "sha256", and "sha512" are each parsed to their corresponding
 * values within the TOTPGenerator.Mode enum. All other string values result in
 * parse errors.
 */
public abstract class TOTPModeProperty
        implements GuacamoleProperty<TOTPGenerator.Mode> {

    @Override
    public TOTPGenerator.Mode parseValue(String value)
            throws GuacamoleException {

        // If no value provided, return null.
        if (value == null)
            return null;

        // SHA1
        if (value.equals("sha1"))
            return TOTPGenerator.Mode.SHA1;

        // SHA256
        if (value.equals("sha256"))
            return TOTPGenerator.Mode.SHA256;

        // SHA512
        if (value.equals("sha512"))
            return TOTPGenerator.Mode.SHA512;

        // The provided value is not legal
        throw new GuacamoleServerException("TOTP mode must be one of "
                + "\"sha1\", \"sha256\", or \"sha512\".");

    }

}
