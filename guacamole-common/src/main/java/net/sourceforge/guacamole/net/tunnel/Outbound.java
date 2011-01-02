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

import java.io.Writer;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.guacamole.Client;
import net.sourceforge.guacamole.net.GuacamoleServlet;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.GuacamoleSession;


public class Outbound extends GuacamoleServlet {

    @Override
    protected void handleRequest(GuacamoleSession session, HttpServletRequest request, HttpServletResponse response) throws GuacamoleException {

        ReentrantLock instructionStreamLock = session.getInstructionStreamLock();
        instructionStreamLock.lock();

        try {

            response.setContentType("text/plain");
            Writer out = response.getWriter();

            try {

                // Query new update from server
                Client client = session.getClient();

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
                    session.disconnect();
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

}

