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

package org.apache.guacamole.auth.common.sharingprofile;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.guacamole.GuacamoleClientException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.auth.common.base.ModeledChildDirectoryObjectServiceAbstract;
import org.apache.guacamole.auth.common.base.ModeledDirectoryObjectMapperInterface;
import org.apache.guacamole.auth.common.permission.SharingProfilePermissionMapperInterface;
import org.apache.guacamole.auth.common.user.ModeledAuthenticatedUser;
import org.apache.guacamole.net.auth.SharingProfile;
import org.apache.guacamole.net.auth.permission.ObjectPermission;
import org.apache.guacamole.net.auth.permission.ObjectPermissionSet;
import org.apache.guacamole.net.auth.permission.SystemPermission;
import org.apache.guacamole.net.auth.permission.SystemPermissionSet;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating sharing profiles.
 */
public abstract class SharingProfileServiceAbstract extends
        ModeledChildDirectoryObjectServiceAbstract<ModeledSharingProfile, SharingProfile, SharingProfileModelInterface> {

    /**
     * Mapper for accessing sharing profiles.
     */
    @Inject
    protected SharingProfileMapperInterface sharingProfileMapper;

    /**
     * Mapper for manipulating sharing profile permissions.
     */
    @Inject
    private SharingProfilePermissionMapperInterface sharingProfilePermissionMapper;

    /**
     * Mapper for accessing sharing profile parameters.
     */
    @Inject
    private SharingProfileParameterMapperInterface parameterMapper;

    /**
     * Provider for creating sharing profiles.
     */
    @Inject
    private Provider<ModeledSharingProfile> sharingProfileProvider;

    @SuppressWarnings("unchecked")
	@Override
    protected ModeledDirectoryObjectMapperInterface<SharingProfileModelInterface> getObjectMapper() {
        return (ModeledDirectoryObjectMapperInterface<SharingProfileModelInterface>) sharingProfileMapper;
    }

    @Override
    protected SharingProfilePermissionMapperInterface getPermissionMapper() {
        return sharingProfilePermissionMapper;
    }

    @Override
    protected ModeledSharingProfile getObjectInstance(
            ModeledAuthenticatedUser currentUser, SharingProfileModelInterface model) {
        ModeledSharingProfile sharingProfile = sharingProfileProvider.get();
        sharingProfile.init(currentUser, model);
        return sharingProfile;
    }

    @Override
    protected boolean hasCreatePermission(ModeledAuthenticatedUser user)
            throws GuacamoleException {

        // Return whether user has explicit sharing profile creation permission
        SystemPermissionSet permissionSet = user.getUser().getEffectivePermissions().getSystemPermissions();
        return permissionSet
                .hasPermission(SystemPermission.Type.CREATE_SHARING_PROFILE);

    }

    @Override
    protected ObjectPermissionSet getEffectivePermissionSet(
            ModeledAuthenticatedUser user) throws GuacamoleException {

        // Return permissions related to sharing profiles
        return user.getUser().getEffectivePermissions().getSharingProfilePermissions();

    }

    @Override
    protected ObjectPermissionSet getParentEffectivePermissionSet(
            ModeledAuthenticatedUser user) throws GuacamoleException {

        // Sharing profiles are children of connections
        return user.getUser().getEffectivePermissions().getConnectionPermissions();

    }

    @Override
    protected void beforeCreate(ModeledAuthenticatedUser user,
            SharingProfile object, SharingProfileModelInterface model)
            throws GuacamoleException {

        super.beforeCreate(user, object, model);

        // Name must not be blank
        if (model.getName() == null || model.getName().trim().isEmpty())
            throw new GuacamoleClientException(
                    "Sharing profile names must not be blank.");

        // Do not attempt to create duplicate sharing profiles
        SharingProfileModelInterface existing = sharingProfileMapper
                .selectOneByName(model.getParentIdentifier(), model.getName());
        if (existing != null)
            throw new GuacamoleClientException("The sharing profile \""
                    + model.getName() + "\" already exists.");

    }

    @Override
    protected void beforeUpdate(ModeledAuthenticatedUser user,
            ModeledSharingProfile object, SharingProfileModelInterface model)
            throws GuacamoleException {

        super.beforeUpdate(user, object, model);

        // Name must not be blank
        if (model.getName() == null || model.getName().trim().isEmpty())
            throw new GuacamoleClientException(
                    "Sharing profile names must not be blank.");

        // Check whether such a sharing profile is already present
        SharingProfileModelInterface existing = sharingProfileMapper
                .selectOneByName(model.getParentIdentifier(), model.getName());
        if (existing != null) {

            // If the specified name matches a DIFFERENT existing sharing
            // profile, the update cannot continue
            if (!existing.getObjectID().equals(model.getObjectID()))
                throw new GuacamoleClientException("The sharing profile \""
                        + model.getName() + "\" already exists.");

        }

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
    protected abstract Collection<SharingProfileParameterModelInterface> getParameterModels(
            ModeledSharingProfile sharingProfile);

    @Override
    public ModeledSharingProfile createObject(ModeledAuthenticatedUser user,
            SharingProfile object) throws GuacamoleException {

        // Create sharing profile
        ModeledSharingProfile sharingProfile = super.createObject(user, object);
        sharingProfile.setParameters(object.getParameters());

        // Insert new parameters, if any
        Collection<SharingProfileParameterModelInterface> parameterModels = getParameterModels(
                sharingProfile);
        if (!parameterModels.isEmpty())
            parameterMapper.insert(parameterModels);

        return sharingProfile;

    }

    @Override
    public void updateObject(ModeledAuthenticatedUser user,
            ModeledSharingProfile object) throws GuacamoleException {

        // Update sharing profile
        super.updateObject(user, object);

        // Replace existing parameters with new parameters, if any
        Collection<SharingProfileParameterModelInterface> parameterModels = getParameterModels(
                object);
        parameterMapper.delete(object.getIdentifier());
        if (!parameterModels.isEmpty())
            parameterMapper.insert(parameterModels);

    }

    /**
     * Retrieves all parameters visible to the given user and associated with
     * the sharing profile having the given identifier. If the given user has no
     * access to such parameters, or no such sharing profile exists, the
     * returned map will be empty.
     *
     * @param user
     *            The user retrieving sharing profile parameters.
     *
     * @param identifier
     *            The identifier of the sharing profile whose parameters are
     *            being retrieved.
     *
     * @return A new map of all parameter name/value pairs that the given user
     *         has access to.
     */
    public Map<String, String> retrieveParameters(ModeledAuthenticatedUser user,
            String identifier) {

        Map<String, String> parameterMap = new HashMap<String, String>();

        // Determine whether we have permission to read parameters
        boolean canRetrieveParameters;
        try {
            canRetrieveParameters = hasObjectPermission(user, identifier,
                    ObjectPermission.Type.UPDATE);
        }

        // Provide empty (but mutable) map if unable to check permissions
        catch (GuacamoleException e) {
            return parameterMap;
        }

        // Populate parameter map if we have permission to do so
        if (canRetrieveParameters) {
            for (SharingProfileParameterModelInterface parameter : parameterMapper
                    .select(identifier))
                parameterMap.put(parameter.getName(), parameter.getValue());
        }

        return parameterMap;

    }

}
