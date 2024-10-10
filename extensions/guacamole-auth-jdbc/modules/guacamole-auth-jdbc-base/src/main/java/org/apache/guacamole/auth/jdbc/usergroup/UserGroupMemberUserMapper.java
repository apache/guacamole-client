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

package org.apache.guacamole.auth.jdbc.usergroup;

import java.util.Collection;
import org.apache.guacamole.auth.jdbc.base.ObjectRelationMapper;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for the one-to-many relationship between a user group and its user
 * members.
 */
public interface UserGroupMemberUserMapper extends ObjectRelationMapper<UserGroupModel> {

    /**
     * Inserts rows as necessary to establish the one-to-many relationship
     * represented by the RelatedObjectSet between the given parent and
     * children. If the relation for any parent/child pair is already present,
     * no attempt is made to insert a new row for that relation.
     *
     * @param parent
     *     The model of the object on the parent side of the one-to-many
     *     relationship represented by the RelatedObjectSet.
     *
     * @param children
     *     The identifiers of the objects on the child side of the one-to-many
     *     relationship represented by the RelatedObjectSet.
     * 
     * @param caseSensitive
     *     True if username case should be respected when looking up the username
     *     in the guacamole_entity table, or false if the query to the
     *     guacamole_entity table should be done case-insensitively.
     *
     * @return
     *     The number of rows inserted.
     */
    int insert(@Param("parent") UserGroupModel parent,
            @Param("children") Collection<String> children,
            @Param("caseSensitive") boolean caseSensitive);
    
    /**
     * Deletes rows as necessary to modify the one-to-many relationship
     * represented by the RelatedObjectSet between the given parent and
     * children. If the relation for any parent/child pair does not exist,
     * that specific relation is ignored, and deletion proceeds with the
     * remaining relations.
     *
     * @param parent
     *     The model of the object on the parent side of the one-to-many
     *     relationship represented by the RelatedObjectSet.
     *
     * @param children
     *     The identifiers of the objects on the child side of the one-to-many
     *     relationship represented by the RelatedObjectSet.
     * 
     * @param caseSensitive
     *     True if username case should be respected when looking up the username
     *     in the guacamole_entity table, or false if the query to the
     *     guacamole_entity table should be done case-insensitively.
     *
     * @return
     *     The number of rows deleted.
     */
    int delete(@Param("parent") UserGroupModel parent,
            @Param("children") Collection<String> children,
            @Param("caseSensitive") boolean caseSensitive);
    
}
