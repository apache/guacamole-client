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

package org.apache.guacamole.auth.jdbc.sharingprofile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import org.apache.guacamole.auth.common.base.ArbitraryAttributeModelInterface;
import org.apache.guacamole.auth.common.permission.ObjectPermissionMapperInterface;
import org.apache.guacamole.auth.common.permission.ObjectPermissionModelInterface;
import org.apache.guacamole.auth.common.sharingprofile.ModeledSharingProfile;
import org.apache.guacamole.auth.common.sharingprofile.SharingProfileModelInterface;
import org.apache.guacamole.auth.common.sharingprofile.SharingProfileServiceAbstract;
import org.apache.guacamole.auth.common.sharingprofile.SharingProfileServiceInterface;
import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;
import org.apache.guacamole.auth.common.user.UserModelInterface;
import org.apache.guacamole.auth.jdbc.base.ModeledDirectoryObjectService;
import org.apache.guacamole.net.auth.SharingProfile;
import org.apache.guacamole.net.auth.permission.ObjectPermission.Type;
import com.google.inject.Inject;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating sharing profiles.
 */
public class SharingProfileService extends SharingProfileServiceAbstract
        implements SharingProfileServiceInterface {

    @Inject
    public SharingProfileService(
            Map<String, ObjectPermissionMapperInterface> mappers) {
        super(mappers);
    }

    @Override
    protected SharingProfileModel getModelInstance(
            ModeledAuthenticatedUser currentUser, final SharingProfile object) {

        // Create new ModeledSharingProfile backed by blank model
        SharingProfileModel model = new SharingProfileModel();
        ModeledSharingProfile sharingProfile = getObjectInstance(currentUser,
                model);

        // Set model contents through ModeledSharingProfile, copying the
        // provided sharing profile
        sharingProfile.setPrimaryConnectionIdentifier(
                object.getPrimaryConnectionIdentifier());
        sharingProfile.setName(object.getName());
        sharingProfile.setParameters(object.getParameters());
        sharingProfile.setAttributes(object.getAttributes());

        return model;

    }

    /**
     * Given an arbitrary Guacamole sharing profile, produces a collection of
     * parameter model objects containing the name/value pairs of that sharing
     * profile's parameters.
     *
     * @param sharingProfile
     *            The sharing profile whose configuration should be used to
     *            produce the collection of parameter models.
     *
     * @return A collection of parameter models containing the name/value pairs
     *         of the given sharing profile's parameters.
     */
    protected Collection<ArbitraryAttributeModelInterface> getParameterModels(
            ModeledSharingProfile sharingProfile) {

        Map<String, String> parameters = sharingProfile.getParameters();

        // Convert parameters to model objects
        Collection<ArbitraryAttributeModelInterface> parameterModels = new ArrayList<ArbitraryAttributeModelInterface>(
                parameters.size());
        for (Map.Entry<String, String> parameterEntry : parameters.entrySet()) {

            // Get parameter name and value
            String name = parameterEntry.getKey();
            String value = parameterEntry.getValue();

            // There is no need to insert empty parameters
            if (value == null || value.isEmpty())
                continue;

            // Produce model object from parameter
            SharingProfileParameterModel model = new SharingProfileParameterModel();
            model.setSharingProfileIdentifier(sharingProfile.getIdentifier());
            model.setName(name);
            model.setValue(value);

            // Add model to list
            parameterModels.add(model);

        }

        return parameterModels;

    }

    @Override
    protected boolean isValidIdentifier(String identifier) {

        // Empty identifiers are invalid
        if (identifier.isEmpty())
            return false;

        // Identifier is invalid if any non-numeric characters are present
        for (int i = 0; i < identifier.length(); i++) {
            if (!Character.isDigit(identifier.charAt(i)))
                return false;
        }

        // Identifier is valid - contains only numeric characters
        return true;

    }

    @Override
    protected void createModelPermission(UserModelInterface userModel,
            Collection<ObjectPermissionModelInterface> implicitPermissions,
            SharingProfileModelInterface model, Type permission) {

        ModeledDirectoryObjectService.getNewModelPermission(userModel,
                implicitPermissions, model, permission);

    }

}
