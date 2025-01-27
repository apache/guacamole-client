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

import { loadRemoteModule } from '@angular-architects/native-federation';
import { DOCUMENT } from '@angular/common';
import { ApplicationRef, EnvironmentInjector, Inject, Injectable, Injector, ViewContainerRef } from '@angular/core';
import $ from 'jquery';
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
                private patchService: PatchService,
                private injector: Injector,
                private _appRef: ApplicationRef) {

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
     * TODO
     * @param element
     * @private
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
     * FIXME
     * @param nativeFederationModule
     * @param nativeFederationElement
     * @private
     */
    public async createNativeFederationAngularComponent(nativeFederationModule: string, nativeFederationElement: string): Promise<Element> {

        const module = await loadRemoteModule({
            remoteName: nativeFederationModule,
            exposedModule: nativeFederationElement
        });

        // Locate a DOM node that would be used as a host.
        const hostElement = document.createElement('div');

        const viewContainerRef = this._appRef.components[0].injector.get(ViewContainerRef);

        const componentRef = viewContainerRef.createComponent(module[nativeFederationElement], {
            environmentInjector: this.injector.get(EnvironmentInjector),
            injector: this.injector
        });
        componentRef.changeDetectorRef.detectChanges();
        componentRef.changeDetectorRef.markForCheck();

        // this.renderer.appendChild(ref.nativeElement, modalElement);

        return hostElement;

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
