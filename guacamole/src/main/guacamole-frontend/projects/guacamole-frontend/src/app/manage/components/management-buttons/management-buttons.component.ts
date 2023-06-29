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

import { Component, Input, OnInit, ViewEncapsulation } from '@angular/core';
import { ManagementPermissions } from '../../types/ManagementPermissions';
import { Observable } from 'rxjs';
import { MenuAction } from '../../../navigation/types/MenuAction';
import { GuacNotificationService } from '../../../notification/services/guac-notification.service';

/**
 * Component which displays a set of object management buttons (save, delete,
 * clone, etc.) representing the actions available to the current user in
 * context of the object being edited/created.
 */
@Component({
    selector: 'management-buttons',
    templateUrl: './management-buttons.component.html',
    encapsulation: ViewEncapsulation.None
})
export class ManagementButtonsComponent implements OnInit {

    /**
     * The translation namespace associated with all applicable
     * translation strings. This directive requires at least the
     * following translation strings within the given namespace:
     *
     *     - ACTION_CANCEL
     *     - ACTION_CLONE
     *     - ACTION_DELETE
     *     - ACTION_SAVE
     *     - DIALOG_HEADER_CONFIRM_DELETE
     *     - TEXT_CONFIRM_DELETE
     */
    @Input({required: true}) namespace!: string;

    /**
     * The permissions which dictate the management actions available
     * to the current user.
     */
    @Input({required: true}) permissions!: ManagementPermissions;

    /**
     * The function to invoke to save the arbitrary object being edited
     * if the current user has permission to do so. The provided
     * function MUST return an observable which completes if the save
     * operation succeeds and fail with an {@link Error} if the
     * save operation fails.
     */
    @Input({required: true}) save!: () => Observable<void>;

    /**
     * The function to invoke when the current user chooses to clone
     * the object being edited. The provided function MUST perform the
     * actions necessary to produce an interface which will clone the
     * object.
     */
    @Input({required: true}) clone!: () => void;

    /**
     * The function to invoke to delete the arbitrary object being edited
     * if the current user has permission to do so. The provided
     * function MUST return a promise which is resolved if the delete
     * operation succeeds and is rejected with an {@link Error} if the
     * delete operation fails.
     */
    @Input({required: true}) delete!: () => Observable<void>;

    /**
     * The function to invoke when the current user chooses to cancel
     * the edit in progress, or when a save/delete operation has
     * succeeded. The provided function MUST perform the actions
     * necessary to return the user to a reasonable starting point.
     */
    @Input({required: true}) return!: () => void;

    /**
     * An action to be provided along with the object sent to showStatus which
     * immediately deletes the current connection.
     */
    private DELETE_ACTION!: MenuAction;

    /**
     * An action to be provided along with the object sent to showStatus which
     * closes the currently-shown status dialog.
     */
    private CANCEL_ACTION!: MenuAction;

    /**
     * Inject required services.
     */
    constructor(private notificationService: GuacNotificationService) {
    }

    ngOnInit(): void {

        this.DELETE_ACTION = {
            name: this.namespace + '.ACTION_DELETE',
            className: 'danger',
            callback: () => {
                this.deleteObjectImmediately();
                this.notificationService.showStatus(false);
            }
        };

        this.CANCEL_ACTION = {
            name: this.namespace + '.ACTION_CANCEL',
            callback: () => {
                this.notificationService.showStatus(false);
            }
        };

    }

    /**
     * Invokes the provided return function to navigate the user back to
     * the page they started from.
     */
    navigateBack(): void {
        this.return();
    }

    /**
     * Invokes the provided delete function, immediately deleting the
     * current object without prompting the user for confirmation. If
     * deletion is successful, the user is navigated back to the page they
     * started from. If the deletion fails, an error notification is
     * displayed.
     */
    private deleteObjectImmediately(): void {
        this.delete().subscribe({
            next: () => this.navigateBack(),
            error: this.notificationService.SHOW_REQUEST_ERROR
        });
    }

    /**
     * Cancels all pending edits, invoking the provided clone function to
     * open an edit page for a new object which is prepopulated with the
     * data from the current object.
     */
    cloneObject(): void {
        this.clone();
    }

    /**
     * Invokes the provided save function to save the current object. If
     * saving is successful, the user is navigated back to the page they
     * started from. If saving fails, an error notification is displayed.
     */
    saveObject(): void {
        this.save().subscribe({
            next: () => this.navigateBack(),
            error: this.notificationService.SHOW_REQUEST_ERROR
        });
    }

    /**
     * Deletes the current object, prompting the user first to confirm that
     * deletion is desired. If the user confirms that deletion is desired,
     * the object is deleted through invoking the provided delete function.
     * The user is automatically navigated back to the page they started
     * from or given an error notification depending on whether deletion
     * succeeds.
     */
    deleteObject(): void {

        // Confirm deletion request
        this.notificationService.showStatus({
            title: this.namespace + '.DIALOG_HEADER_CONFIRM_DELETE',
            text: {key: this.namespace + '.TEXT_CONFIRM_DELETE'},
            actions: [this.DELETE_ACTION, this.CANCEL_ACTION]
        });

    }

}
