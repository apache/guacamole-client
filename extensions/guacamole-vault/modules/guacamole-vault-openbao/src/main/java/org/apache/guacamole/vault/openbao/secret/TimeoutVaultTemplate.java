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

package org.apache.guacamole.vault.openbao.secret;

import javax.inject.Inject;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.vault.openbao.conf.OpenBaoConfigurationService;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.client.VaultEndpointProvider;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.authentication.SessionManager;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The point of this class is to override the doCreateRestTemplate method
 * when creating a VaultTemplate and allow a request and connection 
 * timeout to be added to the HttpClient. 
 */

public class TimeoutVaultTemplate extends VaultTemplate {
    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(TimeoutVaultTemplate.class);

    /**
     * Service for retrieving OpenBao configuration.
     */
    @Inject
    private OpenBaoConfigurationService configService;


    public TimeoutVaultTemplate(VaultEndpoint endpoint,
            ClientHttpRequestFactory requestFactory,
            SessionManager session) {
        super(endpoint, requestFactory, session);
    }

    @Override
    protected RestTemplate doCreateRestTemplate(VaultEndpointProvider endpointProvider,
            ClientHttpRequestFactory requestFactory) {
        int connectionTimeout;
        int requestTimeout;
        try {
            connectionTimeout = configService.getConnectionTimeout();
        }
        catch (GuacamoleException e) {
            connectionTimeout = OpenBaoConfigurationService.DEFAULT_CONNECTION_TIMEOUT;
        }
        try {
            requestTimeout = configService.getRequestTimeout();
        }
        catch (GuacamoleException e) {
            requestTimeout = OpenBaoConfigurationService.DEFAULT_REQUEST_TIMEOUT;
        }        
        if (requestFactory instanceof SimpleClientHttpRequestFactory) {
            logger.debug("Setting http request and connection timeouts");
            ((SimpleClientHttpRequestFactory) requestFactory)
                    .setConnectTimeout(connectionTimeout);
            ((SimpleClientHttpRequestFactory) requestFactory)
                    .setReadTimeout(requestTimeout);
        }      

        return super.doCreateRestTemplate(endpointProvider, requestFactory);
    }
}
