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

package org.apache.guacamole.protocol;

import java.util.List;
import org.apache.guacamole.GuacamoleConnectionClosedException;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.GuacamoleServerException;
import org.apache.guacamole.io.GuacamoleReader;
import org.apache.guacamole.io.GuacamoleWriter;
import org.apache.guacamole.net.GuacamoleSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GuacamoleSocket implementation which transparently switches between an
 * underlying queue of available sockets when errors are encountered during
 * the initial part of a connection (prior to first "sync").
 */
public class FailoverGuacamoleSocket implements GuacamoleSocket {

    /**
     * Logger for this class.
     */
    private Logger logger = LoggerFactory.getLogger(FailoverGuacamoleSocket.class);

    /**
     * A queue of available sockets. The full set of available sockets need not
     * be known ahead of time.
     */
    public static interface SocketQueue {

        /**
         * Returns the next available socket in the queue, or null if no
         * further sockets are available. This function will be invoked when an
         * error occurs within the current socket, and will be invoked again if
         * a GuacamoleException is thrown. Such repeated calls to this function
         * will occur until errors cease or null is returned.
         *
         * @return
         *     The next available socket in the queue, or null if no further
         *     sockets are available.
         *
         * @throws GuacamoleException
         *     If an error occurs preventing the next available socket from
         *     being used.
         */
        GuacamoleSocket nextSocket() throws GuacamoleException;

    }

    /**
     * The queue of available sockets provided when this FailoverGuacamoleSocket
     * was created.
     */
    private final SocketQueue sockets;

    /**
     * The current socket being used, or null if no socket is available.
     */
    private GuacamoleSocket socket;

    /**
     * Creates a new FailoverGuacamoleSocket which pulls its sockets from the
     * given SocketQueue. If an error occurs during the Guacamole connection,
     * other sockets from the SocketQueue are tried, in order, until no error
     * occurs.
     *
     * @param sockets
     *     A SocketQueue which returns the sockets which should be used, in
     *     order.
     *
     * @throws GuacamoleException
     *     If errors prevent use of the sockets defined by the SocketQueue, and
     *     no further sockets remain to be tried.
     */
    public FailoverGuacamoleSocket(SocketQueue sockets)
            throws GuacamoleException {
        this.sockets = sockets;
        selectNextSocket();
    }

    private GuacamoleException tryNextSocket() {

        try {
            if (socket != null)
                socket.close();
        }
        catch (GuacamoleException e) {
            if (socket.isOpen())
                logger.debug("Previous failed socket could not be closed.", e);
        }

        try {

            socket = sockets.nextSocket();
            if (socket == null)
                return new GuacamoleServerException("No remaining sockets to try.");

        }

        catch (GuacamoleException e) {
            if (tryNextSocket() != null)
                return e;
        }

        return null;

    }

    private void selectNextSocket() throws GuacamoleException {
        synchronized (sockets) {
            GuacamoleException failure = tryNextSocket();
            if (failure != null)
                throw failure;
        }
    }

    private class ErrorFilter implements GuacamoleFilter {

        /**
         * Whether a "sync" instruction has been read.
         */
        private boolean receivedSync = false;

        @Override
        public GuacamoleInstruction filter(GuacamoleInstruction instruction)
                throws GuacamoleException {

            // Ignore instructions after first sync is received
            if (receivedSync)
                return instruction;

            String opcode = instruction.getOpcode();

            if (opcode.equals("sync")) {
                receivedSync = true;
                return instruction;
            }

            if (opcode.equals("error"))
                return handleError(instruction);

            return instruction;

        }

        private GuacamoleInstruction handleError(GuacamoleInstruction instruction) {

            // Ignore error instructions which are missing the status code
            List<String> args = instruction.getArgs();
            if (args.size() < 2) {
                logger.debug("Ignoring \"error\" instruction without status code.");
                return instruction;
            }

            int statusCode;
            try {
                statusCode = Integer.parseInt(args.get(1));
            }
            catch (NumberFormatException e) {
                logger.debug("Ignoring \"error\" instruction with non-numeric status code.", e);
                return instruction;
            }

            // Invalid status code
            GuacamoleStatus status = GuacamoleStatus.fromGuacamoleStatusCode(statusCode);
            if (status == null) {
                logger.debug("Ignoring \"error\" instruction with unknown/invalid status code: {}", statusCode);
                return instruction;
            }

            // Only handle error instructions related to the upstream remote desktop
            switch (status) {

                // Transparently connect to a different connection if upstream fails
                case UPSTREAM_ERROR:
                case UPSTREAM_NOT_FOUND:
                case UPSTREAM_TIMEOUT:
                case UPSTREAM_UNAVAILABLE:
                    break;

                // Allow error through otherwise
                default:
                    return instruction;

            }

            logger.debug("Overriding {} \"error\" instruction. Failing over to next connection...", status);

            // Advance through remaining sockets until another functional socket
            // is retrieved, or no more sockets remain
            try {
                selectNextSocket();
                return null;
            }

            catch (GuacamoleException e) {
                logger.debug("No sockets remain to be tried - giving up on failover.");
            }

            // Allow error through if not intercepting
            return instruction;

        }

    }

    /**
     * GuacamoleReader which filters "error" instructions, transparently failing
     * over to the next socket in the queue if an error is encountered. Read
     * attempts are delegated to the GuacamoleReader of the current socket.
     */
    private final GuacamoleReader reader = new FilteredGuacamoleReader(new GuacamoleReader() {

        @Override
        public boolean available() throws GuacamoleException {
            synchronized (sockets) {

                if (socket == null)
                    return false;

                return socket.getReader().available();

            }
        }

        @Override
        public char[] read() throws GuacamoleException {
            synchronized (sockets) {

                if (socket == null)
                    return null;

                return socket.getReader().read();

            }
        }

        @Override
        public GuacamoleInstruction readInstruction()
                throws GuacamoleException {
            synchronized (sockets) {

                if (socket == null)
                    return null;

                return socket.getReader().readInstruction();

            }
        }

    }, new ErrorFilter());

    /**
     * GuacamoleWriter which delegates all write attempts to the GuacamoleWriter
     * of the current socket.
     */
    private final GuacamoleWriter writer = new GuacamoleWriter() {

        @Override
        public void write(char[] chunk, int off, int len)
                throws GuacamoleException {
            synchronized (sockets) {

                if (socket == null)
                    throw new GuacamoleConnectionClosedException("No further sockets remaining in SocketQueue.");

                socket.getWriter().write(chunk, off, len);

            }
        }

        @Override
        public void write(char[] chunk) throws GuacamoleException {
            synchronized (sockets) {

                if (socket == null)
                    throw new GuacamoleConnectionClosedException("No further sockets remaining in SocketQueue.");

                socket.getWriter().write(chunk);

            }
        }

        @Override
        public void writeInstruction(GuacamoleInstruction instruction)
                throws GuacamoleException {
            synchronized (sockets) {

                if (socket == null)
                    throw new GuacamoleConnectionClosedException("No further sockets remaining in SocketQueue.");

                socket.getWriter().writeInstruction(instruction);

            }
        }

    };

    @Override
    public GuacamoleReader getReader() {
        return reader;
    }

    @Override
    public GuacamoleWriter getWriter() {
        return writer;
    }

    @Override
    public void close() throws GuacamoleException {
        synchronized (sockets) {
            socket.close();
        }
    }

    @Override
    public boolean isOpen() {
        synchronized (sockets) {

            if (socket == null)
                return false;

            return socket.isOpen();

        }
    }
    
}
