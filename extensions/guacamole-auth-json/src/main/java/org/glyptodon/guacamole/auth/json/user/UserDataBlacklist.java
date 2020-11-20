/*
 * Copyright (C) 2016 Glyptodon, Inc.
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

package org.glyptodon.guacamole.auth.json.user;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.xml.bind.DatatypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Atomic blacklist of UserData objects, stored by their associated
 * cryptographic signatures. UserData objects stored within this blacklist MUST
 * have an associated expiration timestamp, and will automatically be removed
 * from the blacklist once they have expired.
 *
 * @author Michael Jumper
 */
public class UserDataBlacklist {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(UserDataBlacklist.class);

    /**
     * All blacklisted UserData objects, stored by their associated
     * cryptographic signatures. NOTE: Each key into this map is the hex
     * string produced by encoding the binary signature using DatatypeConverter.
     * A byte[] cannot be used directly.
     */
    private final ConcurrentMap<String, UserData> blacklist =
            new ConcurrentHashMap<String, UserData>();

    /**
     * Removes all expired UserData objects from the blacklist. This will
     * automatically be invoked whenever new UserData is added to the blacklist.
     */
    public void removeExpired() {

        // Remove expired data from blacklist
        Iterator<Map.Entry<String, UserData>> current = blacklist.entrySet().iterator();
        while (current.hasNext()) {

            // Remove entry from map if its associated with expired data
            Map.Entry<String, UserData> entry = current.next();
            if (entry.getValue().isExpired())
                current.remove();
            
        }

    }

    /**
     * Adds the given UserData to the blacklist, storing it according to the
     * provided cryptographic signature. The UserData MUST have an associated
     * expiration timestamp. If any UserData objects already within the
     * blacklist have expired, they will automatically be removed when this
     * function is invoked.
     *
     * @param data
     *     The UserData to store within the blacklist.
     *
     * @param signature
     *     The cryptographic signature associated with the UserData.
     *
     * @return
     *     true if the UserData was not already blacklisted and has
     *     successfully been added, false otherwise.
     */
    public boolean add(UserData data, byte[] signature) {

        // Expiration timestamps must be provided
        if (data.getExpires() == null) {
            logger.warn("An expiration timestamp MUST be provided for "
                    + "single-use data.");
            return false;
        }

        // Remove any expired entries
        removeExpired();

        // Expired user data is implicitly blacklisted
        if (data.isExpired())
            return false;

        // Add to blacklist only if not already present
        String signatureHex = DatatypeConverter.printHexBinary(signature);
        return blacklist.putIfAbsent(signatureHex, data) == null;

    }

}
