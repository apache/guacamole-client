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

package org.apache.guacamole.auth.common.usergroup;

import org.apache.guacamole.auth.common.base.ModeledDirectoryObjectMapperInterface;

/**
 * Mapper for user group objects.
 */
public interface UserGroupMapperInterface<ModelType>
        extends ModeledDirectoryObjectMapperInterface<ModelType> {

    /**
     * Returns the group having the given name, if any. If no such group exists,
     * null is returned.
     *
     * @param name
     *            The name of the group to return.
     *
     * @return The group having the given name, or null if no such group exists.
     */
    UserGroupModelInterface selectOne(String name);

}
