

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
 * Provides the NotificationProgress class, which describes the current status
 * of an operation, and how much of that operation remains to be performed.
 */
export class NotificationProgress {

    /**
     * Creates a new NotificationProgress.
     *
     * @param text
     *     The text describing the operation progress. For the sake of i18n,
     *     the variable VALUE should be applied within the translation
     *     string for formatting plurals, etc., while UNIT should be used
     *     for the progress unit, if any.
     *
     * @param value
     *     The current state of operation progress, as an arbitrary number
     *     which increases as the operation continues.
     *
     * @param unit
     *     The unit of the arbitrary value, if that value has an associated
     *     unit.
     *
     * @param ratio
     *     If known, the current status of the operation as a value between 0
     *     and 1 inclusive, where 0 is not yet started, and 1 is complete.
     */
    constructor(public text: string, public value: number, public unit?: string, public ratio?: number) {
    }

}
