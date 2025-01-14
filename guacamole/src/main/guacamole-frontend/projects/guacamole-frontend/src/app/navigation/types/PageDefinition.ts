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
 * Pairs the URL of a page with
 * an arbitrary, human-readable name.
 */
export class PageDefinition {

    /**
     * The name of the page, which should be a translation table key.
     * Alternatively, this may also be a list of names, where the final
     * name represents the page and earlier names represent categorization.
     * Those categorical names may be rendered hierarchically as a system
     * of menus, tabs, etc.
     */
    name: string | string[];

    /**
     * The URL of the page.
     */
    url: string;

    /**
     * The CSS class name to associate with this page, if any. This will be
     * an empty string by default.
     */
    className?: string;

    /**
     * A numeric value denoting the relative sort order when compared to
     * other sibling PageDefinitions. If unspecified, sort order is
     * determined by the system using the PageDefinition.
     */
    weight?: number;


    /**
     * Creates a new PageDefinition object.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     PageDefinition.
     */
    constructor(template: PageDefinition) {
        this.name = template.name;
        this.url = template.url;
        this.className = template.className || '';
        this.weight = template.weight;
    }

}
