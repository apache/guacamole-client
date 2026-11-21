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

import { ApplicationRef, ComponentRef, createComponent, EnvironmentInjector, Inject, Injectable, Injector, Type, DOCUMENT } from '@angular/core';
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
     * All available HTML patches, each with its operations and a function
     * which returns a new copy of its elements each time it is called.
     */
    private patches: { operations: PatchOperation[]; createElements: () => Element[] }[] = [];

    /**
     * The Angular components which HTML patches inserted into the DOM and
     * which have not been destroyed yet.
     */
    private components: ComponentRef<unknown>[] = [];

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
                const elementFactories = await Promise.all([...metaElements, ...otherElements].filter((element) => {

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
                            await this.getPatchElementFactory(element)
                        ))
                ;

                // Add patch to list of available patches
                this.patches.push({
                    operations,
                    createElements: () => elementFactories.map(createElement => createElement())
                });

            }
        });
    }

    /**
     * Returns a function which creates a new copy of the given patch element
     * each time it is called, so that each element matched by a patch
     * operation receives its own copy. If the element has the attributes
     * nativeFederationModule and nativeFederationElement, each copy is the
     * host element of a new instance of the Angular component exposed under
     * that name by the given native federation module. Otherwise each copy
     * is a deep clone of the element itself.
     *
     * @param element
     *     The element of the patch.
     *
     * @returns
     *     A promise which resolves with a function returning a new copy of
     *     the element to insert into the DOM.
     */
    public async getPatchElementFactory(element: Element): Promise<() => Element> {

        // Check if the element should be replaced by an angular component via nativ federation
        const nativeFederationModule = element.getAttribute('nativeFederationModule');
        const nativeFederationElement = element.getAttribute('nativeFederationElement');

        if (nativeFederationModule && nativeFederationElement) {
            const module = await this.loadRemoteModule<Record<string, Type<unknown>>>(
                nativeFederationModule, nativeFederationElement
            );
            return () => this.createNativeFederationAngularComponent(module[nativeFederationElement]);
        }

        // Just copy the element itself if native federation is not required
        return () => element.cloneNode(true) as Element;

    }

    /**
     * Creates a new instance of the given Angular component, which a native
     * federation module exposes, and returns its host element, which the
     * patch operations then insert into the DOM. The component takes part
     * in the change detection of the application until its host element
     * leaves the DOM.
     *
     * @param component
     *     The Angular component exposed by the native federation module.
     *
     * @returns
     *     The host element of the created component.
     */
    public createNativeFederationAngularComponent(component: Type<unknown>): Element {

        // Create the component with its own host element, outside any view
        // container, and check it along with the views of the application
        const componentRef = createComponent(component, {
            environmentInjector: this.injector.get(EnvironmentInjector)
        });
        this._appRef.attachView(componentRef.hostView);
        this.components.push(componentRef);

        return componentRef.location.nativeElement;

    }

    /**
     * Applies each available HTML patch to the elements of the DOM which it
     * has not been applied to yet, and destroys the Angular components
     * inserted by HTML patches which have since left the DOM.
     */
    applyPatches(): void {

        // Apply each operation implied by the meta tags
        const root = $(this.document.body);
        this.patches.forEach(({ operations, createElements }) => {
            operations.forEach(operation => {
                operation.apply(root, createElements);
            });
        });

        // Destroy the components which Angular removed from the DOM along
        // with the elements they were inserted at
        this.components = this.components.filter(componentRef => {

            if (componentRef.location.nativeElement.isConnected)
                return true;

            componentRef.destroy();
            return false;

        });

    }

}
