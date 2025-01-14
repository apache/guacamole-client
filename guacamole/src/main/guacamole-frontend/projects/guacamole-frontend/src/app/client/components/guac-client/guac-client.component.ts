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
import {
    Component,
    DestroyRef,
    DoCheck,
    ElementRef,
    Inject,
    Injector,
    Input,
    OnChanges,
    OnInit,
    SimpleChanges,
    ViewChild,
    ViewEncapsulation
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { GuacEventService } from 'guacamole-frontend-lib';
import {
    GuacFrontendEventArguments,
    MouseEventName,
    TouchEventName
} from '../../../events/types/GuacFrontendEventArguments';
import { ManagedClientService } from '../../services/managed-client.service';
import { ManagedClient } from '../../types/ManagedClient';
import { ManagedDisplayCursor, ManagedDisplayDimensions } from '../../types/ManagedDisplay';



/**
 * A component for the guacamole client.
 */
@Component({
    selector: 'guac-client',
    templateUrl: './guac-client.component.html',
    encapsulation: ViewEncapsulation.None
})
export class GuacClientComponent implements OnInit, OnChanges, DoCheck {

    /**
     * The client to display within this guacClient directive.
     */
    @Input({ required: true, alias: 'client' }) managedClient!: ManagedClient;

    /**
     * Whether translation of touch to mouse events should emulate an
     * absolute pointer device, or a relative pointer device.
     */
    @Input({ required: true }) emulateAbsoluteMouse!: boolean;

    /**
     * Whether the local, hardware mouse cursor is in use.
     */
    private localCursor = false;

    /**
     * The current Guacamole client instance.
     */
    private guacamoleClient: Guacamole.Client | null = null;

    /**
     * The display of the current Guacamole client instance.
     */
    private display: Guacamole.Display | null = null;

    /**
     * The element associated with the display of the current
     * Guacamole client instance.
     */
    private displayElement: Element | null = null;

    /**
     * A reference to the element which must contain the Guacamole display element.
     */
    @ViewChild('display', { static: true })
    private readonly displayContainerRef!: ElementRef<HTMLDivElement>;

    /**
     * The element which must contain the Guacamole display element.
     */
    private displayContainer!: HTMLDivElement;

    /**
     * TODO
     */
    @ViewChild('main', { static: true })
    private readonly mainRef!: ElementRef<HTMLDivElement>;

    /**
     * The main containing element for the entire directive.
     */
    private main!: HTMLDivElement;

    /**
     * Guacamole mouse event object, wrapped around the main client
     * display.
     */
    private mouse!: Guacamole.Mouse;

    /**
     * Guacamole absolute mouse emulation object, wrapped around the
     * main client display.
     */
    private touchScreen!: Guacamole.Mouse.Touchscreen;

    /**
     * Guacamole relative mouse emulation object, wrapped around the
     * main client display.
     */
    private touchPad!: Guacamole.Mouse.Touchpad;

    /**
     * Guacamole touch event handling object, wrapped around the main
     * client dislay.
     */
    private touch!: Guacamole.Touch;

    /**
     * The last known horizontal scroll position of the client view.
     */
    private lastScrollLeft?: number;


    /**
     * The last known vertical scroll position of the client view.
     */
    private lastScrollTop?: number;

    /**
     * The last known cursor.
     */
    private lastCursor?: ManagedDisplayCursor;

    /**
     * The last known display size.
     */
    private lastDisplaySize: ManagedDisplayDimensions | undefined;

    /**
     * The last known value of multi-touch support.
     */
    private lastMultiTouchSupport?: number;

    /**
     * The last known value of whether absolute mouse emulation is
     * preferred.
     */
    private lastEmulateAbsoluteMouse?: boolean;

    /**
     * The last known scale.
     */
    private lastScale?: number;

    /**
     * The last known auto-fit setting.
     */
    private lastAutoFit?: boolean;


    /**
     * Inject required services.
     */
    constructor(private managedClientService: ManagedClientService,
                private guacEventService: GuacEventService<GuacFrontendEventArguments>,
                private destroyRef: DestroyRef,
                @Inject(DOCUMENT) private document: Document) {
    }

    ngOnInit(): void {
        this.main = this.mainRef.nativeElement;
        this.displayContainer = this.displayContainerRef.nativeElement;


        this.mouse = new Guacamole.Mouse(this.displayContainer);
        this.touchScreen = new Guacamole.Mouse.Touchscreen(this.displayContainer);
        this.touchPad = new Guacamole.Mouse.Touchpad(this.displayContainer);
        this.touch = new Guacamole.Touch(this.displayContainer);

        // Ensure focus is regained via mousedown before forwarding event
        this.mouse.on('mousedown', () => this.document.body.focus.bind(this.document.body)());

        // Forward all mouse events
        this.mouse.onEach(['mousedown', 'mousemove', 'mouseup'],
            (event: Guacamole.Event) => this.handleMouseEvent(event as Guacamole.Mouse.Event));

        // Hide software cursor when mouse leaves display
        this.mouse.on('mouseout', () => {
            if (!this.display) return;
            this.display.showCursor(false);
        });

        // Update remote clipboard if local clipboard changes
        this.guacEventService.on('guacClipboard')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ data }) => {
                this.managedClientService.setClipboard(this.managedClient, data);
            });

        // Translate local keydown events to remote keydown events if keyboard is enabled
        this.guacEventService.on('guacKeydown')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ event, keysym }) => {
                if (this.managedClient.clientProperties.focused) {
                    this.guacamoleClient?.sendKeyEvent(1, keysym);
                    event.preventDefault();
                }
            });

        // Translate local keyup events to remote keyup events if keyboard is enabled
        this.guacEventService.on('guacKeyup')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ event, keysym }) => {
                if (this.managedClient.clientProperties.focused) {
                    this.guacamoleClient?.sendKeyEvent(0, keysym);
                    event.preventDefault();
                }
            });

        // Universally handle all synthetic keydown events
        this.guacEventService.on('guacSyntheticKeydown')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ keysym }) => {
                if (this.managedClient.clientProperties.focused)
                    this.guacamoleClient?.sendKeyEvent(1, keysym);
            });

        // Universally handle all synthetic keyup events
        this.guacEventService.on('guacSyntheticKeyup')
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe(({ keysym }) => {
                if (this.managedClient.clientProperties.focused)
                    this.guacamoleClient?.sendKeyEvent(0, keysym);
            });

        this.main.addEventListener('dragenter', this.notifyDragStart.bind(this), false);
        this.main.addEventListener('dragover', this.notifyDragStart.bind(this), false);
        this.main.addEventListener('dragleave', this.notifyDragEnd.bind(this), false);

        // File drop event handler
        this.main.addEventListener('drop', (e) => {

            this.notifyDragEnd(e);

            // Ignore file drops if no attached client
            if (!this.managedClient)
                return;

            // Upload each file
            const files = (e as DragEvent).dataTransfer?.files;
            if (!files) return;

            for (let i = 0; i < files.length; i++)
                this.managedClientService.uploadFile(this.managedClient, files[i]);

        }, false);


        this.attachManagedClient(this.managedClient);
    }

    /**
     * Updates the scale of the attached Guacamole.Client based on current window
     * size and "auto-fit" setting.
     */
    private updateDisplayScale(): void {

        if (!this.display) return;

        // Calculate scale to fit screen
        this.managedClient.clientProperties.minScale = Math.min(
            this.main.offsetWidth / Math.max(this.display.getWidth(), 1),
            this.main.offsetHeight / Math.max(this.display.getHeight(), 1)
        );

        // Calculate appropriate maximum zoom level
        this.managedClient.clientProperties.maxScale = Math.max(this.managedClient.clientProperties.minScale, 3);

        // Clamp zoom level, maintain auto-fit
        if (this.display.getScale() < this.managedClient.clientProperties.minScale || this.managedClient.clientProperties.autoFit)
            this.managedClient.clientProperties.scale = this.managedClient.clientProperties.minScale;

        else if (this.display.getScale() > this.managedClient.clientProperties.maxScale)
            this.managedClient.clientProperties.scale = this.managedClient.clientProperties.maxScale;

    }

    /**
     * Scrolls the client view such that the mouse cursor is visible.
     *
     * @param mouseState
     *     The current mouse state.
     */
    private scrollToMouse(mouseState: Guacamole.Mouse.State): void {

        // Determine mouse position within view
        const mouse_view_x = mouseState.x + this.displayContainer.offsetLeft - this.main.scrollLeft;
        const mouse_view_y = mouseState.y + this.displayContainer.offsetTop - this.main.scrollTop;

        // Determine viewport dimensions
        const view_width = this.main.offsetWidth;
        const view_height = this.main.offsetHeight;

        // Determine scroll amounts based on mouse position relative to document

        let scroll_amount_x;
        if (mouse_view_x > view_width)
            scroll_amount_x = mouse_view_x - view_width;
        else if (mouse_view_x < 0)
            scroll_amount_x = mouse_view_x;
        else
            scroll_amount_x = 0;

        let scroll_amount_y;
        if (mouse_view_y > view_height)
            scroll_amount_y = mouse_view_y - view_height;
        else if (mouse_view_y < 0)
            scroll_amount_y = mouse_view_y;
        else
            scroll_amount_y = 0;

        // Scroll (if necessary) to keep mouse on screen.
        this.main.scrollLeft += scroll_amount_x;
        this.main.scrollTop += scroll_amount_y;

    }

    /**
     * Return the name of the event associated with the provided
     * mouse event.
     *
     * @param event
     *     The mouse event to determine an event name for.
     *
     * @returns
     *     The name of the event associated with the provided
     *     mouse event.
     */
    private getMouseEventName(event: Guacamole.Event): MouseEventName {
        switch (event.type) {
            case 'mousedown':
                return 'guacClientMouseDown';
            case 'mouseup':
                return 'guacClientMouseUp';
            default:
                return 'guacClientMouseMove';
        }
    }

    /**
     * Handles a mouse event originating from the user's actual mouse.
     * This differs from handleEmulatedMouseEvent() in that the
     * software mouse cursor must be shown only if the user's browser
     * does not support explicitly setting the hardware mouse cursor.
     *
     * @param event
     *     The mouse event to handle.
     */
    private handleMouseEvent(event: Guacamole.Mouse.Event): void {

        // Do not attempt to handle mouse state changes if the client
        // or display are not yet available
        if (!this.guacamoleClient || !this.display)
            return;

        event.stopPropagation();
        event.preventDefault();

        // Send mouse state, show cursor if necessary
        this.display.showCursor(!this.localCursor);
        this.guacamoleClient.sendMouseState(event.state, true);

        // Broadcast the mouse event
        this.guacEventService.broadcast(this.getMouseEventName(event), { event, client: this.managedClient });

    }

    /**
     * Handles a mouse event originating from one of Guacamole's mouse
     * emulation objects. This differs from handleMouseState() in that
     * the software mouse cursor must always be shown (as the emulated
     * mouse device will not have its own cursor).
     *
     * @param event
     *     The mouse event to handle.
     */
    private handleEmulatedMouseEvent: Guacamole.Event.TargetListener = (event: Guacamole.Event) => {

        // Do not attempt to handle mouse state changes if the client
        // or display are not yet available
        if (!this.guacamoleClient || !this.display)
            return;

        const mouseEvent = event as Guacamole.Mouse.Event;

        mouseEvent.stopPropagation();
        mouseEvent.preventDefault();

        // Ensure software cursor is shown
        this.display.showCursor(true);

        // Send mouse state, ensure cursor is visible
        this.scrollToMouse(mouseEvent.state);
        this.guacamoleClient.sendMouseState(mouseEvent.state, true);

        // Broadcast the mouse event
        this.guacEventService.broadcast(this.getMouseEventName(event), { event, client: this.managedClient });

    }

    /**
     * Return the name of the event associated with the provided
     * touch event.
     *
     * @param event
     *     The touch event to determine an event name for.
     *
     * @returns
     *     The name of the event associated with the provided
     *     touch event.
     */
    private getTouchEventName(event: Guacamole.Event): TouchEventName {
        switch (event.type) {
            case 'touchstart':
                return 'guacClientTouchStart';
            case 'touchend':
                return 'guacClientTouchEnd';
            default:
                return 'guacClientTouchMove';
        }
    }

    /**
     * Handles a touch event originating from the user's device.
     *
     * @param event
     *     The touch event.
     */
    private handleTouchEvent: Guacamole.Event.TargetListener = (event: Guacamole.Event) => {

        // Do not attempt to handle touch state changes if the client
        // or display are not yet available
        if (!this.guacamoleClient || !this.display)
            return;

        const touchEvent = event as Guacamole.Touch.Event;
        touchEvent.preventDefault();

        // Send touch state, hiding local cursor
        this.display.showCursor(false);
        this.guacamoleClient.sendTouchState(touchEvent.state, true);

        // Broadcast the touch event
        this.guacEventService.broadcast(this.getTouchEventName(event), { event, client: this.managedClient });

    }

    /**
     * Attach any given managed client.
     *
     * @param managedClient
     *     The managed client to attach.
     */
    private attachManagedClient(managedClient: ManagedClient): void {

        if (!this.displayContainer)
            return;

        // Remove any existing display
        this.displayContainer.innerHTML = '';

        // Only proceed if a client is given
        if (!this.managedClient)
            return;

        // Get Guacamole client instance
        this.guacamoleClient = managedClient.client;

        // Attach possibly new display
        this.display = this.guacamoleClient.getDisplay();
        this.display.scale(this.managedClient.clientProperties.scale);

        // Add display element
        this.displayElement = this.display.getElement();
        this.displayContainer.appendChild(this.displayElement!);

        // Do nothing when the display element is clicked on
        this.display.getElement().onclick = (e: any) => {
            e.preventDefault();
            return false;
        };

        // Connect and update interface to match required size, deferring
        // connecting until a future element resize if the main element
        // size (desired display size) is not known and thus can't be sent
        // during the handshake
        this.mainElementResized();
    }

    ngOnChanges(changes: SimpleChanges): void {

        // Attach any given managed client
        if (changes['managedClient']) {

            const managedClient = changes['managedClient'].currentValue as ManagedClient;
            this.attachManagedClient(managedClient);

        }

    }

    /**
     * Custom change detection logic for the component.
     */
    ngDoCheck(): void {

        // Update actual view scrollLeft when scroll properties change
        const scrollLeft = this.managedClient.clientProperties.scrollLeft;

        if (this.lastScrollLeft !== scrollLeft) {

            this.scrollLeftChanged(scrollLeft);
            this.lastScrollLeft = scrollLeft;

        }


        // Update actual view scrollTop when scroll properties change
        const scrollTop = this.managedClient.clientProperties.scrollTop;

        if (this.lastScrollTop !== scrollTop) {

            this.scrollTopChanged(scrollTop);
            this.lastScrollTop = scrollTop;

        }

        // Update scale when display is resized
        const displaySize = this.managedClient.managedDisplay?.size;

        if (this.lastDisplaySize !== displaySize) {

            Promise.resolve().then(() => this.updateDisplayScale());
            this.lastDisplaySize = displaySize;

        }

        // Keep local cursor up-to-date
        const cursor = this.managedClient.managedDisplay?.cursor;

        if (this.lastCursor !== cursor) {

            this.setCursor(cursor);
            this.lastCursor = cursor;

        }

        // Update touch event handling depending on remote multi-touch
        // support and mouse emulation mode
        const multiTouchSupport = this.managedClient.multiTouchSupport;
        const emulateAbsoluteMouse = this.emulateAbsoluteMouse;

        if (this.lastMultiTouchSupport !== multiTouchSupport || this.lastEmulateAbsoluteMouse !== emulateAbsoluteMouse) {

            this.touchBehaviorChanged();
            this.lastMultiTouchSupport = multiTouchSupport;
            this.lastEmulateAbsoluteMouse = emulateAbsoluteMouse;

        }


        // Adjust scale if modified externally
        const scale = this.managedClient.clientProperties.scale;

        if (this.lastScale !== scale) {

            this.changeScale(scale);
            this.lastScale = scale;

        }


        // If autofit is set, the scale should be set to the minimum scale, filling the screen
        const autoFit = this.managedClient.clientProperties.autoFit;

        if(this.lastAutoFit !== autoFit) {

            this.changeAutoFit(autoFit);
            this.lastAutoFit = autoFit;

        }

    }

    /**
     * Update actual view scrollLeft when scroll properties change.
     *
     * @param scrollLeft New scrollLeft value
     */
    private scrollLeftChanged(scrollLeft: number): void {
        this.main.scrollLeft = scrollLeft;
        this.managedClient.clientProperties.scrollLeft = this.main.scrollLeft;
    }

    /**
     * Update actual view scrollTop when scroll properties change.
     *
     * @param scrollTop New scrollTop value
     */
    private scrollTopChanged(scrollTop: number): void {
        this.main.scrollTop = scrollTop;
        this.managedClient.clientProperties.scrollTop = this.main.scrollTop;
    }

    /**
     * Keep the local cursor up-to-date.
     * @param cursor The cursor to set.
     */
    private setCursor(cursor: ManagedDisplayCursor | undefined): void {
        if (cursor)
            this.localCursor = this.mouse.setCursor(cursor.canvas!, cursor.x!, cursor.y!);
    }

    /**
     * Update touch event handling depending on remote multi-touch
     * support and mouse emulation mode.
     */
    private touchBehaviorChanged() {

        // Clear existing event handling
        this.touch.offEach(['touchstart', 'touchmove', 'touchend'], this.handleTouchEvent);
        this.touchScreen.offEach(['mousedown', 'mousemove', 'mouseup'], this.handleEmulatedMouseEvent);
        this.touchPad.offEach(['mousedown', 'mousemove', 'mouseup'], this.handleEmulatedMouseEvent);

        // Directly forward local touch events
        if (this.managedClient.multiTouchSupport)
            this.touch.onEach(['touchstart', 'touchmove', 'touchend'], this.handleTouchEvent);

            // Switch to touchscreen if mouse emulation is required and
        // absolute mouse emulation is preferred
        else if (this.emulateAbsoluteMouse)
            this.touchScreen.onEach(['mousedown', 'mousemove', 'mouseup'], this.handleEmulatedMouseEvent);

            // Use touchpad for mouse emulation if absolute mouse emulation
        // is not preferred
        else
            this.touchPad.onEach(['mousedown', 'mousemove', 'mouseup'], this.handleEmulatedMouseEvent);
    }

    /**
     * Adjust scale if modified externally.
     * @param scale The new scale value.
     */
    private changeScale(scale: number): void {

        // Fix scale within limits
        scale = Math.max(scale, this.managedClient.clientProperties.minScale);
        scale = Math.min(scale, this.managedClient.clientProperties.maxScale);

        // If at minimum zoom level, hide scroll bars
        if (scale === this.managedClient.clientProperties.minScale)
            this.main.style.overflow = 'hidden';

        // If not at minimum zoom level, show scroll bars
        else
            this.main.style.overflow = 'auto';

        // Apply scale if client attached
        if (this.display)
            this.display.scale(scale);

        if (scale !== this.managedClient.clientProperties.scale)
            this.managedClient.clientProperties.scale = scale;

    }

    /**
     * Update the scale of the attached Guacamole.Client if auto-fit
     * is enabled.
     */
    private changeAutoFit(autoFit: boolean): void {
        if(autoFit)
            this.managedClient.clientProperties.scale = this.managedClient.clientProperties.minScale;
    }

    /**
     * Sends the current size of the main element (the display container)
     * to the Guacamole server, requesting that the remote display be
     * resized. If the Guacamole client is not yet connected, it will be
     * connected and the current size will sent through the initial
     * handshake. If the size of the main element is not yet known, this
     * function may need to be invoked multiple times until the size is
     * known and the client may be connected.
     */
    mainElementResized(): void {

        // Send new display size, if changed
        if (this.guacamoleClient && this.display && this.main.offsetWidth && this.main.offsetHeight) {

            // Connect, if not already connected
            this.managedClientService.connect(this.managedClient, this.main.offsetWidth, this.main.offsetHeight);

            const pixelDensity = window.devicePixelRatio || 1;
            const width = this.main.offsetWidth * pixelDensity;
            const height = this.main.offsetHeight * pixelDensity;

            if (this.display.getWidth() !== width || this.display.getHeight() !== height)
                this.guacamoleClient.sendSize(width, height);

        }

        this.updateDisplayScale();

    }

    // Scroll client display if absolute mouse is in use (the same drag
    // gesture is needed for moving the mouse pointer with relative mouse)
    clientDrag(inProgress: boolean, startX: number, startY: number, currentX: number, currentY: number, deltaX: number, deltaY: number): boolean {

        if (this.emulateAbsoluteMouse) {
            this.managedClient.clientProperties.scrollLeft -= deltaX;
            this.managedClient.clientProperties.scrollTop -= deltaY;
        }

        return false;

    }

    /**
     * If a pinch gesture is in progress, the scale of the client display when
     * the pinch gesture began.
     */
    private initialScale: number | null = null;

    /**
     * If a pinch gesture is in progress, the X coordinate of the point on the
     * client display that was centered within the pinch at the time the
     * gesture began.
     */
    private initialCenterX = 0;

    /**
     * If a pinch gesture is in progress, the Y coordinate of the point on the
     * client display that was centered within the pinch at the time the
     * gesture began.
     */
    private initialCenterY = 0;

    /**
     * Zoom and pan client via pinch gestures.
     */
    clientPinch(inProgress: boolean, startLength: number, currentLength: number, centerX: number, centerY: number): boolean {

        // Do not handle pinch gestures if they would conflict with remote
        // handling of similar gestures
        if (this.managedClient.multiTouchSupport > 1)
            return false;

        // Do not handle pinch gestures while relative mouse is in use (2+
        // contact point gestures are used by relative mouse emulation to
        // support right click, middle click, and scrolling)
        if (!this.emulateAbsoluteMouse)
            return false;

        // Stop gesture if not in progress
        if (!inProgress) {
            this.initialScale = null;
            return false;
        }

        // Set initial scale if gesture has just started
        if (!this.initialScale) {
            this.initialScale = this.managedClient.clientProperties.scale;
            this.initialCenterX = (centerX + this.managedClient.clientProperties.scrollLeft) / this.initialScale;
            this.initialCenterY = (centerY + this.managedClient.clientProperties.scrollTop) / this.initialScale;
        }

        // Determine new scale absolutely
        let currentScale = this.initialScale * currentLength / startLength;

        // Fix scale within limits - scroll will be miscalculated otherwise
        currentScale = Math.max(currentScale, this.managedClient.clientProperties.minScale);
        currentScale = Math.min(currentScale, this.managedClient.clientProperties.maxScale);

        // Update scale based on pinch distance
        this.managedClient.clientProperties.autoFit = false;
        this.managedClient.clientProperties.scale = currentScale;

        // Scroll display to keep original pinch location centered within current pinch
        this.managedClient.clientProperties.scrollLeft = this.initialCenterX * currentScale - centerX;
        this.managedClient.clientProperties.scrollTop = this.initialCenterY * currentScale - centerY;

        return false;

    }

    /**
     * Whether a drag/drop operation is currently in progress (the user has
     * dragged a file over the Guacamole connection but has not yet
     * dropped it).
     */
    dropPending = false;

    /**
     * Displays a visual indication that dropping the file currently
     * being dragged is possible. Further propagation and default behavior
     * of the given event is automatically prevented.
     *
     * @param e
     *     The event related to the in-progress drag/drop operation.
     */
    notifyDragStart(e: Event): void {

        e.preventDefault();
        e.stopPropagation();

        this.dropPending = true;

    }

    /**
     * Removes the visual indication that dropping the file currently
     * being dragged is possible. Further propagation and default behavior
     * of the given event is automatically prevented.
     *
     * @param e
     *     The event related to the end of the former drag/drop operation.
     */
    private notifyDragEnd(e: Event): void {

        e.preventDefault();
        e.stopPropagation();

        this.dropPending = false;

    }


}
