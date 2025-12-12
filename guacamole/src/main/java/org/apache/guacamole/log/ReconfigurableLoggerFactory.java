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

package org.apache.guacamole.log;

import ch.qos.logback.core.joran.spi.JoranException;
import java.io.InputStream;
import org.slf4j.ILoggerFactory;

/**
 * An {@link ILoggerFactory} that can be reconfigured as needed.
 */
public interface ReconfigurableLoggerFactory extends ILoggerFactory {

    /**
     * Reconfigures this {@link ILoggerFactory} using the given InputStream for
     * a Logback XML configuration file. It is the responsibility of the caller
     * to close the provided InputStream.
     *
     * @param logbackConfiguration
     *     An InputStream that provides the desired Logback configuration in
     *     XML format.
     *
     * @throws JoranException
     *     If the configuration is invalid, or an error occurs that prevents
     *     the configuration from being read/applied.
     */
    void reconfigure(InputStream logbackConfiguration) throws JoranException;

}
