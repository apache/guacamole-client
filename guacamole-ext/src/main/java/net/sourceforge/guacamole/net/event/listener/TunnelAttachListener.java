package net.sourceforge.guacamole.net.event.listener;

import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.event.TunnelConnectEvent;

/**
 * A listener whose tunnelAttached() hook will fire immediately after a new
 * tunnel is attached to a session.
 * 
 * @author Michael Jumper
 */
public interface TunnelAttachListener {

   /**
     * Event hook which fires immediately after a new tunnel is attached to a
     * session. The return value of this hook dictates whether the tunnel is
     * allowed to be attached.
     * 
     * @param e The TunnelConnectEvent describing the tunnel being attached and
     *          any associated credentials.
     * @return true if the tunnel should be allowed to be attached, or false
     *         if the attempt should be denied, causing the attempt to
     *         effectively fail.
     * @throws GuacamoleException If an error occurs while handling the
     *                            tunnel attach event. Throwing an exception
     *                            will also stop the tunnel from being attached.
     */
    public boolean tunnelAttached(TunnelConnectEvent e)
            throws GuacamoleException;

}
