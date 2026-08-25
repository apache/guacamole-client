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

package org.apache.guacamole.vault.openbao;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.vault.VaultAuthenticationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenBao authentication provider that retrieves RDP passwords from OpenBao.
 * This provider integrates with the Guacamole vault framework to automatically
 * fetch passwords from OpenBao based on the logged-in username.
 */
public class OpenBaoAuthenticationProvider extends VaultAuthenticationProvider {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(OpenBaoAuthenticationProvider.class);

    /**
     * Creates a new OpenBaoAuthenticationProvider.
     *
     * @throws GuacamoleException
     *     If an error occurs during initialization.
     */
    public OpenBaoAuthenticationProvider() throws GuacamoleException {
        super(new OpenBaoAuthenticationProviderModule());
        logger.info("OpenBaoAuthenticationProvider initialized");
    }

    @Override
    public String getIdentifier() {
        return "openbao";
    }
}
