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

package net.sourceforge.guacamole.net.auth.mysql.dao;

import java.util.Collection;
import java.util.Set;
import net.sourceforge.guacamole.net.auth.mysql.model.UserModel;
import org.apache.ibatis.annotations.Param;

/**
 * Common interface for objects that will ultimately be made available through
 * the Directory class. All such objects will need the same base set of queries
 * to fulfill the needs of the Directory class.
 *
 * @author Michael Jumper
 * @param <ModelType>
 *     The type of object contained within the directory whose objects are
 *     mapped by this mapper.
 */
public interface DirectoryObjectMapper<ModelType> {

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
    Set<String> selectIdentifiers();
    
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
     * @return
     *     A Set containing all identifiers of all readable objects.
     */
    Set<String> selectReadableIdentifiers(@Param("user") UserModel user);
    
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
    Collection<ModelType> select(@Param("identifiers") Collection<String> identifiers);

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
     * @return 
     *     A Collection of all objects having the given identifiers.
     */
    Collection<ModelType> selectReadable(@Param("user") UserModel user,
            @Param("identifiers") Collection<String> identifiers);

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
    int insert(@Param("object") ModelType object);

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
    int delete(@Param("identifier") String identifier);

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
    int update(@Param("object") ModelType object);
    
}