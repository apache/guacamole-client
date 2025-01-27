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

import { Injectable } from '@angular/core';
import { GuacEventService } from 'guacamole-frontend-lib';
import { GuacFrontendEventArguments } from '../../events/types/GuacFrontendEventArguments';
import { RequestService } from '../../rest/service/request.service';
import { TunnelService } from '../../rest/service/tunnel.service';
import { Error } from '../../rest/types/Error';
import { ManagedClient } from '../types/ManagedClient';
import { ManagedFileTransferState } from '../types/ManagedFileTransferState';
import { ManagedFileUpload } from '../types/ManagedFileUpload';

/**
 * A service for creating new ManagedFileUpload instances.
 */
@Injectable({
    providedIn: 'root'
})
export class ManagedFileUploadService {

    /**
     * Inject required services.
     */
    constructor(private requestService: RequestService,
                private tunnelService: TunnelService,
                private guacEventService: GuacEventService<GuacFrontendEventArguments>) {
    }

    /**
     * Creates a new ManagedFileUpload which uploads the given file to the
     * server through the given Guacamole client.
     *
     * @param managedClient
     *     The ManagedClient through which the file is to be uploaded.
     *
     * @param file
     *     The file to upload.
     *
     * @param object
     *     The object to upload the file to, if any, such as a filesystem
     *     object.
     *
     * @param streamName
     *     The name of the stream to upload the file to. If an object is given,
     *     this must be specified.
     *
     * @return
     *     A new ManagedFileUpload object which can be used to track the
     *     progress of the upload.
     */
    getInstance(managedClient: ManagedClient, file: File, object?: any, streamName: string | null = null): ManagedFileUpload {

        const managedFileUpload = new ManagedFileUpload();

        // Pull Guacamole.Tunnel and Guacamole.Client from given ManagedClient
        const client = managedClient.client;
        const tunnel = managedClient.tunnel;

        // Open file for writing
        let stream: Guacamole.OutputStream;
        if (!object)
            stream = client.createFileStream(file.type, file.name);

            // If object/streamName specified, upload to that instead of a file
        // stream
        else
            stream = object.createOutputStream(file.type, streamName);

        // Notify that the file transfer is pending

        // Init managed upload
        managedFileUpload.filename = file.name;
        managedFileUpload.mimetype = file.type;
        managedFileUpload.progress = 0;
        managedFileUpload.length = file.size;

        // Notify that stream is open
        ManagedFileTransferState.setStreamState(managedFileUpload.transferState,
            ManagedFileTransferState.StreamState.OPEN);


        // Upload file once stream is acknowledged
        stream.onack = (status: Guacamole.Status) => {

            // Notify of any errors from the Guacamole server
            if (status.isError()) {
                ManagedFileTransferState.setStreamState(managedFileUpload.transferState,
                    ManagedFileTransferState.StreamState.ERROR, status.code);
                return;
            }

            // Begin upload
            this.tunnelService.uploadToStream(tunnel.uuid!, stream, file, length => {
                managedFileUpload.progress = length;
            })

                // Notify if upload succeeds
                .then(() => {

                        // Upload complete
                        managedFileUpload.progress = file.size;

                        // Close the stream
                        stream.sendEnd();
                        ManagedFileTransferState.setStreamState(managedFileUpload.transferState,
                            ManagedFileTransferState.StreamState.CLOSED);

                        // Notify of upload completion
                        this.guacEventService.broadcast('guacUploadComplete', { filename: file.name });
                    },

                    // Notify if upload fails
                    this.requestService.createPromiseErrorCallback((error: any) => {

                        // Use provide status code if the error is coming from the stream
                        if (error.type === Error.Type.STREAM_ERROR)
                            ManagedFileTransferState.setStreamState(managedFileUpload.transferState,
                                ManagedFileTransferState.StreamState.ERROR,
                                error.statusCode);

                        // Fail with internal error for all other causes
                        else
                            ManagedFileTransferState.setStreamState(managedFileUpload.transferState,
                                ManagedFileTransferState.StreamState.ERROR,
                                Guacamole.Status.Code.SERVER_ERROR); // TODO: Guacamole.Status.Code.INTERNAL_ERROR does
                                                                     // not exist

                        // Close the stream
                        stream.sendEnd();

                    }));

            // Ignore all further acks
            stream.onack = null;


        };

        return managedFileUpload;

    }
}
