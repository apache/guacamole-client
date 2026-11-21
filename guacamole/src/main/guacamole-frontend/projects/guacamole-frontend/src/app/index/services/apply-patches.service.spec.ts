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

import { ApplicationRef, ChangeDetectionStrategy, Component, OnDestroy } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { PatchService } from '../../rest/service/patch.service';
import { LOAD_REMOTE_MODULE, LoadRemoteModule } from '../types/LoadRemoteModule';
import { ApplyPatchesService } from './apply-patches.service';

/**
 * Stands in for an Angular component which an extension exposes via native
 * federation and inserts with an HTML patch.
 */
@Component({
    selector: 'guac-patch-button',
    template: '<a>Patch button</a>',
    changeDetection: ChangeDetectionStrategy.Eager
})
class PatchButtonComponent implements OnDestroy {

    /**
     * The number of instances destroyed so far.
     */
    static destroyed = 0;

    /**
     * Counts the destroyed instances.
     */
    ngOnDestroy(): void {
        PatchButtonComponent.destroyed++;
    }

}

describe('ApplyPatchesService', () => {

    /**
     * The element which contains the elements matched by the patches of
     * each test.
     */
    let container: HTMLElement;

    /**
     * Fake for the loadRemoteModule() function of Native Federation.
     */
    const loadRemoteModule = vi.fn<LoadRemoteModule>();

    /**
     * Creates the service with the given HTML patches and waits until it
     * has parsed them.
     *
     * @param patches
     *     The HTML patches which the REST API returns.
     *
     * @returns
     *     A promise which resolves with the service once its patches are
     *     ready to be applied.
     */
    const createService = async (...patches: string[]): Promise<ApplyPatchesService> => {
        TestBed.configureTestingModule({
            providers: [
                { provide: PatchService, useValue: { getPatches: () => of(patches) } },
                { provide: LOAD_REMOTE_MODULE, useValue: loadRemoteModule }
            ]
        });
        const service = TestBed.inject(ApplyPatchesService);

        // Run all pending promise callbacks, including the parsing of the
        // patches
        await new Promise(resolve => setTimeout(resolve));
        return service;
    };

    /**
     * Adds a new element matched by the patches of the tests to the
     * container.
     *
     * @returns
     *     The new element.
     */
    const addTarget = (): HTMLElement => {
        const target = document.createElement('div');
        target.className = 'target';
        target.textContent = 'content';
        container.appendChild(target);
        return target;
    };

    beforeEach(() => {
        loadRemoteModule.mockReset();
        PatchButtonComponent.destroyed = 0;
        container = document.createElement('div');
        document.body.appendChild(container);
    });

    afterEach(() => {
        container.remove();
    });

    describe('applyPatches()', () => {

        it('should insert exactly one copy into each matched element no matter how often it runs (after-children)', async () => {
            const service = await createService('<meta name="after-children" content=".target"><span class="inserted"></span>');
            const targets = [addTarget(), addTarget()];

            service.applyPatches();
            service.applyPatches();
            service.applyPatches();

            expect(container.querySelectorAll('.inserted')).toHaveLength(2);
            for (const target of targets)
                expect(target.lastElementChild?.className).toBe('inserted');
        });

        it('should insert exactly one copy before each matched element no matter how often it runs (before)', async () => {
            const service = await createService('<meta name="before" content=".target"><span class="inserted"></span>');
            const targets = [addTarget(), addTarget()];

            service.applyPatches();
            service.applyPatches();
            service.applyPatches();

            expect(container.querySelectorAll('.inserted')).toHaveLength(2);
            for (const target of targets)
                expect(target.previousElementSibling?.className).toBe('inserted');
        });

        it.each([
            ['after', (target: Element) => [target.nextElementSibling]],
            ['before-children', (target: Element) => [target.firstElementChild]],
            ['replace-children', (target: Element) => Array.from(target.childNodes)]
        ])('should leave exactly one copy at each matched element no matter how often it runs (%s)', async (operation, insertedAt) => {
            const service = await createService(`<meta name="${operation}" content=".target"><span class="inserted"></span>`);
            const targets = [addTarget(), addTarget()];

            service.applyPatches();
            service.applyPatches();
            service.applyPatches();

            expect(container.querySelectorAll('.inserted')).toHaveLength(2);
            for (const target of targets)
                expect(insertedAt(target).map(node => (node as Element | null)?.className)).toEqual(['inserted']);
        });

        it('should replace each matched element by exactly one copy no matter how often it runs (replace)', async () => {
            const service = await createService('<meta name="replace" content=".target"><span class="inserted"></span>');
            addTarget();
            addTarget();

            service.applyPatches();
            service.applyPatches();
            service.applyPatches();

            expect(Array.from(container.children).map(element => element.className)).toEqual(['inserted', 'inserted']);
        });

        it('should insert exactly one copy into elements which appear after the first run', async () => {
            const service = await createService('<meta name="after-children" content=".target"><span class="inserted"></span>');
            const targets = [addTarget(), addTarget()];

            service.applyPatches();
            targets.push(addTarget());
            service.applyPatches();
            service.applyPatches();

            for (const target of targets)
                expect(target.querySelectorAll('.inserted')).toHaveLength(1);
        });

        it('should not apply an operation to the elements it inserted itself', async () => {
            const service = await createService('<meta name="after-children" content=".target"><div class="target inserted"></div>');
            addTarget();
            addTarget();

            service.applyPatches();
            service.applyPatches();
            service.applyPatches();

            expect(container.querySelectorAll('.inserted')).toHaveLength(2);
            expect(container.querySelectorAll('.inserted .inserted')).toHaveLength(0);
        });

        it('should remove the copy inserted next to an element once that element leaves the DOM', async () => {
            const service = await createService('<meta name="before" content=".target"><span class="inserted"></span>');
            const [removed, kept] = [addTarget(), addTarget()];

            service.applyPatches();
            removed.remove();
            const added = addTarget();
            service.applyPatches();

            expect(container.querySelectorAll('.inserted')).toHaveLength(2);
            for (const target of [kept, added])
                expect(target.previousElementSibling?.className).toBe('inserted');
        });

        it.each([
            ['an HTML patch', '<span class="inserted"></span>'],
            ['a native federation component', '<div nativeFederationModule="example" nativeFederationElement="PatchButtonComponent"></div>']
        ])('should leave the inserted elements of %s untouched on later runs', async (_, element) => {
            loadRemoteModule.mockResolvedValue({ PatchButtonComponent });
            const service = await createService(`<meta name="after" content=".target">${element}`);
            const targets = [addTarget(), addTarget()];

            service.applyPatches();
            const inserted = targets.map(target => target.nextElementSibling);

            // Removing and reinserting an element would lose focus, hover and
            // clicks in progress
            const observer = new MutationObserver(() => undefined);
            observer.observe(container, { childList: true, subtree: true });
            service.applyPatches();
            service.applyPatches();
            const mutations = observer.takeRecords();
            observer.disconnect();

            expect(mutations).toEqual([]);
            targets.forEach((target, index) => expect(target.nextElementSibling).toBe(inserted[index]));
        });

        it('should insert a separate instance of a native federation component at each matched element', async () => {
            loadRemoteModule.mockResolvedValue({ PatchButtonComponent });
            const service = await createService('<meta name="after" content=".target">'
                + '<div nativeFederationModule="example" nativeFederationElement="PatchButtonComponent"></div>');
            const targets = [addTarget(), addTarget()];

            service.applyPatches();
            service.applyPatches();
            TestBed.inject(ApplicationRef).tick();

            expect(loadRemoteModule).toHaveBeenCalledExactlyOnceWith('example', 'PatchButtonComponent');
            expect(container.querySelectorAll('guac-patch-button')).toHaveLength(2);
            for (const target of targets) {
                expect(target.nextElementSibling?.tagName).toBe('GUAC-PATCH-BUTTON');
                expect(target.nextElementSibling?.textContent).toBe('Patch button');
            }
        });

        it('should destroy a native federation component once its element leaves the DOM', async () => {
            loadRemoteModule.mockResolvedValue({ PatchButtonComponent });
            const service = await createService('<meta name="after-children" content=".target">'
                + '<div nativeFederationModule="example" nativeFederationElement="PatchButtonComponent"></div>');
            const [removed] = [addTarget(), addTarget()];

            service.applyPatches();
            removed.remove();
            service.applyPatches();

            expect(PatchButtonComponent.destroyed).toBe(1);
        });

    });

});
