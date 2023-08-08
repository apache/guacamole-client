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

package org.apache.guacamole.rest.work;

import org.apache.guacamole.net.auth.AuthenticatedUser;
import org.apache.guacamole.net.auth.Directory;
import org.apache.guacamole.net.auth.UserContext;
import org.apache.guacamole.net.auth.Work;
import org.apache.guacamole.rest.directory.DirectoryObjectResource;
import org.apache.guacamole.rest.directory.DirectoryObjectTranslator;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class WorkResource extends DirectoryObjectResource<Work, APIWork> {
    
    /**
     * Creates a new UserGroupResource which exposes the operations and
     * subresources available for the given UserGroup.
     *
     * @param authenticatedUser
     *     The user that is accessing this resource.
     *
     * @param userContext
     *     The UserContext associated with the given Directory.
     *
     * @param directory
     *     The Directory which contains the given UserGroup.
     *
     * @param userGroup
     *     The UserGroup that this UserGroupResource should represent.
     *
     * @param translator
     *     A DirectoryObjectTranslator implementation which handles Users.
     */
    @AssistedInject
    public WorkResource(@Assisted AuthenticatedUser authenticatedUser,
            @Assisted UserContext userContext,
            @Assisted Directory<Work> directory,
            @Assisted Work work,
            DirectoryObjectTranslator<Work, APIWork> translator) {
        super(authenticatedUser, userContext, Work.class, directory, work, translator);
    }

}
