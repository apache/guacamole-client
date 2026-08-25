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

import com.google.inject.Scopes;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.vault.VaultAuthenticationProviderModule;
import org.apache.guacamole.vault.conf.VaultConfigurationService;
import org.apache.guacamole.vault.openbao.conf.OpenBaoConfigurationService;
import org.apache.guacamole.vault.openbao.secret.OpenBaoClient;
import org.apache.guacamole.vault.openbao.secret.OpenBaoSecretService;
import org.apache.guacamole.vault.openbao.user.OpenBaoAttributeService;
import org.apache.guacamole.vault.openbao.user.OpenBaoDirectoryService;
import org.apache.guacamole.vault.secret.VaultSecretService;
import org.apache.guacamole.vault.conf.VaultAttributeService;
import org.apache.guacamole.vault.user.VaultDirectoryService;

/**
 * Guice module for configuring OpenBao vault integration.
 * Binds the OpenBao-specific implementations to the vault base interfaces.
 */
public class OpenBaoAuthenticationProviderModule extends VaultAuthenticationProviderModule {

    /**
     * Creates a new OpenBaoAuthenticationProviderModule.
     *
     * @throws GuacamoleException
     *     If an error occurs while reading guacamole.properties.
     */
    public OpenBaoAuthenticationProviderModule() throws GuacamoleException {
        super();
    }

    @Override
    protected void configureVault() {

        // Bind configuration service
        bind(VaultConfigurationService.class)
                .to(OpenBaoConfigurationService.class)
                .in(Scopes.SINGLETON);

        // Bind secret service
        bind(VaultSecretService.class)
                .to(OpenBaoSecretService.class)
                .in(Scopes.SINGLETON);

        // Bind attribute service
        bind(VaultAttributeService.class)
                .to(OpenBaoAttributeService.class)
                .in(Scopes.SINGLETON);

        // Bind directory service
        bind(VaultDirectoryService.class)
                .to(OpenBaoDirectoryService.class)
                .in(Scopes.SINGLETON);

        // Bind OpenBao client
        bind(OpenBaoClient.class)
                .in(Scopes.SINGLETON);
    }
}
