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

import java.io.Closeable;

/**
 * A simple pairing of the index of an intercepted Guacamole stream with the
 * stream-type object which will produce or consume the data sent over the
 * intercepted Guacamole stream.
 *
 * @author Michael Jumper
 * @param <T>
 *     The type of object which will produce or consume the data sent over the
 *     intercepted Guacamole stream. Usually, this will be either InputStream
 *     or OutputStream.
 */
public class InterceptedStream<T extends Closeable> {

    /**
     * The index of the Guacamole stream being intercepted.
     */
    private final String index;

    /**
     * The stream which will produce or consume the data sent over the
     * intercepted Guacamole stream.
     */
    private final T stream;

    /**
     * Creates a new InterceptedStream which associated the given Guacamole
     * stream index with the given stream object.
     *
     * @param index
     *     The index of the Guacamole stream being intercepted.
     *
     * @param stream
     *     The stream which will produce or consume the data sent over the
     *     intercepted Guacamole stream.
     */
    public InterceptedStream(String index, T stream) {
        this.index = index;
        this.stream = stream;
    }

    /**
     * Returns the index of the Guacamole stream being intercepted.
     *
     * @return
     *     The index of the Guacamole stream being intercepted.
     */
    public String getIndex() {
        return index;
    }

    /**
     * Returns the stream which will produce or consume the data sent over the
     * intercepted Guacamole stream.
     *
     * @return
     *     The stream which will produce or consume the data sent over the
     *     intercepted Guacamole stream.
     */
    public T getStream() {
        return stream;
    }

}
