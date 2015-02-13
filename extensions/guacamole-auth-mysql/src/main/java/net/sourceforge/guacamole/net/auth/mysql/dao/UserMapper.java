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

import net.sourceforge.guacamole.net.auth.mysql.model.UserModel;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for user objects.
 *
 * @author Michael Jumper
 */
public interface UserMapper extends DirectoryObjectMapper<UserModel> {

    /**
     * Returns the user having the given username and password, if any. If no
     * such user exists, null is returned.
     *
     * @param username
     *     The username of the user to return.
     *
     * @param password
     *     The password of the user to return.
     *
     * @return
     *     The user having the given username and password, or null if no such
     *     user exists.
     */
    UserModel selectByCredentials(@Param("username") String username,
            @Param("password") String password);
    
}