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

package org.apache.guacamole.auth.ldap.conf;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.properties.GuacamoleProperty;

/**
 *
 * @author nick_couchman
 */
public abstract class BindTypeGuacamoleProperty implements GuacamoleProperty<BindType> {
    
    @Override
    public BindType parseValue(String value) throws GuacamoleException {
        
        if (value == null || value.isEmpty())
            return null;
        
        BindType bindType = BindType.valueOf(value);
        if (bindType != null)
            return bindType;
        
        throw new GuacamoleServerException("Invalid bind type specified - value "
            + " should be one of \"anonymous\", \"derived\", \"direct\", "
            + " or \"search\".");
        
    }
    
}
