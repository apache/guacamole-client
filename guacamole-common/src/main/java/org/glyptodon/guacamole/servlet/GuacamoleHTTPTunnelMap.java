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

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Map-style object which tracks in-use HTTP tunnels, automatically removing
 * and closing tunnels which have not been used recently. This class is
 * intended for use only within the GuacamoleHTTPTunnelServlet implementation,
 * and has no real utility outside that implementation.
 *
 * @author Michael Jumper
 */
class GuacamoleHTTPTunnelMap {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(GuacamoleHTTPTunnelMap.class);

    /**
     * The number of seconds to wait between tunnel accesses before timing out
     * Note that this will be enforced only within a factor of 2. If a tunnel
     * is unused, it will take between TUNNEL_TIMEOUT and TUNNEL_TIMEOUT*2
     * seconds before that tunnel is closed and removed.
     */
    private static final int TUNNEL_TIMEOUT = 15;

    /**
     * Executor service which runs the periodic tunnel timeout task.
     */
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    /**
     * Map of all tunnels that are using HTTP, indexed by tunnel UUID.
     */
    private final ConcurrentMap<String, GuacamoleHTTPTunnel> tunnelMap =
            new ConcurrentHashMap<String, GuacamoleHTTPTunnel>();

    /**
     * Creates a new GuacamoleHTTPTunnelMap which automatically closes and
     * removes HTTP tunnels which are no longer in use.
     */
    public GuacamoleHTTPTunnelMap() {

        // Check for unused tunnels every few seconds
        executor.scheduleAtFixedRate(
            new TunnelTimeoutTask(TUNNEL_TIMEOUT * 1000l),
            TUNNEL_TIMEOUT, TUNNEL_TIMEOUT, TimeUnit.SECONDS);

    }

    /**
     * Task which iterates through all registered tunnels, removing and those
     * tunnels which have not been accessed for a given number of milliseconds.
     */
    private class TunnelTimeoutTask implements Runnable {

        /**
         * The maximum amount of time to allow between accesses to any one
         * HTTP tunnel, in milliseconds.
         */
        private final long tunnelTimeout;

        /**
         * Creates a new task which automatically closes and removes tunnels
         * which have not been accessed for at least the given number of
         * milliseconds.
         *
         * @param tunnelTimeout
         *     The maximum amount of time to allow between separate tunnel
         *     read/write requests, in milliseconds.
         */
        public TunnelTimeoutTask(long tunnelTimeout) {
            this.tunnelTimeout = tunnelTimeout;
        }

        @Override
        public void run() {

            // Get current time
            long now = System.currentTimeMillis();

            // For each tunnel, close and remove any tunnels which have expired
            Iterator<Map.Entry<String, GuacamoleHTTPTunnel>> entries = tunnelMap.entrySet().iterator();
            while (entries.hasNext()) {

                Map.Entry<String, GuacamoleHTTPTunnel> entry = entries.next();
                GuacamoleHTTPTunnel tunnel = entry.getValue();

                // Get elapsed time since last access
                long age = now - tunnel.getLastAccessedTime();

                // If tunnel is too old, close and remove it
                if (age >= tunnelTimeout) {

                    // Remove old entry
                    logger.debug("HTTP tunnel \"{}\" has timed out.", entry.getKey());
                    entries.remove();

                    // Attempt to close tunnel
                    try {
                        tunnel.close();
                    }
                    catch (GuacamoleException e) {
                        logger.debug("Unable to close expired HTTP tunnel.", e);
                    }

                }

            } // end for each tunnel

        } // end timeout task run()

    }

    /**
     * Returns the GuacamoleTunnel having the given UUID, wrapped within a
     * GuacamoleHTTPTunnel. If the no tunnel having the given UUID is
     * available, null is returned.
     *
     * @param uuid
     *     The UUID of the tunnel to retrieve.
     *
     * @return
     *     The GuacamoleTunnel having the given UUID, wrapped within a
     *     GuacamoleHTTPTunnel, if such a tunnel exists, or null if there is no
     *     such tunnel.
     */
    public GuacamoleHTTPTunnel get(String uuid) {

        // Update the last access time
        GuacamoleHTTPTunnel tunnel = tunnelMap.get(uuid);
        if (tunnel != null)
            tunnel.access();

        // Return tunnel, if any
        return tunnel;

    }

    /**
     * Registers that a new connection has been established using HTTP via the
     * given GuacamoleTunnel.
     *
     * @param uuid
     *     The UUID of the tunnel being added (registered).
     *
     * @param tunnel
     *     The GuacamoleTunnel being registered, its associated connection
     *     having just been established via HTTP.
     */
    public void put(String uuid, GuacamoleTunnel tunnel) {
        tunnelMap.put(uuid, new GuacamoleHTTPTunnel(tunnel));
    }

    /**
     * Removes the GuacamoleTunnel having the given UUID, if such a tunnel
     * exists. The original tunnel is returned wrapped within a
     * GuacamoleHTTPTunnel.
     *
     * @param uuid
     *     The UUID of the tunnel to remove (deregister).
     *
     * @return
     *     The GuacamoleTunnel having the given UUID, if such a tunnel exists,
     *     wrapped within a GuacamoleHTTPTunnel, or null if no such tunnel
     *     exists and no removal was performed.
     */
    public GuacamoleHTTPTunnel remove(String uuid) {
        return tunnelMap.remove(uuid);
    }

    /**
     * Shuts down this tunnel map, disallowing future tunnels from being
     * registered and reclaiming any resources.
     */
    public void shutdown() {
        executor.shutdownNow();
    }

}
