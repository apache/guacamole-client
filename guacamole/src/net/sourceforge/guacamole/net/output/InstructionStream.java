package net.sourceforge.guacamole.net.output;

/*
 *  Guacamole - Pure JavaScript/HTML VNC Client
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

        // Instruction buffer
        StringBuilder instructions = new StringBuilder();

        try {

            // Query new update from VNC server
            Client client = session.getClient();

            int messageLimit = Integer.parseInt(request.getParameter("messageLimit"));

            // For all messages, up to given message limit.
            Instruction message = client.nextInstruction(true); // Block until first message is read
            while (message != null) {

                // Add message
                instructions.append(message.toString());

                // No more messages if we're exceeding our limit
                if (instructions.length() >= messageLimit) break;

                message = client.nextInstruction(false); // Read remaining messages, do not block.
            }

        }
        catch (GuacamoleException e) {
            instructions.append(new ErrorInstruction(e.getMessage()).toString());
        }

        try {

            // Get output bytes
            byte[] outputBytes = instructions.toString().getBytes("UTF-8");

            // Compress if enabled and supported by browser
            if (session.getConfiguration().getCompressStream()) {

                String encodingHeader = request.getHeader("Accept-Encoding");
                if (encodingHeader != null) {

                    String[] encodings = encodingHeader.split(",");
                    for (String encoding : encodings) {

                        // Use gzip if supported
                        if (encoding.equals("gzip")) {
                            response.setHeader("Content-Encoding", "gzip");
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            OutputStream zout = new GZIPOutputStream(bos);
                            zout.write(outputBytes);
                            zout.close();
                            outputBytes = bos.toByteArray();
                            break;
                        }

                        // Use deflate if supported
                        if (encoding.equals("deflate")) {
                            response.setHeader("Content-Encoding", "deflate");
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            OutputStream zout = new DeflaterOutputStream(bos);
                            zout.write(outputBytes);
                            zout.close();
                            outputBytes = bos.toByteArray();
                            break;
                        }

                    }

                }

            }

            response.setContentType("text/plain");
            response.setContentLength(outputBytes.length);

            // Use default output stream if no compression.
            OutputStream out = response.getOutputStream();
            out.write(outputBytes);
            out.flush();
        }
        catch (UnsupportedEncodingException e) {
            throw new GuacamoleException("UTF-8 not supported by Java.", e);
        }
        catch (IOException e) {
            throw new GuacamoleException("I/O error writing to servlet output stream.", e);
        }

    }

}

