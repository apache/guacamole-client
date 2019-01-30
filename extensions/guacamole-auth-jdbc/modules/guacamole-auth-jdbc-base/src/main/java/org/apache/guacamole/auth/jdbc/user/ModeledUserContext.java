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

package org.apache.guacamole.auth.jdbc.user;


import java.util.Date;

import org.apache.guacamole.auth.common.base.RestrictedObject;
import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.common.user.ModeledUserContextAbstract;
import org.apache.guacamole.auth.jdbc.base.ActivityRecordModel;

/**
 * UserContext implementation which is driven by an arbitrary, underlying
 * database.
 */
public class ModeledUserContext extends ModeledUserContextAbstract {

    
    @Override
    public void init(ModeledAuthenticatedUser currentUser) {

        super.init(currentUser);
        
        // Init directories
        ((RestrictedObject) userDirectory).init(currentUser);
        ((RestrictedObject) userGroupDirectory).init(currentUser);
        connectionDirectory.init(currentUser);
        ((RestrictedObject) connectionGroupDirectory).init(currentUser);
        sharingProfileDirectory.init(currentUser);
        activeConnectionDirectory.init(currentUser);

        // Create login record for user
        userRecord = new ActivityRecordModel();
        ((ActivityRecordModel) userRecord).setUsername(currentUser.getIdentifier());
        userRecord.setStartDate(new Date());
        userRecord.setRemoteHost(currentUser.getCredentials().getRemoteHostname());

        // Insert record representing login
        userRecordMapper.insert(userRecord);

    }

}
