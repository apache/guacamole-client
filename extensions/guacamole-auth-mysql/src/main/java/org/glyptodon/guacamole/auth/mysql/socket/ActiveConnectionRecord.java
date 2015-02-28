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

package org.glyptodon.guacamole.auth.mysql.socket;

import java.util.Date;
import org.glyptodon.guacamole.auth.mysql.user.AuthenticatedUser;
import org.glyptodon.guacamole.net.auth.ConnectionRecord;


/**
 * A connection record implementation that describes an active connection. As
 * the associated connection has not yet ended, getEndDate() will always return
 * null, and isActive() will always return true. The associated start date will
 * be the time of this objects creation.
 *
 * @author Michael Jumper
 */
public class ActiveConnectionRecord implements ConnectionRecord {

    /**
     * The user that connected to the connection associated with this connection
     * record.
     */
    private final AuthenticatedUser user;

    /**
     * The time this connection record was created.
     */
    private final Date startDate = new Date();

    /**
     * Creates a new connection record associated with the given user. The
     * start date of this connection record will be the time of its creation.
     *
     * @param user
     *     The user that connected to the connection associated with this
     *     connection record.
     */
    public ActiveConnectionRecord(AuthenticatedUser user) {
        this.user = user;
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public Date getEndDate() {

        // Active connections have not yet ended
        return null;
        
    }

    @Override
    public String getUsername() {
        return user.getUser().getIdentifier();
    }

    @Override
    public boolean isActive() {

        // Active connections are active by definition
        return true;
        
    }

}
