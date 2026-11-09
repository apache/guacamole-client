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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.morphia.base.RestrictedObject;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.SharingProfile;

import com.google.inject.Inject;

/**
 * Implementation of the SharingProfile Directory which is driven by an
 * underlying, arbitrary database.
 */
public class SharingProfileDirectory extends RestrictedObject
        implements Directory<SharingProfile> {

    /**
     * Service for managing sharing profile objects.
     */
    @Inject
    private SharingProfileService sharingProfileService;

    @Override
    public SharingProfile get(String identifier) throws GuacamoleException {
        return sharingProfileService.retrieveObject(getCurrentUser(),
                identifier);
    }

    @Override
    public Collection<SharingProfile> getAll(Collection<String> identifiers)
            throws GuacamoleException {
        return Collections
                .<SharingProfile>unmodifiableCollection(sharingProfileService
                        .retrieveObjects(getCurrentUser(), identifiers));
    }

    @Override
    public Set<String> getIdentifiers() throws GuacamoleException {
        return sharingProfileService.getIdentifiers(getCurrentUser());
    }

    @Override
    public void add(SharingProfile object) throws GuacamoleException {
        sharingProfileService.createObject(getCurrentUser(), object);
    }

    @Override
    public void update(SharingProfile object) throws GuacamoleException {
        ModeledSharingProfile sharingProfile = (ModeledSharingProfile) object;
        sharingProfileService.updateObject(getCurrentUser(), sharingProfile);
    }

    @Override
    public void remove(String identifier) throws GuacamoleException {
        sharingProfileService.deleteObject(getCurrentUser(), identifier);
    }

}
