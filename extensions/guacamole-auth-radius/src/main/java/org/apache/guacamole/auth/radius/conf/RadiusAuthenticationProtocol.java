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
    
    /**
     * Password Authentication Protocol (PAP)
     */
    PAP("pap"),
    
    /**
     * Challenge-Handshake Authentication Protocol (CHAP)
     */
    CHAP("chap"),
    
    /**
     * Microsoft implementation of CHAP, Version 1 (MS-CHAPv1)
     */
    MSCHAPv1("mschapv1"),
    
    /**
     * Microsoft implementation of CHAP, Version 2 (MS-CHAPv2)
     */
    MSCHAPv2("mschapv2"),
    
    /**
     * Extensible Authentication Protocol (EAP) with MD5 Hashing (EAP-MD5)
     */
    EAP_MD5("eap-md5"),

    /**
     * Extensible Authentication Protocol (EAP) with TLS encryption (EAP-TLS).
     */
    EAP_TLS("eap-tls"),

    /**
     * Extensible Authentication Protocol (EAP) with Tunneled TLS (EAP-TTLS).
     */
    EAP_TTLS("eap-ttls");

    /**
     * This variable stores the string value of the protocol, and is also
     * used within the extension to pass to JRadius for configuring the
     * library to talk to the RADIUS server.
     */
    private final String strValue;
    
    /**
     * Create a new RadiusAuthenticationProtocol object having the
     * given string value.
     * 
     * @param strValue
     *     The value of the protocol to store as a string, which will be used
     *     in specifying the protocol within the guacamole.properties file, and
     *     will also be used by the JRadius library for its configuration.
     */
    RadiusAuthenticationProtocol(String strValue) {
        this.strValue = strValue;
    }
    
    /**
    * {@inheritDoc}
    * <p>
    * This function returns the stored string values of the selected RADIUS
    * protocol, which is used both in Guacamole configuration and also to pass
    * on to the JRadius library for its configuration.
    * 
    * @return
    *     The string value stored for the selected RADIUS protocol.
    */
    @Override
    public String toString() {
        return strValue;
    }
    
    /**
     * For a given String value, return the enum value that matches that string,
     * or null if no matchi is found.
     * 
     * @param value
     *     The string value to search for in the list of enums.
     * 
     * @return
     *     The RadiusAuthenticationProtocol value that is identified by the
     *     provided String value.
     */
    public static RadiusAuthenticationProtocol getEnum(String value) {
    
        for (RadiusAuthenticationProtocol v : values())
            if(v.toString().equals(value))
                return v;
        
        return null;
    }
    
}
