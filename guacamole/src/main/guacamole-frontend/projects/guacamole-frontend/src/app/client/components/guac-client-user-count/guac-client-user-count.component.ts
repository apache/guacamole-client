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

import {
    AfterViewInit,
    Component,
    DoCheck,
    ElementRef,
    Input,
    KeyValueDiffer,
    KeyValueDiffers,
    OnChanges,
    Renderer2,
    SimpleChanges,
    ViewChild,
    ViewEncapsulation
} from '@angular/core';
import { TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs';
import { AuthenticationResult } from '../../../auth/types/AuthenticationResult';
import { ManagedClient } from '../../types/ManagedClient';

/**
 * A component that displays a status indicator showing the number of users
 * joined to a connection. The specific usernames of those users are visible in
 * a tooltip on mouseover, and small notifications are displayed as users
 * join/leave the connection.
 */
@Component({
    selector     : 'guac-client-user-count',
    templateUrl  : './guac-client-user-count.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacClientUserCountComponent implements AfterViewInit, OnChanges, DoCheck {

    /**
     * The client whose current users should be displayed.
     */
    @Input({ required: true }) client!: ManagedClient;

    /**
     * The maximum number of messages displayed by this directive at any
     * given time. Old messages will be discarded as necessary to ensure
     * the number of messages displayed never exceeds this value.
     */
    private readonly MAX_MESSAGES: number = 3;

    /**
     * A reference to the list element that should contain any notifications regarding users
     * joining or leaving the connection.
     */
    @ViewChild('clientUserCountMessages')
    private readonly messagesRef!: ElementRef<HTMLUListElement>;

    /**
     * The list that should contain any notifications regarding users
     * joining or leaving the connection.
     */
    private messages!: HTMLUListElement;

    /**
     * Map of the usernames of all users of the current connection to the
     * number of concurrent connections those users have to the current
     * connection.
     */
    userCounts: Record<string, number> = {};

    /**
     * The ManagedClient attached to this directive at the time the
     * notification update scope watch was last invoked. This is necessary
     * as $scope.$watchGroup() does not allow for the callback to know
     * whether the scope was previously uninitialized (it's "oldValues"
     * parameter receives a copy of the new values if there are no old
     * values).
     */
    private oldClient: ManagedClient | null = null;

    /**
     * TODO: remove
     */
    private clientDiffer?: KeyValueDiffer<string, any>;

    /**
     * Inject required services.
     */
    constructor(private translocoService: TranslocoService,
                private renderer: Renderer2,
                private differs: KeyValueDiffers) {
    }

    ngAfterViewInit(): void {
        this.messages = this.messagesRef.nativeElement;
    }

    /**
     * Displays a message noting that a change related to a particular user
     * of this connection has occurred.
     *
     * @param str
     *     The key of the translation string containing the message to
     *     display. This translation key must accept "USERNAME" as the
     *     name of the translation parameter containing the username of
     *     the user in question.
     *
     * @param username
     *     The username of the user in question.
     */
    private notify(str: string, username: string): void {
        this.translocoService.selectTranslate(str, { 'USERNAME': username })
            .pipe(take(1))
            .subscribe(text => {

                if (this.messages.childNodes.length === 3)
                    this.renderer.removeChild(this.messages, this.messages.lastChild);


                const message = this.renderer.createElement('li');
                this.renderer.addClass(message, 'client-user-count-message');
                this.renderer.setProperty(message, 'textContent', text);
                this.messages.insertBefore(message, this.messages.firstChild);

                // Automatically remove the notification after its "fadeout"
                // animation ends. NOTE: This will not fire if the element is
                // not visible at all.
                this.renderer.listen(message, 'animationend', () => {
                    this.renderer.removeChild(this.messages, message);
                });

            });
    }

    /**
     * Displays a message noting that a particular user has joined the
     * current connection.
     *
     * @param username
     *     The username of the user that joined.
     */
    private notifyUserJoined(username: string): void {
        if (this.isAnonymous(username))
            this.notify('CLIENT.TEXT_ANONYMOUS_USER_JOINED', username);
        else
            this.notify('CLIENT.TEXT_USER_JOINED', username);
    }

    /**
     * Displays a message noting that a particular user has left the
     * current connection.
     *
     * @param username
     *     The username of the user that left.
     */
    private notifyUserLeft(username: string): void {
        if (this.isAnonymous(username))
            this.notify('CLIENT.TEXT_ANONYMOUS_USER_LEFT', username);
        else
            this.notify('CLIENT.TEXT_USER_LEFT', username);
    }

    /**
     * Returns whether the given username represents an anonymous user.
     *
     * @param username
     *     The username of the user to check.
     *
     * @returns
     *     true if the given username represents an anonymous user, false
     *     otherwise.
     */
    isAnonymous(username: string): boolean {
        return username === AuthenticationResult.ANONYMOUS_USERNAME;
    }

    /**
     * Returns the translation key of the translation string that should be
     * used to render the number of connections a user with the given
     * username has to the current connection. The appropriate string will
     * vary by whether the user is anonymous.
     *
     * @param username
     *     The username of the user to check.
     *
     * @returns
     *     The translation key of the translation string that should be
     *     used to render the number of connections the user with the given
     *     username has to the current connection.
     */
    getUserCountTranslationKey(username: string): string {
        return this.isAnonymous(username) ? 'CLIENT.INFO_ANONYMOUS_USER_COUNT' : 'CLIENT.INFO_USER_COUNT';
    }

    ngOnChanges({ client }: SimpleChanges): void {
        if (client) {
            this.clientDiffer = this.differs.find(client.currentValue).create();
        }
    }

    ngDoCheck(): void {
        if (!this.clientDiffer) return;

        // TODO: Temporary workaround for $scope.$watchGroup()
        const changes = this.clientDiffer.diff(this.client);

        // TODO:  $scope.$watchGroup([ 'client', 'client.userCount' ], function usersChanged() {

        // Update visible notifications as users join/leave
        if (changes) {

            // Resynchronize directive with state of any attached client when
            // the client changes, to ensure notifications are only shown for
            // future changes in users present
            if (this.oldClient !== this.client) {

                this.userCounts = {};
                this.oldClient = this.client;

                for (const username in this.client.users) {
                    const connections = this.client.users[username];
                    const count = Object.keys(connections).length;
                    this.userCounts[username] = count;
                }

                return;

            }

            // Display join/leave notifications for users who are currently
            // connected but whose connection counts have changed
            for (const username in this.client.users) {
                const connections = this.client.users[username];

                const count = Object.keys(connections).length;
                const known = this.userCounts[username] || 0;

                if (count > known)
                    this.notifyUserJoined(username);
                else if (count < known)
                    this.notifyUserLeft(username);

                this.userCounts[username] = count;

            }

            // Display leave notifications for users who are no longer connected
            for (const username in this.userCounts) {
                const count = this.userCounts[username];
                if (!this.client.users[username]) {
                    this.notifyUserLeft(username);
                    delete this.userCounts[username];
                }
            }

        }

    }


}
