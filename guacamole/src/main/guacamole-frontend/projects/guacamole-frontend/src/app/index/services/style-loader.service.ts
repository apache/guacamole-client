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
import { Inject, Injectable, Renderer2, RendererFactory2 } from '@angular/core';

const STYLE_ELEMENT_ID = 'loaded-style-';

/**
 * Service for dynamically loading additional styles at runtime.
 * Used to load the styles of extensions.
 */
@Injectable({
    providedIn: 'root'
})
export class StyleLoaderService {

    /**
     * The Angular renderer used to manipulate the DOM.
     */
    private renderer: Renderer2;

    /**
     * Inject required Services.
     */
    constructor(@Inject(DOCUMENT) private document: Document,
                private rendererFactory: RendererFactory2) {
        this.renderer = this.rendererFactory.createRenderer(null, null);
    }

    /**
     * Loads the given CSS file by adding a link element for it to
     * the head of the page.
     *
     * @param styleName
     *     The name of the CSS file that should be loaded.
     */
    loadStyle(styleName: string): void {

        const elementId = STYLE_ELEMENT_ID + styleName

        // If the element already exists there is nothing to do
        if (this.document.getElementById(elementId))
            return;

        // Create a link element via Angular's renderer
        const styleElement = this.renderer.createElement('link') as HTMLLinkElement;

        // Add the style to the head section
        this.renderer.appendChild(this.document.head, styleElement);

        // Set type of the link item and path to the css file
        this.renderer.setProperty(styleElement, 'rel', 'stylesheet');
        this.renderer.setProperty(styleElement, 'id', elementId);
        // TODO: ?b=${guacamole.build.identifier}
        this.renderer.setProperty(styleElement, 'href', styleName);

    }

}
