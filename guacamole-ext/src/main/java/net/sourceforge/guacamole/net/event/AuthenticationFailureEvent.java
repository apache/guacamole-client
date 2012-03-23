package net.sourceforge.guacamole.net.event;

import net.sourceforge.guacamole.net.auth.Credentials;

/**
 * An event which is triggered whenever a user's credentials fail to be
 * authenticated. The credentials that failed to be authenticated are included
 * within this event, and can be retrieved using getCredentials().
 * 
 * @author Michael Jumper
 */
public class AuthenticationFailureEvent implements CredentialEvent {
    
    /**
     * The credentials which failed authentication
     */
    private Credentials credentials;

    /**
     * Creates a new AuthenticationFailureEvent which represents the failure
     * to authenticate the given credentials.
     * 
     * @param credentials The credentials which failed authentication.
     */
    public AuthenticationFailureEvent(Credentials credentials) {
        this.credentials = credentials;
    }
   
    @Override
    public Credentials getCredentials() {
        return credentials;
    }
    
}
