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
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.glyptodon.guacamole.auth.jdbc.user.AuthenticatedUser;
import org.glyptodon.guacamole.auth.jdbc.connection.ModeledConnection;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleResourceConflictException;


/**
 * GuacamoleSocketService implementation which allows only one user per
 * connection at any time, but does not disallow concurrent use. Once
 * connected, a user has effectively reserved that connection, and may
 * continue to concurrently use that connection any number of times. The
 * connection will remain reserved until all associated connections are closed.
 * Other users will be denied access to that connection while it is reserved.
 *
 * @author Michael Jumper
 */
@Singleton
public class ReservedGuacamoleSocketService
    extends AbstractGuacamoleSocketService {

    /**
     * An arbitrary number of reservations associated with a specific user.
     * Initially, each Reservation instance represents exactly one reservation,
     * but future calls to acquire() may increase this value. Once the
     * reservation count is reduced to zero by calls to release(), a
     * Reservation instance is empty and cannot be reused. It must be discarded
     * and replaced with a fresh Reservation.
     * 
     * This is necessary as each Reservation will be stored within a Map, and
     * the effect of acquire() must be deterministic. If Reservations could be
     * reused, the internal count could potentially increase after being
     * removed from the map, resulting in a successful acquire() that really
     * should have failed.
     */
    private static class Reservation {

        /**
         * The username of the user associated with this reservation.
         */
        private final String username;

        /**
         * The number of reservations effectively present under the associated
         * username.
         */
        private int count = 1;

        /**
         * Creates a new reservation which tracks the overall number of
         * reservations for a given user.
         * @param username 
         */
        public Reservation(String username) {
            this.username = username;
        }

        /**
         * Attempts to acquire a new reservation under the given username. If
         * this reservation is for a different user, or the reservation has
         * expired, this will fail.
         *
         * @param username
         *     The username of the user to acquire the reservation for.
         *
         * @return
         *     true if the reservation was successful, false otherwise.
         */
        public boolean acquire(String username) {

            // Acquire always fails if for the wrong user
            if (!this.username.equals(username))
                return false;

            // Determine success/failure based on count
            synchronized (this) {

                // If already expired, no further reservations are allowed
                if (count == 0)
                    return false;

                // Otherwise, add another reservation, report success
                count++;
                return true;
                
            }
            
        }

        /**
         * Releases a previous reservation. The result of calling this function
         * without a previous matching call to acquire is undefined.
         *
         * @return
         *     true if the last reservation has been released and this
         *     reservation is now empty, false otherwise.
         */
        public boolean release() {
            synchronized (this) {

                // Reduce reservation count
                count--;

                // Empty if no reservations remain
                return count == 0;
                
            }
        }
        
    }

    /**
     * Map of connection identifier to associated reservations.
     */
    private final ConcurrentMap<String, Reservation> reservations =
            new ConcurrentHashMap<String, Reservation>();
    
    @Override
    protected ModeledConnection acquire(AuthenticatedUser user,
            List<ModeledConnection> connections) throws GuacamoleException {

        String username = user.getUser().getIdentifier();
        
        // Return the first successfully-reserved connection
        for (ModeledConnection connection : connections) {

            String identifier = connection.getIdentifier();

            // Attempt to reserve connection, return if successful
            Reservation reservation = reservations.putIfAbsent(identifier, new Reservation(username));
            if (reservation == null || reservation.acquire(username))
                return connection;

        }

        // Already in use
        throw new GuacamoleResourceConflictException("Cannot connect. This connection is in use.");

    }

    @Override
    protected void release(AuthenticatedUser user, ModeledConnection connection) {

        String identifier = connection.getIdentifier();
        
        // Retrieve active reservation (which must exist)
        Reservation reservation = reservations.get(identifier);
        assert(reservation != null);

        // Release reservation, remove from map if empty
        if (reservation.release())
            reservations.remove(identifier);
        
    }

}
