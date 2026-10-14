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

package org.apache.guacamole.morphia.sharingprofile;

import org.apache.guacamole.morphia.base.ChildObjectModel;
import org.mongodb.morphia.annotations.Entity;

/**
 * 
 * Object representation of a Guacamole sharing profile, as represented in the
 * database.
 * 
 * guacamole_sharing_profile: { id: string, name: string, connection:
 * ConnectionModel }
 *
 */
@Entity("guacamole_sharing_profile")
public class SharingProfileModel extends ChildObjectModel {

    @Override
    public String getIdentifier() {
        return super.getIdentifier() != null ? super.getIdentifier()
                : super.getId().toString();
    }

}
