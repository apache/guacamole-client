package net.sourceforge.guacamole.net.event;

import net.sourceforge.guacamole.net.auth.Credentials;

/**
 * Abstract basis for events which may have associated user credentials when
 * triggered.
 * 
 * @author Michael Jumper
 */
public interface CredentialEvent {
   
    /**
     * Returns the current credentials of the user triggering the event, if any.
     * 
     * @return The current credentials of the user triggering the event, if
     *         any, or null if no credentials are associated with the event.
     */
    public Credentials getCredentials();

}
