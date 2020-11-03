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

package org.apache.guacamole.auth.cas.conf;

import org.apache.guacamole.properties.URIGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;

/**
 * Provides properties required for use of the CAS authentication provider.
 * These properties will be read from guacamole.properties when the CAS
 * authentication provider is used.
 */
public class CASGuacamoleProperties {

    /**
     * This class should not be instantiated.
     */
    private CASGuacamoleProperties() {}

    /**
     * The authorization endpoint (URI) of the CAS service.
     */
    public static final URIGuacamoleProperty CAS_AUTHORIZATION_ENDPOINT =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "cas-authorization-endpoint"; }

    };

    /**
     * The URI that the CAS service should redirect to after the
     * authentication process is complete. This must be the full URL that a
     * user would enter into their browser to access Guacamole.
     */
    public static final URIGuacamoleProperty CAS_REDIRECT_URI =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "cas-redirect-uri"; }

    };

    /**
     * The location of the private key file used to retrieve the
     * password if CAS is configured to support ClearPass.
     */
    public static final PrivateKeyGuacamoleProperty CAS_CLEARPASS_KEY =
            new PrivateKeyGuacamoleProperty() {

        @Override
        public String getName() { return "cas-clearpass-key"; }

    };
  
    /**
     * The attribute used for group membership
     * example:  memberOf  (case sensitive)
     */
    public static final StringGuacamoleProperty CAS_GROUP_ATTRIBUTE =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "cas-group-attribute"; }

    };

    /**
     * The name of the attribute used for group membership, such as "memberOf".
     * This attribute is case sensitive.
     */
    public static final StringGuacamoleProperty CAS_GROUP_DN_FORMAT =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "cas-group-dn-format"; }

    };
}
