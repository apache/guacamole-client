
package net.sourceforge.guacamole.servlet;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-common.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.GuacamoleSecurityException;
import net.sourceforge.guacamole.net.GuacamoleTunnel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides abstract access to the tunnels associated with a Guacamole session.
 *
 * @author Michael Jumper
 */
public class GuacamoleSession {

    private Logger logger = LoggerFactory.getLogger(GuacamoleSession.class);

    private ConcurrentMap<String, GuacamoleTunnel> tunnels;

    /**
     * Creates a new GuacamoleSession, storing and retrieving tunnels from the
     * given HttpSession. Note that the true Guacamole session is tied to the
     * HttpSession provided, thus creating a new GuacamoleSession does not
     * create a new Guacamole session; it merely creates a new object for
     * accessing the tunnels of an existing Guacamole session represented by
     * the provided HttpSession.
     *
     * @param session The HttpSession to use as tunnel storage.
     * @throws GuacamoleException If session is null.
     */
    @SuppressWarnings("unchecked")
    public GuacamoleSession(HttpSession session) throws GuacamoleException {

        if (session == null)
            throw new GuacamoleSecurityException("User has no session.");

        synchronized (session) {

            tunnels = (ConcurrentMap<String, GuacamoleTunnel>) session.getAttribute("GUAC_TUNNELS");
            if (tunnels == null) {
                tunnels = new ConcurrentHashMap<String, GuacamoleTunnel>();
                session.setAttribute("GUAC_TUNNELS", tunnels);
            }

        }

    }

    /**
     * Attaches the given tunnel to this GuacamoleSession.
     * @param tunnel The tunnel to attach to this GucacamoleSession.
     */
    public void attachTunnel(GuacamoleTunnel tunnel) {
        tunnels.put(tunnel.getUUID().toString(), tunnel);
        logger.debug("Attached tunnel {}.", tunnel.getUUID());
    }

    /**
     * Detaches the given tunnel to this GuacamoleSession.
     * @param tunnel The tunnel to detach to this GucacamoleSession.
     */
    public void detachTunnel(GuacamoleTunnel tunnel) {
        tunnels.remove(tunnel.getUUID().toString());
        logger.debug("Detached tunnel {}.", tunnel.getUUID());
    }

    /**
     * Returns the tunnel with the given UUID attached to this GuacamoleSession,
     * if any.
     *
     * @param tunnelUUID The UUID of an attached tunnel.
     * @return The tunnel corresponding to the given UUID, if attached, or null
     *         if no such tunnel is attached.
     */
    public GuacamoleTunnel getTunnel(String tunnelUUID) {
        return tunnels.get(tunnelUUID);
    }

}
