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

package org.apache.guacamole.auth.common.base;

import java.util.Collection;
import java.util.Set;
import org.apache.guacamole.auth.common.CommonEnvironment;
import com.google.inject.Inject;

/**
 * Service which provides convenience methods for creating, retrieving, and
 * manipulating entities.
 */
public abstract class EntityServiceAbstract {

    /**
     * The Guacamole server environment.
     */
    @Inject
    protected CommonEnvironment environment;

    /**
     * Mapper for Entity model objects.
     */
    @Inject
    protected EntityMapperInterface entityMapper;

    /**
     * Returns the set of all group identifiers of which the given entity is a
     * member, taking into account the given collection of known group
     * memberships which are not necessarily defined within the database.
     * 
     * Note that group visibility with respect to the queried entity is NOT
     * taken into account. If the entity is a member of a group, the identifier
     * of that group will be included in the returned set even if the current
     * user lacks "READ" permission for that group.
     *
     * @param entity
     *            The entity whose effective groups should be returned.
     *
     * @param effectiveGroups
     *            The identifiers of any known effective groups that should be
     *            taken into account, such as those defined externally to the
     *            database.
     *
     * @return The set of identifiers of all groups that the given entity is a
     *         member of, including those where membership is inherited through
     *         membership in other groups.
     */
    public abstract Set<String> retrieveEffectiveGroups(
            ModeledPermissions<? extends EntityModelInterface> entity,
            Collection<String> effectiveGroups);

}
