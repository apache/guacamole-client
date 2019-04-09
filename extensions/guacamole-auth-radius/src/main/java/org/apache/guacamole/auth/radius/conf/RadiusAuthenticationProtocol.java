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

package org.apache.guacamole.auth.radius.conf;

/**
 * This enum represents supported RADIUS authentication protocols for
 * the guacamole-auth-radius extension.
 */
public enum RadiusAuthenticationProtocol {
    
    // Password authentication protocol
    PAP("pap"),
    
    // Challenge-Handshake AUthentication Protocol
    CHAP("chap"),
    
    // Microsoft CHAP version 1
    MSCHAPv1("mschapv1"),
    
    // Microsoft CHAP version 2
    MSCHAPv2("mschapv2"),
    
    // Extensible authentication protocol with MD5 hashing.
    EAP_MD5("eap-md5"),

    // Extensible authentication protocol with TLS
    EAP_TLS("eap-tls"),

    // Extensible authentication protocol with Tunneled TLS
    EAP_TTLS("eap-ttls");

    // Store the string value used in the configuration file.
    private final String strValue;
    
    /**
     * Create a new RadiusAuthenticationProtocol object having the
     * given string value.
     * 
     * @param strValue
     *     The string value of the protocol.
     */
    RadiusAuthenticationProtocol(String strValue) {
        this.strValue = strValue;
    }
    
    @Override
    public String toString() {
        return strValue;
    }
    
    public static RadiusAuthenticationProtocol getEnum(String value) {
    
        for (RadiusAuthenticationProtocol v : values())
            if(v.toString().equals(value))
                return v;
        
        return null;
    }
    
}
