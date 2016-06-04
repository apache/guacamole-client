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
import java.io.OutputStream;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.io.GuacamoleReader;
import org.apache.guacamole.net.DelegatingGuacamoleTunnel;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.protocol.FilteredGuacamoleReader;
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
    private static final Logger logger =
            LoggerFactory.getLogger(StreamInterceptingTunnel.class);

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
     * The filter to use for rerouting received stream data to OutputStreams.
     */
    private final OutputStreamInterceptingFilter outputStreamFilter =
            new OutputStreamInterceptingFilter(this);

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

        // Log beginning of intercepted stream
        logger.debug("Intercepting output stream #{} of tunnel \"{}\".",
                index, getUUID());

        outputStreamFilter.interceptStream(index, new BufferedOutputStream(stream));

        // Log end of intercepted stream
        logger.debug("Intercepted output stream #{} of tunnel \"{}\" ended.",
                index, getUUID());

    }

    @Override
    public GuacamoleReader acquireReader() {
        return new FilteredGuacamoleReader(super.acquireReader(), outputStreamFilter);
    }

    @Override
    public synchronized void close() throws GuacamoleException {

        // Close first, such that no further streams can be added via
        // interceptStream()
        try {
            super.close();
        }

        // Close all intercepted streams
        finally {
            outputStreamFilter.closeAllInterceptedStreams();
        }

    }

}
