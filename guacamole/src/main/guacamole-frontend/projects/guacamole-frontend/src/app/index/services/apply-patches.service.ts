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
import { Inject, Injectable } from '@angular/core';
import { PatchService } from '../../rest/service/patch.service';
import { PatchOperation } from '../types/PatchOperation';

/**
 * Applies HTML patches defined within Guacamole extensions to the DOM.
 */
@Injectable({
    providedIn: 'root'
})
export class ApplyPatchesService {

    /**
     * All available HTML patches.
     */
    private patches: { operations: PatchOperation[]; elements: Element[] }[] = [];

    /**
     * Inject required services.
     */
    constructor(@Inject(DOCUMENT) private document: Document,
                private patchService: PatchService) {

        // Retrieve all patches
        this.patchService.getPatches().subscribe(patches => {
            const domParser = new DOMParser();
            // Apply all defined patches
            patches.forEach(patch => {

                const parsedPatch = domParser.parseFromString(patch, 'text/html');
                const metaElements = Array.from(parsedPatch.querySelectorAll('meta'));
                const otherElements = Array.from(parsedPatch.body.children);

                // Filter out and parse all applicable meta tags
                const operations: PatchOperation[] = [];
                const elements = [...metaElements, ...otherElements].filter((element) => {

                    // Leave non-meta tags untouched
                    if (element.tagName !== 'META')
                        return true;

                    // Only meta tags having a valid "name" attribute need
                    // to be filtered
                    const name = element.getAttribute('name');
                    if (!name || !(name in PatchOperation.Operations))
                        return true;

                    // The "content" attribute must be present for any
                    // valid "name" meta tag
                    const content = element.getAttribute('content');
                    if (!content)
                        return true;

                    // Filter out and parse meta tag
                    operations.push(new PatchOperation(name, content));
                    return false;

                });

                // Add patch to list of available patches
                this.patches.push({
                    operations,
                    elements
                });

            });
        });
    }

    /**
     * Applies each available HTML patch to the DOM.
     */
    applyPatches(): void {

        // Apply each operation implied by the meta tags
        this.patches.forEach(({operations, elements}) => {
            operations.forEach(operation => {
                operation.apply([this.document.body], elements);
            });
        });

    }

}
