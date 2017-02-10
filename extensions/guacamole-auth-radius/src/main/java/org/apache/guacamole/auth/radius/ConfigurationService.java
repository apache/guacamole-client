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

package org.apache.guacamole.auth.radius;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.environment.Environment;

/**
 * Service for retrieving configuration information regarding the RADIUS server.
 *
 * @author Michael Jumper
 */
public class ConfigurationService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;

    /**
     * Returns the hostname of the RADIUS server as configured with
     * guacamole.properties. By default, this will be "localhost".
     *
     * @return
     *     The hostname of the RADIUS server, as configured with
     *     guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public String getRadiusServer() throws GuacamoleException {
        return environment.getProperty(
            RadiusGuacamoleProperties.RADIUS_SERVER,
            "localhost"
        );
    }

    /**
     * Returns the authentication port of the RADIUS server configured with
     * guacamole.properties.
     *
     * @return
     *     The authentication port of the RADIUS server, as configured with
     *     guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public int getRadiusAuthPort() throws GuacamoleException {
        return environment.getProperty(
            RadiusGuacamoleProperties.RADIUS_AUTH_PORT,
            1812
        );
    }

    /**
     * Returns the accounting port of the RADIUS server configured with
     * guacamole.properties. 
     *
     * @return
     *     The accouting port of the RADIUS server, as configured with
     *     guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public int getRadiusAcctPort() throws GuacamoleException {
        return environment.getProperty(
            RadiusGuacamoleProperties.RADIUS_ACCT_PORT,
            1813
        );
    }

    /**
     * Returns the shared secret of the RADIUS server configured with
     * guacamole.properties. 
     *
     * @return
     *     The shared secret of the RADIUS server, as configured with
     *     guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public String getRadiusSharedSecret() throws GuacamoleException {
        return environment.getProperty(
            RadiusGuacamoleProperties.RADIUS_SHARED_SECRET,
            null
        );
    }

    /**
     * Returns the authentication protocol of the RADIUS server
     * from guacamole.properties.
     *
     * @return
     *     The authentication protocol of the RADIUS server, 
     *     from guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public String getRadiusAuthProtocol() throws GuacamoleException {
        return environment.getProperty(
            RadiusGuacamoleProperties.RADIUS_AUTH_PROTOCOL,
            null
        );
    }

    /**
     * Returns the number of retries for connecting to the RADIUS server
     * from guacamole.properties.
     *
     * @return
     *     The number of retries for connection to the RADIUS server,
     *     from guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public int getRadiusRetries() throws GuacamoleException {
        return environment.getProperty(
            RadiusGuacamoleProperties.RADIUS_RETRIES,
            5
        );
    }

    /**
     * Returns the timeout for connecting to the RADIUS server
     * from guacamole.properties.
     *
     * @return
     *     The timeout for connection to the RADIUS server,
     *     from guacamole.properties.
     *
     * @throws GuacamoleException
     *     If guacamole.properties cannot be parsed.
     */
    public int getRadiusTimeout() throws GuacamoleException {
        return environment.getProperty(
            RadiusGuacamoleProperties.RADIUS_TIMEOUT,
            60
        );
    }

    public String getRadiusCAFile() throws GuacamoleException {
        return environment.getProperty(
            RadiusGuacamoleProperties.RADIUS_CA_FILE,
            null 
        );
    }

    public String getRadiusKeyFile() throws GuacamoleException {
        return environment.getProperty(
            RadiusGuacamoleProperties.RADIUS_KEY_FILE,
            "radius.pem"
        );
    }

    public String getRadiusCAPassword() throws GuacamoleException {
        return environment.getProperty(
            RadiusGuacamoleProperties.RADIUS_CA_PASSWORD,
            null
        );
    }

    public String getRadiusCAType() throws GuacamoleException {
        return environment.getProperty(
            RadiusGuacamoleProperties.RADIUS_CA_TYPE,
            "pkcs12"
        );
    }

    public String getRadiusKeyPassword() throws GuacamoleException {
        return environment.getProperty(
            RadiusGuacamoleProperties.RADIUS_KEY_PASSWORD,
            null
        );
    }

    public String getRadiusKeyType() throws GuacamoleException {
        return environment.getProperty(
            RadiusGuacamoleProperties.RADIUS_KEY_TYPE,
            "pkcs12"
        );
    }

    public Boolean getRadiusTrustAll() throws GuacamoleException {
        return environment.getProperty(
            RadiusGuacamoleProperties.RADIUS_TRUST_ALL,
            false
        );
    }

    public String getRadiusEAPTTLSInnerProtocol() throws GuacamoleException {
        return environment.getProperty(
            RadiusGuacamoleProperties.RADIUS_EAP_TTLS_INNER_PROTOCOL,
            null
        );
    }

}
