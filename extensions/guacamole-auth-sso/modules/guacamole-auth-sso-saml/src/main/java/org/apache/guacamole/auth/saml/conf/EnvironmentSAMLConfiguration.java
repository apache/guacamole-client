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

import java.io.File;
import java.net.URI;
import javax.ws.rs.core.UriBuilder;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;

/**
 * SAMLConfiguration implementation that reads its configuration details from
 * guacamole.properties.
 */
public class EnvironmentSAMLConfiguration implements SAMLConfiguration {

    /**
     * The Guacamole server environment.
     */
    private final Environment environment;

    /**
     * The default configuration options for all parameters.
     */
    private static final SAMLConfiguration DEFAULT = new DefaultSAMLConfiguration();

    /**
     * Creates a new EnvironmentSAMLConfiguration that reads its configuration
     * details from guacamole.properties, as exposed by the given Environment.
     *
     * @param environment
     *     The Guacamole server environment.
     */
    public EnvironmentSAMLConfiguration(Environment environment) {
        this.environment = environment;
    }

    @Override
    public String getId() {
        return DEFAULT_ID;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public URI getIdpMetadata() throws GuacamoleException {
        return environment.getProperty(
                SAMLGuacamoleProperties.SAML_IDP_METADATA_URL,
                DEFAULT.getIdpMetadata());
    }

    @Override
    public URI getIdpUrl() throws GuacamoleException {
        return environment.getProperty(
                SAMLGuacamoleProperties.SAML_IDP_URL,
                DEFAULT.getIdpUrl());
    }

    @Override
    public URI getEntityId() throws GuacamoleException {
        return environment.getProperty(
                SAMLGuacamoleProperties.SAML_ENTITY_ID,
                DEFAULT.getEntityId());
    }

    @Override
    public URI getCallbackUrl() throws GuacamoleException {
        return environment.getRequiredProperty(
                SAMLGuacamoleProperties.SAML_CALLBACK_URL);
    }

    @Override
    public URI getAssertionConsumerServiceUrl() throws GuacamoleException {
        URI explicitAcsUrl = environment.getProperty(SAMLGuacamoleProperties.SAML_ACS_URL);
        if (explicitAcsUrl != null)
            return explicitAcsUrl;

        return UriBuilder.fromUri(getCallbackUrl())
                .path("api/ext/saml/callback")
                .build();
    }

    @Override
    public boolean getDebug() throws GuacamoleException {
        return environment.getProperty(
                SAMLGuacamoleProperties.SAML_DEBUG,
                DEFAULT.getDebug());
    }

    @Override
    public boolean getCompressRequest() throws GuacamoleException {
        return environment.getProperty(
                SAMLGuacamoleProperties.SAML_COMPRESS_REQUEST,
                DEFAULT.getCompressRequest());
    }

    @Override
    public boolean getCompressResponse() throws GuacamoleException {
        return environment.getProperty(
                SAMLGuacamoleProperties.SAML_COMPRESS_RESPONSE,
                DEFAULT.getCompressResponse());
    }

    @Override
    public boolean getStrict() throws GuacamoleException {
        return environment.getProperty(
                SAMLGuacamoleProperties.SAML_STRICT,
                DEFAULT.getStrict());
    }

    @Override
    public String getGroupAttribute() throws GuacamoleException {
        return environment.getProperty(
                SAMLGuacamoleProperties.SAML_GROUP_ATTRIBUTE,
                DEFAULT.getGroupAttribute());
    }

    @Override
    public int getAuthenticationTimeout() throws GuacamoleException {
        return environment.getProperty(
                SAMLGuacamoleProperties.SAML_AUTH_TIMEOUT,
                DEFAULT.getAuthenticationTimeout());
    }

    @Override
    public File getCertificateFile() throws GuacamoleException {
        return environment.getProperty(
                SAMLGuacamoleProperties.SAML_X509_CERT_PATH,
                DEFAULT.getCertificateFile());
    }

    @Override
    public File getPrivateKeyFile() throws GuacamoleException {
        return environment.getProperty(
                SAMLGuacamoleProperties.SAML_PRIVATE_KEY_PATH,
                DEFAULT.getPrivateKeyFile());
    }

    @Override
    public URI getLogoutEndpoint() throws GuacamoleException {
        return environment.getProperty(
                SAMLGuacamoleProperties.SAML_LOGOUT_ENDPOINT,
                DEFAULT.getLogoutEndpoint());
    }

    @Override
    public URI getPostLogoutRedirectURI() throws GuacamoleException {
        return environment.getProperty(
                SAMLGuacamoleProperties.SAML_POST_LOGOUT_REDIRECT_URI,
                DEFAULT.getPostLogoutRedirectURI());
    }

}
