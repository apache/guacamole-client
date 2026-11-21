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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { GuacEventService } from 'guacamole-frontend-lib';
import type { Mock } from 'vitest';
import { GuacFrontendEventArguments } from '../../../events/types/GuacFrontendEventArguments';
import { ManagedClientService } from '../../services/managed-client.service';
import { ManagedClient } from '../../types/ManagedClient';
import { GuacClientComponent } from './guac-client.component';

describe('GuacClientComponent', () => {
    let fixture: ComponentFixture<GuacClientComponent>;
    let guacEventService: GuacEventService<GuacFrontendEventArguments>;
    let sendMouseState: Mock<(state: Guacamole.Mouse.State, applyDisplayScale?: boolean) => void>;
    let client: ManagedClient;

    beforeEach(() => {
        vi.useFakeTimers();

        TestBed.configureTestingModule({
            declarations: [GuacClientComponent],
            providers: [{ provide: ManagedClientService, useValue: { connect: vi.fn(), setClipboard: vi.fn() } }]
        });

        // Only the handling of mouse events is under test, not the touch
        // gestures and resizing of the template
        TestBed.overrideTemplate(GuacClientComponent, '<div #main><div #display class="display"></div></div>');
        guacEventService = TestBed.inject(GuacEventService<GuacFrontendEventArguments>);

        // A display and a client which only record what the component sends
        const display = {
            getElement: () => document.createElement('div'),
            getWidth: () => 1024,
            getHeight: () => 768,
            getScale: () => 1,
            scale: vi.fn(),
            showCursor: vi.fn()
        } as unknown as Guacamole.Display;

        sendMouseState = vi.fn();
        client = new ManagedClient({
            id: 'c2lnbmFs',
            client: { getDisplay: () => display, sendMouseState, sendSize: vi.fn() } as unknown as Guacamole.Client,
            tunnel: {} as Guacamole.Tunnel
        });

        fixture = TestBed.createComponent(GuacClientComponent);
        fixture.componentRef.setInput('client', client);
        fixture.componentRef.setInput('emulateAbsoluteMouse', false);
        fixture.detectChanges();
    });

    afterEach(() => {
        fixture.destroy();
        vi.useRealTimers();
        vi.restoreAllMocks();
    });

    /**
     * Dispatches a DOM mouse event for the middle button at a fixed
     * position to the element containing the display, as the browser would
     * for the actual mouse of the user.
     *
     * @param type
     *     The type of the DOM mouse event, such as "mousedown".
     */
    function dispatchMiddleButton(type: 'mousedown' | 'mouseup'): void {
        const displayContainer = fixture.nativeElement.querySelector('.display') as HTMLDivElement;
        displayContainer.dispatchEvent(new MouseEvent(type, { button: 1, clientX: 10, clientY: 20 }));
    }

    /**
     * Dispatches a mouse event as if the touchpad that emulates the mouse for
     * touch devices without multi-touch support had produced it, and returns
     * that event.
     *
     * @param type
     *     The type of the mouse event, such as "mousedown".
     *
     * @param pressed
     *     Whether the left mouse button is pressed.
     *
     * @returns
     *     The mouse event that was dispatched.
     */
    function dispatchEmulatedMouseEvent(type: 'mousedown' | 'mouseup', pressed: boolean): Guacamole.Mouse.Event {
        const touchPad = (fixture.componentInstance as unknown as { touchPad: { dispatch(event: Guacamole.Event): void } }).touchPad;
        const emulatedEvent = new Guacamole.Mouse.Event(type, new Guacamole.Mouse.State(5, 6, pressed, false, false, false, false));
        touchPad.dispatch(emulatedEvent);
        return emulatedEvent;
    }

    /**
     * Returns, for each mouse state sent to the Guacamole client so far,
     * whether the middle button was pressed.
     */
    function sentMiddleButtonStates(): boolean[] {
        return sendMouseState.mock.calls.map(([state]) => state.middle);
    }

    it('should send mouse events of the actual mouse without delay if the clipboard is not being synchronized', async () => {
        dispatchMiddleButton('mousedown');
        await vi.advanceTimersByTimeAsync(0);

        // Guacamole.Mouse moves the pointer to the position of the press first
        expect(sentMiddleButtonStates()).toEqual([false, true]);
    });

    it('should send mouse events of the actual mouse only after the clipboard has been synchronized', async () => {
        const broadcast = vi.spyOn(guacEventService, 'broadcast');

        // Middle-click pastes in many terminals; the click must not reach the
        // remote desktop before the local clipboard does
        guacEventService.broadcast('clipboardSyncInProgress', { status: true });
        dispatchMiddleButton('mousedown');
        dispatchMiddleButton('mouseup');

        await vi.advanceTimersByTimeAsync(100);
        expect(sendMouseState).not.toHaveBeenCalled();
        expect(broadcast).not.toHaveBeenCalledWith('guacClientMouseDown', expect.anything());

        guacEventService.broadcast('clipboardSyncInProgress', { status: false });
        await vi.advanceTimersByTimeAsync(10);

        expect(sentMiddleButtonStates()).toEqual([false, true, false]);
        expect(broadcast).toHaveBeenCalledWith('guacClientMouseDown', expect.objectContaining({ client }));
        expect(broadcast).toHaveBeenCalledWith('guacClientMouseUp', expect.objectContaining({ client }));
    });

    it('should pass the mouse event and the client to subscribers of guacClientMouseDown', async () => {
        const subscriber = vi.fn();
        guacEventService.on('guacClientMouseDown')
            .subscribe(({ inputEvent, client }) => subscriber(inputEvent, client));

        dispatchMiddleButton('mousedown');
        await vi.advanceTimersByTimeAsync(0);

        expect(subscriber).toHaveBeenCalledExactlyOnceWith(expect.any(Guacamole.Mouse.Event), client);
        expect(subscriber.mock.calls[0][0].state.middle).toBe(true);
    });

    it('should broadcast guacBeforeClientMouseDown with the mouse event before the clipboard synchronization and before the event is stopped', async () => {
        const subscriber = vi.fn();
        const stopPropagation = vi.fn();
        const preventDefault = vi.fn();

        // The spies replace the methods only while the event is broadcast, so
        // they record just the calls that follow the broadcast
        guacEventService.on('guacBeforeClientMouseDown').subscribe(({ inputEvent, client }) => {
            vi.spyOn(inputEvent, 'stopPropagation').mockImplementation(stopPropagation);
            vi.spyOn(inputEvent, 'preventDefault').mockImplementation(preventDefault);
            subscriber(inputEvent, client, sendMouseState.mock.calls.length);
        });

        // The broadcast must not wait for the synchronization, or the
        // keyboard could not resolve its deferred modifier keys in time
        guacEventService.broadcast('clipboardSyncInProgress', { status: true });
        dispatchMiddleButton('mousedown');

        expect(subscriber).toHaveBeenCalledExactlyOnceWith(expect.any(Guacamole.Mouse.Event), client, 0);
        expect(subscriber.mock.calls[0][0].state.middle).toBe(true);

        // The event is stopped only afterwards, and the state is sent last
        expect(stopPropagation).toHaveBeenCalledOnce();
        expect(preventDefault).toHaveBeenCalledOnce();
        expect(sendMouseState).not.toHaveBeenCalled();

        guacEventService.broadcast('clipboardSyncInProgress', { status: false });
        await vi.advanceTimersByTimeAsync(10);
        expect(sentMiddleButtonStates()).toEqual([false, true]);
    });

    it('should broadcast guacBeforeClientMouseDown before the mouse state is sent', async () => {
        let sentBefore: number | undefined;
        guacEventService.on('guacBeforeClientMouseDown')
            .subscribe(() => sentBefore = sendMouseState.mock.calls.length);

        dispatchMiddleButton('mousedown');
        await vi.advanceTimersByTimeAsync(0);

        expect(sentBefore).toBe(0);
        expect(sendMouseState).toHaveBeenCalled();
    });

    it('should not broadcast guacBeforeClientMouseDown for other mouse events', async () => {
        const subscriber = vi.fn();
        guacEventService.on('guacBeforeClientMouseDown').subscribe(subscriber);

        dispatchMiddleButton('mouseup');
        await vi.advanceTimersByTimeAsync(0);

        expect(sendMouseState).toHaveBeenCalled();
        expect(subscriber).not.toHaveBeenCalled();
    });

    it('should broadcast guacBeforeClientMouseDown for the mousedown of an emulated mouse before sending it', () => {
        let sentBefore: number | undefined;
        const subscriber = vi.fn();
        guacEventService.on('guacBeforeClientMouseDown').subscribe(({ inputEvent, client }) => {
            sentBefore = sendMouseState.mock.calls.length;
            subscriber(inputEvent, client);
        });

        const emulatedMouseDown = dispatchEmulatedMouseEvent('mousedown', true);

        expect(subscriber).toHaveBeenCalledExactlyOnceWith(emulatedMouseDown, client);
        expect(sentBefore).toBe(0);
        expect(sendMouseState).toHaveBeenCalledExactlyOnceWith(emulatedMouseDown.state, true);
    });

    it('should not broadcast guacBeforeClientMouseDown for other events of an emulated mouse', () => {
        const subscriber = vi.fn();
        guacEventService.on('guacBeforeClientMouseDown').subscribe(subscriber);

        dispatchEmulatedMouseEvent('mouseup', false);

        expect(sendMouseState).toHaveBeenCalled();
        expect(subscriber).not.toHaveBeenCalled();
    });

});
