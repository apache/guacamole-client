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
package org.apache.guacamole.auth.restrict.conf;

import org.apache.guacamole.properties.BooleanGuacamoleProperty;

/**
 * A class that implements the various properties that this module can
 * leverage from guacamole.properties.
 */
public class RestrictionProperties {
    
    /**
     * A property that allows for configuring whether or not Guacamole applies
     * the restrictions to admin accounts. By default, login restrictions do
     * NOT apply to admin accounts.
     */
    public static final BooleanGuacamoleProperty RESTRICT_ADMIN_ACCOUNTS = new BooleanGuacamoleProperty() {
        
        @Override
        public String getName() { return "restrict-admin-accounts"; }
        
    };
    
}
