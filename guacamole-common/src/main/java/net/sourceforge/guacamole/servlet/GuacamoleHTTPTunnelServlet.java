package net.sourceforge.guacamole.servlet;

/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-common.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

import net.sourceforge.guacamole.net.GuacamoleTunnel;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.*;
import net.sourceforge.guacamole.io.GuacamoleReader;
import net.sourceforge.guacamole.io.GuacamoleWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A HttpServlet implementing and abstracting the operations required by the
 * HTTP implementation of the JavaScript Guacamole client's tunnel.
 *
 * @author Michael Jumper
 */
public abstract class GuacamoleHTTPTunnelServlet extends HttpServlet {

    private Logger logger = LoggerFactory.getLogger(GuacamoleHTTPTunnelServlet.class);
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        handleTunnelRequest(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        handleTunnelRequest(request, response);
    }

    /**
     * Sends an error on the given HTTP response with the given integer error
     * code.
     * 
     * @param response The HTTP response to use to send the error.
     * @param code The HTTP status code of the error.
     * @throws ServletException If an error prevents sending of the error
     *                          code.
     */
    private void sendError(HttpServletResponse response, int code) throws ServletException {

        try {

            // If response not committed, send error code
            if (!response.isCommitted())
                response.sendError(code);

        }
        catch (IOException ioe) {

            // If unable to send error at all due to I/O problems,
            // rethrow as servlet exception
            throw new ServletException(ioe);

        }

    }


    
    /**
     * Dispatches every HTTP GET and POST request to the appropriate handler
     * function based on the query string.
     *
     * @param request The HttpServletRequest associated with the GET or POST
     *                request received.
     * @param response The HttpServletResponse associated with the GET or POST
     *                 request received.
     * @throws ServletException If an error occurs while servicing the request.
     */
    protected void handleTunnelRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException {

        try {

            String query = request.getQueryString();
            if (query == null)
                throw new GuacamoleClientException("No query string provided.");

            // If connect operation, call doConnect() and return tunnel UUID
            // in response.
            if (query.equals("connect")) {

                GuacamoleTunnel tunnel = doConnect(request);
                if (tunnel != null) {

                    // Get session
                    HttpSession httpSession = request.getSession(true);
                    GuacamoleSession session = new GuacamoleSession(httpSession);

                    // Attach tunnel to session
                    session.attachTunnel(tunnel);

                    logger.info("Connection from {} succeeded.", request.getRemoteAddr());
                
                    try {
                        // Send UUID to client
                        response.getWriter().print(tunnel.getUUID().toString());
                    }
                    catch (IOException e) {
                        throw new GuacamoleServerException(e);
                    }
                    
                }

                // Failed to connect
                else {
                    logger.info("Connection from {} failed.", request.getRemoteAddr());
                    throw new GuacamoleResourceNotFoundException("No tunnel created.");
                }

            }

            // If read operation, call doRead() with tunnel UUID
            else if(query.startsWith("read:"))
                doRead(request, response, query.substring(5));

            // If write operation, call doWrite() with tunnel UUID
            else if(query.startsWith("write:"))
                doWrite(request, response, query.substring(6));

            // Otherwise, invalid operation
            else
                throw new GuacamoleClientException("Invalid tunnel operation: " + query);
        }

        // Catch any thrown guacamole exception and attempt to pass within the
        // HTTP response, logging each error appropriately.
        catch (GuacamoleSecurityException e) {
            logger.warn("Authorization failed.", e);
            sendError(response, HttpServletResponse.SC_FORBIDDEN);
        }
        catch (GuacamoleResourceNotFoundException e) {
            logger.debug("Resource not found.", e);
            sendError(response, HttpServletResponse.SC_NOT_FOUND);
        }
        catch (GuacamoleClientException e) {
            logger.warn("Error in client request.", e);
            sendError(response, HttpServletResponse.SC_BAD_REQUEST);
        }
        catch (GuacamoleException e) {
            logger.error("Server error in tunnel", e);
            sendError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

    }

    /**
     * Called whenever the JavaScript Guacamole client makes a connection
     * request. It it up to the implementor of this function to define what
     * conditions must be met for a tunnel to be configured and returned as a
     * result of this connection request (whether some sort of credentials must
     * be specified, for example).
     *
     * @param request The HttpServletRequest associated with the connection
     *                request received. Any parameters specified along with
     *                the connection request can be read from this object.
     * @return A newly constructed GuacamoleTunnel if successful,
     *         null otherwise.
     * @throws GuacamoleException If an error occurs while constructing the
     *                            GuacamoleTunnel, or if the conditions
     *                            required for connection are not met.
     */
    protected abstract GuacamoleTunnel doConnect(HttpServletRequest request) throws GuacamoleException;

    /**
     * Called whenever the JavaScript Guacamole client makes a read request.
     * This function should in general not be overridden, as it already
     * contains a proper implementation of the read operation.
     *
     * @param request The HttpServletRequest associated with the read request
     *                received.
     * @param response The HttpServletResponse associated with the write request
     *                 received. Any data to be sent to the client in response
     *                 to the write request should be written to the response
     *                 body of this HttpServletResponse.
     * @param tunnelUUID The UUID of the tunnel to read from, as specified in
     *                   the write request. This tunnel must be attached to
     *                   the Guacamole session.
     * @throws GuacamoleException If an error occurs while handling the read
     *                            request.
     */
    protected void doRead(HttpServletRequest request, HttpServletResponse response, String tunnelUUID) throws GuacamoleException {

        HttpSession httpSession = request.getSession(false);
        GuacamoleSession session = new GuacamoleSession(httpSession);

        // Get tunnel, ensure tunnel exists
        GuacamoleTunnel tunnel = session.getTunnel(tunnelUUID);
        if (tunnel == null)
            throw new GuacamoleResourceNotFoundException("No such tunnel.");

        // Ensure tunnel is open
        if (!tunnel.isOpen())
            throw new GuacamoleResourceNotFoundException("Tunnel is closed.");
        
        // Obtain exclusive read access
        GuacamoleReader reader = tunnel.acquireReader();

        try {

            // Note that although we are sending text, Webkit browsers will
            // buffer 1024 bytes before starting a normal stream if we use
            // anything but application/octet-stream.
            response.setContentType("application/octet-stream");

            Writer out = response.getWriter();

            // Detach tunnel and throw error if EOF (and we haven't sent any
            // data yet.
            char[] message = reader.read();
            if (message == null)
                throw new GuacamoleResourceNotFoundException("Tunnel reached end of stream.");

            // For all messages, until another stream is ready (we send at least one message)
            do {

                // Get message output bytes
                out.write(message, 0, message.length);

                // Flush if we expect to wait
                if (!reader.available()) {
                    out.flush();
                    response.flushBuffer();
                }

                // No more messages another stream can take over
                if (tunnel.hasQueuedReaderThreads())
                    break;

            } while (tunnel.isOpen() && (message = reader.read()) != null);

            // Close tunnel immediately upon EOF
            if (message == null)
                tunnel.close();
            
            // End-of-instructions marker
            out.write("0.;");
            out.flush();
            response.flushBuffer();

        }
        catch (GuacamoleException e) {

            // Detach and close
            session.detachTunnel(tunnel);
            tunnel.close();

            throw e;
        }
        catch (IOException e) {

            // Log typically frequent I/O error if desired
            logger.debug("Error writing to servlet output stream", e);
            
            // Detach and close
            session.detachTunnel(tunnel);
            tunnel.close();
                
        }
        finally {
            tunnel.releaseReader();
        }

    }

    /**
     * Called whenever the JavaScript Guacamole client makes a write request.
     * This function should in general not be overridden, as it already
     * contains a proper implementation of the write operation.
     *
     * @param request The HttpServletRequest associated with the write request
     *                received. Any data to be written will be specified within
     *                the body of this request.
     * @param response The HttpServletResponse associated with the write request
     *                 received.
     * @param tunnelUUID The UUID of the tunnel to write to, as specified in
     *                   the write request. This tunnel must be attached to
     *                   the Guacamole session.
     * @throws GuacamoleException If an error occurs while handling the write
     *                            request.
     */
    protected void doWrite(HttpServletRequest request, HttpServletResponse response, String tunnelUUID) throws GuacamoleException {

        HttpSession httpSession = request.getSession(false);
        GuacamoleSession session = new GuacamoleSession(httpSession);

        GuacamoleTunnel tunnel = session.getTunnel(tunnelUUID);
        if (tunnel == null)
            throw new GuacamoleResourceNotFoundException("No such tunnel.");

        // We still need to set the content type to avoid the default of
        // text/html, as such a content type would cause some browsers to
        // attempt to parse the result, even though the JavaScript client
        // does not explicitly request such parsing.
        response.setContentType("application/octet-stream");
        response.setContentLength(0);

        // Send data
        try {

            GuacamoleWriter writer = tunnel.acquireWriter();

            Reader input = request.getReader();
            char[] buffer = new char[8192];

            int length;
            while (tunnel.isOpen() && 
                    (length = input.read(buffer, 0, buffer.length)) != -1)
                writer.write(buffer, 0, length);

        }
        catch (IOException e) {

            // Detach and close
            session.detachTunnel(tunnel);
            tunnel.close();

            throw new GuacamoleServerException("I/O Error sending data to server: " + e.getMessage(), e);
        }
        finally {
            tunnel.releaseWriter();
        }

    }

}

/**
 * \example ExampleTunnelServlet.java
 *
 * A basic example demonstrating extending GuacamoleTunnelServlet and
 * implementing doConnect() to configure the Guacamole connection as
 * desired.
 */

