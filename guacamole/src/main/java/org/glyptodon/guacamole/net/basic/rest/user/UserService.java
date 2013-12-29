/*
 * Copyright (C) 2013 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.glyptodon.guacamole.net.basic.rest.user;

import java.util.ArrayList;
import java.util.List;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.auth.Directory;
import org.glyptodon.guacamole.net.auth.User;

/**
 * A service for performing useful manipulations on REST Users.
 * 
 * @author James Muehlner
 */
public class UserService {
    
    /**
     * Converts a user directory to a list of APIUser objects for 
     * exposing with the REST endpoints.
     * 
     * @param userDirectory The user directory to convert for REST endpoint use.
     * @return A List of APIUser objects for use with the REST endpoint.
     * @throws GuacamoleException If an error occurs while converting the 
     *                            user directory.
     */
    public List<APIUser> convertUserList(Directory<String, User> userDirectory) 
            throws GuacamoleException {
        List<APIUser> restUsers = new ArrayList<APIUser>();
        
        for(String username : userDirectory.getIdentifiers()) {
            restUsers.add(new APIUser(userDirectory.get(username)));
        }
            
        return restUsers;
    }
    
}
