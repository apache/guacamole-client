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
import { ActivatedRoute } from '@angular/router';
import _ from 'lodash';
import { forkJoin, map, Observable } from 'rxjs';
import { SchemaService } from '../../rest/service/schema.service';
import { ImportConnection } from '../types/ImportConnection';
import { ParseError } from '../types/ParseError';
import { TransformConfig } from '../types/TransformConfig';

// A suffix that indicates that a particular header refers to a parameter
const PARAMETER_SUFFIX = ' (parameter)';

// A suffix that indicates that a particular header refers to an attribute
const ATTRIBUTE_SUFFIX = ' (attribute)';

/**
 * A service for parsing user-provided CSV connection data for bulk import.
 */
@Injectable()
export class ConnectionCSVService {

    constructor(private schemaService: SchemaService,
                private route: ActivatedRoute) {
    }

    /**
     * Returns an observable that emits a object detailing the connection
     * attributes for the current data source, as well as the connection
     * parameters for every protocol, for the current data source.
     *
     * The object is emitted will contain an "attributes" key that maps to
     * a set of attribute names, and a "protocolParameters" key that maps to an
     * object mapping protocol names to sets of parameter names for that protocol.
     *
     * The intended use case for this object is to determine if there is a
     * connection parameter or attribute with a given name, by e.g. checking the
     * path `.protocolParameters[protocolName]` to see if a protocol exists,
     * checking the path `.protocolParameters[protocolName][fieldName]` to see
     * if a parameter exists for a given protocol, or checking the path
     * `.attributes[fieldName]` to check if a connection attribute exists.
     *
     * @returns
     *     An observable that emits a object detailing the connection
     *     attributes and parameters for every protocol, for the current data
     *     source.
     */
    private getFieldLookups(): Observable<{
        attributes: Record<string, boolean>;
        protocolParameters: Record<string, Record<string, boolean>>;
    }> {

        // The current data source - the one that the connections will be
        // imported into
        const dataSource = this.route.snapshot.paramMap.get('dataSource')!;

        // Fetch connection attributes and protocols for the current data source
        return forkJoin([
            this.schemaService.getConnectionAttributes(dataSource),
            this.schemaService.getProtocols(dataSource)
        ]).pipe(
            map(([attributes, protocols]) => ({

                // Translate the forms and fields into a flat map of attribute
                // name to `true` boolean value
                attributes: attributes.reduce(
                    (attributeMap: Record<string, boolean>, form) => {
                        form.fields.forEach(
                            field => attributeMap[field.name] = true);
                        return attributeMap;
                    }, {}),

                // Translate the protocol definitions into a map of protocol
                // name to map of field name to `true` boolean value
                protocolParameters: _.mapValues(
                    protocols, protocol => protocol.connectionForms.reduce(
                        (protocolFieldMap: Record<string, boolean>, form) => {
                            form.fields.forEach(
                                field => protocolFieldMap[field.name] = true);
                            return protocolFieldMap;
                        }, {}))
            }))
        );
    }

    /**
     * Split a raw user-provided, semicolon-seperated list of identifiers into
     * an array of identifiers. If identifiers contain semicolons, they can be
     * escaped with backslashes, and backslashes can also be escaped using other
     * backslashes.
     *
     * @param rawIdentifiers
     *     The raw string value as fetched from the CSV.
     *
     * @returns
     *     An array of identifier values.
     */
    private splitIdentifiers(rawIdentifiers: string): string[] {

        // Keep track of whether a backslash was seen
        let escaped = false;

        return _.reduce(rawIdentifiers, (identifiers, ch) => {

            // The current identifier will be the last one in the final list
            let identifier = identifiers[identifiers.length - 1];

            // If a semicolon is seen, set the "escaped" flag and continue
            // to the next character
            if (!escaped && ch == '\\') {
                escaped = true;
                return identifiers;
            }

                // End the current identifier and start a new one if there's an
            // unescaped semicolon
            else if (!escaped && ch == ';') {
                identifiers.push('');
                return identifiers;
            }

            // In all other cases, just append to the identifier
            else {
                identifier += ch;
                escaped = false;
            }

            // Save the updated identifier to the list
            identifiers[identifiers.length - 1] = identifier;

            return identifiers;

        }, [''])

            // Filter out any 0-length (empty) identifiers
            .filter(identifier => identifier.length);

    }

    /**
     * Given a CSV header row, create and return a promise that will resolve to
     * a function that can take a CSV data row and return a ImportConnection
     * object. If an error occurs while parsing a particular row, the resolved
     * function will throw a ParseError describing the failure.
     *
     * The provided CSV must contain columns for name and protocol. Optionally,
     * the parentIdentifier of the target parent connection group, or a connection
     * name path e.g. "ROOT/parent/child" may be included. Additionally,
     * connection parameters or attributes can be included.
     *
     * The names of connection attributes and parameters are not guaranteed to
     * be mutually exclusive, so the CSV import format supports a distinguishing
     * suffix. A column may be explicitly declared to be a parameter using a
     * " (parameter)" suffix, or an attribute using an " (attribute)" suffix.
     * No suffix is required if the name is unique across connections and
     * attributes.
     *
     * If a parameter or attribute name conflicts with the standard
     * "name", "protocol", "group", or "parentIdentifier" fields, the suffix is
     * required.
     *
     * If a failure occurs while attempting to create the transformer function,
     * the promise will be rejected with a ParseError describing the failure.
     *
     * @returns
     *     A promise that will resolve to a function that translates a CSV data
     *     row (array of strings) to a ImportConnection object.
     */
    getCSVTransformer(headerRow: string[]): Promise<(rows: string[]) => ImportConnection> {

        // A promise that will be resolved with the transformer or rejected if
        // an error occurs
        return new Promise<(row: string[]) => ImportConnection>((resolve, reject) => {

            this.getFieldLookups().subscribe(({ attributes, protocolParameters }) => {

                // All configuration required to generate a function that can
                // transform a row of CSV into a connection object.
                // NOTE: This is a single object instead of a collection of variables
                // to ensure that no stale references are used - e.g. when one getter
                // invokes another getter
                const transformConfig: TransformConfig = {

                    // Callbacks for required fields
                    nameGetter    : undefined,
                    protocolGetter: undefined,

                    // Callbacks for a parent group ID or group path
                    groupGetter           : undefined,
                    parentIdentifierGetter: undefined,

                    // Callbacks for user and user group identifiers
                    usersGetter     : () => [],
                    userGroupsGetter: () => [],

                    // Callbacks that will generate either connection attributes or
                    // parameters. These callbacks will return a {type, name, value}
                    // object containing the type ("parameter" or "attribute"),
                    // the name of the attribute or parameter, and the corresponding
                    // value.
                    parameterOrAttributeGetters: []

                };

                // A set of all headers that have been seen so far. If any of these
                // are duplicated, the CSV is invalid.
                const headerSet: Record<string, boolean> = {};

                // Iterate through the headers one by one
                headerRow.forEach((rawHeader, index) => {

                    // Trim to normalize all headers
                    const header = rawHeader.trim();

                    // Check if the header is duplicated
                    if (headerSet[header]) {
                        reject(new ParseError({
                            message  : 'Duplicate CSV Header: ' + header,
                            key      : 'IMPORT.ERROR_DUPLICATE_CSV_HEADER',
                            variables: { HEADER: header }
                        }));
                        return;
                    }

                    // Mark that this particular header has already been seen
                    headerSet[header] = true;

                    // A callback that returns the field at the current index
                    const fetchFieldAtIndex = (row: string[]) => row[index];

                    // A callback that splits raw string identifier lists by
                    // semicolon characters into an array of identifiers
                    const identifierListCallback = (row: string[]) =>
                        this.splitIdentifiers(fetchFieldAtIndex(row));

                    // Set up the name callback
                    if (header == 'name')
                        transformConfig.nameGetter = fetchFieldAtIndex;

                    // Set up the protocol callback
                    else if (header == 'protocol')
                        transformConfig.protocolGetter = fetchFieldAtIndex;

                    // Set up the group callback
                    else if (header == 'group')
                        transformConfig.groupGetter = fetchFieldAtIndex;

                    // Set up the group parent ID callback
                    else if (header == 'parentIdentifier')
                        transformConfig.parentIdentifierGetter = fetchFieldAtIndex;

                    // Set the user identifiers callback
                    else if (header == 'users')
                        transformConfig.usersGetter = (
                            identifierListCallback);

                    // Set the user group identifiers callback
                    else if (header == 'groups')
                        transformConfig.userGroupsGetter = (
                            identifierListCallback);

                        // At this point, any other header might refer to a connection
                        // parameter or to an attribute

                    // A field may be explicitly specified as a parameter
                    else if (header.endsWith(PARAMETER_SUFFIX)) {

                        // Push as an explicit parameter getter
                        const parameterName = header.replace(PARAMETER_SUFFIX, '');
                        transformConfig.parameterOrAttributeGetters.push(
                            row => ({
                                type : 'parameters',
                                name : parameterName,
                                value: fetchFieldAtIndex(row)
                            })
                        );
                    }

                    // A field may be explicitly specified as a parameter
                    else if (header.endsWith(ATTRIBUTE_SUFFIX)) {

                        // Push as an explicit attribute getter
                        const attributeName = header.replace(ATTRIBUTE_SUFFIX, '');
                        transformConfig.parameterOrAttributeGetters.push(
                            row => ({
                                type : 'attributes',
                                name : attributeName,
                                value: fetchFieldAtIndex(row)
                            })
                        );
                    }

                        // The field is ambiguous, either an attribute or parameter,
                    // so the getter will have to determine this for every row
                    else
                        transformConfig.parameterOrAttributeGetters.push(row => {

                            // The name is just the value of the current header
                            const name = header;

                            // The value is at the index that matches the position
                            // of the header
                            const value = fetchFieldAtIndex(row);

                            // If no value is provided, do not check the validity
                            // of the parameter/attribute. Doing so would prevent
                            // the import of a list of mixed protocol types, where
                            // fields are only populated for protocols for which
                            // they are valid parameters. If a value IS provided,
                            // it must be a valid parameter or attribute for the
                            // current protocol, which will be checked below.
                            if (!value)
                                return {};

                            // The protocol may determine whether a field is
                            // a parameter or an attribute (or both)
                            const protocol = transformConfig.protocolGetter!(row);

                            // Any errors encountered while processing this row
                            const errors = [];

                            // Before checking whether it's an attribute or protocol,
                            // make sure this is a valid protocol to start
                            if (!protocolParameters[protocol])

                                // If the protocol is invalid, do not throw an error
                                // here - this will be handled further downstream
                                // by non-CSV-specific error handling
                                return {};

                            // Determine if the field refers to an attribute or a
                            // parameter (or both, which is an error)
                            const isAttribute = !!attributes[name];
                            const isParameter = !!_.get(
                                protocolParameters, [protocol, name]);

                            // If there is both an attribute and a protocol-specific
                            // parameter with the provided name, it's impossible to
                            // figure out which this should be
                            if (isAttribute && isParameter)
                                errors.push(new ParseError({
                                    message  : 'Ambiguous CSV Header: ' + header,
                                    key      : 'IMPORT.ERROR_AMBIGUOUS_CSV_HEADER',
                                    variables: { HEADER: header }
                                }));

                            // It's neither an attribute or a parameter
                            else if (!isAttribute && !isParameter)
                                errors.push(new ParseError({
                                    message  : 'Invalid CSV Header: ' + header,
                                    key      : 'IMPORT.ERROR_INVALID_CSV_HEADER',
                                    variables: { HEADER: header }
                                }));

                            // Choose the appropriate type
                            const type = isAttribute ? 'attributes' : 'parameters';

                            return { type, name, value, errors };
                        });
                });

                const {
                          nameGetter, protocolGetter,
                          parentIdentifierGetter, groupGetter,
                          usersGetter, userGroupsGetter,
                          parameterOrAttributeGetters
                      } = transformConfig;

                // Fail if the name wasn't provided. Note that this is a file-level
                // error, not specific to any connection.
                if (!nameGetter)
                    reject(new ParseError({
                        message  : 'The connection name must be provided',
                        key      : 'IMPORT.ERROR_REQUIRED_NAME_FILE',
                        variables: undefined
                    }));

                // Fail if the protocol wasn't provided
                if (!protocolGetter)
                    reject(new ParseError({
                        message  : 'The connection protocol must be provided',
                        key      : 'IMPORT.ERROR_REQUIRED_PROTOCOL_FILE',
                        variables: undefined
                    }));

                // The function to transform a CSV row into a connection object
                resolve((row: string[]) => {

                    // Get name and protocol
                    const name = nameGetter!(row);
                    const protocol = protocolGetter!(row);

                    // Get any users or user groups who should be granted access
                    const users = usersGetter(row);
                    const groups = userGroupsGetter(row);

                    // Get the parent group ID and/or group path
                    const group = groupGetter && groupGetter(row);
                    const parentIdentifier = (
                        parentIdentifierGetter && parentIdentifierGetter(row));

                    return new ImportConnection({
                        // Fields that are not protocol-specific
                        name,
                        protocol,
                        parentIdentifier,
                        group,
                        users,
                        groups,

                        // Fields that might potentially be either attributes or
                        // parameters, depending on the protocol
                        ...parameterOrAttributeGetters.reduce((values: Pick<ImportConnection, 'parameters' | 'attributes' | 'errors'>, getter) => {

                            // Determine the type, name, and value
                            const { type, name, value, errors } = getter(row);

                            // Set the value if available
                            if (type && name && value)
                                (values[type as 'parameters' | 'attributes'])[name] = value;

                            // If there were errors
                            if (errors && errors.length)
                                values.errors = [...values.errors, ...errors];

                            // Continue on to the next attribute or parameter
                            return values;

                        }, { parameters: {}, attributes: {}, errors: [] })
                    } as unknown as ImportConnection);

                });

            });

        });

    }

}
