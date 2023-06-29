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

import { Component, Input, OnChanges, SimpleChanges, ViewEncapsulation } from '@angular/core';
import { ManagementPermissions } from '../../types/ManagementPermissions';
import { PageDefinition } from '../../../navigation/types/PageDefinition';
import keys from 'lodash/keys';
import { canonicalize } from "../../../locale/service/translation.service";

/**
 * Component which displays a set of tabs pointing to the same object within
 * different data sources, such as user accounts which span multiple data
 * sources.
 */
@Component({
    selector: 'data-source-tabs',
    templateUrl: './data-source-tabs.component.html',
    encapsulation: ViewEncapsulation.None
})
export class DataSourceTabsComponent implements OnChanges {

    /**
     * The permissions which dictate the management actions available
     * to the current user.
     */
    @Input() permissions: Record<string, ManagementPermissions> | null = null;

    /**
     * A function which returns the URL of the object within a given
     * data source. The relevant data source will be made available to
     * the Angular expression defining this function as the
     * "dataSource" variable. No other values will be made available,
     * including values from the scope.
     */
    @Input() url?: (dataSource: string) => string;

    /**
     * The set of pages which each manage the same object within different
     * data sources.
     */
    pages: PageDefinition[] = [];

    ngOnChanges(changes: SimpleChanges): void {
        if (changes['permissions']) {
            const permissions = changes['permissions'].currentValue as Record<string, ManagementPermissions>;

            this.pages = [];

            const dataSources = keys(this.permissions).sort();
            dataSources.forEach(dataSource => {

                // Determine whether data source contains this object
                const managementPermissions = permissions[dataSource];
                const exists = !!managementPermissions.identifier;

                // Data source is not relevant if the associated object does not
                // exist and cannot be created
                const readOnly = !managementPermissions.canSaveObject;
                if (!exists && readOnly)
                    return;

                // Determine class name based on read-only / linked status
                let className;
                if (readOnly) className = 'read-only';
                else if (exists) className = 'linked';
                else className = 'unlinked';

                // Add page entry
                this.pages!.push(new PageDefinition({
                    name: canonicalize('DATA_SOURCE_' + dataSource) + '.NAME',
                    url: this.url?.(dataSource) || '',
                    className: className
                }));

            });

        }

    }


}
