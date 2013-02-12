/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sourceforge.guacamole.net.auth.mysql.utility;

import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;

/**
 * Generates password salts via the SecureRandom utility.
 * @author dagger10k
 */
public class SecureRandomSaltUtility implements SaltUtility {
    
    SecureRandom secureRandom = new SecureRandom();
    
    @Override
    public String generateSalt() {
        byte[] salt = new byte[32];
        secureRandom.nextBytes(salt);
        try {
            return new String(salt, "UTF-8");
        } catch (UnsupportedEncodingException ex) { // should not happen
            throw new RuntimeException(ex);
        }
    }
}
