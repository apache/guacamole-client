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

package org.apache.guacamole.auth.openid.conf;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import javax.ws.rs.core.UriBuilder;
import org.apache.guacamole.auth.openid.util.JsonUrlReader;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;
import org.apache.guacamole.properties.URIGuacamoleProperty;
import org.jose4j.json.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for retrieving well-known endpoint data.
 */
@Singleton
public class OpenIDWellKnown {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(OpenIDWellKnown.class);


    /**
     * The number of attempts to the well-known endpoint to get the values before giving up
     */
    private static final int MAX_ATTEMPTS = 24;

    /**
     * The delay between each attempt to well-known endpoint in seconds
     */
    private static final long DELAY_SECONDS = 5;

    /**
     * The detected issuer
     */
    private static String issuer = null;

    /**
     * The detected authorization edpoint
     */
    private static URI authorization_endpoint = null;

    /**
     * The detected token edpoint
     */
    private static URI token_endpoint = null;

    /**
     * The detected jwks_uri
     */
    private static URI jwks_uri = null;
    
    /**
     * Empty constructor of the class to populate data recovered from a OIDC
     * well-known URL. The class will be populated on injection by Guice
     */
    public OpenIDWellKnown() { }

    /**
     * The well-known endpoint (URI) of the OIDC service.
     */
    private static final URIGuacamoleProperty OPENID_WELL_KNOWN_ENDPOINT =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "openid-well-known-endpoint"; }
    };

    /**
     * Returns the well-known endpoint (URI) of the OIDC service as
     * configured with guacamole.properties.
     *
     * @return
     *     The well-known endpoint of the OIDC service, as configured with
     *     guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed, or if the authorization
     *     endpoint property is missing.
     */
    private URI getWellKnownEndpoint() throws GuacamoleException {
        return environment.getProperty(OPENID_WELL_KNOWN_ENDPOINT);
    }

    /**
     * Returns the issuer to expect for all received ID tokens, as configured
     * from the well_known endpoint.
     *
     * @return
     *     The issuer to expect for all received ID tokens, as returned by the
     *     well-known endpoint.
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Returns the authorization endpoint (URI) of the OpenID service as
     * configured from the well_known endpoint.
     *
     * @return
     *     The authorization endpoint of the OpenID service, as returned by the
     *     well-known endpoint.
     */
    public URI getAuthorizationEndpoint() {
        return authorization_endpoint;
    }

    /**
     * Returns the token endpoint (URI) of the OpenID service as
     * configured from the well_known endpoint.
     *
     * @return
     *     The token endpoint of the OpenID service, as returned by the
     *     well-known endpoint.
     */
    public URI getTokenEndpoint() {
        return token_endpoint;
    }

    /**
     * Returns the endpoint (URI) of the JWKS service which defines how
     * received ID tokens (JWTs) shall be validated, as configured from
     * the well-known endpoint.
     *
     * @return
     *     The endpoint (URI) of the JWKS service which defines how received ID
     *     tokens (JWTs) shall be validated, as configured from the
     *     well-known endpoint.
     */
    public URI getJWKSEndpoint() {
        return jwks_uri;
    }

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /*
     * On injection, when the environment is non null, populates the OpenIDWellKnown 
     * class by reading the json from an OIDC well-known endpoint and saves these values
     * for later use. Use Guice to ensure environment exists before initializing.
     */
    @Inject
    public void init() {
        // Fast return if there is no well-known endpoint or its unreadable
        try {
            if (getWellKnownEndpoint() == null) {
                return;
            }
        }
        catch (Exception e) {
            return;
        }
    
        // Call to well-known endpoint might fail, so allow several tries before giving up
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        
        Runnable task = new Runnable() {
            int attempts = 0;

            @Override
            public void run() {
                attempts++;

                try {
                    Map<String,Object> json = JsonUrlReader.fetch("GET", getWellKnownEndpoint().toURL(), "");
                    issuer = (String) json.get("issuer");
                    authorization_endpoint = UriBuilder.fromUri((String) json.get("authorization_endpoint")).build();
                    token_endpoint = UriBuilder.fromUri((String) json.get("token_endpoint")).build();
                    jwks_uri = UriBuilder.fromUri((String) json.get("jwks_uri")).build();

                    logger.debug("OIDC well-known\n" +
                                 "  issuer                 : {}\n" +
                                 "  authorization_endpoint : {}\n" +
                                 "  token_endpoint         : {}\n" +
                                 "  jwks_uri               : {}\n",
                                 issuer, authorization_endpoint, token_endpoint, jwks_uri);

                    scheduler.shutdown();
                    return;
                }
                catch (Exception e) {
                    logger.debug("Rejecting well-known endpoint : {}", e.getMessage());
                }

                if (attempts >= MAX_ATTEMPTS) {
                    logger.info("Timeout on well-known on endpoint");
                    scheduler.shutdown();
                }
            }
        };

        scheduler.scheduleAtFixedRate(task, 0, DELAY_SECONDS, TimeUnit.SECONDS);
    }
}
