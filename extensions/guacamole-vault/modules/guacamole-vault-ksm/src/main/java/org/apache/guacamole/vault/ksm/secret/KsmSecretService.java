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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
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
    public Map<String, Future<String>> getTokens(GuacamoleConfiguration config)
            throws GuacamoleException {
        // STUB
        return Collections.emptyMap();
    }

}
