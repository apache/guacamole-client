/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.servlet;

import javax.servlet.http.HttpSession;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides abstract access to the tunnels associated with a Guacamole session.
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
