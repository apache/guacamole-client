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

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PlayerDisplayComponent } from './player-display.component';

/**
 * A stand-in for Guacamole.Display with the given size, which counts the
 * scale values it receives. Like the real display, the test calls its
 * onresize callback as a method of the display.
 *
 * @param width
 *     The initial width of the display, in pixels.
 *
 * @param height
 *     The initial height of the display, in pixels.
 *
 * @returns
 *     The stand-in display.
 */
function createDisplay(width: number, height: number) {
    const element = document.createElement('div');
    return {
        width,
        height,
        getElement: () => element,
        getWidth() { return this.width; },
        getHeight() { return this.height; },
        scale: vi.fn(),
        onresize: null as ((width: number, height: number) => void) | null
    };
}

describe('PlayerDisplayComponent', () => {

    let fixture: ComponentFixture<PlayerDisplayComponent>;
    let display: ReturnType<typeof createDisplay>;

    beforeEach(() => {
        TestBed.configureTestingModule({
            declarations: [PlayerDisplayComponent],
            schemas     : [NO_ERRORS_SCHEMA]
        });
        fixture = TestBed.createComponent(PlayerDisplayComponent);

        // Available space of 200x200 pixels, which jsdom does not lay out
        display = createDisplay(100, 50);
        fixture.componentRef.setInput('display', display as unknown as Guacamole.Display);
        fixture.detectChanges();
        const element = fixture.componentInstance.element.nativeElement;
        Object.defineProperty(element, 'offsetWidth', { configurable: true, value: 200 });
        Object.defineProperty(element, 'offsetHeight', { configurable: true, value: 200 });
    });

    it('should scale the display to the available space when it is added', () => {
        fixture.componentInstance.fitDisplay();

        expect(display.scale).toHaveBeenLastCalledWith(2);
    });

    it('should rescale the display when the display changes its size', () => {
        display.width = 400;
        display.height = 100;

        display.onresize!(400, 100);

        expect(display.scale).toHaveBeenLastCalledWith(0.5);
    });
});
