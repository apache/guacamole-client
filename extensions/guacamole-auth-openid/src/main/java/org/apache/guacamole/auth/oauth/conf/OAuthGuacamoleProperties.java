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

package org.apache.guacamole.auth.oauth.conf;

import org.glyptodon.guacamole.properties.StringGuacamoleProperty;

/**
 * Provides properties required for use of the OAuth authentication provider.
 * These properties will be read from guacamole.properties when the OAuth
 * authentication provider is used.
 */
public class OAuthGuacamoleProperties {

    /**
     * This class should not be instantiated.
     */
    private OAuthGuacamoleProperties() {}

    /**
     * The authorization endpoint (URI) of the OAuth service.
     */
    public static final StringGuacamoleProperty OAUTH_AUTHORIZATION_ENDPOINT =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "oauth-authorization-endpoint"; }

    };

    /**
     * The token endpoint (URI) of the OAuth service.
     */
    public static final StringGuacamoleProperty OAUTH_TOKEN_ENDPOINT =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "oauth-token-endpoint"; }

    };

    /**
     * OAuth client ID which should be submitted to the OAuth service when
     * necessary. This value is typically provided by the OAuth service when
     * OAuth credentials are generated for your application.
     */
    public static final StringGuacamoleProperty OAUTH_CLIENT_ID =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "oauth-client-id"; }

    };

    /**
     * OAuth client secret which should be submitted to the OAuth service when
     * necessary. This value is typically provided by the OAuth service when
     * OAuth credentials are generated for your application.
     */
    public static final StringGuacamoleProperty OAUTH_CLIENT_SECRET =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "oauth-client-secret"; }

    };

    /**
     * The URI that the OAuth service should redirect to after the
     * authentication process is complete. This must be the full URL that a
     * user would enter into their browser to access Guacamole.
     */
    public static final StringGuacamoleProperty OAUTH_REDIRECT_URI =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "oauth-redirect-uri"; }

    };

}
