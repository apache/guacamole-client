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
 * Provides the Marker class definition.
 */
export class Marker {

    /**
     * Creates a new Marker which allows its associated element to be scrolled
     * into view as desired.
     *
     * @param {Element} element
     *     The element to associate with this marker.
     */
    constructor(private element: Element) {
    }

    /**
     * Scrolls scrollable elements, or the window, as needed to bring the
     * element associated with this marker into view.
     */
    scrollIntoView() {
        this.element.scrollIntoView();
    }

}
