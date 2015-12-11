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

package org.glyptodon.guacamole.servlet;

import javax.servlet.http.HttpSession;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides abstract access to the tunnels associated with a Guacamole session.
 *
 * @author Michael Jumper
 */
@Deprecated
public class GuacamoleSession {

    /**
     * Logger for this class.
     */
    private final Logger logger = LoggerFactory.getLogger(GuacamoleSession.class);

    /**
     * Creates a new GuacamoleSession. In prior versions of Guacamole, the
     * GuacamoleSession object stored the tunnels associated with a particular
     * user's use of the HTTP tunnel. The HTTP tunnel now stores all of these
     * tunnels itself, and thus this class is no longer necessary. Its use will
     * result in a warning being logged, and its functions will have no effect.
     *
     * @param session
     *     The HttpSession that older versions of Guacamole would use as tunnel
     *     storage. This parameter is now ignored, and the GuacamoleSession
     *     class overall is deprecated.
     */
    public GuacamoleSession(HttpSession session) {
        logger.warn("GuacamoleSession is deprecated. It is no longer "
                  + "necessary and its use will have no effect.");
    }

    /**
     * Attaches the given tunnel to this GuacamoleSession. The GuacamoleSession
     * class is now deprecated, and this function has no effect.
     *
     * @param tunnel
     *     The tunnel to attach to this GucacamoleSession.
     */
    public void attachTunnel(GuacamoleTunnel tunnel) {
        // Deprecated - no effect
    }

    /**
     * Detaches the given tunnel to this GuacamoleSession. The GuacamoleSession
     * class is now deprecated, and this function has no effect.
     *
     * @param tunnel
     *     The tunnel to detach to this GucacamoleSession.
     */
    public void detachTunnel(GuacamoleTunnel tunnel) {
        // Deprecated - no effect
    }

    /**
     * Returns the tunnel with the given UUID attached to this GuacamoleSession,
     * if any. The GuacamoleSession class is now deprecated, and this function
     * has no effect. It will ALWAYS return null.
     *
     * @param tunnelUUID
     *     The UUID of an attached tunnel.
     *
     * @return
     *     The tunnel corresponding to the given UUID, if attached, or null if
     *     if no such tunnel is attached.
     */
    public GuacamoleTunnel getTunnel(String tunnelUUID) {

        // Deprecated - no effect
        return null;

    }

}
