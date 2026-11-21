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

import $ from 'jquery';

/**
 * Represents a single HTML patching operation which will be applied
 * to the elements of the DOM matched by a CSS selector. The name of the
 * patching operation MUST be one of the valid names defined within
 * PatchOperation.Operations.
 */
export class PatchOperation {

    /**
     * The elements this patch operation has been applied to, along with
     * all elements it inserted itself. HTML patches are applied to the DOM
     * after each change detection, so this patch operation must be applied
     * to each element only once.
     */
    private readonly patchedElements = new WeakSet<Element>();

    /**
     * Each element this patch operation inserted elements next to, along
     * with those elements, if the patch operation is "before" or "after".
     * Angular does not know of the inserted elements and leaves them in
     * place when it removes the element they were inserted next to.
     */
    private insertedSiblings: { target: Element; elements: Element[] }[] = [];

    /**
     * Creates a new PatchOperation.
     *
     * @param name
     *     The name of the patching operation that will be applied. Valid
     *     names are defined within PatchOperation.Operations.
     *
     * @param selector
     *     The CSS selector which determines which elements will be
     *     affected by the patch operation.
     */
    constructor(private name: string, private selector: string) {
    }

    /**
     * Applies this patch operation to each element within the DOM tree
     * defined by the given root element which matches the selector of this
     * patch operation and has not been patched by it before. Each such
     * element receives its own copy of the elements of the patch, as each
     * rendered instance of a patched template did in the AngularJS
     * frontend. Elements inserted next to an element which has since left
     * the DOM are removed.
     *
     * @param root
     *     The JQuery-wrapped root element of the DOM tree to which this
     *     patch operation should be applied.
     *
     * @param createElements
     *     A function which returns a new copy of the elements which should
     *     be applied by the patch operation each time it is called. For
     *     example, if the patch operation is inserting elements, these are
     *     the elements that will be inserted.
     */
    apply(root: JQuery, createElements: () => Element[]): void {

        // Remove elements which were inserted next to an element that has
        // been removed from the DOM
        this.insertedSiblings = this.insertedSiblings.filter(({ target, elements }) => {

            if (target.isConnected)
                return true;

            elements.forEach(element => element.remove());
            return false;

        });

        // Apply this operation to each matched element only once
        for (const target of root.find(this.selector).toArray()) {

            if (this.patchedElements.has(target))
                continue;

            const elements = createElements();
            PatchOperation.Operations[this.name]($(target), elements);

            // Never apply this operation again to the same element or to
            // the elements it inserted itself
            this.patchedElements.add(target);
            elements.forEach(element => {
                this.patchedElements.add(element);
                element.querySelectorAll('*').forEach(child => this.patchedElements.add(child));
            });

            if (this.name === 'before' || this.name === 'after')
                this.insertedSiblings.push({ target, elements });

        }

    }

    /**
     * Mapping of all valid patch operation names to their corresponding
     * implementations. Each implementation accepts the same two
     * parameters: the elements being patched and an array of elements
     * which make up the body of the patch.
     */
    static Operations: Record<string, PatchOperation.PatchOperationFunction> = {

        /**
         * Inserts the given elements before the matched elements.
         */
        'before': (targets, elements) => {
            targets.before(elements);
        },

        /**
         * Inserts the given elements after the matched elements.
         */
        'after': (targets, elements) => {
            targets.after(elements);
        },

        /**
         * Replaces the matched elements with the given elements.
         */
        'replace': (targets, elements) => {
            targets.replaceWith(elements);
        },

        /**
         * Inserts the given elements within the matched elements, before
         * any existing children.
         */
        'before-children': (targets, elements) => {
            targets.prepend(elements);
        },

        /**
         * Inserts the given elements within the matched elements, after
         * any existing children.
         */
        'after-children': (targets, elements) => {
            targets.append(elements);
        },

        /**
         * Inserts the given elements within the matched elements,
         * replacing any existing children.
         */
        'replace-children': (targets, elements) => {
            targets.empty().append(elements);
        }

    };

}

export namespace PatchOperation {

    /**
     * A function which applies a specific patch operation to the
     * given elements.
     *
     * @param targets
     *     The JQuery-wrapped elements being patched.
     *
     * @param elements
     *     The contents of the patch which should be applied to the
     *     elements being patched.
     */
    export type PatchOperationFunction = (targets: JQuery, elements: Element[]) => void;
}
