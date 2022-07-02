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

package org.apache.guacamole.vault.ksm;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.vault.VaultAuthenticationProviderModule;
import org.apache.guacamole.vault.conf.VaultConfigurationService;
import org.apache.guacamole.vault.ksm.conf.KsmConfigurationService;
import org.apache.guacamole.vault.ksm.secret.KsmClient;
import org.apache.guacamole.vault.ksm.secret.KsmRecordService;
import org.apache.guacamole.vault.ksm.secret.KsmSecretService;
import org.apache.guacamole.vault.secret.VaultSecretService;

/**
 * Guice module which configures injections specific to Keeper Secrets Manager support.
 */
public class KsmAuthenticationProviderModule
    extends VaultAuthenticationProviderModule {

  /**
   * Creates a new KsmAuthenticationProviderModule which configures dependency injection for the
   * Keeper Secrets Manager authentication provider and related services.
   *
   * @throws GuacamoleException If configuration details in guacamole.properties cannot be parsed.
   */
  public KsmAuthenticationProviderModule() throws GuacamoleException {
  }

  @Override
  protected void configureVault() {

    // Bind services specific to Keeper Secrets Manager
    bind(KsmClient.class);
    bind(KsmRecordService.class);
    bind(VaultConfigurationService.class).to(KsmConfigurationService.class);
    bind(VaultSecretService.class).to(KsmSecretService.class);
  }

}
