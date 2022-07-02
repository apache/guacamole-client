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

package org.apache.guacamole.vault.ksm.secret;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.keepersecurity.secretsManager.core.KeeperRecord;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.token.TokenFilter;
import org.apache.guacamole.vault.ksm.conf.KsmConfigurationService;
import org.apache.guacamole.vault.secret.VaultSecretService;
import org.apache.guacamole.vault.secret.WindowsUsername;

/**
 * Service which retrieves secrets from Keeper Secrets Manager.
 */
@Singleton
public class KsmSecretService implements VaultSecretService {

  /**
   * Client for retrieving records and secrets from Keeper Secrets Manager.
   */
  @Inject
  private KsmClient ksm;

  /**
   * Service for retrieving data from records.
   */
  @Inject
  private KsmRecordService recordService;

  /**
   * Service for retrieving configuration information.
   */
  @Inject
  private KsmConfigurationService confService;

  @Override
  public String canonicalize(String nameComponent) {
    try {

      // As Keeper notation is essentially a URL, encode all components
      // using standard URL escaping
      return URLEncoder.encode(nameComponent, "UTF-8");

    } catch (UnsupportedEncodingException e) {
      throw new UnsupportedOperationException("Unexpected lack of UTF-8 support.", e);
    }
  }

  @Override
  public Future<String> getValue(String name) throws GuacamoleException {
    return ksm.getSecret(name);
  }

  /**
   * Adds contextual parameter tokens for the secrets in the given record to the given map of
   * existing tokens. The values of each token are determined from secrets within the record.
   * Depending on the record, this will be a subset of the username, password, private key, and
   * passphrase.
   *
   * @param tokens The map of parameter tokens that any new tokens should be added to.
   * @param prefix The prefix that should be prepended to each added token.
   * @param record The record to retrieve secrets from when generating tokens. This may be null.
   * @throws GuacamoleException If configuration details in guacamole.properties cannot be parsed.
   */
  private void addRecordTokens(Map<String, Future<String>> tokens, String prefix,
      KeeperRecord record) throws GuacamoleException {

    if (record == null) {
      return;
    }

    // Domain of server-related record
    String domain = recordService.getDomain(record);
    if (domain != null) {
      tokens.put(prefix + "DOMAIN", CompletableFuture.completedFuture(domain));
    }

    // Username of server-related record
    String username = recordService.getUsername(record);
    if (username != null) {

      // If the record had no directly defined domain, but there is a
      // username, and the configuration is enabled to split Windows
      // domains out of usernames, attempt to split the domain out now
      if (domain == null && confService.getSplitWindowsUsernames()) {
        WindowsUsername usernameAndDomain =
            WindowsUsername.splitWindowsUsernameFromDomain(username);

        // Always store the username token
        tokens.put(prefix + "USERNAME", CompletableFuture.completedFuture(
            usernameAndDomain.getUsername()));

        // Only store the domain if one is detected
        if (usernameAndDomain.hasDomain()) {
          tokens.put(prefix + "DOMAIN", CompletableFuture.completedFuture(
              usernameAndDomain.getDomain()));
        }

      }

      // If splitting is not enabled, store the whole value in the USERNAME token
      else {
        tokens.put(prefix + "USERNAME", CompletableFuture.completedFuture(username));
      }
    }

    // Password of server-related record
    String password = recordService.getPassword(record);
    if (password != null) {
      tokens.put(prefix + "PASSWORD", CompletableFuture.completedFuture(password));
    }

    // Key passphrase of server-related record
    String passphrase = recordService.getPassphrase(record);
    if (passphrase != null) {
      tokens.put(prefix + "PASSPHRASE", CompletableFuture.completedFuture(passphrase));
    }

    // Private key of server-related record
    Future<String> privateKey = recordService.getPrivateKey(record);
    tokens.put(prefix + "KEY", privateKey);

  }

  @Override
  public Map<String, Future<String>> getTokens(GuacamoleConfiguration config,
      TokenFilter filter) throws GuacamoleException {

    Map<String, Future<String>> tokens = new HashMap<>();
    Map<String, String> parameters = config.getParameters();

    // Retrieve and define server-specific tokens, if any
    String hostname = parameters.get("hostname");
    if (hostname != null && !hostname.isEmpty()) {
      addRecordTokens(tokens, "KEEPER_SERVER_",
          ksm.getRecordByHost(filter.filter(hostname)));
    }

    // Retrieve and define user-specific tokens, if any
    String username = parameters.get("username");
    if (username != null && !username.isEmpty()) {
      addRecordTokens(tokens, "KEEPER_USER_",
          ksm.getRecordByLogin(filter.filter(username)));
    }

    // Tokens specific to RDP
    if ("rdp".equals(config.getProtocol())) {

      // Retrieve and define gateway server-specific tokens, if any
      String gatewayHostname = parameters.get("gateway-hostname");
      if (gatewayHostname != null && !gatewayHostname.isEmpty()) {
        addRecordTokens(tokens, "KEEPER_GATEWAY_",
            ksm.getRecordByHost(filter.filter(gatewayHostname)));
      }

      // Retrieve and define gateway user-specific tokens, if any
      String gatewayUsername = parameters.get("gateway-username");
      if (gatewayUsername != null && !gatewayUsername.isEmpty()) {
        addRecordTokens(tokens, "KEEPER_GATEWAY_USER_",
            ksm.getRecordByLogin(filter.filter(gatewayUsername)));
      }

    }

    return tokens;

  }

}
