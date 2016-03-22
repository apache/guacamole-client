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

package org.apache.guacamole.auth.jdbc.tunnel;

/**
 * A unique pairing of user and connection or connection group.
 * 
 * @author Michael Jumper
 */
public class Seat {

    /**
     * The user using this seat.
     */
    private final String username;

    /**
     * The connection or connection group associated with this seat.
     */
    private final String identifier;

    /**
     * Creates a new seat which associated the given user with the given
     * connection or connection group.
     *
     * @param username
     *     The username of the user using this seat.
     *
     * @param identifier
     *     The identifier of the connection or connection group associated with
     *     this seat.
     */
    public Seat(String username, String identifier) {
        this.username = username;
        this.identifier = identifier;
    }

    @Override
    public int hashCode() {

        // The various properties will never be null
        assert(username != null);
        assert(identifier != null);

        // Derive hashcode from username and connection identifier
        int hash = 5;
        hash = 37 * hash + username.hashCode();
        hash = 37 * hash + identifier.hashCode();
        return hash;

    }

    @Override
    public boolean equals(Object object) {

        // We are only comparing against other seats here
        assert(object instanceof Seat);
        Seat seat = (Seat) object;

        // The various properties will never be null
        assert(seat.username != null);
        assert(seat.identifier != null);

        return username.equals(seat.username)
            && identifier.equals(seat.identifier);

    }
  
}
