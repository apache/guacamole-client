package net.sourceforge.guacamole.net.event.listener;

import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.event.AuthenticationFailureEvent;

/**
 * A listener whose authenticationFailed() hook will fire immediately
 * after a user's authentication attempt fails. Note that this hook cannot
 * be used to cancel the authentication failure.
 * 
 * @author Michael Jumper
 */
public interface AuthenticationFailureListener  {
   
    /**
     * Event hook which fires immediately after a user's authentication attempt
     * fails.
     * 
     * @param e The AuthenticationFailureEvent describing the authentication
     *          failure that just occurred.
     * @throws GuacamoleException If an error occurs while handling the
     *                            authentication failure event. Note that
     *                            throwing an exception will NOT cause the
     *                            authentication failure to be canceled.
     */
    public void authenticationFailed(AuthenticationFailureEvent e)
            throws GuacamoleException;
    
}
