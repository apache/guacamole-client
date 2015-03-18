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

package org.glyptodon.guacamole.auth.jdbc.tunnel;

import com.google.inject.Singleton;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
 * GuacamoleTunnelService implementation which restricts concurrency only on a
 * per-user basis. Each connection or group may be used concurrently any number
 * of times, but each concurrent use must be associated with a different user.
 *
 * @author Michael Jumper
 */
@Singleton
public class MultiseatGuacamoleTunnelService
    extends AbstractGuacamoleTunnelService {

    /**
     * The set of all active user/connection pairs.
     */
    private final Set<Seat> activeSeats =
            Collections.newSetFromMap(new ConcurrentHashMap<Seat, Boolean>());

    /**
     * The set of all active user/connection group pairs.
     */
    private final Set<Seat> activeGroupSeats =
            Collections.newSetFromMap(new ConcurrentHashMap<Seat, Boolean>());
   
    @Override
    protected ModeledConnection acquire(AuthenticatedUser user,
            List<ModeledConnection> connections) throws GuacamoleException {

        String username = user.getUser().getIdentifier();

        // Sort connections in ascending order of usage
        ModeledConnection[] sortedConnections = connections.toArray(new ModeledConnection[connections.size()]);
        Arrays.sort(sortedConnections, new Comparator<ModeledConnection>() {

            @Override
            public int compare(ModeledConnection a, ModeledConnection b) {

                return getActiveConnections(a).size()
                     - getActiveConnections(b).size();

            }

        });
        
        // Return the first unreserved connection
        for (ModeledConnection connection : sortedConnections) {
            if (activeSeats.add(new Seat(username, connection.getIdentifier())))
                return connection;
        }

        // Already in use
        throw new GuacamoleResourceConflictException("Cannot connect. This connection is in use.");

    }

    @Override
    protected void release(AuthenticatedUser user, ModeledConnection connection) {
        activeSeats.remove(new Seat(user.getUser().getIdentifier(), connection.getIdentifier()));
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
