package net.sourceforge.guacamole.net.event.listener;

import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.event.AuthenticationSuccessEvent;

/**
 * A listener whose hooks will fire immediately before and after a user's
 * authentication attempt succeeds. If a user successfully authenticates,
 * the authenticationSucceeded() hook has the opportunity to cancel the
 * authentication and force it to fail.
 * 
 * @author Michael Jumper
 */
public interface AuthenticationSuccessListener {
    
    /**
     * Event hook which fires immediately after a user's authentication attempt
     * succeeds. The return value of this hook dictates whether the
     * successful authentication attempt is canceled.
     * 
     * @param e The AuthenticationFailureEvent describing the authentication
     *          failure that just occurred.
     * @return true if the successful authentication attempt should be
     *         allowed, or false if the attempt should be denied, causing
     *         the attempt to effectively fail.
     * @throws GuacamoleException If an error occurs while handling the
     *                            authentication success event. Throwing an
     *                            exception will also cancel the authentication
     *                            success.
     */
    public boolean authenticationSucceeded(AuthenticationSuccessEvent e)
            throws GuacamoleException;

}
