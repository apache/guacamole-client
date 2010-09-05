package net.sourceforge.guacamole.net.output;

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

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;
import java.util.concurrent.locks.ReentrantLock;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sourceforge.guacamole.Client;
import net.sourceforge.guacamole.net.GuacamoleServlet;
import net.sourceforge.guacamole.GuacamoleException;
import net.sourceforge.guacamole.net.GuacamoleSession;
import net.sourceforge.guacamole.instruction.Instruction;
import net.sourceforge.guacamole.instruction.ErrorInstruction;


public class InstructionStream extends GuacamoleServlet {

    @Override
    protected void handleRequest(GuacamoleSession session, HttpServletRequest request, HttpServletResponse response) throws GuacamoleException {

        ReentrantLock instructionStreamLock = session.getInstructionStreamLock();
        instructionStreamLock.lock();

        try {

            response.setContentType("text/plain");
            OutputStream out = response.getOutputStream();

            // Compress if enabled and supported by browser
            if (session.getConfiguration().getCompressStream()) {

                String encodingHeader = request.getHeader("Accept-Encoding");
                if (encodingHeader != null) {

                    String[] encodings = encodingHeader.split(",");
                    for (String encoding : encodings) {

                        // Use gzip if supported
                        if (encoding.equals("gzip")) {
                            response.setHeader("Content-Encoding", "gzip");
                            out = new GZIPOutputStream(out);
                            break;
                        }

                        // Use deflate if supported
                        if (encoding.equals("deflate")) {
                            response.setHeader("Content-Encoding", "deflate");
                            out = new DeflaterOutputStream(out);
                            break;
                        }

                    }

                }

            }

            try {

                // Query new update from server
                Client client = session.getClient();

                // For all messages, until another stream is ready (we send at least one message)
                Instruction message = client.nextInstruction(true); // Block until first message is read
                while (message != null) {

                    // Get message output bytes
                    byte[] outputBytes = message.toString().getBytes("UTF-8");
                    out.write(outputBytes);
                    out.flush();
                    response.flushBuffer();

                    // No more messages another stream can take over
                    if (instructionStreamLock.hasQueuedThreads())
                        break;

                    message = client.nextInstruction(false); // Read remaining messages, do not block.
                }

            }
            catch (GuacamoleException e) {
                Instruction message = new ErrorInstruction(e.getMessage());
                byte[] outputBytes = message.toString().getBytes("UTF-8");
                out.write(outputBytes);
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

