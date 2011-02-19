package net.sourceforge.guacamole.net.tunnel;

/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sourceforge.guacamole.GuacamoleClient;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.GuacamoleSession;


public abstract class GuacamoleTunnelServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        service(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        service(request, response);
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException {

        try {

            String query = request.getQueryString();
            if (query == null)
                throw new GuacamoleException("No query string provided.");

            if (query.equals("connect"))
                doConnect(request, response);

            else if(query.equals("read"))
                doRead(request, response);

            else if(query.equals("write"))
                doWrite(request, response);

            else
                throw new GuacamoleException("Invalid tunnel operation: " + query);
        }
        catch (GuacamoleException e) {
            throw new ServletException(e);
        }

    }

    protected abstract void doConnect(HttpServletRequest request, HttpServletResponse response) throws GuacamoleException;

    protected void doRead(HttpServletRequest request, HttpServletResponse response) throws GuacamoleException {

        HttpSession httpSession = request.getSession(false);
        GuacamoleSession session = new GuacamoleSession(httpSession);

        ReentrantLock instructionStreamLock = session.getInstructionStreamLock();
        instructionStreamLock.lock();

        try {

            // Note that although we are sending text, Webkit browsers will
            // buffer 1024 bytes before starting a normal stream if we use
            // anything but application/octet-stream.
            response.setContentType("application/octet-stream");

            Writer out = response.getWriter();

            try {

                // Query new update from server
                GuacamoleClient client = session.getClient();

                // For all messages, until another stream is ready (we send at least one message)
                char[] message;
                while ((message = client.read()) != null) {

                    // Get message output bytes
                    out.write(message, 0, message.length);
                    out.flush();
                    response.flushBuffer();

                    // No more messages another stream can take over
                    if (instructionStreamLock.hasQueuedThreads())
                        break;

                }

                if (message == null) {
                    session.detachClient();
                    throw new GuacamoleException("Disconnected.");
                }

            }
            catch (GuacamoleException e) {
                out.write("error:" + e.getMessage() + ";");
                out.flush();
                response.flushBuffer();
            }

            // End-of-instructions marker
            out.write(';');
            out.flush();
            response.flushBuffer();

        }
        catch (UnsupportedEncodingException e) {
            throw new GuacamoleException("UTF-8 not supported by Java.", e);
        }
        catch (IOException e) {
            throw new GuacamoleException("I/O error writing to servlet output stream.", e);
        }
        finally {
            instructionStreamLock.unlock();
        }

    }

    protected void doWrite(HttpServletRequest request, HttpServletResponse response) throws GuacamoleException {

        HttpSession httpSession = request.getSession(false);
        GuacamoleSession session = new GuacamoleSession(httpSession);

        // Send data
        try {

            Reader input = request.getReader();
            char[] buffer = new char[8192];

            int length;
            while ((length = input.read(buffer, 0, buffer.length)) != -1)
                session.getClient().write(buffer, 0, length);

        }
        catch (IOException e) {
            throw new GuacamoleException("I/O Error sending data to server: " + e.getMessage(), e);
        }

    }

}

