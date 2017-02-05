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

import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;


/**
 * Provides properties required for use of the RADIUS authentication provider.
 * These properties will be read from guacamole.properties when the RADIUS
 * authentication provider is used.
 *
 * @author Michael Jumper
 */
public class RadiusGuacamoleProperties {

    /**
     * This class should not be instantiated.
     */
    private RadiusGuacamoleProperties() {}

    /**
     * The port on the RADIUS server to connect to when authenticating users.
     */
    public static final IntegerGuacamoleProperty RADIUS_AUTH_PORT = new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "radius-auth-port"; }

    };

    /**
     * The port on the RADIUS server to connect to when accounting users.
     */
    public static final IntegerGuacamoleProperty RADIUS_ACCT_PORT = new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "radius-acct-port"; }

    };


    /**
     * The hostname or ip of the RADIUS server to connect to when authenticating users.
     */
    public static final StringGuacamoleProperty RADIUS_SERVER = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "radius-server"; }

    };

    /**
     * The shared secret of the RADIUS server to connect to when authenticating users.
     */
    public static final StringGuacamoleProperty RADIUS_SHARED_SECRET = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "radius-shared-secret"; }

    };

    /**
     * The authentication protocol of the RADIUS server to connect to when authenticating users.
     */
    public static final StringGuacamoleProperty RADIUS_AUTH_PROTOCOL = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "radius-auth-protocol"; }

    };

    /**
     * The number of retries when attempting a radius packet transaction.
     */
    public static final IntegerGuacamoleProperty RADIUS_RETRIES = new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "radius-retries"; }

    };

    /**
     * The network timeout when attempting a radius packet transaction.
     */
    public static final IntegerGuacamoleProperty RADIUS_TIMEOUT = new IntegerGuacamoleProperty() {

        @Override
        public String getName() { return "radius-timeout"; }

    };


}
