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

import { Component, Input, ViewEncapsulation } from '@angular/core';
import { AuthenticationService } from '../../../auth/service/authentication.service';

/**
 * The component for the session recording player page.
 */
@Component({
    selector: 'guac-connection-history-player',
    templateUrl: './connection-history-player.component.html',
    encapsulation: ViewEncapsulation.None
})
export class ConnectionHistoryPlayerComponent {

    /**
     * The data source of the session recording to play.
     */
    @Input({required: true}) dataSource!: string;

    /**
     * The identifier of the connection associated with the session
     * recording to play.
     */
    @Input({required: true}) identifier!: string;

    /**
     * The name of the session recording to play.
     */
    @Input({required: true}) name!: string;

    /**
     * The URL of the REST API resource exposing the requested session
     * recording.
     */
    private readonly recordingURL = 'api/session/data/' + encodeURIComponent(this.dataSource)
        + '/history/connections/' + encodeURIComponent(this.identifier)
        + '/logs/' + encodeURIComponent(this.name);

    /**
     * The tunnel which should be used to download the Guacamole session
     * recording.
     */
    readonly tunnel: Guacamole.Tunnel = new Guacamole.StaticHTTPTunnel(this.recordingURL, false, {
        'Guacamole-Token': this.authenticationService.getCurrentToken()
    });

    /**
     * Inject required services.
     */
    constructor(private authenticationService: AuthenticationService) {
    }

}
