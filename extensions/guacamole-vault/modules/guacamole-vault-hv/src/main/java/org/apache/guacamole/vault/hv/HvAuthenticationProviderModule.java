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

package org.apache.guacamole.vault.hv;

import com.google.inject.assistedinject.FactoryModuleBuilder;
import java.security.Security;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.vault.VaultAuthenticationProviderModule;
import org.apache.guacamole.vault.conf.VaultAttributeService;
import org.apache.guacamole.vault.conf.VaultConfigurationService;
import org.apache.guacamole.vault.hv.conf.HvAttributeService;
import org.apache.guacamole.vault.hv.conf.HvConfigurationService;
import org.apache.guacamole.vault.hv.secret.HvClient;
import org.apache.guacamole.vault.hv.secret.HvClientFactory;
import org.apache.guacamole.vault.hv.secret.HvSecretService;
import org.apache.guacamole.vault.hv.user.HvConnectionGroup;
import org.apache.guacamole.vault.hv.user.HvDirectoryService;
import org.apache.guacamole.vault.hv.user.HvUser;
import org.apache.guacamole.vault.hv.user.HvUserFactory;
import org.apache.guacamole.vault.secret.VaultSecretService;
import org.apache.guacamole.vault.user.VaultDirectoryService;
import org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider;

/**
 * Guice module which configures injections specific to Hashicorp Vault
 * support.
 */
public class HvAuthenticationProviderModule
        extends VaultAuthenticationProviderModule {

    /**
     * Creates a new HvAuthenticationProviderModule which
     * configures dependency injection for the Hashicorp Vault
     * authentication provider and related services.
     *
     * @throws GuacamoleException
     *     If configuration details in guacamole.properties cannot be parsed.
     */
    public HvAuthenticationProviderModule() throws GuacamoleException {
        Security.addProvider(new BouncyCastleFipsProvider());
    }

    @Override
    protected void configureVault() {

        // Bind services specific to Hashicorp Vault
        bind(HvAttributeService.class);
        bind(VaultAttributeService.class).to(HvAttributeService.class);
        bind(VaultConfigurationService.class).to(HvConfigurationService.class);
        bind(VaultSecretService.class).to(HvSecretService.class);
        bind(VaultDirectoryService.class).to(HvDirectoryService.class);

        // Bind factory for creating HV Clients
        install(new FactoryModuleBuilder()
                .implement(HvClient.class, HvClient.class)
                .build(HvClientFactory.class));

        // Bind factory for creating HvUsers
        install(new FactoryModuleBuilder()
                .implement(HvUser.class, HvUser.class)
                .build(HvUserFactory.class));
    }

}
