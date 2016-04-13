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

package org.apache.guacamole.tunnel;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.bind.DatatypeConverter;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.io.GuacamoleReader;
import org.apache.guacamole.io.GuacamoleWriter;
import org.apache.guacamole.net.DelegatingGuacamoleTunnel;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.protocol.FilteredGuacamoleReader;
import org.apache.guacamole.protocol.GuacamoleFilter;
import org.apache.guacamole.protocol.GuacamoleInstruction;
import org.apache.guacamole.protocol.GuacamoleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * GuacamoleTunnel implementation which provides for intercepting the contents
 * of in-progress streams, rerouting received blobs to a provided OutputStream.
 * Interception of streams is requested on a per stream basis and lasts only
 * for the duration of that stream.
 *
 * @author Michael Jumper
 */
public class StreamInterceptingTunnel extends DelegatingGuacamoleTunnel {

    /**
     * Logger for this class.
     */
    private static final Logger logger = LoggerFactory.getLogger(StreamInterceptingTunnel.class);

    /**
     * Creates a new StreamInterceptingTunnel which wraps the given tunnel,
     * reading and intercepting stream-related instructions as necessary to
     * fulfill calls to interceptStream().
     *
     * @param tunnel
     *     The tunnel whose stream-related instruction should be intercepted if
     *     interceptStream() is invoked.
     */
    public StreamInterceptingTunnel(GuacamoleTunnel tunnel) {
        super(tunnel);
    }

    /**
     * Mapping of the indexes of all streams whose associated "blob" and "end"
     * instructions should be intercepted.
     */
    private final Map<String, OutputStream> streams =
            new ConcurrentHashMap<String, OutputStream>();

    /**
     * Filter which selectively intercepts "blob" and "end" instructions,
     * automatically writing to or closing the stream given with
     * interceptStream(). The required "ack" responses to received blobs are
     * sent automatically.
     */
    private final GuacamoleFilter STREAM_FILTER = new GuacamoleFilter() {

        /**
         * Handles a single "blob" instruction, decoding its base64 data,
         * sending that data to the associated OutputStream, and ultimately
         * dropping the "blob" instruction such that the client never receives
         * it. If no OutputStream is associated with the stream index within
         * the "blob" instruction, the instruction is passed through untouched.
         *
         * @param instruction
         *     The "blob" instruction being handled.
         *
         * @return
         *     The originally-provided "blob" instruction, if that instruction
         *     should be passed through to the client, or null if the "blob"
         *     instruction should be dropped.
         */
        private GuacamoleInstruction handleBlob(GuacamoleInstruction instruction) {

            // Verify all required arguments are present
            List<String> args = instruction.getArgs();
            if (args.size() < 2)
                return instruction;

            // Pull associated stream
            String index = args.get(0);
            OutputStream stream = streams.get(index);
            if (stream == null)
                return instruction;

            // Decode blob
            byte[] blob;
            try {
                String data = args.get(1);
                blob = DatatypeConverter.parseBase64Binary(data);
            }
            catch (IllegalArgumentException e) {
                logger.warn("Received base64 data for intercepted stream was invalid.");
                logger.debug("Decoding base64 data for intercepted stream failed.", e);
                return null;
            }

            // Attempt to write data to stream
            try {
                stream.write(blob);
                sendAck(index, "OK", GuacamoleStatus.SUCCESS);
            }
            catch (IOException e) {
                sendAck(index, "FAIL", GuacamoleStatus.SERVER_ERROR);
                logger.debug("Write failed for intercepted stream.", e);
            }

            // Instruction was handled purely internally
            return null;

        }

        /**
         * Handles a single "end" instruction, closing the associated
         * OutputStream. If no OutputStream is associated with the stream index
         * within the "end" instruction, this function has no effect.
         *
         * @param instruction
         *     The "end" instruction being handled.
         */
        private void handleEnd(GuacamoleInstruction instruction) {

            // Verify all required arguments are present
            List<String> args = instruction.getArgs();
            if (args.size() < 1)
                return;

            // Terminate stream
            closeStream(args.get(0));

        }

        @Override
        public GuacamoleInstruction filter(GuacamoleInstruction instruction)
                throws GuacamoleException {

            // Intercept "blob" instructions for in-progress streams
            if (instruction.getOpcode().equals("blob"))
                return handleBlob(instruction);

            // Intercept "end" instructions for in-progress streams
            if (instruction.getOpcode().equals("end")) {
                handleEnd(instruction);
                return instruction;
            }

            // Pass instruction through untouched
            return instruction;

        }

    };

    /**
     * Closes the given OutputStream, logging any errors that occur during
     * closure. The monitor of the OutputStream is notified via a single call
     * to notify() once the attempt to close has been made.
     *
     * @param stream
     *     The OutputStream to close and notify.
     */
    private void closeStream(OutputStream stream) {

        // Attempt to close stream
        try {
            stream.close();
        }
        catch (IOException e) {
            logger.warn("Unable to close intercepted stream: {}", e.getMessage());
            logger.debug("I/O error prevented closure of intercepted stream.", e);
        }

        // Notify waiting threads that the stream has ended
        synchronized (stream) {
            stream.notify();
        }

    }

    /**
     * Closes the OutputStream associated with the stream having the given
     * index, if any, logging any errors that occur during closure. If no such
     * stream exists, this function has no effect. The monitor of the
     * OutputStream is notified via a single call to notify() once the attempt
     * to close has been made.
     *
     * @param index
     *     The index of the stream whose associated OutputStream should be
     *     closed and notified.
     */
    private OutputStream closeStream(String index) {

        // Remove associated stream
        OutputStream stream = streams.remove(index);
        if (stream == null)
            return null;

        // Close stream if it exists
        closeStream(stream);
        return stream;

    }

    /**
     * Injects an "ack" instruction into the outbound Guacamole protocol
     * stream, as if sent by the connected client. "ack" instructions are used
     * to acknowledge the receipt of a stream and its subsequent blobs, and are
     * the only means of communicating success/failure status.
     *
     * @param index
     *     The index of the stream that this "ack" instruction relates to.
     *
     * @param message
     *     An arbitrary human-readable message to include within the "ack"
     *     instruction.
     *
     * @param status
     *     The status of the stream operation being acknowledged via the "ack"
     *     instruction. Error statuses will implicitly close the stream via
     *     closeStream().
     */
    private void sendAck(String index, String message, GuacamoleStatus status) {

        // Temporarily acquire writer to send "ack" instruction
        GuacamoleWriter writer = acquireWriter();

        // Send successful "ack"
        try {
            writer.writeInstruction(new GuacamoleInstruction("ack", index, message,
                    Integer.toString(status.getGuacamoleStatusCode())));
        }
        catch (GuacamoleException e) {
            logger.debug("Unable to send \"ack\" for intercepted stream.", e);
        }

        // Error "ack" instructions implicitly close the stream
        if (status != GuacamoleStatus.SUCCESS)
            closeStream(index);

        // Done writing
        releaseWriter();

    }

    /**
     * Intercept all data received along the stream having the given index,
     * writing that data to the given OutputStream. The OutputStream will
     * automatically be closed when the stream ends. If there is no such
     * stream, then the OutputStream will be closed immediately. This function
     * will block until all received data has been written to the OutputStream
     * and the OutputStream has been closed.
     *
     * @param index
     *     The index of the stream to intercept.
     *
     * @param stream
     *     The OutputStream to write all intercepted data to.
     */
    public void interceptStream(int index, OutputStream stream) {

        String indexString = Integer.toString(index);

        // Atomically verify tunnel is open and add the given stream
        OutputStream oldStream;
        synchronized (this) {

            // Do nothing if tunnel is not open
            if (!isOpen()) {
                closeStream(stream);
                return;
            }

            // Wrap stream
            stream = new BufferedOutputStream(stream);

            // Replace any existing stream
            oldStream = streams.put(indexString, stream);

        }

        // If a previous stream DID exist, close it
        if (oldStream != null)
            closeStream(oldStream);

        // Log beginning of intercepted stream
        logger.debug("Intercepting stream #{} of tunnel \"{}\".",
                index, getUUID());

        // Acknowledge stream receipt
        sendAck(indexString, "OK", GuacamoleStatus.SUCCESS);

        // Wait for stream to close
        synchronized (stream) {
            while (streams.get(indexString) == stream) {
                try {
                    stream.wait();
                }
                catch (InterruptedException e) {
                    // Ignore
                }
            }
        }

        // Log end of intercepted stream
        logger.debug("Intercepted stream #{} of tunnel \"{}\" ended.", index, getUUID());

    }

    @Override
    public GuacamoleReader acquireReader() {
        return new FilteredGuacamoleReader(super.acquireReader(), STREAM_FILTER);
    }

    @Override
    public synchronized void close() throws GuacamoleException {

        // Close first, such that no further streams can be added via
        // interceptStream()
        try {
            super.close();
        }

        // Ensure all waiting threads are notified that all streams have ended
        finally {

            // Close any active streams
            for (OutputStream stream : streams.values())
                closeStream(stream);

            // Remove now-useless references
            streams.clear();

        }

    }

}
