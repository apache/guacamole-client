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

import org.apache.guacamole.auth.jdbc.user.UserModel;
import org.apache.guacamole.auth.jdbc.user.UserModelInterface;

/**
 * Common interface for objects that will ultimately be made available through
 * the Directory class. All such objects will need the same base set of queries
 * to fulfill the needs of the Directory class.
 *
 * @param <ModelType>
 *            The type of object contained within the directory whose objects
 *            are mapped by this mapper.
 *            
 * @param <Mapper>
 *            The specific mapper.
 */
public abstract class ModeledDirectoryObjectMapperImp<ModelType, Mapper extends ModeledDirectoryObjectMapper<ModelType>> implements ModeledDirectoryObjectMapperInterface<ModelType> {

	protected abstract Mapper getMapper();
	
	/**
     * Selects the identifiers of all objects, regardless of whether they
     * are readable by any particular user. This should only be called on
     * behalf of a system administrator. If identifiers are needed by a non-
     * administrative user who must have explicit read rights, use
     * selectReadableIdentifiers() instead.
     *
     * @return
     *     A Set containing all identifiers of all objects.
     */
    public Set<String> selectIdentifiers() {
    	return getMapper().selectIdentifiers();
    }
    
    /**
     * Selects the identifiers of all objects that are explicitly readable by
     * the given user. If identifiers are needed by a system administrator
     * (who, by definition, does not need explicit read rights), use
     * selectIdentifiers() instead.
     *
     * @param user
     *    The user whose permissions should determine whether an identifier
     *    is returned.
     *
     * @param effectiveGroups
     *     The identifiers of any known effective groups that should be taken
     *     into account, such as those defined externally to the database.
     *
     * @return
     *     A Set containing all identifiers of all readable objects.
     */
	public Set<String> selectReadableIdentifiers(UserModelInterface user,
			Collection<String> effectiveGroups) {
    	return getMapper().selectReadableIdentifiers((UserModel) user, effectiveGroups);
    }
    
    /**
     * Selects all objects which have the given identifiers. If an identifier
     * has no corresponding object, it will be ignored. This should only be
     * called on behalf of a system administrator. If objects are needed by a
     * non-administrative user who must have explicit read rights, use
     * selectReadable() instead.
     *
     * @param identifiers
     *     The identifiers of the objects to return.
     *
     * @return 
     *     A Collection of all objects having the given identifiers.
     */
    public Collection<ModelType> select(Collection<String> identifiers) {
    	return getMapper().select(identifiers);
    }

    /**
     * Selects all objects which have the given identifiers and are explicitly
     * readably by the given user. If an identifier has no corresponding
     * object, or the corresponding object is unreadable, it will be ignored.
     * If objects are needed by a system administrator (who, by definition,
     * does not need explicit read rights), use select() instead.
     *
     * @param user
     *    The user whose permissions should determine whether an object 
     *    is returned.
     *
     * @param identifiers
     *     The identifiers of the objects to return.
     *
     * @param effectiveGroups
     *     The identifiers of any known effective groups that should be taken
     *     into account, such as those defined externally to the database.
     *
     * @return 
     *     A Collection of all objects having the given identifiers.
     */
    public Collection<ModelType> selectReadable(UserModelInterface user,
            Collection<String> identifiers,
            Collection<String> effectiveGroups) {
    	return getMapper().selectReadable((UserModel) user, identifiers, effectiveGroups);
    }

    /**
     * Inserts the given object into the database. If the object already
     * exists, this will result in an error.
     *
     * @param object
     *     The object to insert.
     *
     * @return
     *     The number of rows inserted.
     */
    public int insert(ModelType object) {
    	return getMapper().insert(object);
    }

    /**
     * Deletes the given object into the database. If the object does not 
     * exist, this operation has no effect.
     *
     * @param identifier
     *     The identifier of the object to delete.
     *
     * @return
     *     The number of rows deleted.
     */
    public int delete(String identifier) {
    	return getMapper().delete(identifier);
    }

    /**
     * Updates the given existing object in the database. If the object does 
     * not actually exist, this operation has no effect.
     *
     * @param object
     *     The object to update.
     *
     * @return
     *     The number of rows updated.
     */
    public int update(ModelType object) {
    	return getMapper().update(object);
    }

    /**
     * Deletes any arbitrary attributes currently associated with the given
     * object in the database.
     *
     * @param object
     *     The object whose arbitrary attributes should be deleted.
     *
     * @return
     *     The number of rows deleted.
     */
    public int deleteAttributes(ModelType object) {
    	return getMapper().deleteAttributes(object);
    }

    /**
     * Inserts all arbitrary attributes associated with the given object.
     *
     * @param object
     *     The object whose arbitrary attributes should be inserted.
     *
     * @return
     *     The number of rows inserted.
     */
    public int insertAttributes(ModelType object) {
    	return getMapper().insertAttributes(object);
    }


}
