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

import { Injectable } from '@angular/core';

/**
 * A service for providing true fullscreen and keyboard lock support.
 * Keyboard lock is currently only supported by Chromium based browsers
 * (Edge >= V79, Chrome >= V68 and Opera >= V55)
 */
@Injectable({
    providedIn: 'root'
})
export class GuacFullscreenService {

    /**
     * Check is browser in true fullscreen mode
     */
    isInFullscreenMode(): Element | null {
        return document.fullscreenElement;
    }

    /**
     * Set fullscreen mode
     */
    setFullscreenMode(state: boolean): void {
        if (document.fullscreenEnabled) {
            if (state && !this.isInFullscreenMode())
                // @ts-ignore navigator.keyboard limited availability
                document.documentElement.requestFullscreen().then(navigator.keyboard.lock());
            else if (!state && this.isInFullscreenMode())
                // @ts-ignore navigator.keyboard limited availability
                document.exitFullscreen().then(navigator.keyboard.unlock());
        }
    }

    // toggles current fullscreen mode (off if on, on if off)
    toggleFullscreenMode(): void {
        if (!this.isInFullscreenMode())
            this.setFullscreenMode(true);
        else
            this.setFullscreenMode(false);
    }

}
