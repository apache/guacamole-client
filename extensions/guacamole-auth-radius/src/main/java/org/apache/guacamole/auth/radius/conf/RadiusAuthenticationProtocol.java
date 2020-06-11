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

import org.apache.guacamole.properties.EnumGuacamoleProperty.PropertyValue;

/**
 * This enum represents supported RADIUS authentication protocols for
 * the guacamole-auth-radius extension.
 */
public enum RadiusAuthenticationProtocol {
    
    /**
     * Password Authentication Protocol (PAP).
     */
    @PropertyValue("pap")
    PAP,
    
    /**
     * Challenge-Handshake Authentication Protocol (CHAP).
     */
    @PropertyValue("chap")
    CHAP,
    
    /**
     * Microsoft implementation of CHAP, Version 1 (MS-CHAPv1).
     */
    @PropertyValue("mschapv1")
    MSCHAP_V1,
    
    /**
     * Microsoft implementation of CHAP, Version 2 (MS-CHAPv2).
     */
    @PropertyValue("mschapv2")
    MSCHAP_V2,
    
    /**
     * Extensible Authentication Protocol (EAP) with MD5 Hashing (EAP-MD5).
     */
    @PropertyValue("eap-md5")
    EAP_MD5,

    /**
     * Extensible Authentication Protocol (EAP) with TLS encryption (EAP-TLS).
     */
    @PropertyValue("eap-tls")
    EAP_TLS,

    /**
     * Extensible Authentication Protocol (EAP) with Tunneled TLS (EAP-TTLS).
     */
    @PropertyValue("eap-ttls")
    EAP_TTLS;

}
