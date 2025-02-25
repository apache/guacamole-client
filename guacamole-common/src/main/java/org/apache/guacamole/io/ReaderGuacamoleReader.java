/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.guacamole.io;


import java.io.IOException;
import java.io.Reader;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import org.apache.guacamole.GuacamoleConnectionClosedException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.GuacamoleUpstreamTimeoutException;
import org.apache.guacamole.protocol.GuacamoleInstruction;
import org.apache.guacamole.protocol.GuacamoleParser;

/**
 * A GuacamoleReader which wraps a standard Java Reader, using that Reader as
 * the Guacamole instruction stream.
 */
public class ReaderGuacamoleReader implements GuacamoleReader {

    /**
     * The GuacamoleParser instance for parsing instructions.
     */
    private GuacamoleParser parser = new GuacamoleParser();

    /**
     * Wrapped Reader to be used for all input.
     */
    private Reader input;

    /**
     * Creates a new ReaderGuacamoleReader which will use the given Reader as
     * the Guacamole instruction stream.
     *
     * @param input The Reader to use as the Guacamole instruction stream.
     */
    public ReaderGuacamoleReader(Reader input) {
        this.input = input;
    }

    /**
     * The location within the received data buffer that parsing should begin
     * when more data is read.
     */
    private int parseStart = 0;

    /**
     * The buffer holding all received data.
     */
    private char[] buffer = new char[20480];

    /**
     * The number of characters currently used within the data buffer. All
     * other characters within the buffer are free space available for
     * future reads.
     */
    private int usedLength = 0;

    @Override
    public boolean available() throws GuacamoleException {
        try {
            return input.ready() || usedLength > parseStart || parser.hasNext();
        }
        catch (IOException e) {
            throw new GuacamoleServerException(e);
        }
    }

    @Override
    public char[] read() throws GuacamoleException {
        GuacamoleInstruction instruction = readInstruction();
        if (instruction == null)
            return null;

        return instruction.toCharArray();
    }

    @Override
    public GuacamoleInstruction readInstruction() throws GuacamoleException {
        try {
            // Loop until the parser has prepared a full instruction
            while (!parser.hasNext()) {

                // Parse as much data from the buffer as we can
                int parsed = 0;
                while (parseStart < usedLength && (parsed = parser.append(buffer, parseStart, usedLength - parseStart)) != 0) {
                    parseStart += parsed;
                }

                // If we still don't have a full instruction attempt to read more data into the buffer
                if (!parser.hasNext()) {

                    // If we have already parsed some of the buffer and the buffer is almost full then we can trim the parsed data off the buffer
                    if (parseStart > 0 && buffer.length - usedLength < GuacamoleParser.INSTRUCTION_MAX_LENGTH) {
                        System.arraycopy(buffer, parseStart, buffer, 0, usedLength - parseStart);
                        usedLength -= parseStart;
                        parseStart = 0;
                    }

                    // Read more instruction data into the buffer
                    int numRead = input.read(buffer, usedLength, buffer.length - usedLength);
                    if (numRead == -1)
                        break;

                    usedLength += numRead;

                }
 
            }

            return parser.next();

        }
        catch (SocketTimeoutException e) {
            throw new GuacamoleUpstreamTimeoutException("Connection to guacd timed out.", e);
        }
        catch (SocketException e) {
            throw new GuacamoleConnectionClosedException("Connection to guacd is closed.", e);
        }
        catch (IOException e) {
            throw new GuacamoleServerException(e);
        }

    }

}
