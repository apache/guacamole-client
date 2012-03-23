package net.sourceforge.guacamole.net.event.listener;

import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.event.TunnelDetachEvent;

/**
 * A listener whose tunnelDetached() hook will fire immediately after an
 * existing tunnel is detached from a session.
 * 
 * @author Michael Jumper
 */
public interface TunnelDetachListener {
   
    /**
     * Event hook which fires immediately after an existing tunnel is detached
     * from a session. The return value of this hook dictates whether the
     * tunnel is allowed to be detached.
     * 
     * @param e The TunnelDetachEvent describing the tunnel being detached and
     *          any associated credentials.
     * @return true if the tunnel should be allowed to be detached, or false
     *         if the attempt should be denied, causing the attempt to
     *         effectively fail.
     * @throws GuacamoleException If an error occurs while handling the
     *                            tunnel detach event. Throwing an exception
     *                            will also stop the tunnel from being detached.
     */
    public boolean tunnelDetached(TunnelDetachEvent e)
            throws GuacamoleException;

}
