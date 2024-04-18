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

import { Injectable } from '@angular/core';
import { GuacEventService } from 'guacamole-frontend-lib';
import cloneDeep from 'lodash/cloneDeep';
import isEqual from 'lodash/isEqual';
import { AuthenticationService } from '../auth/service/authentication.service';
import { GuacFrontendEventArguments } from '../events/types/GuacFrontendEventArguments';
import { isDefined } from '../util/is-defined';

@Injectable({
    providedIn: 'root'
})
export class SessionStorageFactory {

    /**
     * Inject required services.
     */
    constructor(private authenticationService: AuthenticationService,
                private readonly guacEventService: GuacEventService<GuacFrontendEventArguments>) {
    }

    /**
     * Creates session-local storage that uses the provided default value or
     * getter to obtain new values as necessary. Beware that if the default is
     * an object, the resulting getter provide deep copies for new values.
     *
     * @param template
     *     The default value for new users, or a getter which returns a newly-
     *     created default value.
     *
     * @param destructor
     *     Function which will be called just before the stored value is
     *     destroyed on logout, if a value is stored.
     *
     * @returns
     *     A getter/setter which returns or sets the current value of the new
     *     session-local storage. Newly-set values will only persist of the
     *     user is actually logged in.
     */
    create(template: Function | any, destructor?: Function): Function {

        /**
         * Whether new values may be stored and retrieved.
         */
        let enabled = !!this.authenticationService.getCurrentToken();

        /**
         * Getter which returns the default value for this storage.
         */
        let getter: Function;

        // If getter provided, use that
        if (typeof template === 'function')
            getter = template;

            // Otherwise, create and maintain a deep copy (automatically cached to
        // avoid "infdig" errors)
        else {
            let cached = cloneDeep(template);
            getter = function getIndependentCopy() {

                // Reset to template only if changed externally, such that
                // session storage values can be safely used in scope watches
                // even if not logged in
                if (!isEqual(cached, template))
                    cached = cloneDeep(template);

                return cached;

            };
        }

        /**
         * The current value of this storage, or undefined if not yet set.
         */
        let value: any = undefined;

        // Reset value and allow storage when the user is logged in
        this.guacEventService.on('guacLogin')
            .subscribe(() => {
                enabled = true;
                value = undefined;
            });

        // Reset value and disallow storage when the user is logged out
        this.guacEventService.on('guacLogout')
            .subscribe(() => {

                // Call destructor before storage is torn down
                if (isDefined(value) && destructor)
                    destructor(value);

                // Destroy storage
                enabled = false;
                value = undefined;

            });

        // Return getter/setter for value
        return function sessionLocalGetterSetter(newValue: any) {

            // Only actually store/retrieve values if enabled
            if (enabled) {

                // Set value if provided
                if (isDefined(newValue))
                    value = newValue;

                // Obtain new value if unset
                if (!isDefined(value))
                    value = getter();

                // Return current value
                return value;

            }

            // Otherwise, just pretend to store/retrieve
            return isDefined(newValue) ? newValue : getter();

        };

    }


}
