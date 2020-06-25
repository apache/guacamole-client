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

package org.apache.guacamole.properties;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;

/**
 * A GuacamoleProperty whose value is a URI.
 */
public abstract class URIGuacamoleProperty implements GuacamoleProperty<URI> {
    
    @Override
    public URI parseValue(String value) throws GuacamoleException {
        
        // If nothing is provided, just return null.
        if (value == null)
            return null;
        
        try {
            return new URI(value);
        }
        catch (URISyntaxException e) {
            throw new GuacamoleServerException("Value \"" + value
                + "\" is not a valid URI.");
        }
        
    }
    
}
