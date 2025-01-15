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
import { TranslocoService } from '@ngneat/transloco';
import { take } from 'rxjs';
import { AuthenticationResult } from '../../../auth/types/AuthenticationResult';

/**
 * A component which graphically represents an individual user.
 */
@Component({
    selector: 'guac-user-item',
    templateUrl: './guac-user-item.component.html',
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class GuacUserItemComponent implements OnChanges {

    /**
     * The username of the user represented by this guacUserItem.
     */
    @Input() username?: string | undefined;

    /**
     * The string to display when listing the user having the provided
     * username. Generally, this will be the username itself, but can
     * also be an arbitrary human-readable representation of the user,
     * or null if the display name is not yet determined.
     */
    displayName: string | null;

    constructor(private translocoService: TranslocoService) {
        this.displayName = null;
    }

    /**
     * Returns whether the username provided to this directive denotes
     * a user that authenticated anonymously.
     *
     * @returns {Boolean}
     *     true if the username provided represents an anonymous user,
     *     false otherwise.
     */
    isAnonymous(): boolean {
        return this.username === AuthenticationResult.ANONYMOUS_USERNAME;
    }

    ngOnChanges(changes: SimpleChanges): void {

        // Update display name whenever provided username changes
        if (changes['username']) {

            const username = changes['username'].currentValue as string;

            // If the user is anonymous, pull the display name for anonymous
            // users from the translation service
            if (this.isAnonymous()) {
                this.translocoService.selectTranslate<string>('LIST.TEXT_ANONYMOUS_USER')
                    .pipe(take(1))
                    .subscribe(anonymousDisplayName => {
                        this.displayName = anonymousDisplayName;
                    });
            }

            // For all other users, use the username verbatim
            else
                this.displayName = username;
        }

    }


}
