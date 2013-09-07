/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glyptodon.guacamole.net.basic.rest.auth;

import java.security.SecureRandom;
import org.apache.commons.codec.binary.Hex;

/**
 * An implementation of the AuthTokenGenerator based around SecureRandom.
 * 
 * @author James Muehlner
 */
public class SecureRandomAuthTokenGenerator implements AuthTokenGenerator {

    /**
     * Instance of SecureRandom for generating the auth token.
     */
    private SecureRandom secureRandom = new SecureRandom();

    @Override
    public String getToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        
        return Hex.encodeHexString(bytes);
    }
    
}
