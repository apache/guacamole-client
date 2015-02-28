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

package org.glyptodon.guacamole.auth.jdbc.connection;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for connection record objects.
 *
 * @author Michael Jumper
 */
public interface ConnectionRecordMapper {

    /**
     * Returns a collection of all connection records associated with the
     * connection having the given identifier.
     *
     * @param identifier
     *     The identifier of the connection whose records are to be retrieved.
     *
     * @return
     *     A collection of all connection records associated with the
     *     connection having the given identifier. This collection will be
     *     empty if no such connection exists.
     */
    List<ConnectionRecordModel> select(@Param("identifier") String identifier);

    /**
     * Inserts the given connection record.
     *
     * @param record
     *     The connection record to insert.
     *
     * @return
     *     The number of rows inserted.
     */
    int insert(@Param("record") ConnectionRecordModel record);
    
}
