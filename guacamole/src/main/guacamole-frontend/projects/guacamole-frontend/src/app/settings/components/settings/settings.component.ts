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

import { Component, OnInit, ViewEncapsulation } from '@angular/core';
import { PageDefinition } from '../../../navigation/types/PageDefinition';
import { UserPageService } from '../../../manage/services/user-page.service';

/**
 * The component for the general settings page.
 */
@Component({
    selector: 'guac-settings',
    templateUrl: './settings.component.html',
    encapsulation: ViewEncapsulation.None
})
export class SettingsComponent implements OnInit {

    /**
     * The array of settings pages available to the current user, or null if
     * not yet known.
     */
    settingsPages: PageDefinition[] | null = null;

    constructor(private userPageService: UserPageService) {
    }

    /**
     * Retrieve settings pages.
     */
    ngOnInit(): void {
        this.userPageService.getSettingsPages()
            .subscribe(pages => {
                this.settingsPages = pages;
            });
    }

}
