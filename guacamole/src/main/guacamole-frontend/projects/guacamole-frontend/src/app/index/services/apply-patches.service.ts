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

import { ApplicationRef, createComponent, EnvironmentInjector, Inject, Injectable, Injector, Type, DOCUMENT } from '@angular/core';
import $ from 'jquery';
import { PatchService } from '../../rest/service/patch.service';
import { LOAD_REMOTE_MODULE, LoadRemoteModule } from '../types/LoadRemoteModule';
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
                private patchService: PatchService,
                private injector: Injector,
                private _appRef: ApplicationRef,
                @Inject(LOAD_REMOTE_MODULE) private loadRemoteModule: LoadRemoteModule) {

        // Retrieve all patches
        this.patchService.getPatches().subscribe(async patches => {
            const domParser = new DOMParser();
            // Apply all defined patches
            for (const patch of patches) {

                const parsedPatch = domParser.parseFromString(patch, 'text/html');
                const metaElements = Array.from(parsedPatch.querySelectorAll('meta'));
                const otherElements = Array.from(parsedPatch.body.children);

                // Filter out and parse all applicable meta tags
                const operations: PatchOperation[] = [];
                let elements = await Promise.all([...metaElements, ...otherElements].filter((element) => {

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

                    })
                        .map(async (element) =>
                            await this.getPatchElement(element)
                        ))
                ;

                // Add patch to list of available patches
                this.patches.push({
                    operations,
                    elements
                });

            }
        });
    }

    /**
     * Returns the element which should be inserted into the DOM for the given
     * patch element. If the element has the attributes nativeFederationModule
     * and nativeFederationElement, it is replaced by the host element of the
     * Angular component exposed under that name by the given native
     * federation module. Otherwise the element itself is returned.
     *
     * @param element
     *     The element of the patch.
     *
     * @returns
     *     A promise which resolves with the element to insert into the DOM.
     */
    public async getPatchElement(element: Element): Promise<Element> {

        // Check if the element should be replaced by an angular component via nativ federation
        const nativeFederationModule = element.getAttribute('nativeFederationModule');
        const nativeFederationElement = element.getAttribute('nativeFederationElement');

        if (nativeFederationModule && nativeFederationElement) {
            return this.createNativeFederationAngularComponent(nativeFederationModule, nativeFederationElement)
        }

        // Just return the element itself if native federation is not required
        return element;

    }

    /**
     * Creates the Angular component exposed under the given name by the given
     * native federation module and returns its host element, which the patch
     * operations then insert into the DOM. The component takes part in the
     * change detection of the application.
     *
     * @param nativeFederationModule
     *     The name of the native federation module (the remote) exposing the
     *     component.
     *
     * @param nativeFederationElement
     *     The name under which the native federation module exposes the
     *     component.
     *
     * @returns
     *     A promise which resolves with the host element of the created
     *     component.
     */
    public async createNativeFederationAngularComponent(nativeFederationModule: string, nativeFederationElement: string): Promise<Element> {

        const module = await this.loadRemoteModule<Record<string, Type<unknown>>>(
            nativeFederationModule, nativeFederationElement
        );

        // Create the component with its own host element, outside any view
        // container, and check it along with the views of the application
        const componentRef = createComponent(module[nativeFederationElement], {
            environmentInjector: this.injector.get(EnvironmentInjector)
        });
        this._appRef.attachView(componentRef.hostView);

        return componentRef.location.nativeElement;

    }

    /**
     * Applies each available HTML patch to the DOM.
     */
    applyPatches(): void {

        // Apply each operation implied by the meta tags
        this.patches.forEach(({ operations, elements }) => {
            operations.forEach(operation => {
                operation.apply($(this.document.body), elements);
            });
        });

    }

}
