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
    AfterViewChecked,
    Component,
    computed,
    DestroyRef,
    Input,
    OnChanges,
    OnDestroy,
    OnInit,
    Signal,
    signal,
    SimpleChanges,
    ViewChild,
    ViewEncapsulation
} from '@angular/core';
import { AuthenticationService } from '../../../auth/service/authentication.service';
import { ClipboardService } from '../../../clipboard/services/clipboard.service';
import { GuacFrontendEventArguments } from '../../../events/types/GuacFrontendEventArguments';
import { ConnectionGroupService } from '../../../rest/service/connection-group.service';
import { GuacEventService, ScrollState } from 'guacamole-frontend-lib';
import { DataSourceService } from '../../../rest/service/data-source-service.service';
import { PreferenceService } from '../../../settings/services/preference.service';
import { RequestService } from '../../../rest/service/request.service';
import { TunnelService } from '../../../rest/service/tunnel.service';
import { UserPageService } from '../../../manage/services/user-page.service';
import { ClientMenu } from '../../types/ClientMenu';
import { ManagedClient } from '../../types/ManagedClient';
import { ManagedClientService } from '../../services/managed-client.service';
import { ManagedClientGroup } from '../../types/ManagedClientGroup';
import { ConnectionListContext } from '../../types/ConnectionListContext';
import { ActivatedRoute, Router } from '@angular/router';
import { GuacClientManagerService } from '../../services/guac-client-manager.service';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import _filter from 'lodash/filter';
import findIndex from 'lodash/findIndex';
import findKey from 'lodash/findKey';
import intersection from 'lodash/intersection';
import isEmpty from 'lodash/isEmpty';
import pull from 'lodash/pull';
import { pairwise, startWith, tap } from 'rxjs';
import { ConnectionGroup } from '../../../rest/types/ConnectionGroup';
import { SharingProfile } from '../../../rest/types/SharingProfile';
import { ManagedClientState } from '../../types/ManagedClientState';
import { ManagedFilesystem } from '../../types/ManagedFilesystem';
import { ManagedFilesystemService } from '../../services/managed-filesystem.service';
import { NotificationAction } from '../../../notification/types/NotificationAction';
import { Protocol } from '../../../rest/types/Protocol';
import { FormGroup } from '@angular/forms';
import { FormService } from '../../../form/service/form.service';
import { ConnectionGroupDataSource } from "../../../group-list/types/ConnectionGroupDataSource";
import { FilterService } from "../../../list/services/filter.service";
import {
    GuacGroupListFilterComponent
} from "../../../group-list/components/guac-group-list-filter/guac-group-list-filter.component";
import { Title } from "@angular/platform-browser";

/**
 * The Component for the page used to connect to a connection or balancing group.
 */
@Component({
    selector: 'guac-client-page',
    templateUrl: './client-page.component.html',
    encapsulation: ViewEncapsulation.None
})
export class ClientPageComponent implements OnInit, OnChanges, AfterViewChecked, OnDestroy {

    /**
     * TODO
     */
    @Input({required: true, alias: 'id'}) groupId!: string;

    /**
     * The minimum number of pixels a drag gesture must move to result in the
     * menu being shown or hidden.
     */
    private readonly MENU_DRAG_DELTA: number = 64;

    /**
     * The maximum X location of the start of a drag gesture for that gesture
     * to potentially show the menu.
     */
    private readonly MENU_DRAG_MARGIN: number = 64;

    /**
     * When showing or hiding the menu via a drag gesture, the maximum number
     * of pixels the touch can move vertically and still affect the menu.
     */
    private readonly MENU_DRAG_VERTICAL_TOLERANCE: number = 10;

    /**
     * In order to open the guacamole menu, we need to hit ctrl-alt-shift. There are
     * several possible keysysms for each key.
     */
    private readonly SHIFT_KEYS: Record<number, boolean> = {
        0xFFE1: true,
        0xFFE2: true
    };
    private readonly ALT_KEYS: Record<number, boolean> = {
        0xFFE9: true, 0xFFEA: true, 0xFE03: true,
        0xFFE7: true, 0xFFE8: true
    };
    private readonly CTRL_KEYS: Record<number, boolean> = {
        0xFFE3: true,
        0xFFE4: true
    };
    private readonly MENU_KEYS: Record<number, boolean> = {
        ...this.SHIFT_KEYS,
        ...this.ALT_KEYS,
        ...this.CTRL_KEYS
    };

    /**
     * Keysym for detecting any END key presses, for the purpose of passing through
     * the Ctrl-Alt-Del sequence to a remote system.
     */
    private readonly END_KEYS: Record<number, boolean> = {
        0xFF57: true,
        0xFFB1: true
    };

    /**
     * Keysym for sending the DELETE key when the Ctrl-Alt-End hotkey
     * combo is pressed.
     */
    private readonly DEL_KEY: number = 0xFFFF;

    /**
     * Menu-specific properties.
     */
    protected menu: ClientMenu = {
        shown: signal(false),
        inputMethod: signal(this.preferenceService.preferences.inputMethod),
        emulateAbsoluteMouse: signal(this.preferenceService.preferences.emulateAbsoluteMouse),
        scrollState: signal(new ScrollState()),
        connectionParameters: {}
    };

    /**
     * Form group for editing connection parameters which may be modified while the connection is open.
     */
    protected connectionParameters: FormGroup = new FormGroup({});

    /**
     * The currently-focused client within the current ManagedClientGroup. If
     * there is no current group, no client is focused, or multiple clients are
     * focused, this will be null.
     */
    protected focusedClient: ManagedClient | null = null;

    /**
     * The set of clients that should be attached to the client UI. This will
     * be immediately initialized by a call to updateAttachedClients() below.
     */
    protected clientGroup: ManagedClientGroup | null = null;

    /**
     * The root connection groups of the connection hierarchy that should be
     * presented to the user for selecting a different connection, as a map of
     * data source identifier to the root connection group of that data
     * source. This will be null if the connection group hierarchy has not yet
     * been loaded or if the hierarchy is inapplicable due to only one
     * connection or balancing group being available.
     */
    protected rootConnectionGroups: Record<string, ConnectionGroup> | null = null;

    /**
     * Filtered view of the root connection groups which satisfy the current
     * search string.
     */
    protected rootConnectionGroupsDataSource: ConnectionGroupDataSource;

    /**
     * Array of all connection properties that are filterable.
     */
    private filteredConnectionProperties: string[] = [
        'name'
    ];

    /**
     * Array of all connection group properties that are filterable.
     */
    private filteredConnectionGroupProperties: string[] = [
        'name'
    ];

    /**
     * Map of all available sharing profiles for the current connection by
     * their identifiers. If this information is not yet available, or no such
     * sharing profiles exist, this will be an empty object.
     */
    protected sharingProfiles: Record<string, SharingProfile> = {};

    /**
     * Map of all substituted key presses.  If one key is pressed in place of another
     * the value of the substituted key is stored in an object with the keysym of
     * the original key.
     */
    private substituteKeysPressed: Record<number, number> = {};


    /**
     * Inject required services.
     */
    constructor(private authenticationService: AuthenticationService,
                private connectionGroupService: ConnectionGroupService,
                private clipboardService: ClipboardService,
                private dataSourceService: DataSourceService,
                private guacClientManager: GuacClientManagerService,
                // TODO: private iconService: IconService,
                private preferenceService: PreferenceService,
                private requestService: RequestService,
                private tunnelService: TunnelService,
                private userPageService: UserPageService,
                private managedClientService: ManagedClientService,
                private router: Router,
                private route: ActivatedRoute,
                private destroyRef: DestroyRef,
                private guacEventService: GuacEventService<GuacFrontendEventArguments>,
                private managedFilesystemService: ManagedFilesystemService,
                private formService: FormService,
                private filterService: FilterService,
                private title: Title) {

        // Create a new data source for the root connection groups
        this.rootConnectionGroupsDataSource = new ConnectionGroupDataSource(this.filterService,
            {}, // Start without any data
            null, // Start without a search string
            this.filteredConnectionProperties,
            this.filteredConnectionGroupProperties);

        // Automatically refresh display when filesystem menu is shown
        toObservable(this.menu.shown).pipe(takeUntilDestroyed()).subscribe(() => {
            // Refresh filesystem, if defined
            const filesystem = this.filesystemMenuContents;
            if (filesystem)
                managedFilesystemService.refresh(filesystem, filesystem.currentDirectory());
        });

    }

    ngOnInit(): void {

        // Init sets of clients based on current URL.
        this.reparseRoute();

        // Retrieve root groups and all descendants
        this.dataSourceService.apply(
            (dataSource: string, connectionGroupID: string) => this.connectionGroupService.getConnectionGroupTree(dataSource, connectionGroupID),
            this.authenticationService.getAvailableDataSources(),
            ConnectionGroup.ROOT_IDENTIFIER
        )
            .then(rootConnectionGroups => {

                // Store retrieved groups only if there are multiple connections or
                // balancing groups available
                const clientPages = this.userPageService.getClientPages(rootConnectionGroups);
                if (clientPages.length > 1) {
                    this.rootConnectionGroups = rootConnectionGroups;
                    this.rootConnectionGroupsDataSource.updateSource(this.rootConnectionGroups);
                }

            }, this.requestService.WARN);


        // Automatically track and cache the currently-focused client
        this.guacEventService.on('guacClientFocused')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({newFocusedClient}) => {

                const oldFocusedClient = this.focusedClient;
                this.focusedClient = newFocusedClient;

                // Apply any parameter changes when focus is changing
                if (oldFocusedClient)
                    this.applyParameterChanges(oldFocusedClient);

                // Update available connection parameters, if there is a focused
                // client
                this.menu.connectionParameters = newFocusedClient ?
                    this.managedClientService.getArgumentModel(newFocusedClient) : {};

            });

        // Opening the Guacamole menu after Ctrl+Alt+Shift, preventing those
        // keypresses from reaching any Guacamole client
        this.guacEventService.on('guacBeforeKeydown')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({event, keyboard}) => {

                // Toggle menu if menu shortcut (Ctrl+Alt+Shift) is pressed
                if (this.isMenuShortcutPressed(keyboard)) {

                    // Don't send this key event through to the client, and release
                    // all other keys involved in performing this shortcut
                    event.preventDefault();
                    keyboard.reset();

                    // Toggle the menu
                    this.menu.shown.update(shown => !shown);

                }

                // Prevent all keydown events while menu is open
                else if (this.menu.shown())
                    event.preventDefault();

            });

        // Prevent all keyup events while menu is open
        this.guacEventService.on('guacBeforeKeyup')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({event}) => {
                if (this.menu.shown())
                    event.preventDefault();
            });

        // Send Ctrl-Alt-Delete when Ctrl-Alt-End is pressed.
        this.guacEventService.on('guacKeydown')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({event, keysym, keyboard}) => {

                // If one of the End keys is pressed, and we have a one keysym from each
                // of Ctrl and Alt groups, send Ctrl-Alt-Delete.
                if (this.END_KEYS[keysym as any]
                    && findKey(this.ALT_KEYS, (val, keysym) => keyboard.pressed[keysym as any])
                    && findKey(this.CTRL_KEYS, (val, keysym) => keyboard.pressed[keysym as any])
                ) {

                    // Don't send this event through to the client.
                    event.preventDefault();

                    // Record the substituted key press so that it can be
                    // properly dealt with later.
                    this.substituteKeysPressed[keysym] = this.DEL_KEY;

                    // Send through the delete key.
                    this.guacEventService.broadcast('guacSyntheticKeydown', {keysym: this.DEL_KEY});
                }

            });

        // Update pressed keys as they are released
        this.guacEventService.on('guacKeyup')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({event, keysym}) => {

                // Deal with substitute key presses
                if (this.substituteKeysPressed[keysym]) {
                    event.preventDefault();
                    this.guacEventService.broadcast('guacSyntheticKeyup', {keysym: this.substituteKeysPressed[keysym]});
                    delete this.substituteKeysPressed[keysym];
                }

            });

    } // ngOnInit() end

    /**
     * Add the filter string observable to the data source when the filter
     * component is available.
     *
     * @param filterComponent
     *     The filter component which will provide the filter string.
     */
    @ViewChild(GuacGroupListFilterComponent, {static: false}) set filterComponent(filterComponent: GuacGroupListFilterComponent | undefined) {

        if (filterComponent) {
            this.rootConnectionGroupsDataSource.setSearchString(filterComponent.searchStringChange);
        }

    }

    ngOnChanges(changes: SimpleChanges): void {

        // Re-initialize the client group if the group id has changed without reloading the route
        if (changes['groupId'])
            this.reparseRoute();


        if (changes['focusedClient']) {

            /**
             * TODO: Document
             */
            if (this.focusedClient) {
                this.connectionParameters = this.formService.getFormGroup(this.focusedClient.forms);
                this.connectionParameters.valueChanges.pipe(takeUntilDestroyed(this.destroyRef))
                    .subscribe(value => this.menu.connectionParameters = value);
            } else {
                this.connectionParameters = new FormGroup({});
            }

        }

    }


    /**
     * Convenience method for closing the menu.
     */
    closeMenu() {
        this.menu.shown.set(false);
    }

    /**
     * Applies any changes to connection parameters made by the user within the
     * Guacamole menu to the given ManagedClient. If no client is supplied,
     * this function has no effect.
     *
     * @param client
     *     The client to apply parameter changes to.
     */
    applyParameterChanges(client: ManagedClient | null): void {
        for (const name in this.menu.connectionParameters) {
            const value = this.menu.connectionParameters[name];
            if (client)
                this.managedClientService.setArgument(client, name, value);
        }
    }

    /**
     * @borrows ManagedClientGroup.getName
     */
    getName(group: ManagedClientGroup): string {
        return ManagedClientGroup.getName(group);
    }

    /**
     * Arbitrary context that should be exposed to the guacGroupList directive
     * displaying the dropdown list of available connections within the
     * Guacamole menu.
     */
    connectionListContext: ConnectionListContext = {
        attachedClients: {},
        updateAttachedClients: id => {
            this.addRemoveClient(id, !this.connectionListContext.attachedClients[id]);
        }
    };

    /**
     * Adds or removes the client with the given ID from the set of clients
     * within the current view, updating the current URL accordingly.
     *
     * @param id
     *     The ID of the client to add or remove from the current view.
     *
     * @param remove=false
     *     Whether the specified client should be added (false) or removed
     *     (true).
     */
    addRemoveClient(id: string, remove = false): void {

        // Deconstruct current path into corresponding client IDs
        const ids = ManagedClientGroup.getClientIdentifiers(this.groupId);

        // Add/remove ID as requested
        if (remove)
            pull(ids, id);
        else
            ids.push(id);

        // Reconstruct path, updating attached clients via change in route
        this.router.navigate(['/client', ManagedClientGroup.getIdentifier(ids)]);

    }

    /**
     * Reloads the contents of this.clientGroup to reflect the client IDs
     * currently listed in the URL.
     */
    private reparseRoute(): void {

        const previousClients = this.clientGroup ? this.clientGroup.clients.slice() : [];

        // Replace existing group with new group
        this.setAttachedGroup(this.guacClientManager.getManagedClientGroup(this.groupId));

        // Store current set of attached clients for later use within the
        // Guacamole menu
        this.connectionListContext.attachedClients = {};
        this.clientGroup?.clients.forEach((client) => {
            this.connectionListContext.attachedClients[client.id] = true;
        });

        // Ensure menu is closed if updated view is not a modification of the
        // current view (has no clients in common). The menu should remain open
        // only while the current view is being modified, not when navigating
        // to an entirely different view.
        if (isEmpty(intersection(previousClients, this.clientGroup!.clients)))
            this.menu.shown.set(false);

        // Update newly-attached clients with current contents of clipboard
        this.clipboardService.resyncClipboard();

    }

    /**
     * Replaces the ManagedClientGroup currently attached to the client
     * interface via $scope.clientGroup with the given ManagedClientGroup,
     * safely cleaning up after the previous group. If no ManagedClientGroup is
     * provided, the existing group is simply removed.
     *
     * @param managedClientGroup
     *     The ManagedClientGroup to attach to the interface, if any.
     */
    private setAttachedGroup(managedClientGroup?: ManagedClientGroup | null): void {

        // Do nothing if group is not actually changing
        if (this.clientGroup === managedClientGroup)
            return;

        if (this.clientGroup) {

            // Remove all disconnected clients from management (the user has
            // seen their status)
            _filter(this.clientGroup.clients, client => {

                const connectionState = client.clientState.connectionState;
                return connectionState === ManagedClientState.ConnectionState.DISCONNECTED
                    || connectionState === ManagedClientState.ConnectionState.TUNNEL_ERROR
                    || connectionState === ManagedClientState.ConnectionState.CLIENT_ERROR;

            }).forEach(client => {
                this.guacClientManager.removeManagedClient(client.id);
            });

            // Flag group as detached
            this.clientGroup.attached = false;

        }

        if (managedClientGroup) {
            this.clientGroup = managedClientGroup;
            this.clientGroup.attached = true;
            this.clientGroup.lastUsed = new Date().getTime();
        }

    }

    /**
     * Returns whether the shortcut for showing/hiding the Guacamole menu
     * (Ctrl+Alt+Shift) has been pressed.
     *
     * @param keyboard
     *     The Guacamole.Keyboard object tracking the local keyboard state.
     *
     * @returns
     *     true if Ctrl+Alt+Shift has been pressed, false otherwise.
     */
    private isMenuShortcutPressed(keyboard: Guacamole.Keyboard): boolean {

        // Ctrl+Alt+Shift has NOT been pressed if any key is currently held
        // down that isn't Ctrl, Alt, or Shift
        if (findKey(keyboard.pressed, (val, keysym) => !this.MENU_KEYS[Number(keysym)]))
            return false;

        // Verify that one of each required key is held, regardless of
        // left/right location on the keyboard
        return !!(
            findKey(this.SHIFT_KEYS, (val, keysym: any) => keyboard.pressed[keysym])
            && findKey(this.ALT_KEYS, (val, keysym: any) => keyboard.pressed[keysym])
            && findKey(this.CTRL_KEYS, (val, keysym: any) => keyboard.pressed[keysym])
        );

    }

    // Show menu if the user swipes from the left, hide menu when the user
    // swipes from the right, scroll menu while visible
    menuDrag(inProgress: boolean, startX: number, startY: number, currentX: number, currentY: number, deltaX: number, deltaY: number): boolean {

        if (this.menu.shown()) {

            // Hide menu if swipe-from-right gesture is detected
            if (Math.abs(currentY - startY) < this.MENU_DRAG_VERTICAL_TOLERANCE
                && startX - currentX >= this.MENU_DRAG_DELTA)
                this.menu.shown.set(false);

            // Scroll menu by default
            else {
                this.menu.scrollState.mutate(scrollState => {
                    scrollState.left -= deltaX;
                    scrollState.top -= deltaY;
                });
            }

        }

        // Show menu if swipe-from-left gesture is detected
        else if (startX <= this.MENU_DRAG_MARGIN) {
            if (Math.abs(currentY - startY) < this.MENU_DRAG_VERTICAL_TOLERANCE
                && currentX - startX >= this.MENU_DRAG_DELTA)
                this.menu.shown.set(true);
        }

        return false;

    }

    /**
     * Controls whether the on-screen keyboard is shown.
     */
    showOSK: Signal<boolean> = computed(() => this.menu.inputMethod() === 'osk');

    /**
     * Controls whether the text input field is shown.
     */
    showTextInput: Signal<boolean> = computed(() => this.menu.inputMethod() === 'text');

    /**
     * Update client state/behavior as visibility of the Guacamole menu changes
     */
    private readonly menuVisibilityChanged = toObservable(this.menu.shown)
        .pipe(
            takeUntilDestroyed(this.destroyRef),
            startWith(this.menu.shown()),
            pairwise(),
            tap(([menuShownPreviousState, menuShown]) => {

                // Re-update available connection parameters, if there is a focused
                // client (parameter information may not have been available at the
                // time focus changed)
                if (menuShown)
                    this.menu.connectionParameters = this.focusedClient ?
                        this.managedClientService.getArgumentModel(this.focusedClient) : {};

                // Send any argument value data once menu is hidden
                else if (menuShownPreviousState)
                    this.applyParameterChanges(this.focusedClient);

            })
        ).subscribe();


    // TODO: Update page icon when thumbnail changes
    // $scope.$watch('focusedClient.thumbnail.canvas', function thumbnailChanged(canvas) {
    //     iconService.setIcons(canvas);
    // });

    // TODO: Pull sharing profiles once the tunnel UUID is known
    // $scope.$watch('focusedClient.tunnel.uuid', function retrieveSharingProfiles(uuid) {
    //
    //     // Only pull sharing profiles if tunnel UUID is actually available
    //     if (!uuid) {
    //         $scope.sharingProfiles = {};
    //         return;
    //     }
    //
    //     // Pull sharing profiles for the current connection
    //     tunnelService.getSharingProfiles(uuid)
    //         .then(function sharingProfilesRetrieved(sharingProfiles) {
    //             $scope.sharingProfiles = sharingProfiles;
    //         }, requestService.WARN);
    //
    // });

    /**
     * Produces a sharing link for the current connection using the given
     * sharing profile. The resulting sharing link, and any required login
     * information, will be displayed to the user within the Guacamole menu.
     *
     * @param sharingProfile
     *     The sharing profile to use to generate the sharing link.
     */
    share(sharingProfile: SharingProfile): void {
        if (this.focusedClient)
            this.managedClientService.createShareLink(this.focusedClient, sharingProfile);
    }

    /**
     * Returns whether the current connection has any associated share links.
     *
     * @returns
     *     true if the current connection has at least one associated share
     *     link, false otherwise.
     */
    isShared(): boolean {
        return !!this.focusedClient && this.managedClientService.isShared(this.focusedClient);
    }

    /**
     * Returns the total number of share links associated with the current
     * connection.
     *
     * @returns
     *     The total number of share links associated with the current
     *     connection.
     */
    getShareLinkCount(): number {

        if (!this.focusedClient)
            return 0;

        // Count total number of links within the ManagedClient's share link map
        let linkCount = 0;
        for (const dummy in this.focusedClient.shareLinks)
            linkCount++;

        return linkCount;

    }

    /**
     * Update page title when client title changes.
     */
    ngAfterViewChecked(): void {
        if (!this.clientGroup)
            return;

        const title = ManagedClientGroup.getTitle(this.clientGroup);

        if (!title || title === this.title.getTitle())
            return;

        this.title.setTitle(title);
    }

    /**
     * Returns whether the current connection has been flagged as unstable due
     * to an apparent network disruption.
     *
     * @returns
     *     true if the current connection has been flagged as unstable, false
     *     otherwise.
     */
    isConnectionUnstable(): boolean {
        return findIndex(this.clientGroup?.clients, client => client.clientState.tunnelUnstable) !== -1;
    }

    /**
     * Immediately disconnects all currently-focused clients, if any.
     */
    disconnect(): void {

        // Disconnect if client is available
        if (this.clientGroup) {
            this.clientGroup.clients.forEach(client => {
                if (client.clientProperties.focused)
                    client.client.disconnect();
            });
        }

        // Hide menu
        this.menu.shown.set(false);

    }

    /**
     * Disconnects the given ManagedClient, removing it from the current
     * view.
     *
     * @param client
     *     The client to disconnect.
     */
    closeClientTile(client: ManagedClient): void {

        this.addRemoveClient(client.id, true);
        this.guacClientManager.removeManagedClient(client.id);

        // Ensure at least one client has focus (the only client with
        // focus may just have been removed)
        ManagedClientGroup.verifyFocus(this.clientGroup);

    }

    /**
     * Action which immediately disconnects the currently-connected client, if
     * any.
     */
    private DISCONNECT_MENU_ACTION: NotificationAction = {
        name: 'CLIENT.ACTION_DISCONNECT',
        className: 'danger disconnect',
        callback: () => this.disconnect()
    };

    // Set client-specific menu actions
    clientMenuActions: NotificationAction[] = [this.DISCONNECT_MENU_ACTION];

    /**
     * @borrows Protocol.getNamespace
     */
    getProtocolNamespace(protocolName: string): string | undefined {
        return Protocol.getNamespace(protocolName);
    }

    /**
     * The currently-visible filesystem within the filesystem menu, if the
     * filesystem menu is open. If no filesystem is currently visible, this
     * will be null.
     */
    filesystemMenuContents: ManagedFilesystem | null = null;

    /**
     * Hides the filesystem menu.
     */
    hideFilesystemMenu(): void {
        this.filesystemMenuContents = null;
    }

    /**
     * Shows the filesystem menu, displaying the contents of the given
     * filesystem within it.
     *
     * @param filesystem
     *     The filesystem to show within the filesystem menu.
     */
    showFilesystemMenu(filesystem: ManagedFilesystem): void {
        this.filesystemMenuContents = filesystem;
    }

    /**
     * Returns whether the filesystem menu should be visible.
     *
     * @returns
     *     true if the filesystem menu is shown, false otherwise.
     */
    isFilesystemMenuShown(): boolean {
        return !!this.filesystemMenuContents && this.menu.shown();
    }

    /**
     * Returns the full path to the given file as an ordered array of parent
     * directories.
     *
     * @param file
     *     The file whose full path should be retrieved.
     *
     * @returns
     *     An array of directories which make up the hierarchy containing the
     *     given file, in order of increasing depth.
     */
    getPath(file: ManagedFilesystem.File): ManagedFilesystem.File[] {

        const path = [];

        // Add all files to path in ascending order of depth
        while (file && file.parent) {
            path.unshift(file);
            file = file.parent;
        }

        return path;

    }

    /**
     * Changes the current directory of the given filesystem to the given
     * directory.
     *
     * @param filesystem
     *     The filesystem whose current directory should be changed.
     *
     * @param file
     *     The directory to change to.
     */
    changeDirectory(filesystem: ManagedFilesystem, file: ManagedFilesystem.File): void {
        this.managedFilesystemService.changeDirectory(filesystem, file);
    }

    /**
     * Begins a file upload through the attached Guacamole client for
     * each file in the given FileList.
     *
     * @param files
     *     The files to upload.
     */
    uploadFiles(files: FileList): void {

        // Upload each file
        for (let i = 0; i < files.length; i++)
            this.managedClientService.uploadFile(this.filesystemMenuContents!.client, files[i], this.filesystemMenuContents!);

    }

    /**
     * Determines whether the attached client group has any associated file
     * transfers, regardless of those file transfers' state.
     *
     * @returns
     *     true if there are any file transfers associated with the
     *     attached client group, false otherise.
     */
    hasTransfers(): boolean {

        // There are no file transfers if there is no client group
        if (!this.clientGroup)
            return false;

        return findIndex(this.clientGroup.clients, this.managedClientService.hasTransfers) !== -1;

    }

    /**
     * Returns whether the current user can share the current connection with
     * other users. A connection can be shared if and only if there is at least
     * one associated sharing profile.
     *
     * @returns
     *     true if the current user can share the current connection with other
     *     users, false otherwise.
     */
    canShareConnection(): boolean {

        // If there is at least one sharing profile, the connection can be shared
        for (const dummy in this.sharingProfiles)
            return true;

        // Otherwise, sharing is not possible
        return false;

    }

    /**
     * Clean up when component is destroyed.
     */
    ngOnDestroy(): void {
        this.setAttachedGroup(null);
    }

}

