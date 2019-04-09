/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
    public RadiusAuthenticationProtocol(String strValue) {
        this.strValue = strValue;
    }
    
    @Override
    public String toString() {
        return strValue;
    }
    
    @Override
    public static RadiusAuthenticationProtocol valueOf(String value) {
    
        for (RadiusAuthenticationProtocol v : values())
            if(v.toString().equals(value))
                return v;
        
        return null;
    }
    
}
