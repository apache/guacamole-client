/*
 * Copyright (C) 2015 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package org.apache.guacamole.auth.jdbc.connectiongroup;

import java.util.Set;
import org.apache.guacamole.auth.jdbc.base.ModeledDirectoryObjectMapper;
import org.apache.guacamole.auth.jdbc.user.UserModel;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for connection group objects.
 *
 * @author Michael Jumper
 */
public interface ConnectionGroupMapper extends ModeledDirectoryObjectMapper<ConnectionGroupModel> {

    /**
     * Selects the identifiers of all connection groups within the given parent
     * connection group, regardless of whether they are readable by any
     * particular user. This should only be called on behalf of a system
     * administrator. If identifiers are needed by a non-administrative user
     * who must have explicit read rights, use
     * selectReadableIdentifiersWithin() instead.
     *
     * @param parentIdentifier
     *     The identifier of the parent connection group, or null if the root
     *     connection group is to be queried.
     *
     * @return
     *     A Set containing all identifiers of all objects.
     */
    Set<String> selectIdentifiersWithin(@Param("parentIdentifier") String parentIdentifier);
    
    /**
     * Selects the identifiers of all connection groups within the given parent
     * connection group that are explicitly readable by the given user. If
     * identifiers are needed by a system administrator (who, by definition,
     * does not need explicit read rights), use selectIdentifiersWithin()
     * instead.
     *
     * @param user
     *    The user whose permissions should determine whether an identifier
     *    is returned.
     *
     * @param parentIdentifier
     *     The identifier of the parent connection group, or null if the root
     *     connection group is to be queried.
     *
     * @return
     *     A Set containing all identifiers of all readable objects.
     */
    Set<String> selectReadableIdentifiersWithin(@Param("user") UserModel user,
            @Param("parentIdentifier") String parentIdentifier);

    /**
     * Selects the connection group within the given parent group and having
     * the given name. If no such connection group exists, null is returned.
     *
     * @param parentIdentifier
     *     The identifier of the parent group to search within.
     *
     * @param name
     *     The name of the connection group to find.
     *
     * @return
     *     The connection group having the given name within the given parent
     *     group, or null if no such connection group exists.
     */
    ConnectionGroupModel selectOneByName(@Param("parentIdentifier") String parentIdentifier,
            @Param("name") String name);
    
}