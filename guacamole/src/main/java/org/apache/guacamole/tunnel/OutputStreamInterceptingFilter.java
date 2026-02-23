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

import com.google.common.io.BaseEncoding;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.protocol.GuacamoleInstruction;
import org.apache.guacamole.protocol.GuacamoleStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filter which selectively intercepts "blob" and "end" instructions,
 * automatically writing to or closing the stream given with
 * interceptStream(). The required "ack" responses to received blobs are
 * sent automatically.
 */
public class OutputStreamInterceptingFilter
        extends StreamInterceptingFilter<OutputStream>
        implements OutputStreamWriter.ExecutionListener {

    /**
     * Logger for this class.
     */
    private static final Logger logger =
            LoggerFactory.getLogger(OutputStreamInterceptingFilter.class);

    /**
     * Active download writers, keyed by stream index. Blobs are enqueued here
     * on the tunnel read thread and written asynchronously on the HTTP
     * streaming thread.
     */
    private final ConcurrentMap<String, OutputStreamWriter> streamWriters =
            new ConcurrentHashMap<>();

    /**
     * Whether this OutputStreamInterceptingFilter should respond to received
     * blobs with "ack" messages on behalf of the client. If false, blobs will
     * still be handled by this filter, but empty blobs will be sent to the
     * client, forcing the client to respond on its own.
     */
    private boolean acknowledgeBlobs = true;

    /**
     * Creates a new OutputStreamInterceptingFilter which selectively intercepts
     * "blob" and "end" instructions. The required "ack" responses will
     * automatically be sent over the given tunnel.
     *
     * @param tunnel
     *     The GuacamoleTunnel over which any required "ack" instructions
     *     should be sent.
     */
    public OutputStreamInterceptingFilter(GuacamoleTunnel tunnel) {
        super(tunnel);
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

        // Error "ack" instructions implicitly close the stream
        if (status != GuacamoleStatus.SUCCESS)
            closeInterceptedStream(index);

        sendInstruction(new GuacamoleInstruction("ack", index, message,
                Integer.toString(status.getGuacamoleStatusCode())));

    }

    @Override
    public void onBlobWritten(String streamIndex, boolean requiresAck) {
        if (requiresAck) {
            sendAck(streamIndex, "OK", GuacamoleStatus.SUCCESS);
        }
    }

    @Override
    public void onWriteFailed(String streamIndex) {
        sendAck(streamIndex, "FAIL", GuacamoleStatus.SERVER_ERROR);
    }

    @Override
    public void onStreamEnd(String streamIndex) {
        closeInterceptedStream(streamIndex);
    }

    /**
     * Handles a single "blob" instruction, decoding its base64 data and
     * enqueueing it for writing to the associated output stream, ultimately
     * dropping the "blob" instruction such that the client never receives
     * it. If no writer is registered for the stream index within the "blob"
     * instruction, the instruction is passed through untouched.
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

        // Get the stream index
        String streamIndex = args.get(0);

        // Enqueue the blob for the HTTP streaming thread if a writer exists
        OutputStreamWriter streamWriter = streamWriters.get(streamIndex);
        if (streamWriter == null)
            return instruction;

        // Decode blob
        byte[] blob;
        try {
            String data = args.get(1);
            blob = BaseEncoding.base64().decode(data);
        }
        catch (IllegalArgumentException e) {
            logger.warn("Received base64 data for intercepted stream was invalid.", e);
            return null;
        }

        // Force client to respond with their own "ack" to confirm they are not
        // falling behind with respect to the graphical session, only if
        // - There are no blobs in the queue currently
        // - Previous blob required server side acknowledgement 
        // This may lead to more than one blob in the writer queue temporarily,
        // but not more than two blobs anyways.
        if (!acknowledgeBlobs &&
                streamWriter.getQueuedMessageCount() == 0 &&
                streamWriter.didPrevBlobRequireAck()) {
            streamWriter.handleBlob(blob, false);
            acknowledgeBlobs = true;

            // Send an empty blob to trigger client "ack"
            return new GuacamoleInstruction("blob", streamIndex, "");
        }

        // Put the blob to the writer queue
        streamWriter.handleBlob(blob, true);
        return null;
    }

    /**
     * Handles a single "end" instruction. If a writer is registered for the
     * stream index, an end marker is queued so the stream is closed after all
     * prior blobs have been written. Otherwise this function has no effect.
     *
     * @param instruction
     *     The "end" instruction being handled.
     */
    private void handleEnd(GuacamoleInstruction instruction) {

        // Verify all required arguments are present
        List<String> args = instruction.getArgs();
        if (args.size() < 1)
            return;

        OutputStreamWriter streamWriter = streamWriters.get(args.get(0));
        if (streamWriter == null)
            return;
        
        // Queue an end marker; the writer closes the stream after queued blobs
        // have been written.
        streamWriter.handleEnd();
    }

    @Override
    public void closeAllInterceptedStreams() {

        // When the tunnel closes during a download, the HTTP thread may be
        // blocked in OutputStreamWriter.run() waiting for the next blob or
        // end instruction. guacd will not send either once the connection is
        // gone, and closing the OutputStream does not unblock take() on an
        // empty queue, so each writer must be stopped explicitly.
        for (OutputStreamWriter writer : streamWriters.values())
            writer.stop();

        super.closeAllInterceptedStreams();

    }

    /**
     * Handles a single "sync" instruction, updating internal tracking of
     * client render state.
     *
     * @param instruction
     *     The "sync" instruction being handled.
     */
    private void handleSync(GuacamoleInstruction instruction) {
        acknowledgeBlobs = false;
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

        // Monitor "sync" instructions to ensure the client does not starve
        // from lack of graphical updates
        if (instruction.getOpcode().equals("sync")) {
            handleSync(instruction);
            return instruction;
        }

        // Pass instruction through untouched
        return instruction;

    }

    @Override
    protected void handleInterceptedStream(InterceptedStream<OutputStream> stream) {

        // Create the stream writer
        OutputStreamWriter streamWriter = new OutputStreamWriter(stream, this);

        // Put it into the container and check if there was another writer for the index
        OutputStreamWriter old = streamWriters.put(stream.getIndex(), streamWriter);
        if (old != null) {
            logger.debug("Found an older stream #{}; will close it", stream.getIndex());
            // Close the stream to be sure it does not get stuck on write
            closeInterceptedStream(old.getStream());
            // Unblock the previous HTTP thread if it is still in run()
            old.stop();
        }

        // Acknowledge that the stream is ready to receive data
        sendAck(stream.getIndex(), "OK", GuacamoleStatus.SUCCESS);

        // Block the HTTP streaming thread while queued blobs are written, until
        // the stream ends, fails, or the tunnel is closed.
        streamWriter.run();

        // Close the stream if not closed yet
        closeInterceptedStream(stream);

        // Remove the stream from the container
        streamWriters.entrySet().removeIf(entry -> entry.getValue().equals(streamWriter));        
    }
}
