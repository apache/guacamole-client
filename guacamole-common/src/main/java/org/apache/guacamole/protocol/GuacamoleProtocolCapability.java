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

package org.apache.guacamole.protocol;

/**
 * An enum that specifies protocol capabilities that can be used to help
 * detect whether or not a particular protocol version contains a capability.
 */
public enum GuacamoleProtocolCapability {
    
    /**
     * Whether or not the protocol supports arbitrary ordering of the
     * handshake instructions.  This was introduced in VERSION_1_1_0.
     */
    ARBITRARY_HANDSHAKE_ORDER(GuacamoleProtocolVersion.VERSION_1_1_0),
    
    /**
     * Whether or not the protocol supports the ability to dynamically
     * detect the version client and server are running in order to allow
     * compatibility between differing client and server versions.  This
     * was introduced in VERSION_1_1_0.
     */
    PROTOCOL_VERSION_DETECTION(GuacamoleProtocolVersion.VERSION_1_1_0),
    
    /**
     * Whether or not the protocol supports the timezone instruction during
     * the Client-Server handshake phase.  This was introduced in
     * VERSION_1_1_0.
     */
    TIMEZONE_HANDSHAKE(GuacamoleProtocolVersion.VERSION_1_1_0);
    
    /**
     * The minimum protocol version required to support this capability.
     */
    private final GuacamoleProtocolVersion version;
    
    /**
     * Create a new enum value with the given protocol version as the minimum
     * required to support the capability.
     * 
     * @param version
     *     The minimum required protocol version for supporting the
     *     capability.
     */
    GuacamoleProtocolCapability(GuacamoleProtocolVersion version) {
        this.version = version;
    }
    
    /**
     * Returns the minimum protocol version required to support this
     * capability.
     * 
     * @return
     *     The minimum protocol version required to support this capability.
     */
    public GuacamoleProtocolVersion getVersion() {
        return version;
    }
    
}
