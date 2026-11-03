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

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Queues blob data received on the tunnel read thread and writes it to an
 * intercepted output stream on the HTTP streaming thread that calls run().
 * Reports the result of each write to an ExecutionListener.
 */
public class OutputStreamWriter {

    /**
     * Listener for results of blob writes and other events from the writing
     * loop in run().
     */
    public interface ExecutionListener {
        void onBlobWritten(String streamIndex, boolean requiresAck);
        void onWriteFailed(String streamIndex);
        void onStreamEnd(String streamIndex);
    }

    /**
     * Internal queue message type.
     */
    private interface Message {}

    /**
     * Message that carries a blob to write.
     */
    private final class MessageBlob implements Message {
        public final byte[] blob;
        public final boolean requiresAck;
        public MessageBlob(byte[] blob, boolean requiresAck) {
            this.blob = blob;
            this.requiresAck = requiresAck;
        }
    }

    /**
     * Message that signals the end of the stream.
     */
    private final class MessageEnd implements Message {}

    /**
     * Message that signals the writing loop in run() to stop.
     */
    private final class MessageStop implements Message {}

    /**
     * Logger for this class.
     */
    private static final Logger logger =
            LoggerFactory.getLogger(OutputStreamWriter.class);

    /**
     * Index of the output stream.
     */
    private final String streamIndex;

    /**
     * The stream to write blobs.
     */
    private final InterceptedStream<OutputStream> stream;

    /**
     * Listener that receives the results of blob writes and other events from
     * the writing loop in run().
     */
    private final ExecutionListener executionListener;

    /**
     * Queue of blobs and control messages passed from the tunnel read thread to
     * the writing loop in run().
     */
    private final BlockingQueue<Message> messageQueue = new LinkedBlockingQueue<>();

    /**
     * Whether the writing loop in run() is still active.
     */
    private volatile boolean isRunning = true;

    /**
     * Whether the latest queued blob will trigger an acknowledge packet. 
     */
    private boolean prevBlobRequiresAck = false;

    /**
     * Creates a new OutputStreamWriter which will write blobs
     * into the output stream and return results of the writing operations
     * to the execution listener.
     * 
     * @param stream
     *     The stream to write blobs.
     * 
     * @param executionListener
     *     Listener to receive the results of writing operations.
     */
    public OutputStreamWriter(InterceptedStream<OutputStream> stream,
            ExecutionListener executionListener) {
        this.streamIndex = stream.getIndex();
        this.stream = stream;
        this.executionListener = executionListener;
    }

    /**
     * Stops the writer loop and unblocks run() if it is waiting on an empty
     * queue. Closing the output stream alone does not do this; a stop message
     * must be enqueued. Used when the tunnel closes mid-download or when a
     * writer is replaced for the same stream index.
     */
    public void stop() {
        isRunning = false;
        messageQueue.offer(new MessageStop());
    }

    /**
     * Return the stream where the blobs are written.
     * 
     * @return
     *     The stream related to this writer.
     */
    public InterceptedStream<OutputStream> getStream() {
        return stream;
    }

    /**
     * Puts a blob into the internal queue to be written to the stream.
     * 
     * @param blob
     *     Blob which has to be written into the stream.
     * 
     * @param requiresAck
     *     ACK packet must be sent when the blob is written.
     */
    public void handleBlob(byte[] blob, boolean requiresAck) {
        messageQueue.offer(new MessageBlob(blob, requiresAck));
        prevBlobRequiresAck = requiresAck;
    }

    /**
     * Puts the end marker message into the queue.
     */
    public void handleEnd() {
        messageQueue.offer(new MessageEnd());
    }

    /**
     * Checks whether the previous blob required ACK to be sent to the sender.
     * 
     * @return
     *     true if the previous blob required ACK. 
     */
    public boolean didPrevBlobRequireAck() {
        return prevBlobRequiresAck;
    }

    /**
     * Returns current queue message count.
     * 
     * @return
     *     Current queue message count.
     */
    public int getQueuedMessageCount() {
        int size = messageQueue.size();
        return size;
    }

    /**
     * Drains the message queue on the calling thread, writing each blob to the
     * output stream and notifying the execution listener of the result.
     */
    public void run() {

        logger.debug("Started processing message queue for stream #{}",
                streamIndex);

        try {

            // Run the writing loop
            while (isRunning) {

                // Pull a queued message
                Message message = messageQueue.take();

                // Stop was requested; isRunning is sufficient here.
                if (!isRunning) {
                    break;
                }

                // End marker received; close the stream after prior blobs
                if (message instanceof MessageEnd) {
                    logger.debug("Received the end marker for stream #{}", streamIndex);
                    executionListener.onStreamEnd(streamIndex);
                    isRunning = false;
                    break;
                }

                // Write the blob
                if (message instanceof MessageBlob) {
                    MessageBlob streamWriterBlob = (MessageBlob) message;

                    // Attempt to write data to stream
                    stream.getStream().write(streamWriterBlob.blob);

                    // Acknowledge the blob on the client's behalf if required
                    executionListener.onBlobWritten(stream.getIndex(),
                            streamWriterBlob.requiresAck);
                }

            }
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        catch (IOException e) {
            executionListener.onWriteFailed(streamIndex);
            logger.debug("Write failed for intercepted stream.", e);
        }

        logger.debug("Finished processing message queue for stream #{}",
                streamIndex);
    }
}

