package org.glyptodon.guacamole.net.basic.rest.auth;

/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
