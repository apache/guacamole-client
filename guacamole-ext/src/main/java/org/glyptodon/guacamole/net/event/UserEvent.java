package org.glyptodon.guacamole.net.event;

import org.glyptodon.guacamole.net.auth.UserContext;

/**
 * Abstract basis for events which may have an associated UserContext when
 * triggered.
 *
 * @author Michael Jumper
 */
public interface UserEvent {

    /**
     * Returns the current UserContext of the user triggering the event, if any.
     *
     * @return The current UserContext of the user triggering the event, if
     *         any, or null if no UserContext is associated with the event.
     */
    UserContext getUserContext();

}
