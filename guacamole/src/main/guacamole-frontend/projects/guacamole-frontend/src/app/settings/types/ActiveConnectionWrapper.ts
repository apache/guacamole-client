

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

import { ActiveConnection } from '../../rest/types/ActiveConnection';
import { Optional } from '../../util/utility-types';

/**
 * Wrapper for ActiveConnection which adds display-specific
 * properties, such as a checked option.
 */
export class ActiveConnectionWrapper {

    /**
     * The identifier of the data source associated with the
     * ActiveConnection wrapped by this ActiveConnectionWrapper.
     */
    dataSource: string;

    /**
     * The display name of this connection.
     */
    name?: string;

    /**
     * The date and time this session began, pre-formatted for display.
     */
    startDate: string;

    /**
     * The wrapped ActiveConnection.
     */
    activeConnection: ActiveConnection;

    /**
     * A flag indicating that the active connection has been selected.
     */
    checked: boolean;

    /**
     * Creates a new ActiveConnectionWrapper. This constructor initializes the properties of the
     * new ActiveConnectionWrapper with the corresponding properties of the given template.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     ActiveConnectionWrapper.
     */
    constructor(template: Optional<ActiveConnectionWrapper, 'checked'>) {
        this.dataSource = template.dataSource;
        this.name = template.name;
        this.startDate = template.startDate;
        this.activeConnection = template.activeConnection;
        this.checked = template.checked || false;
    }
}
