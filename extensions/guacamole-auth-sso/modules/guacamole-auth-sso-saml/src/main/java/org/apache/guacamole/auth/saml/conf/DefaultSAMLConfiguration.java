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
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;

/**
 * SAMLConfiguration implementation that returns default values for all
 * configuration parameters. For required parameters (such as
 * {@link #getCallbackUrl()}), an exception is thrown.
 */
public class DefaultSAMLConfiguration implements SAMLConfiguration {

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
        return false;
    }

    @Override
    public URI getIdpMetadata() {
        return null;
    }

    @Override
    public URI getIdpUrl() {
        return null;
    }

    @Override
    public URI getEntityId() {
        return null;
    }

    @Override
    public URI getCallbackUrl() throws GuacamoleException {
        throw new GuacamoleServerException("All SAML identity providers must have a defined callback URL.");
    }

    @Override
    public URI getAssertionConsumerServiceUrl() throws GuacamoleException {
        throw new GuacamoleServerException("All SAML identity providers must have a defined callback URL.");
    }

    @Override
    public boolean getDebug() {
        return false;
    }

    @Override
    public boolean getCompressRequest() {
        return true;
    }

    @Override
    public boolean getCompressResponse() {
        return true;
    }

    @Override
    public boolean getStrict() {
        return true;
    }

    @Override
    public String getGroupAttribute() {
        return "groups";
    }

    @Override
    public int getAuthenticationTimeout() {
        return 5;
    }

    @Override
    public File getCertificateFile() {
        return null;
    }

    @Override
    public File getPrivateKeyFile() {
        return null;
    }

    @Override
    public URI getLogoutEndpoint() {
        return null;
    }

    @Override
    public URI getPostLogoutRedirectURI() {
        return null;
    }

}
