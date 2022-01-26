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

package org.apache.guacamole.vault.ksm.conf;

import com.keepersecurity.secretsManager.core.InMemoryStorage;
import com.keepersecurity.secretsManager.core.KeyValueStorage;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.properties.GuacamoleProperty;

/**
 * A GuacamoleProperty whose value is Keeper Secrets Manager {@link KeyValueStorage}
 * object. The value of this property must be base64-encoded JSON, as output by
 * the Keeper Commander CLI tool via the "sm client add" command.
 */
public abstract class KsmConfigProperty implements GuacamoleProperty<KeyValueStorage> {

    @Override
    public KeyValueStorage parseValue(String value) throws GuacamoleException {

        // If no property provided, return null.
        if (value == null)
            return null;

        // Parse base64 value as KSM config storage
        try {
            return new InMemoryStorage(value);
        }
        catch (IllegalArgumentException e) {
            throw new GuacamoleServerException("Invalid base64 configuration "
                    + "for Keeper Secrets Manager.", e);
        }

    }

}
