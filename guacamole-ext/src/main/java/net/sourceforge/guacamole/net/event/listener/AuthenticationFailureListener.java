package net.sourceforge.guacamole.net.event.listener;

import net.sourceforge.guacamole.net.event.AuthenticationFailureEvent;

/**
 * A listener whose postAuthenticationFailure() hook will fire immediately
 * after a user's authentication attempt fails. Note that there is no
 * preAuthenticationFailure() hook - authentication failure cannot be canceled,
 * it can only be observed after the fact.
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
     */
    public void postAuthenticationFailure(AuthenticationFailureEvent e);
    
}
