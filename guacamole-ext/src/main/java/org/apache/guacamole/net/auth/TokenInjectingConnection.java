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

package org.apache.guacamole.net.auth;

import java.util.HashMap;
import java.util.Map;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.protocol.GuacamoleClientInformation;

/**
 * Connection implementation which overrides the connect() function of an
 * underlying Connection, adding a given set of parameter tokens to the tokens
 * already supplied.
 */
public class TokenInjectingConnection extends DelegatingConnection {

    /**
     * The additional tokens to include with each call to connect().
     */
    private final Map<String, String> tokens;

    /**
     * Wraps the given Connection, automatically adding the given tokens to
     * each invocation of connect(). Any additional tokens which have the same
     * name as existing tokens will override the existing values.
     *
     * @param connection
     *     The Connection to wrap.
     *
     * @param tokens
     *     The additional tokens to include with each call to connect().
     */
    public TokenInjectingConnection(Connection connection,
            Map<String, String> tokens) {
        super(connection);
        this.tokens = tokens;
    }

    @Override
    public GuacamoleTunnel connect(GuacamoleClientInformation info,
            Map<String, String> tokens) throws GuacamoleException {

        // Apply provided tokens over those given to connect()
        tokens = new HashMap<>(tokens);
        tokens.putAll(this.tokens);

        return super.connect(info, tokens);

    }

}
