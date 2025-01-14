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
import { AuthenticationService } from '../../auth/service/authentication.service';
import { ClientIdentifier } from '../types/ClientIdentifier';

/**
 * Encodes the given value as base64url, a variant of base64 defined by
 * RFC 4648: https://datatracker.ietf.org/doc/html/rfc4648#section-5.
 *
 * The "base64url" variant is identical to standard base64 except that it
 * uses "-" instead of "+", "_" instead of "/", and padding with "=" is
 * optional.
 *
 * @param value
 *     The string value to encode.
 *
 * @returns
 *     The provided string value encoded as unpadded base64url.
 */
const base64urlEncode = (value: string): string => {

    // Translate padded standard base64 to unpadded base64url
    return window.btoa(value).replace(/[+/=]/g,
        (str) => ({
            '+': '-',
            '/': '_',
            '=': ''
        })[str] as string
    );

};

/**
 * Decodes the given base64url or base64 string. The input string may
 * contain "=" padding characters, but this is not required.
 *
 * @param value
 *     The base64url or base64 value to decode.
 *
 * @returns
 *     The result of decoding the provided base64url or base64 string.
 */
const base64urlDecode = (value: string): string => {

    // Add any missing padding (standard base64 requires input strings to
    // be multiples of 4 in length, padded using '=')
    value += ([
        '',
        '===',
        '==',
        '='
    ])[value.length % 4];

    // Translate padded base64url to padded standard base64
    return window.atob(value.replace(/[-_]/g,
        (str) => ({
            '-': '+',
            '_': '/'
        })[str] as string
    ));
};

@Injectable({
    providedIn: 'root'
})
export class ClientIdentifierService {

    /**
     * Inject required services.
     */
    constructor(private authenticationService: AuthenticationService
    ) {
    }

    /**
     * Converts the given ClientIdentifier or ClientIdentifier-like object to
     * a String representation. Any object having the same properties as
     * ClientIdentifier may be used, but only those properties will be taken
     * into account when producing the resulting String.
     *
     * @param id
     *     The ClientIdentifier or ClientIdentifier-like object to convert to
     *     a String representation.
     *
     * @returns
     *     A deterministic String representation of the given ClientIdentifier
     *     or ClientIdentifier-like object.
     */
    getString(id: ClientIdentifier): string {
        return ClientIdentifierService.getString(id);
    }

    /**
     * Converts the given ClientIdentifier or ClientIdentifier-like object to
     * a String representation. Any object having the same properties as
     * ClientIdentifier may be used, but only those properties will be taken
     * into account when producing the resulting String.
     *
     * @param id
     *     The ClientIdentifier or ClientIdentifier-like object to convert to
     *     a String representation.
     *
     * @returns
     *     A deterministic String representation of the given ClientIdentifier
     *     or ClientIdentifier-like object.
     */
    static getString(id: ClientIdentifier): string {
        return base64urlEncode([
            id.id,
            id.type,
            id.dataSource
        ].join('\0'));
    }

    /**
     * Converts the given String into the corresponding ClientIdentifier. If
     * the provided String is not a valid identifier, it will be interpreted
     * as the identifier of a connection within the data source that
     * authenticated the current user.
     *
     * @param str
     *     The String to convert to a ClientIdentifier.
     *
     * @returns
     *     The ClientIdentifier represented by the given String.
     */
    fromString(str: string): ClientIdentifier {

        try {
            const values = base64urlDecode(str).split('\0');
            return new ClientIdentifier({
                id        : values[0],
                type      : values[1],
                dataSource: values[2]
            });
        }

            // If the provided string is invalid, transform into a reasonable guess
        catch (e) {
            return new ClientIdentifier({
                id        : str,
                type      : ClientIdentifier.Types.CONNECTION,
                dataSource: this.authenticationService.getDataSource() || 'default'
            });
        }

    }


}
