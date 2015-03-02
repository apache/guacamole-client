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
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.jdbc.connection.ModeledConnection;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleResourceConflictException;


/**
 * GuacamoleSocketService implementation which restricts concurrency only on a
 * per-user basis. Each connection may be used concurrently any number of
 * times, but each concurrent use must be associated with a different user.
 *
 * @author Michael Jumper
 */
@Singleton
public class MultiseatGuacamoleSocketService
    extends AbstractGuacamoleSocketService {

    /**
     * A unique pairing of user and connection.
     */
    private static class Seat {

        /**
         * The user using this seat.
         */
        private final String username;

        /**
         * The connection associated with this seat.
         */
        private final String connectionIdentifier;

        /**
         * Creates a new seat which associated the given user with the given
         * connection.
         *
         * @param username
         *     The username of the user using this seat.
         *
         * @param connectionIdentifier
         *     The identifier of the connection associated with this seat.
         */
        public Seat(String username, String connectionIdentifier) {
            this.username = username;
            this.connectionIdentifier = connectionIdentifier;
        }

        @Override
        public int hashCode() {

            // The various properties will never be null
            assert(username != null);
            assert(connectionIdentifier != null);

            // Derive hashcode from username and connection identifier
            int hash = 5;
            hash = 37 * hash + username.hashCode();
            hash = 37 * hash + connectionIdentifier.hashCode();
            return hash;

        }

        @Override
        public boolean equals(Object object) {

            // We are only comparing against other seats here
            assert(object instanceof Seat);
            Seat seat = (Seat) object;

            // The various properties will never be null
            assert(seat.username != null);
            assert(seat.connectionIdentifier != null);

            return username.equals(seat.username)
                && connectionIdentifier.equals(seat.connectionIdentifier);

        }

    }
    
    /**
     * The set of all active user/connection pairs.
     */
    private final Set<Seat> activeSeats =
            Collections.newSetFromMap(new ConcurrentHashMap<Seat, Boolean>());
   
    @Override
    protected ModeledConnection acquire(AuthenticatedUser user,
            List<ModeledConnection> connections) throws GuacamoleException {

        String username = user.getUser().getIdentifier();
        
        // Return the first unreserved connection
        for (ModeledConnection connection : connections) {
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

}
