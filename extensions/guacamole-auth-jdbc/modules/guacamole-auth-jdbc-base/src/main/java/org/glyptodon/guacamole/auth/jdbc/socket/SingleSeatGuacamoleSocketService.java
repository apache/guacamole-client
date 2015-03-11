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

package org.glyptodon.guacamole.auth.jdbc.socket;

import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.glyptodon.guacamole.GuacamoleClientTooManyException;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.jdbc.connection.ModeledConnection;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleResourceConflictException;
import org.glyptodon.guacamole.auth.jdbc.connectiongroup.ModeledConnectionGroup;


/**
 * GuacamoleSocketService implementation which allows exactly one use
 * of any connection at any time. Concurrent usage of connections is not
 * allowed, and concurrent usage of connection groups is allowed only between
 * different users.
 *
 * @author Michael Jumper
 */
@Singleton
public class SingleSeatGuacamoleSocketService
    extends AbstractGuacamoleSocketService {

    /**
     * The set of all active connection identifiers.
     */
    private final Set<String> activeConnections =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * The set of all active user/connection group pairs.
     */
    private final Set<Seat> activeGroupSeats =
            Collections.newSetFromMap(new ConcurrentHashMap<Seat, Boolean>());
   
    @Override
    protected ModeledConnection acquire(AuthenticatedUser user,
            List<ModeledConnection> connections) throws GuacamoleException {

        // Return the first unused connection
        for (ModeledConnection connection : connections) {
            if (activeConnections.add(connection.getIdentifier()))
                return connection;
        }

        // Already in use
        throw new GuacamoleResourceConflictException("Cannot connect. This connection is in use.");

    }

    @Override
    protected void release(AuthenticatedUser user, ModeledConnection connection) {
        activeConnections.remove(connection.getIdentifier());
    }

    @Override
    protected void acquire(AuthenticatedUser user,
            ModeledConnectionGroup connectionGroup) throws GuacamoleException {

        // Do not allow duplicate use of connection groups
        Seat seat = new Seat(user.getUser().getIdentifier(), connectionGroup.getIdentifier());
        if (!activeGroupSeats.add(seat))
            throw new GuacamoleClientTooManyException("Cannot connect. Connection group already in use by this user.");

    }

    @Override
    protected void release(AuthenticatedUser user,
            ModeledConnectionGroup connectionGroup) {
        activeGroupSeats.remove(new Seat(user.getUser().getIdentifier(), connectionGroup.getIdentifier()));
    }

}
