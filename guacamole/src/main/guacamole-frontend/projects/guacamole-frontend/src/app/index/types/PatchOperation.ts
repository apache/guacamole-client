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
 * Represents a single HTML patching operation which will be applied
 * to the raw HTML of a template. The name of the patching operation
 * MUST be one of the valid names defined within
 * PatchOperation.Operations.
 */
export class PatchOperation {

    /**
     * Create a new PatchOperation.
     *
     * @param name
     *     The name of the patching operation that will be applied. Valid
     *     names are defined within PatchOperation.Operations.
     *
     * @param selector
     *     The CSS selector which determines which elements within a
     *     template will be affected by the patch operation.
     */
    constructor(private name: string, private selector: string) {
    }

    /**
     * Applies this patch operation to the template defined by the
     * given root element, which must be a single element wrapped by
     * JQuery.
     *
     * @param root
     *     The root element of the template to which
     *     this patch operation should be applied.
     *
     * @param elements
     *     The elements which should be applied by the patch
     *     operation. For example, if the patch operation is inserting
     *     elements, these are the elements that will be inserted.
     */
    apply(root: Element[], elements: Element[]): void {
        PatchOperation.Operations[this.name](root, this.selector, elements);
    }

    /**
     * Mapping of all valid patch operation names to their corresponding
     * implementations. Each implementation accepts the same three
     * parameters: the root element of the template being patched, the CSS
     * selector determining which elements within the template are patched,
     * and an array of elements which make up the body of the patch.
     */
    static Operations: Record<string, PatchOperation.PatchOperationFunction> = {

        /**
         * Inserts the given elements before the elements matched by the
         * provided CSS selector.
         */
        'before': (root, selector, elements) => {
            root.forEach(rootElement => {
                const targets = rootElement.querySelectorAll(selector);
                targets.forEach(target => {
                    elements.forEach(element => {
                        target.before(element);
                    });
                });
            });
        },

        /**
         * Inserts the given elements after the elements matched by the
         * provided CSS selector.
         */
        'after': (root, selector, elements) => {
            root.forEach(rootElement => {
                const targets = rootElement.querySelectorAll(selector);
                targets.forEach(target => {
                    elements.forEach(element => {
                        target.after(element);
                    });
                });
            });
        },

        /**
         * Replaces the elements matched by the provided CSS selector with
         * the given elements.
         */
        'replace': (root, selector, elements) => {
            root.forEach(rootElement => {
                const targets = rootElement.querySelectorAll(selector);
                targets.forEach(target => {
                    target.replaceWith(...elements);
                });
            });
        },

        /**
         * Inserts the given elements within the elements matched by the
         * provided CSS selector, before any existing children.
         */
        'before-children': (root, selector, elements) => {
            root.forEach(rootElement => {
                const targets = rootElement.querySelectorAll(selector);
                targets.forEach(target => {
                    target.prepend(...elements);
                });
            });
        },

        /**
         * Inserts the given elements within the elements matched by the
         * provided CSS selector, after any existing children.
         */
        'after-children': (root, selector, elements) => {
            root.forEach(rootElement => {
                const targets = rootElement.querySelectorAll(selector);
                targets.forEach(target => {
                    target.append(...elements);
                });
            });
        },

        /**
         * Inserts the given elements within the elements matched by the
         * provided CSS selector, replacing any existing children.
         */
        'replace-children': (root, selector, elements) => {
            root.forEach(rootElement => {
                const targets = rootElement.querySelectorAll(selector);
                targets.forEach(target => {
                    while (target.firstChild) {
                        target.removeChild(target.firstChild);
                    }
                    elements.forEach(element => {
                        target.appendChild(element);
                    });
                });
            });

        }

    };

}

export namespace PatchOperation {

    /**
     * A function which applies a specific patch operation to the
     * given element.
     *
     * @param {Element[]} root
     *     The root element of the template being patched.
     *
     * @param {String} selector
     *     The CSS selector which determines where this patch operation
     *     should be applied within the template defined by root.
     *
     * @param {Element[]} elements
     *     The contents of the patch which should be applied to the
     *     template defined by root at the locations selected by the
     *     given CSS selector.
     */
    export type PatchOperationFunction = (root: Element[], selector: string, elements: Element[]) => void;
}

