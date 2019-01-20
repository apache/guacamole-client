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

package org.apache.guacamole.auth.jdbc.base;

import java.util.Collection;
import java.util.Set;

import org.apache.guacamole.auth.jdbc.user.UserModelInterface;

/**
 * Mapper for the relations represented by a particular RelatedObjectSet
 * implementation.
 *
 * @param <ParentModelType>
 *     The underlying database model of the object on the parent side of the
 *     one-to-many relationship represented by the RelatedObjectSet mapped by
 *     this ObjectRelationMapper.
 *     
 * @param <Mapper>
 *            The specific mapper.
 */
public abstract class ObjectRelationMapperImp<ParentModelType extends ObjectModelInterface, Mapper extends ObjectRelationMapper<ParentModelType>> 
	implements ObjectRelationMapperInterface<ParentModelType> {

	protected abstract Mapper getMapper();
	
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
     * @return
     *     The number of rows inserted.
     */
    public int insert(ParentModelType parent,
            Collection<String> children) {
    	return getMapper().insert(parent, children);
    }

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
     * @return
     *     The number of rows deleted.
     */
    public int delete(ParentModelType parent,
            Collection<String> children) {
    	return getMapper().delete(parent, children);
    }

    /**
     * Retrieves the identifiers of all objects on the child side of the
     * one-to-many relationship represented by the RelatedObjectSet mapped by
     * this ObjectRelationMapper. This should only be called on behalf of a
     * system administrator. If identifiers are needed by a non-administrative
     * user who must have explicit read rights, use
     * selectReadableChildIdentifiers() instead.
     *
     * @param parent
     *     The model of the object on the parent side of the one-to-many
     *     relationship represented by the RelatedObjectSet.
     *
     * @return
     *     A Set containing the identifiers of all objects on the child side
     *     of the one-to-many relationship.
     */
    public Set<String> selectChildIdentifiers(ParentModelType parent) {
    	return getMapper().selectChildIdentifiers(parent);
    }

    /**
     * Retrieves the identifiers of all objects on the child side of the
     * one-to-many relationship represented by the RelatedObjectSet mapped by
     * this ObjectRelationMapper, including only those objects which are
     * explicitly readable by the given user. If identifiers are needed by a
     * system administrator (who, by definition, does not need explicit read
     * rights), use selectChildIdentifiers() instead.

     *
     * @param user
     *    The user whose permissions should determine whether an identifier
     *    is returned.
     *
     * @param effectiveGroups
     *     The identifiers of any known effective groups that should be taken
     *     into account, such as those defined externally to the database.
     *
     * @param parent
     *     The model of the object on the parent side of the one-to-many
     *     relationship represented by the RelatedObjectSet.
     *
     * @return
     *     A Set containing the identifiers of all readable objects on the
     *     child side of the one-to-many relationship.
     */
    public Set<String> selectReadableChildIdentifiers(UserModelInterface user,
            Collection<String> effectiveGroups,
            ParentModelType parent) {
    	return getMapper().selectReadableChildIdentifiers(user, effectiveGroups, parent);
    }

}
