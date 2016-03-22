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

package org.apache.guacamole.auth.jdbc.connection;

import java.util.Collection;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for connection parameter objects.
 *
 * @author Michael Jumper
 */
public interface ParameterMapper {

    /**
     * Returns a collection of all parameters associated with the connection
     * having the given identifier.
     *
     * @param identifier
     *     The identifier of the connection whose parameters are to be
     *     retrieved.
     *
     * @return
     *     A collection of all parameters associated with the connection
     *     having the given identifier. This collection will be empty if no
     *     such connection exists.
     */
    Collection<ParameterModel> select(@Param("identifier") String identifier);

    /**
     * Inserts each of the parameter model objects in the given collection as
     * new connection parameters.
     *
     * @param parameters
     *     The connection parameters to insert.
     *
     * @return
     *     The number of rows inserted.
     */
    int insert(@Param("parameters") Collection<ParameterModel> parameters);

    /**
     * Deletes all parameters associated with the connection having the given
     * identifier.
     *
     * @param identifier
     *     The identifier of the connection whose parameters should be
     *     deleted.
     *
     * @return
     *     The number of rows deleted.
     */
    int delete(@Param("identifier") String identifier);
    
}
