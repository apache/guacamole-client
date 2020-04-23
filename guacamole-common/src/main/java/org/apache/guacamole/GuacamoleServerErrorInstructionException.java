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

package org.apache.guacamole;

import org.apache.guacamole.protocol.GuacamoleStatus;

/**
 * The exception thrown when the Guacamole server explicitly sends an error
 * instruction to the client. The error message and status code reflect the arguments
 * of the error instruction as determined by the server.
 */
public class GuacamoleServerErrorInstructionException extends GuacamoleServerException {
    /**
     * The Guacamole protocol status code, as determined by the server;
     */
    private final GuacamoleStatus status;

    /**
     * Creates a new GuacamoleServerErrorInstructionException with the given
     * message and status.
     *
     * @param message
     *     A human readable description of the exception that occurred.
     *
     * @param status
     *     The status code, as determined by the server from which the
     *     instruction originated.
     */
    public GuacamoleServerErrorInstructionException(String message, GuacamoleStatus status) {
        super(message);
        this.status = status;
    }

    @Override
    public GuacamoleStatus getStatus() {
        return status;
    }
}
