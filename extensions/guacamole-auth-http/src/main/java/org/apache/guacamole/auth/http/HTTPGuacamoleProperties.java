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

package org.apache.guacamole.auth.http;

import org.apache.guacamole.properties.IntegerGuacamoleProperty;
import org.apache.guacamole.properties.StringGuacamoleProperty;


/**
 * Provides properties required for use of the HTTP authentication provider.
 * These properties will be read from guacamole.properties when the HTTP
 * authentication provider is used.
 *
 * @author Michael Jumper
 */
public class HTTPGuacamoleProperties {

    /**
     * This class should not be instantiated.
     */
    private HTTPGuacamoleProperties() {}

    /**
     * The header used for HTTP basic authentication.
     */
    public static final StringGuacamoleProperty HTTP_AUTH_HEADER = new StringGuacamoleProperty() {

        @Override
        public String getName() { return "http-auth-header"; }

    };

}
