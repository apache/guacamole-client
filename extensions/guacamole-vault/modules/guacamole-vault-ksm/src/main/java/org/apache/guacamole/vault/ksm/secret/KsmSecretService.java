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
import org.apache.guacamole.vault.secret.VaultSecretService;

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

    @Override
    public String canonicalize(String nameComponent) {
        try {

            // As Keeper notation is essentially a URL, encode all components
            // using standard URL escaping
            return URLEncoder.encode(nameComponent, "UTF-8");

        }
        catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException("Unexpected lack of UTF-8 support.", e);
        }
    }

    @Override
    public Future<String> getValue(String name) throws GuacamoleException {
        return ksm.getSecret(name);
    }

    @Override
    public Map<String, Future<String>> getTokens(GuacamoleConfiguration config,
            TokenFilter filter) throws GuacamoleException {

        Map<String, Future<String>> tokens = new HashMap<>();

        // TODO: Verify protocol before assuming meaning of "hostname"
        // parameter

        Map<String, String> parameters = config.getParameters();

        // Retrieve and define server-specific tokens, if any
        String hostname = parameters.get("hostname");
        if (hostname != null && !hostname.isEmpty()) {
            KeeperRecord record = ksm.getRecordByHost(filter.filter(hostname));
            if (record != null) {

                // Username of server-related record
                String username = recordService.getUsername(record);
                if (username != null)
                    tokens.put("KEEPER_SERVER_USERNAME", CompletableFuture.completedFuture(username));

                // Password of server-related record
                String password = recordService.getPassword(record);
                if (password != null)
                    tokens.put("KEEPER_SERVER_PASSWORD", CompletableFuture.completedFuture(password));

                // Key passphrase of server-related record
                String passphrase = recordService.getPassphrase(record);
                if (passphrase != null)
                    tokens.put("KEEPER_SERVER_PASSPHRASE", CompletableFuture.completedFuture(passphrase));

                // Private key of server-related record
                String privateKey = recordService.getPrivateKey(record);
                if (privateKey != null)
                    tokens.put("KEEPER_SERVER_KEY", CompletableFuture.completedFuture(privateKey));

            }
        }

        return tokens;

    }

}
