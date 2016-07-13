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

package org.apache.guacamole.rest.user;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.auth.User;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.rest.directory.DirectoryObjectResource;
import org.apache.guacamole.rest.directory.DirectoryObjectResourceFactory;
import org.apache.guacamole.rest.directory.DirectoryObjectTranslator;
import org.apache.guacamole.rest.directory.DirectoryResource;

/**
 * A REST resource which abstracts the operations available on a Directory of
 * Users.
 *
 * @author Michael Jumper
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserDirectoryResource extends DirectoryResource<User, APIUser> {

    /**
     * The UserContext associated with the Directory which contains the
     * User exposed by this resource.
     */
    private final UserContext userContext;

    /**
     * The Directory exposed by this resource.
     */
    private final Directory<User> directory;

    /**
     * A factory which can be used to create instances of resources representing
     * Users.
     */
    private final DirectoryObjectResourceFactory<User, APIUser> resourceFactory;

    /**
     * Creates a new UserDirectoryResource which exposes the operations and
     * subresources available for the given User Directory.
     *
     * @param userContext
     *     The UserContext associated with the given Directory.
     *
     * @param directory
     *     The Directory being exposed.
     *
     * @param translator
     *     A DirectoryObjectTranslator implementation which handles
     *     Users.
     *
     * @param resourceFactory
     *     A factory which can be used to create instances of resources
     *     representing Users.
     */
    @AssistedInject
    public UserDirectoryResource(@Assisted UserContext userContext,
            @Assisted Directory<User> directory,
            DirectoryObjectTranslator<User, APIUser> translator,
            DirectoryObjectResourceFactory<User, APIUser> resourceFactory) {
        super(userContext, directory, translator, resourceFactory);
        this.userContext = userContext;
        this.directory = directory;
        this.resourceFactory = resourceFactory;
    }

    @Override
    public APIUser createObject(APIUser object) throws GuacamoleException {

        // Randomly set the password if it wasn't provided
        if (object.getPassword() == null)
            object.setPassword(UUID.randomUUID().toString());

        return super.createObject(object);

    }

    @Override
    public DirectoryObjectResource<User, APIUser>
        getObjectResource(String identifier) throws GuacamoleException {

        // If username is own username, just use self - might not have query permissions
        if (userContext.self().getIdentifier().equals(identifier))
            return resourceFactory.create(userContext, directory, userContext.self());

        return super.getObjectResource(identifier);

    }

}
