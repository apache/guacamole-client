/*
 * Copyright (C) 2013 Glyptodon LLC
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

package org.glyptodon.guacamole.net.basic.rest.clipboard;

import com.google.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.GuacamoleUnsupportedException;
import org.glyptodon.guacamole.environment.Environment;
import org.glyptodon.guacamole.net.basic.ClipboardState;
import org.glyptodon.guacamole.net.basic.GuacamoleSession;
import org.glyptodon.guacamole.net.basic.rest.AuthProviderRESTExposure;
import org.glyptodon.guacamole.net.basic.rest.auth.AuthenticationService;
import org.glyptodon.guacamole.properties.BooleanGuacamoleProperty;

/**
 * A REST service for reading the current contents of the clipboard.
 *
 * @author Michael Jumper
 */
@Path("/clipboard")
public class ClipboardRESTService {

    /**
     * The Guacamole server environment.
     */
    @Inject
    private Environment environment;
    
    /**
     * A service for authenticating users from auth tokens.
     */
    @Inject
    private AuthenticationService authenticationService;
    
    /**
     * The amount of time to wait for clipboard changes, in milliseconds.
     */
    private static final int CLIPBOARD_TIMEOUT = 250;

    /**
     * Whether clipboard integration is enabled.
     */
    public static final BooleanGuacamoleProperty INTEGRATION_ENABLED = new BooleanGuacamoleProperty() {

        @Override
        public String getName() { return "enable-clipboard-integration"; }

    };

    @GET
    @AuthProviderRESTExposure
    public Response getClipboard(@QueryParam("token") String authToken) 
    throws GuacamoleException {

        // Only bother if actually enabled
        if (environment.getProperty(INTEGRATION_ENABLED, false)) {
        
            // Get clipboard
            GuacamoleSession session = authenticationService.getGuacamoleSession(authToken);
            final ClipboardState clipboard = session.getClipboardState();

            // Send clipboard contents
            synchronized (clipboard) {
                clipboard.waitForContents(CLIPBOARD_TIMEOUT);
                return Response.ok(clipboard.getContents(),
                                   clipboard.getMimetype()).build();
            }

        }

        // Otherwise, inform not supported
        else
            throw new GuacamoleUnsupportedException("Clipboard integration not supported");

    }

}
