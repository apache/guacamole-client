

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
 * Provides the ManagedClientThumbnail class used by ManagedClient.
 * Object which represents a thumbnail of the Guacamole client display,
 * along with the time that the thumbnail was generated.
 */
export class ManagedClientThumbnail {

    /**
     * The time that this thumbnail was generated, as the number of
     * milliseconds elapsed since midnight of January 1, 1970 UTC.
     */
    timestamp: number;

    /**
     * The thumbnail of the Guacamole client display.
     */
    canvas: HTMLCanvasElement;

    /**
     * Creates a new ManagedClientThumbnail. This constructor initializes the properties of the
     * new ManagedClientThumbnail with the corresponding properties of the given template.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     ManagedClientThumbnail.
     */
    constructor(template: ManagedClientThumbnail) {
        this.timestamp = template.timestamp;
        this.canvas = template.canvas;
    }
}
