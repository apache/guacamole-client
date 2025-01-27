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

/**
 * Represents the state of a Guacamole stream, including any
 * error conditions.
 *
 * This class is used by the guacClientManager service.
 */
export class ManagedFileTransferState {

    /**
     * The current stream state. Valid values are described by
     * ManagedFileTransferState.StreamState.
     *
     * @default ManagedFileTransferState.StreamState.IDLE
     */
    streamState: ManagedFileTransferState.StreamState;

    /**
     * The status code of the current error condition, if streamState
     * is ERROR. For all other streamState values, this will be
     * {@link Guacamole.Status.Code.SUCCESS}.
     *
     * @default Guacamole.Status.Code.SUCCESS
     */
    statusCode: number;

    /**
     * Creates a new ManagedFileTransferState object. This constructor initializes the properties of the
     * new ManagedFileTransferState with the corresponding properties of the given template.
     *
     * @param [template={}]
     *     The object whose properties should be copied within the new
     *     ManagedFileTransferState.
     */
    constructor(template: Partial<ManagedFileTransferState> = {}) {
        this.streamState = template.streamState || ManagedFileTransferState.StreamState.IDLE;
        this.statusCode = template.statusCode || Guacamole.Status.Code.SUCCESS;
    }

    /**
     * Sets the current transfer state and, if given, the associated status
     * code. If an error is already represented, this function has no effect.
     *
     * @param transferState
     *     The ManagedFileTransferState to update.
     *
     * @param streamState
     *     The stream state to assign to the given ManagedFileTransferState, as
     *     listed within ManagedFileTransferState.StreamState.
     *
     * @param statusCode
     *     The status code to assign to the given ManagedFileTransferState, if
     *     any, as listed within Guacamole.Status.Code. If no status code is
     *     specified, the status code of the ManagedFileTransferState is not
     *     touched.
     */
    static setStreamState(transferState: ManagedFileTransferState, streamState: ManagedFileTransferState.StreamState, statusCode?: number): void {

        // Do not set state after an error is registered
        if (transferState.streamState === ManagedFileTransferState.StreamState.ERROR)
            return;

        // Update stream state
        transferState.streamState = streamState;

        // Set status code, if given
        if (statusCode)
            transferState.statusCode = statusCode;

    }
}

export namespace ManagedFileTransferState {

    /**
     * Valid stream state strings. Each state string is associated with a
     * specific state of a Guacamole stream.
     */
    export enum StreamState {

        /**
         * The stream has not yet been opened.
         */
        IDLE   = 'IDLE',

        /**
         * The stream has been successfully established. Data can be sent or
         * received.
         */
        OPEN   = 'OPEN',

        /**
         * The stream has terminated successfully. No errors are indicated.
         */
        CLOSED = 'CLOSED',

        /**
         * The stream has terminated due to an error. The associated error code
         * is stored in statusCode.
         */
        ERROR  = 'ERROR'

    }
}
