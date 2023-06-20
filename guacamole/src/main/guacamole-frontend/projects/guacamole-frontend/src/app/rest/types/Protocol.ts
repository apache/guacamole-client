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

import { Form } from './Form';
import { canonicalize } from '../../form/components/form-field-base/form-field-base.component';

/**
 * Returned by REST API calls when representing the data
 * associated with a supported remote desktop protocol.
 */
export class Protocol {

    /**
     * The name which uniquely identifies this protocol.
     */
    name?: string;

    /**
     * An array of forms describing all known parameters for a connection
     * using this protocol, including their types and other information.
     *
     * @default []
     */
    connectionForms: Form[];

    /**
     * An array of forms describing all known parameters relevant to a
     * sharing profile whose primary connection uses this protocol,
     * including their types, and other information.
     *
     * @default []
     */
    sharingProfileForms: Form[];

    /**
     * Creates a new Protocol.
     *
     * @param template
     *     The object whose properties should be copied within the new
     *     Protocol.
     */
    constructor(template: Partial<Protocol> = {}) {
        this.name = template.name;
        this.connectionForms = template.connectionForms || [];
        this.sharingProfileForms = template.sharingProfileForms || [];
    }

    /**
     * Returns the translation string namespace for the protocol having the
     * given name. The namespace will be of the form:
     *
     * <code>PROTOCOL_NAME</code>
     *
     * where <code>NAME</code> is the protocol name transformed via
     * canonicalize().
     *
     * @param protocolName
     *     The name of the protocol.
     *
     * @returns
     *     The translation namespace for the protocol specified, or undefined if no
     *     namespace could be generated.
     */
    static getNamespace(protocolName: string): string | undefined {

        // Do not generate a namespace if no protocol is selected
        if (!protocolName)
            return undefined;

        return 'PROTOCOL_' + canonicalize(protocolName);

    }

    /**
     * Given the internal name of a protocol, produces the translation string
     * for the localized version of that protocol's name. The translation
     * string will be of the form:
     *
     * <code>NAMESPACE.NAME<code>
     *
     * where <code>NAMESPACE</code> is the namespace generated from
     * Protocol.getNamespace().
     *
     * @param protocolName
     *     The name of the protocol.
     *
     * @returns
     *     The translation string which produces the localized name of the
     *     protocol specified.
     */
    static getName(protocolName: string): string | undefined {
        return Protocol.getNamespace(protocolName) + '.NAME';
    }

}
