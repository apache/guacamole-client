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

package org.apache.guacamole.auth.saml.conf;

import org.apache.guacamole.properties.BooleanGuacamoleProperty;
import org.apache.guacamole.properties.FileGuacamoleProperty;
import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;
import org.apache.guacamole.properties.URIGuacamoleProperty;

/**
 * Provides properties required for use of the SAML authentication provider.
 * These properties will be read from guacamole.properties when the SAML
 * authentication provider is used, and also serve as default values for
 * shared SP settings when multiple identity providers are configured via
 * {@code saml-idps.yml}.
 */
public class SAMLGuacamoleProperties {

    /**
     * This class should not be instantiated.
     */
    private SAMLGuacamoleProperties() {}

    /**
     * The URI of the file containing the XML Metadata associated with the
     * SAML IdP.
     */
    public static final URIGuacamoleProperty SAML_IDP_METADATA_URL =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "saml-idp-metadata-url"; }

    };

    /**
     * The URL of the SAML IdP.
     */
    public static final URIGuacamoleProperty SAML_IDP_URL =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "saml-idp-url"; }

    };

    /**
     * The URL identifier for this SAML client.
     */
    public static final URIGuacamoleProperty SAML_ENTITY_ID =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "saml-entity-id"; }

    };

    /**
     * The callback URL to use for SAML IdP, normally the base
     * of the Guacamole install. The SAML extension's callback
     * endpoint will be appended to this value.
     */
    public static final URIGuacamoleProperty SAML_CALLBACK_URL =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "saml-callback-url"; }

    };

    /**
     * An optional explicit Assertion Consumer Service (ACS) URL to advertise
     * to the SAML IdP instead of deriving one from {@link #SAML_CALLBACK_URL}.
     */
    public static final URIGuacamoleProperty SAML_ACS_URL =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "saml-acs-url"; }

    };

    /**
     * Whether or not debugging should be enabled in the SAML library to help
     * track down errors.
     */
    public static final BooleanGuacamoleProperty SAML_DEBUG =
            new BooleanGuacamoleProperty() {

        @Override
        public String getName() { return "saml-debug"; }

    };

    /**
     * Whether or not to enable compression for the SAML request.
     */
    public static final BooleanGuacamoleProperty SAML_COMPRESS_REQUEST =
            new BooleanGuacamoleProperty() {

        @Override
        public String getName() { return "saml-compress-request"; }

    };

    /**
     * Whether or not to enable compression for the SAML response.
     */
    public static final BooleanGuacamoleProperty SAML_COMPRESS_RESPONSE =
            new BooleanGuacamoleProperty() {

        @Override
        public String getName() { return "saml-compress-response"; }

    };

    /**
     * Whether or not to enforce strict SAML security during processing.
     */
    public static final BooleanGuacamoleProperty SAML_STRICT =
            new BooleanGuacamoleProperty() {

        @Override
        public String getName() { return "saml-strict"; }

    };

    /**
     * The property that defines what attribute the SAML provider will return
     * that contains group membership for the authenticated user.
     */
    public static final StringGuacamoleProperty SAML_GROUP_ATTRIBUTE =
            new StringGuacamoleProperty() {

        @Override
        public String getName() { return "saml-group-attribute"; }

    };

    /**
     * The maximum amount of time to allow for an in-progress SAML
     * authentication attempt to be completed, in minutes. A user that takes
     * longer than this amount of time to complete authentication with their
     * identity provider will be redirected back to the identity provider to
     * try again.
     */
    public static final IntegerGuacamoleProperty SAML_AUTH_TIMEOUT =
            new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "saml-auth-timeout"; }

    };

    /**
     * The file containing the X.509 cert to use when signing or encrypting
     * requests to the SAML IdP.
     */
    public static final FileGuacamoleProperty SAML_X509_CERT_PATH =
            new FileGuacamoleProperty() {

        @Override
        public String getName() { return "saml-x509-cert-path"; }

    };

    /**
     * The file containing the private key to use when signing or encrypting
     * requests to the SAML IdP.
     */
    public static final FileGuacamoleProperty SAML_PRIVATE_KEY_PATH =
            new FileGuacamoleProperty() {

        @Override
        public String getName() { return "saml-private-key-path"; }

    };

    /**
     * The logout endpoint (URI) of the SAML IdP. When configured, users will
     * be redirected to this endpoint when they log out of Guacamole.
     */
    public static final URIGuacamoleProperty SAML_LOGOUT_ENDPOINT =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "saml-logout-endpoint"; }

    };

    /**
     * The URI to redirect to after logout is complete. Defaults to the
     * callback URL if not specified.
     */
    public static final URIGuacamoleProperty SAML_POST_LOGOUT_REDIRECT_URI =
            new URIGuacamoleProperty() {

        @Override
        public String getName() { return "saml-post-logout-redirect-uri"; }

    };

}
