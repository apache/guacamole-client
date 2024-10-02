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

package org.apache.guacamole.auth.jdbc.user;

import java.util.Collection;
import org.apache.guacamole.auth.jdbc.base.ModeledDirectoryObjectMapper;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for user objects.
 */
public interface UserMapper extends ModeledDirectoryObjectMapper<UserModel> {

    /**
     * Returns the user having the given username, if any. If no such user
     * exists, null is returned.
     *
     * @param username
     *     The username of the user to return.
     * 
     * @param caseSensitive
     *     true if the search should evaluate usernames in a case-sensitive
     *     manner, otherwise false.
     *
     * @return
     *     The user having the given username, or null if no such user exists.
     */
    UserModel selectOne(@Param("username") String username,
                        @Param("caseSensitive") boolean caseSensitive);
    
    /**
     * Selects all users which have the given identifiers. If an identifier
     * has no corresponding object, it will be ignored. This should only be
     * called on behalf of a system administrator. If users are needed by a
     * non-administrative user who must have explicit read rights, use
     * selectReadable() instead.
     *
     * @param identifiers
     *     The identifiers of the users to return.
     * 
     * @param caseSensitive
     *     true if the query should evaluate username identifiers in a
     *     case-sensitive manner, otherwise false.
     *
     * @return 
     *     A Collection of all objects having the given identifiers.
     */
    Collection<UserModel> select(@Param("identifiers") Collection<String> identifiers,
                                 @Param("caseSensitive") boolean caseSensitive);
    
    /**
     * Selects all users which have the given identifiers and are explicitly
     * readable by the given user. If an identifier has no corresponding
     * object, or the corresponding user is unreadable, it will be ignored.
     * If users are needed by a system administrator (who, by definition,
     * does not need explicit read rights), use select() instead.
     *
     * @param user
     *    The user whose permissions should determine whether an object 
     *    is returned.
     *
     * @param identifiers
     *     The identifiers of the users to return.
     *
     * @param effectiveGroups
     *     The identifiers of any known effective groups that should be taken
     *     into account, such as those defined externally to the database.
     * 
     * @param caseSensitive
     *     true if the query should evaluate username identifiers in a
     *     case-sensitive manner, otherwise false.
     *
     * @return 
     *     A Collection of all objects having the given identifiers.
     */
    Collection<UserModel> selectReadable(@Param("user") UserModel user,
            @Param("identifiers") Collection<String> identifiers,
            @Param("effectiveGroups") Collection<String> effectiveGroups,
            @Param("caseSensitive") boolean caseSensitive);
    
    /**
     * Deletes the given user from the database. If the user does not 
     * exist, this operation has no effect.
     *
     * @param identifier
     *     The identifier of the user to delete.
     * 
     * @param caseSensitive
     *     true if the query should evaluate username identifiers in a
     *     case-sensitive manner, otherwise false.
     *
     * @return
     *     The number of rows deleted.
     */
    int delete(@Param("identifier") String identifier,
            @Param("caseSensitive") boolean caseSensitive);

}
