
package org.glyptodon.guacamole.net.basic.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.glyptodon.guacamole.GuacamoleException;
import org.glyptodon.guacamole.net.GuacamoleResourcePipe;
import org.glyptodon.guacamole.net.GuacamoleTunnel;
import org.glyptodon.guacamole.net.basic.AuthenticatingHttpServlet;
import org.glyptodon.guacamole.protocol.GuacamoleConfiguration;
import org.glyptodon.guacamole.servlet.GuacamoleSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides access to resources received by the proxy server and buffered
 * in the session.
 *
 * @author Michael Jumper
 */
public class BasicResource extends AuthenticatingHttpServlet {

    private Logger logger = LoggerFactory.getLogger(BasicResource.class);

    @Override
    protected void authenticatedService(
            Map<String, GuacamoleConfiguration> configs,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        // If no index, ignore request
        String indexString = request.getParameter("index");
        if (indexString == null) {
            logger.info("Empty resource returned due to lack of index.");
            return;
        }

        // Get index and name
        int index = Integer.parseInt(indexString);
        String name  = request.getParameter("name");
        String tunnelUUID = request.getParameter("uuid");
        
        // Get session
        GuacamoleSession session;
        try {
            HttpSession httpSession = request.getSession(true);
            session = new GuacamoleSession(httpSession);
        }
        catch (GuacamoleException e) {
            throw new ServletException(e);
        }

        // Get tunnel, ensure tunnel exists
        GuacamoleTunnel tunnel = session.getTunnel(tunnelUUID);

        // Acquire lock
        final Lock closeLock = new ReentrantLock();
        closeLock.lock();
        
        // If name provided, provide name in header.
        if (name != null)
            response.setHeader("Content-Disposition", "attachment; filename=" + name);
        
        // Get output stream and pipe
        OutputStream out = response.getOutputStream();
        GuacamoleResourcePipe resourcePipe = tunnel.getResourcePipe(index);

        // Transfer all data from pipe
        try {
            byte[] block;
            while ((block = resourcePipe.read()) != null) {
                out.write(block);
            }
        }
        catch (GuacamoleException e) {
            throw new ServletException(e);
        }
        
    }

}
