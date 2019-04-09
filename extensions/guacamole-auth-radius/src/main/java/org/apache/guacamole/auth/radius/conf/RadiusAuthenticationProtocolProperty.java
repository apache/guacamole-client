/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.apache.guacamole.auth.radius.conf;

import org.apache.guacamole.GuacamoleServerException;

/**
 * A GuacamoleProperty whose value is a RadiusAuthenticationProtocol.
 */
public abstract class RadiusAuthenticationProtocolProperty
        implements GuacamoleProperty<RadiusAuthenticationProtocol> {
    
    @Override
    public RadiusAuthenticationProtocol parseValue(String value)
            throws GuacamoleException {
        
        // Nothing provided, nothing returned
        if (value == null)
            return null;
        
        // Attempt to parse the string value
        RadiusAuthenticationProtocol authProtocol = 
                RadiusAuthenticationProtocol.valueOf(value);
        
        // Throw an exception if nothing matched.
        if (authProtocol == null)
            throw new GuacamoleServerException(
                    "Invalid or unsupported RADIUS authentication protocol.");
        
        // Return the answer
        return authProtocol;
        
    }
    
}
