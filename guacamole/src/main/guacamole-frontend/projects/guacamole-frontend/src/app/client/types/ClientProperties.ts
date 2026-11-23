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

/**
 * A service for generating new guacClient properties objects.
 * Object used for interacting with a guacClient directive.
 */
export class ClientProperties {

    /**
     * Whether the display should be scaled automatically to fit within the
     * available space.
     *
     * @default true
     */
    autoFit: boolean;

    /**
     * The current scale. If autoFit is true, the effect of setting this
     * value is undefined.
     *
     * @default 1
     */
    scale: number;

    /**
     * The minimum scale value.
     *
     * @default 1
     */
    minScale: number;

    /**
     * The maximum scale value.
     *
     * @default 3
     */
    maxScale: number;

    /**
     * Whether this client should receive keyboard events.
     *
     * @default false
     */
    focused: boolean;

    /**
     * The relative Y coordinate of the scroll offset of the display within
     * the client element.
     *
     * @default 0
     */
    scrollTop: number;

    /**
     * The relative X coordinate of the scroll offset of the display within
     * the client element.
     *
     * @default 0
     */
    scrollLeft: number;

    /**
     * @param template
     *     The object whose properties should be copied within the new
     *     ClientProperties.
     */
    constructor(template: Partial<ClientProperties> = {}) {
        this.autoFit = template.autoFit || true;
        this.scale = template.scale || 1;
        this.minScale = template.minScale || 1;
        this.maxScale = template.maxScale || 3;
        this.focused = template.focused || false;
        this.scrollTop = template.scrollTop || 0;
        this.scrollLeft = template.scrollLeft || 0;
    }
}
