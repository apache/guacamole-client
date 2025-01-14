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
import { NavigationEnd, Router } from '@angular/router';
import { filter, Observable, throwError } from 'rxjs';
import { RequestService } from '../../rest/service/request.service';
import { SessionStorageEntry, SessionStorageFactory } from '../../storage/session-storage-factory.service';
import { Notification } from '../types/Notification';
import { NotificationAction } from '../types/NotificationAction';

/**
 * Service for displaying notifications and modal status dialogs.
 */
@Injectable({
    providedIn: 'root'
})
export class GuacNotificationService {

    /**
     * Getter/setter which retrieves or sets the current status notification,
     * which may simply be false if no status is currently shown.
     */
    private storedStatus: SessionStorageEntry<Notification | boolean | Object>
        = this.sessionStorageFactory.create<Notification | boolean | Object>(false);

    /**
     * Inject required services.
     */
    constructor(private router: Router,
                private requestService: RequestService,
                private sessionStorageFactory: SessionStorageFactory) {

        // Hide status upon navigation
        this.router.events
            .pipe(filter(event => event instanceof NavigationEnd))
            .subscribe(() => this.showStatus(false));

    }

    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    readonly ACKNOWLEDGE_ACTION: NotificationAction = {
        name    : 'APP.ACTION_ACKNOWLEDGE',
        callback: () => {
            this.showStatus(false);
        }
    }

    /**
     * Retrieves the current status notification, which may simply be false if
     * no status is currently shown.
     */
    getStatus(): Notification | boolean | Object {
        return this.storedStatus();
    }

    /**
     * Shows or hides the given notification as a modal status. If a status
     * notification is currently shown, no further statuses will be shown
     * until the current status is hidden.
     *
     * @param status
     *     The status notification to show.
     *
     * @example
     *
     * // To show a status message with actions
     * guacNotification.showStatus({
     *     'title'      : 'Disconnected',
     *     'text'       : {
     *         'key' : 'NAMESPACE.SOME_TRANSLATION_KEY'
     *     },
     *     'actions'    : [{
     *         'name'       : 'reconnect',
     *         'callback'   : function () {
     *             // Reconnection code goes here
     *         }
     *     }]
     * });
     *
     * // To hide the status message
     * guacNotification.showStatus(false);
     */
    showStatus(status: Notification | boolean | Object): void {
        if (!this.storedStatus() || !status)
            this.storedStatus(status);
    }

    /**
     * Observable error callback which displays a modal notification for all
     * failures due to REST errors. The message displayed to the user within
     * the notification is provided by the contents of the @link{Error} object
     * within the REST response. All other errors, such as those due to
     * JavaScript errors, are logged to the browser console without displaying
     * any notification.
     */
    readonly SHOW_REQUEST_ERROR: (error: any) => Observable<any> = this.requestService.createErrorCallback(error => {
        this.showStatus({
            className: 'error',
            title    : 'APP.DIALOG_HEADER_ERROR',
            text     : error.translatableMessage,
            actions  : [this.ACKNOWLEDGE_ACTION]
        });

        return throwError(() => error);
    });

}
