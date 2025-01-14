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

import { signal, WritableSignal } from '@angular/core';


export type ManagedDisplayTemplate = {
    display?: Guacamole.Display;
    size?: ManagedDisplayDimensions;
    cursor?: ManagedDisplayCursor;
}


/**
 * Provides the ManagedDisplay class used by the guacClientManager service.
 * Object which serves as a surrogate interface, encapsulating a Guacamole
 * display while it is active, allowing it to be detached and reattached
 * from different client views.
 */
export class ManagedDisplay {
    /**
     * The underlying Guacamole display.
     */
    display: WritableSignal<Guacamole.Display | undefined>;

    /**
     * The current size of the Guacamole display.
     */
    size: ManagedDisplayDimensions;

    /**
     * The current mouse cursor, if any.
     */
    cursor: ManagedDisplayCursor | undefined;

    /**
     * @param [template={}]
     *     The object whose properties should be copied within the new
     *     ManagedDisplay.
     */
    constructor(template: ManagedDisplayTemplate = {}) {
        this.display = signal(template.display);
        this.size = new ManagedDisplayDimensions(template.size);
        this.cursor = template.cursor;
    }

    /**
     * Creates a new ManagedDisplay which represents the current state of the
     * given Guacamole display.
     *
     * @param display
     *     The Guacamole display to represent. Changes to this display will
     *     affect this ManagedDisplay.
     *
     * @returns
     *     A new ManagedDisplay which represents the current state of the
     *     given Guacamole display.
     */
    static getInstance(display: Guacamole.Display): ManagedDisplay {

        const managedDisplay = new ManagedDisplay({
            display: display
        });

        // Store changes to display size
        display.onresize = function setClientSize() {

            managedDisplay.size = new ManagedDisplayDimensions({
                width : display.getWidth(),
                height: display.getHeight()
            });

        };

        // Store changes to display cursor
        display.oncursor = function setClientCursor(canvas, x, y) {

            managedDisplay.cursor = new ManagedDisplayCursor({
                canvas: canvas,
                x     : x,
                y     : y
            });

        };

        return managedDisplay;
    }

}

/**
 * Object which represents the size of the Guacamole display.
 */
export class ManagedDisplayDimensions {

    /**
     * The current width of the Guacamole display, in pixels.
     */
    width: number;

    /**
     * The current width of the Guacamole display, in pixels.
     */
    height: number;

    /**
     * Creates a new ManagedDisplayDimensions object. This constructor initializes
     * the properties of the new Dimensions with the corresponding properties
     * of the given template.
     *
     * @param [template={}]
     *     The object whose properties should be copied within the new
     *     ManagedDisplayDimensions.
     */
    constructor(template: Partial<ManagedDisplayDimensions> = {}) {
        this.width = template.width || 0;
        this.height = template.height || 0;
    }
}

/**
 * Object which represents a mouse cursor used by the Guacamole display.
 */
export class ManagedDisplayCursor {

    /**
     * The actual mouse cursor image.
     */
    canvas?: HTMLCanvasElement;

    /**
     * The X coordinate of the cursor hotspot.
     */
    x?: number;

    /**
     * The Y coordinate of the cursor hotspot.
     */
    y?: number;

    /**
     * Creates a new ManagedDisplayCursor. This constructor initializes the properties of the
     * new Cursor with the corresponding properties of the given template.
     *
     * @param [template={}]
     *     The object whose properties should be copied within the new
     *     ManagedDisplayCursor.
     */
    constructor(template: Partial<ManagedDisplayCursor> = {}) {
        this.canvas = template.canvas;
        this.x = template.x;
        this.y = template.y;
    }
}
