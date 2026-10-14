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

import org.apache.guacamole.morphia.base.ArbitraryAttributeModel;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Reference;

/**
 * 
 * A single parameter name/value pair belonging to a sharing profile.
 * 
 * guacamole_sharing_profile_parameter: { id: string, parameter_name: string,
 * parameter_value, string, sharing_profile: SharingProfileModel
 * (sharing_profile_id) }
 *
 */
@Entity("guacamole_sharing_profile_parameter")
public class SharingProfileParameterModel extends ArbitraryAttributeModel {

    /** The sharing profile. */
    @Reference(value = "sharing_profile")
    private SharingProfileModel sharingProfile;

    /**
     * Gets the sharing profile.
     *
     * @return the sharing profile
     */
    public SharingProfileModel getSharingProfile() {
        return sharingProfile;
    }

    /**
     * Sets the sharing profile.
     *
     * @param sharingProfile
     *            the new sharing profile
     */
    public void setSharingProfile(SharingProfileModel sharingProfile) {
        this.sharingProfile = sharingProfile;
    }

}
