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

import java.util.Set;
import org.apache.guacamole.auth.jdbc.base.ModeledDirectoryObjectMapper;
import org.apache.guacamole.auth.jdbc.user.UserModel;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for sharing profile objects.
 *
 * @author Michael Jumper
 */
public interface SharingProfileMapper
        extends ModeledDirectoryObjectMapper<SharingProfileModel> {

    /**
     * Selects the identifiers of all sharing profiles associated with the given
     * primary connection, regardless of whether they are readable by any
     * particular user. This should only be called on behalf of a system
     * administrator. If identifiers are needed by a non-administrative user who
     * must have explicit read rights, use selectReadableIdentifiersWithin()
     * instead.
     *
     * @param primaryConnectionIdentifier
     *     The identifier of the primary connection.
     *
     * @return
     *     A Set containing all identifiers of all objects.
     */
    Set<String> selectIdentifiersWithin(
            @Param("primaryConnectionIdentifier") String primaryConnectionIdentifier);
    
    /**
     * Selects the identifiers of all sharing profiles associated with the given
     * primary connection that are explicitly readable by the given user. If
     * identifiers are needed by a system administrator (who, by definition,
     * does not need explicit read rights), use selectIdentifiersWithin()
     * instead.
     *
     * @param user
     *    The user whose permissions should determine whether an identifier
     *    is returned.
     *
     * @param primaryConnectionIdentifier
     *     The identifier of the primary connection.
     *
     * @return
     *     A Set containing all identifiers of all readable objects.
     */
    Set<String> selectReadableIdentifiersWithin(@Param("user") UserModel user,
            @Param("primaryConnectionIdentifier") String primaryConnectionIdentifier);

    /**
     * Selects the sharing profile associated with the given primary connection
     * and having the given name. If no such sharing profile exists, null is
     * returned.
     *
     * @param primaryConnectionIdentifier
     *     The identifier of the primary connection to search against.
     *
     * @param name
     *     The name of the sharing profile to find.
     *
     * @return
     *     The sharing profile having the given name and associated with the
     *     given primary connection, or null if no such sharing profile exists.
     */
    SharingProfileModel selectOneByName(
            @Param("primaryConnectionIdentifier") String primaryConnectionIdentifier,
            @Param("name") String name);
    
}