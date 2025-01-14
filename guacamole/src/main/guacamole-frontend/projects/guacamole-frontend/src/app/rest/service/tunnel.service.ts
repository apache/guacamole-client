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

import { DOCUMENT } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AuthenticationService } from '../../auth/service/authentication.service';
import { Error } from '../types/Error';
import { Protocol } from '../types/Protocol';
import { SharingProfile } from '../types/SharingProfile';
import { UserCredentials } from '../types/UserCredentials';

/**
 * Service for operating on the tunnels of in-progress connections (and their
 * underlying objects) via the REST API.
 */
@Injectable({
    providedIn: 'root'
})
export class TunnelService {

    /**
     * The number of milliseconds to wait after a stream download has completed
     * before cleaning up related DOM resources, if the browser does not
     * otherwise notify us that cleanup is safe.
     */
    private readonly DOWNLOAD_CLEANUP_WAIT: number = 5000;

    /**
     * The maximum size a chunk may be during uploadToStream() in bytes.
     */
    private readonly CHUNK_SIZE: number = 1024 * 1024 * 4;

    /**
     * Inject required services.
     */
    constructor(private http: HttpClient, private authenticationService: AuthenticationService) {
    }

    /**
     * Makes a request to the REST API to get the list of all tunnels
     * associated with in-progress connections, returning an Observable that
     * provides an array of their UUIDs (strings) if successful.
     *
     * @returns
     *     An Observable which will emit an array of UUID strings, uniquely
     *     identifying each active tunnel.
     */
    getTunnels(): Observable<string[]> {

        // Retrieve tunnels
        return this.http.get<string[]>('api/session/tunnels');
    }

    /**
     * Makes a request to the REST API to retrieve the underlying protocol of
     * the connection associated with a particular tunnel, returning an Observable
     * that provides a @link{Protocol} object if successful.
     *
     * @param tunnel
     *     The UUID of the tunnel associated with the Guacamole connection
     *     whose underlying protocol is being retrieved.
     *
     * @returns
     *     An Observable which will resolve with a @link{Protocol} object upon
     *     success.
     */
    getProtocol(tunnel: string): Observable<Protocol> {

        return this.http.get<Protocol>('api/session/tunnels/' + encodeURIComponent(tunnel) + '/protocol');

    }

    /**
     * Retrieves the set of sharing profiles that the current user can use to
     * share the active connection of the given tunnel.
     *
     * @param tunnel
     *     The UUID of the tunnel associated with the Guacamole connection
     *     whose sharing profiles are being retrieved.
     *
     * @returns
     *     An observable which will emit a map of @link{SharingProfile}
     *     objects where each key is the identifier of the corresponding
     *     sharing profile.
     */
    getSharingProfiles(tunnel: string): Observable<Record<string, SharingProfile>> {

        // Retrieve all associated sharing profiles
        return this.http.get<Record<string, SharingProfile>>('api/session/tunnels/' + encodeURIComponent(tunnel) +
            '/activeConnection/connection/sharingProfiles');

    }

    /**
     * Makes a request to the REST API to generate credentials which have
     * access strictly to the active connection associated with the given
     * tunnel, using the restrictions defined by the given sharing profile,
     * returning an observable that emits the resulting @link{UserCredentials}
     * object if successful.
     *
     * @param tunnel
     *     The UUID of the tunnel associated with the Guacamole connection
     *     being shared.
     *
     * @param sharingProfile
     *     The identifier of the connection object dictating the
     *     semantics/restrictions which apply to the shared session.
     *
     * @returns
     *     An observable which will emit a @link{UserCredentials} object
     *     upon success.
     */
    getSharingCredentials(tunnel: string, sharingProfile: string): Observable<UserCredentials> {

        // Generate sharing credentials
        return this.http.get<UserCredentials>('api/session/tunnels/' + encodeURIComponent(tunnel) +
            '/activeConnection/sharingCredentials/' + encodeURIComponent(sharingProfile));

    }

    /**
     * Sanitize a filename, replacing all URL path separators with safe
     * characters.
     *
     * @param filename
     *     An unsanitized filename that may need cleanup.
     *
     * @returns
     *     The sanitized filename.
     */
    sanitizeFilename(filename: string): string {
        return filename.replace(/[\\\/]+/g, '_');
    }

    /**
     * Makes a request to the REST API to retrieve the contents of a stream
     * which has been created within the active Guacamole connection associated
     * with the given tunnel. The contents of the stream will automatically be
     * downloaded by the browser.
     *
     * WARNING: Like Guacamole's various reader implementations, this function
     * relies on assigning an "onend" handler to the stream object for the sake
     * of cleaning up resources after the stream closes. If the "onend" handler
     * is overwritten after this function returns, resources may not be
     * properly cleaned up.
     *
     * @param tunnel
     *     The UUID of the tunnel associated with the Guacamole connection
     *     whose stream should be downloaded as a file.
     *
     * @param stream
     *     The stream whose contents should be downloaded.
     *
     * @param mimetype
     *     The mimetype of the stream being downloaded. This is currently
     *     ignored, with the download forced by using
     *     "application/octet-stream".
     *
     * @param filename
     *     The filename that should be given to the downloaded file.
     */
    downloadStream(tunnel: string, stream: Guacamole.InputStream, mimetype: string, filename: string): void {

        const streamOrigin = window.location.origin;

        // Build download URL
        const url = streamOrigin
            + window.location.pathname
            + 'api/session/tunnels/' + encodeURIComponent(tunnel)
            + '/streams/' + encodeURIComponent(stream.index)
            + '/' + encodeURIComponent(this.sanitizeFilename(filename))
            + '?token=' + encodeURIComponent(this.authenticationService.getCurrentToken() || '');

        // Create temporary hidden iframe to facilitate download
        const iframe = document.createElement('iframe');
        iframe.style.position = 'fixed';
        iframe.style.border = 'none';
        iframe.style.width = '1px';
        iframe.style.height = '1px';
        iframe.style.left = '-1px';
        iframe.style.top = '-1px';

        // The iframe MUST be part of the DOM for the download to occur
        document.body.appendChild(iframe);

        // Automatically remove iframe from DOM when download completes, if
        // browser supports tracking of iframe downloads via the "load" event
        iframe.onload = () => {
            document.body.removeChild(iframe);
        };

        // Acknowledge (and ignore) any received blobs
        stream.onblob = () => {
            stream.sendAck('OK', Guacamole.Status.Code.SUCCESS);
        };

        // Automatically remove iframe from DOM a few seconds after the stream
        // ends, in the browser does NOT fire the "load" event for downloads
        stream.onend = () => {
            window.setTimeout(() => {
                if (iframe.parentElement) {
                    document.body.removeChild(iframe);
                }
            }, this.DOWNLOAD_CLEANUP_WAIT);
        };

        // Begin download
        iframe.src = url;

    }

    /**
     * Makes a request to the REST API to send the contents of the given file
     * along a stream which has been created within the active Guacamole
     * connection associated with the given tunnel. The contents of the file
     * will automatically be split into individual "blob" instructions, as if
     * sent by the connected Guacamole client.
     *
     * @param tunnel
     *     The UUID of the tunnel associated with the Guacamole connection
     *     whose stream should receive the given file.
     *
     * @param stream
     *     The stream that should receive the given file.
     *
     * @param file
     *     The file that should be sent along the given stream.
     *
     * @param progressCallback
     *     An optional callback which, if provided, will be invoked as the
     *     file upload progresses. The current position within the file, in
     *     bytes, will be provided to the callback as the sole argument.
     *
     * @return
     *     A promise which resolves when the upload has completed, and is
     *     rejected with an Error if the upload fails. The Guacamole protocol
     *     status code describing the failure will be included in the Error if
     *     available. If the status code is available, the type of the Error
     *     will be STREAM_ERROR.
     */
    uploadToStream(tunnel: string, stream: Guacamole.OutputStream, file: File, progressCallback?: (progress: number) => void): Promise<void> {

        let resolve: () => void;
        let reject: (error: Error) => void;

        const deferred: Promise<void> = new Promise<void>(
            (resolveFn: () => void, rejectFn: (error: Error) => void) => {
                resolve = resolveFn;
                reject = rejectFn;
            }
        );

        const streamOrigin = window.location.origin;

        // Build upload URL
        const url = streamOrigin
            + window.location.pathname
            + 'api/session/tunnels/' + encodeURIComponent(tunnel)
            + '/streams/' + encodeURIComponent(stream.index)
            + '/' + encodeURIComponent(this.sanitizeFilename(file.name))
            + '?token=' + encodeURIComponent(this.authenticationService.getCurrentToken() || '');

        /**
         * Creates a chunk of the inputted file to be uploaded.
         *
         * @param offset
         *      The byte at which to begin the chunk.
         *
         * @return
         *      The file chunk created by this function.
         */
        const createChunk = (offset: number): Blob => {
            const chunkEnd = Math.min(offset + this.CHUNK_SIZE, file.size);
            const chunk = file.slice(offset, chunkEnd);
            return chunk;
        };

        /**
         * POSTs the inputted chunks and recursively calls uploadHandler()
         * until the upload is complete.
         *
         * @param chunk
         *      The chunk to be uploaded to the stream.
         *
         * @param offset
         *      The byte at which the inputted chunk begins.
         */
        const uploadChunk = (chunk: Blob, offset: number): void => {
            const xhr = new XMLHttpRequest();
            xhr.open('POST', url, true);

            // Invoke provided callback if upload tracking is supported.
            if (progressCallback && xhr.upload) {
                xhr.upload.addEventListener('progress', function updateProgress(e) {
                    progressCallback(e.loaded + offset);
                });
            }

            // Continue to next chunk, resolve, or reject promise as appropriate
            // once upload has stopped
            xhr.onreadystatechange = () => {

                // Ignore state changes prior to completion.
                if (xhr.readyState !== 4)
                    return;

                // Resolve if last chunk or begin next chunk if HTTP status
                // code indicates success.
                if (xhr.status >= 200 && xhr.status < 300) {
                    offset += this.CHUNK_SIZE;

                    if (offset < file.size)
                        uploadHandler(offset);
                    else
                        resolve();
                }

                // Parse and reject with resulting JSON error
                else if (xhr.getResponseHeader('Content-Type') === 'application/json')
                    reject(new Error(JSON.parse(xhr.responseText)));

                // Warn of lack of permission of a proxy rejects the upload
                else if (xhr.status >= 400 && xhr.status < 500)
                    reject(new Error({
                        type      : Error.Type.STREAM_ERROR,
                        statusCode: Guacamole.Status.Code.CLIENT_FORBIDDEN,
                        message   : 'HTTP ' + xhr.status
                    }));

                // Assume internal error for all other cases
                else
                    reject(new Error({
                        type      : Error.Type.STREAM_ERROR,
                        statusCode: Guacamole.Status.Code.SERVER_ERROR, // TODO: Guacamole.Status.Code.INTERNAL_ERROR does not exist
                        message   : 'HTTP ' + xhr.status
                    }));

            };

            // Perform upload
            xhr.send(chunk);

        };

        /**
         * Handles the recursive upload process. Each time it is called, a
         * chunk is made with createChunk(), starting at the offset parameter.
         * The chunk is then sent by uploadChunk(), which recursively calls
         * this handler until the upload process is either completed and the
         * promise is resolved, or fails and the promise is rejected.
         *
         * @param offset
         *      The byte at which to begin the chunk.
         */
        const uploadHandler = (offset: number): void => {
            uploadChunk(createChunk(offset), offset);
        };

        uploadHandler(0);

        return deferred;

    }


}
