

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
import { parse as parseCSVData } from 'csv-parse/browser/esm/sync';
import forEach from 'lodash/forEach';
import get from 'lodash/get';
import mapKeys from 'lodash/mapKeys';
import mapValues from 'lodash/mapValues';
import setWith from 'lodash/setWith';
import trim from 'lodash/trim';
import { firstValueFrom, map } from 'rxjs';
import { parse as parseYAMLData } from 'yaml';
import { ConnectionGroupService } from '../../rest/service/connection-group.service';
import { SchemaService } from '../../rest/service/schema.service';
import { Connection } from '../../rest/types/Connection';
import { ConnectionGroup } from '../../rest/types/ConnectionGroup';
import { DirectoryPatch } from '../../rest/types/DirectoryPatch';
import { ConnectionImportConfig } from '../types/ConnectionImportConfig';
import { ImportConnection } from '../types/ImportConnection';
import { ParseError } from '../types/ParseError';
import { ParseResult } from '../types/ParseResult';
import { TreeLookups } from '../types/TreeLookups';
import { ConnectionCSVService } from './connection-csv.service';

/**
 * A particularly unfriendly looking error that the CSV parser throws if a
 * binary file parse attempt is made. If at all possible, this message should
 * never be displayed to the user since it makes it look like the application
 * is broken. As such, the code will attempt to filter out this error and print
 * something a bit more generic. Lowercased for slightly fuzzier matching.
 */
const BINARY_CSV_ERROR_MESSAGE = 'Argument must be a Buffer'.toLowerCase();

/**
 * The identifier of the root connection group, under which all other groups
 * and connections exist.
 */
const ROOT_GROUP_IDENTIFIER: string = 'ROOT';

/**
 * A service for parsing user-provided JSON, YAML, or JSON connection data into
 * an appropriate format for bulk uploading using the PATCH REST endpoint.
 */
@Injectable()
export class ConnectionParseService {

    constructor(private route: ActivatedRoute,
                private schemaService: SchemaService,
                private connectionCSVService: ConnectionCSVService,
                private connectionGroupService: ConnectionGroupService) {
    }

    /**
     * Perform basic checks, common to all file types - namely that the parsed
     * data is an array, and contains at least one connection entry. Returns an
     * error if any of these basic checks fails.
     *
     * @returns
     *     An error describing the parsing failure, if one of the basic checks
     *     fails.
     */
    private performBasicChecks(parsedData: any): ParseError | undefined {

        // Make sure that the file data parses to an array (connection list)
        if (!(parsedData instanceof Array))
            return new ParseError({
                message  : 'Import data must be a list of connections',
                key      : 'IMPORT.ERROR_ARRAY_REQUIRED',
                variables: undefined
            });

        // Make sure that the connection list is not empty - contains at least
        // one connection
        if (!parsedData.length)
            return new ParseError({
                message  : 'The provided file is empty',
                key      : 'IMPORT.ERROR_EMPTY_FILE',
                variables: undefined
            });

        return undefined;
    }

    /**
     * Returns a promise that resolves to a TreeLookups object containing maps
     * useful for processing user-supplied connections to be imported, derived
     * from the current connection group tree, starting at the ROOT group.
     *
     * @returns
     *     A promise that resolves to a TreeLookups object containing maps
     *     useful for processing connections.
     */
    private getTreeLookups() {

        // The current data source - defines all the groups that the connections
        // might be imported into
        const dataSource = this.route.snapshot.paramMap.get('dataSource')!;

        return new Promise<TreeLookups>((resolve) => {


            this.connectionGroupService.getConnectionGroupTree(dataSource).subscribe(
                rootGroup => {

                    const lookups = new TreeLookups({});

                    // Add the specified group to the lookup, appending all specified
                    // prefixes, and then recursively call saveLookups for all children
                    // of the group, appending to the prefix for each level
                    const saveLookups = (prefix: string, group: ConnectionGroup) => {

                        // To get the path for the current group, add the name
                        const currentPath = prefix + group.name;

                        // Add the current path to the identifier map
                        lookups.groupPathsByIdentifier[currentPath] = group.identifier!;

                        // Add the current identifier to the path map
                        lookups.groupIdentifiersByPath[group.identifier!] = currentPath;

                        // Add each connection to the connection map
                        forEach(group.childConnections,
                            connection => setWith(
                                lookups.connectionIdsByGroupAndName,
                                [group.identifier!, connection.name!],
                                connection.identifier, Object));

                        // Add each child group to the lookup
                        const nextPrefix = currentPath + '/';
                        forEach(group.childConnectionGroups,
                            childGroup => saveLookups(nextPrefix, childGroup));

                    }

                    // Start at the root group
                    saveLookups('', rootGroup);

                    // Resolve with the now fully-populated lookups
                    resolve(lookups);

                });

        });

    }

    /**
     * Returns a promise that will resolve to a transformer function that will
     * perform various checks and transforms relating to the connection group
     * tree hierarchy, pushing any errors into the resolved connection object.
     * It will:
     * - Ensure that a connection specifies either a valid group path (no path
     *   defaults to ROOT), or a valid parent group identifier, but not both
     * - Ensure that this connection does not duplicate another connection
     *   earlier in the import file
     * - Handle import connections that match existing connections connections
     *   based on the provided import config.
     *
     * The group set on the connection may begin with the root identifier, a
     * leading slash, or may omit the root identifier entirely. The group may
     * optionally end with a trailing slash.
     *
     * @param importConfig
     *     The configuration options selected by the user prior to import.
     *
     * @returns
     *     A promise that will resolve to a function that will apply various
     *     connection tree based checks and transforms to this connection.
     */
    private getTreeTransformer(importConfig: ConnectionImportConfig): Promise<(connection: ImportConnection) => ImportConnection> {

        // A map of group path with connection name, to connection object, used
        // for detecting duplicate connections within the import file itself
        const connectionsInFile = {};

        return this.getTreeLookups().then(treeLookups => connection => {

            const {
                groupPathsByIdentifier, groupIdentifiersByPath,
                connectionIdsByGroupAndName
            } = treeLookups;

            const providedIdentifier = connection.parentIdentifier;

            // The normalized group path for this connection, of the form
            // "ROOT/parent/child"
            let group;

            // The identifier for the parent group of this connection
            let parentIdentifier;

            // The operator to apply for this connection
            let op = DirectoryPatch.Operation.ADD;

            // If both are specified, the parent group is ambiguous
            if (providedIdentifier && connection.group) {
                connection.errors.push(new ParseError({
                    message  : 'Only one of group or parentIdentifier can be set',
                    key      : 'IMPORT.ERROR_AMBIGUOUS_PARENT_GROUP',
                    variables: undefined
                }));
                return connection;
            }

            // If a parent group identifier is present, but not valid
            else if (providedIdentifier
                && !groupPathsByIdentifier[providedIdentifier]) {
                connection.errors.push(new ParseError({
                    message  : 'No group with identifier: ' + providedIdentifier,
                    key      : 'IMPORT.ERROR_INVALID_GROUP_IDENTIFIER',
                    variables: { IDENTIFIER: providedIdentifier }
                }));
                return connection;
            }

            // If the parent identifier is valid, use it to determine the path
            else if (providedIdentifier) {
                parentIdentifier = providedIdentifier;
                group = groupPathsByIdentifier[providedIdentifier];
            }

                // If a user-supplied group path is provided, attempt to normalize
            // and match it to an existing connection group
            else if (connection.group) {

                // The group path extracted from the user-provided connection,
                // to be translated into an absolute path starting at the root
                group = connection.group;

                // If the provided group isn't a string, it can never be valid
                if (typeof group !== 'string') {
                    connection.errors.push(new ParseError({
                        message  : 'Invalid group type - must be a string',
                        key      : 'IMPORT.ERROR_INVALID_GROUP_TYPE',
                        variables: undefined
                    }));
                    return connection;
                }

                // Allow the group to start with a leading slash instead
                // of explicitly requiring the root connection group
                if (group.startsWith('/'))
                    group = ROOT_GROUP_IDENTIFIER + group;

                // Allow groups to begin directly with the path under the root
                else if (!group.startsWith(ROOT_GROUP_IDENTIFIER))
                    group = ROOT_GROUP_IDENTIFIER + '/' + group;

                // Allow groups to end with a trailing slash
                if (group.endsWith('/'))
                    group = group.slice(0, -1);

                // Look up the parent identifier for the specified group path
                parentIdentifier = groupPathsByIdentifier[group];

                // If the group doesn't match anything in the tree
                if (!parentIdentifier) {
                    connection.errors.push(new ParseError({
                        message  : 'No group found named: ' + connection.group,
                        key      : 'IMPORT.ERROR_INVALID_GROUP',
                        variables: { GROUP: connection.group }
                    }));
                    return connection;
                }

            }

            // If no group is specified at all, default to the root group
            else {
                parentIdentifier = ROOT_GROUP_IDENTIFIER;
                group = ROOT_GROUP_IDENTIFIER;
            }

            // The full path, of the form "ROOT/Child Group/Connection Name"
            const path = group + '/' + connection.name;

            // Error out if this is a duplicate of a connection already in the
            // file
            if (!!get(connectionsInFile, path))
                connection.errors.push(new ParseError({
                    message  : 'Duplicate connection in file: ' + path,
                    key      : 'IMPORT.ERROR_DUPLICATE_CONNECTION_IN_FILE',
                    variables: { NAME: connection.name, PATH: group }
                }));

            // Mark the current path as already seen in the file
            setWith(connectionsInFile, path, connection, Object);

            // Check if this would be an update to an existing connection
            const existingIdentifier = get(connectionIdsByGroupAndName,
                [parentIdentifier, connection.name]);

            // The default behavior is to create connections if no conflict
            let importMode = ImportConnection.ImportMode.CREATE;
            let identifier;

            // If updates to existing connections are disallowed
            if (existingIdentifier && importConfig.existingConnectionMode ===
                ConnectionImportConfig.ExistingConnectionMode.REJECT)
                connection.errors.push(new ParseError({
                    message  : 'Rejecting update to existing connection: ' + path,
                    key      : 'IMPORT.ERROR_REJECT_UPDATE_CONNECTION',
                    variables: { NAME: connection.name, PATH: group }
                }));

            // If the connection is being replaced, set the existing identifer
            else if (existingIdentifier) {
                identifier = existingIdentifier;
                importMode = ImportConnection.ImportMode.REPLACE;
            } else
                importMode = ImportConnection.ImportMode.CREATE;

            // Set the import mode, normalized path, and validated identifier
            return new ImportConnection({
                ...connection,
                importMode, group, identifier, parentIdentifier
            });

        });
    }

    /**
     * Returns a promise that resolves to a map of all valid protocols to a map
     * of connection parameter names to a map of lower-cased and trimmed option
     * values for that parameter to the actual valid option value.
     *
     * This format is designed for easy retrieval of corrected parameter values
     * if the user-provided value matches a valid option except for case or
     * leading/trailing whitespace.
     *
     * If a parameter has no options (i.e. any string value is allowed), the
     * parameter name will map to a null value.
     *
     * @returns
     *     A promise that resolves to a map of all valid protocols to parameter
     *     names to valid values.
     */
    private getProtocolParameterOptions(): Promise<Record<string, Record<string, Record<string, string> | null>>> {

        // The current data source - the one that the connections will be
        // imported into
        const dataSource = this.route.snapshot.paramMap.get('dataSource')!;

        // Fetch the protocols and convert to a set of valid protocol names
        return firstValueFrom(this.schemaService.getProtocols(dataSource).pipe(map(
                protocols => mapValues(protocols, ({ connectionForms }) => {

                    const fieldMap: Record<string, Record<string, string> | null> = {};

                    // Go through all the connection forms and get the fields for each
                    connectionForms.forEach(({ fields }) => fields.forEach(field => {

                        const { name, options } = field;

                        // Set the value to null to indicate that there are no options
                        if (!options)
                            fieldMap[name] = null;

                            // Set the value to a map of lowercased/trimmed option values
                        // to actual option values
                        else
                            fieldMap[name] = mapKeys(
                                options, option => option.trim().toLowerCase());

                    }));

                    return fieldMap;
                }))
            )
        );
    }

    /**
     * Resolves to function that will perform field-level (not connection
     * hierarchy dependent) checks and transforms to a provided connection,
     * returning the transformed connection.
     *
     * @returns
     *     A promise resolving to a function that will apply field-level
     *     transforms and checks to a provided connection, returning the
     *     transformed connection.
     */
    private getFieldTransformer(): Promise<(connection: ImportConnection) => ImportConnection> {

        return this.getProtocolParameterOptions().then(protocols => connection => {

            // Ensure that a protocol was specified for this connection
            const protocol = connection.protocol;
            if (!protocol)
                connection.errors.push(new ParseError({
                    message  : 'Missing required protocol field',
                    key      : 'IMPORT.ERROR_REQUIRED_PROTOCOL_CONNECTION',
                    variables: undefined
                }));

            // Ensure that a valid protocol was specified for this connection
            if (!protocols[protocol])
                connection.errors.push(new ParseError({
                    message  : 'Invalid protocol: ' + protocol,
                    key      : 'IMPORT.ERROR_INVALID_PROTOCOL',
                    variables: { PROTOCOL: protocol }
                }));

            // Ensure that a name was specified for this connection
            if (!connection.name)
                connection.errors.push(new ParseError({
                    message  : 'Missing required name field',
                    key      : 'IMPORT.ERROR_REQUIRED_NAME_CONNECTION',
                    variables: undefined
                }));

            // Ensure that the specified user list, if any, is an array
            const users = connection.users;
            if (users) {

                // Ensure all users in the array are trimmed strings
                if (Array.isArray(users))
                    connection.users = users.map(user => String(user).trim());

                else
                    connection.errors.push(new ParseError({
                        message  : 'Invalid users list - must be an array',
                        key      : 'IMPORT.ERROR_INVALID_USERS_TYPE',
                        variables: undefined
                    }));

            }

            // Ensure that the specified user group list, if any, is an array
            const groups = connection.groups;
            if (groups) {

                // Ensure all groups in the array are trimmed strings
                if (Array.isArray(groups))
                    connection.groups = groups.map(group => String(group).trim());

                else
                    connection.errors.push(new ParseError({
                        message  : 'Invalid groups list - must be an array',
                        key      : 'IMPORT.ERROR_INVALID_USER_GROUPS_TYPE',
                        variables: undefined
                    }));

            }

            // If the protocol is not valid, there's no point in trying to check
            // parameter case sensitivity
            if (!protocols[protocol])
                return connection;

            forEach(connection.parameters, (value, name) => {

                // An explicit null value for a parameter is valid - do not
                // process it further
                if (value === null)
                    return;

                // All non-null connection parameters must be strings.
                const stringValue = String(value);

                // Convert the provided value to the format that would match
                // the lookup object format
                const comparisonValue = stringValue.toLowerCase().trim();

                // The validated / corrected option value for this connection
                // parameter, if any
                const validOptionValue = get(
                    protocols, [protocol, name, comparisonValue]);

                // If the provided value fuzzily matches a valid option value,
                // use the valid option value instead
                if (validOptionValue)
                    connection.parameters[name] = validOptionValue;

                // Even if no option is found, the value must be a string
                else
                    connection.parameters[name] = stringValue;

            });

            forEach(connection.attributes, (value, name) => {

                // An explicit null value for an attribute is valid - do not
                // process it further
                if (value === null)
                    return;

                // All non-null connection attributes must be strings
                connection.attributes[name] = String(value);

            });

            return connection;
        });
    }

    /**
     * Convert a provided connection array into a ParseResult. Any provided
     * transform functions will be run on each entry in `connectionData` before
     * any other processing is done.
     *
     * @param importConfig
     *     The configuration options selected by the user prior to import.
     *
     * @param connectionData
     *     An arbitrary array of data. This must evaluate to a ImportConnection
     *     object after being run through all functions in `transformFunctions`.
     *
     * @param transformFunctions
     *     An array of transformation functions to run on each entry in
     *     `connection` data.
     *
     * @return
     *     A promise resolving to ParseResult object representing the result of
     *     parsing all provided connection data.
     */
    private parseConnectionData(
        importConfig: ConnectionImportConfig, connectionData: any[], transformFunctions: Function[]): Promise<ParseResult> {

        // Check that the provided connection data array is not empty
        const checkError = this.performBasicChecks(connectionData);
        if (checkError) {
            return Promise.reject(checkError);
        }

        let index = 0;

        // Get the tree transformer and relevant protocol information
        return Promise.all([
            this.getFieldTransformer(),
            this.getTreeTransformer(importConfig),
        ])
            .then(([fieldTransformer, treeTransformer]) =>
                connectionData.reduce((parseResult, data) => {

                    const { patches, users, groups, groupPaths } = parseResult;

                    // Run the array data through each provided transform
                    let connectionObject = data;
                    forEach(transformFunctions, transform => {
                        connectionObject = transform(connectionObject);
                    });

                    // Apply the field level transforms
                    connectionObject = fieldTransformer(connectionObject);

                    // Apply the connection group hierarchy transforms
                    connectionObject = treeTransformer(connectionObject);

                    // If there are any errors for this connection, fail the whole batch
                    if (connectionObject.errors.length)
                        parseResult.hasErrors = true;

                    // The value for the patch is a full-fledged Connection
                    const value = new Connection(connectionObject);

                    // If a new connection is being created
                    if (connectionObject.importMode
                        === ImportConnection.ImportMode.CREATE)

                        // Add a patch for creating the connection
                        patches.push(new DirectoryPatch({
                            op  : DirectoryPatch.Operation.ADD,
                            path: '/',
                            value
                        }));

                        // The connection is being replaced, and permissions are only being
                    // added, not replaced
                    else if (importConfig.existingPermissionMode ===
                        ConnectionImportConfig.ExistingPermissionMode.PRESERVE)

                        // Add a patch for replacing the connection
                        patches.push(new DirectoryPatch({
                            op  : DirectoryPatch.Operation.REPLACE,
                            path: '/' + connectionObject.identifier,
                            value
                        }));

                        // The connection is being replaced, and permissions are also being
                    // replaced
                    else {

                        // Add a patch for removing the existing connection
                        patches.push(new DirectoryPatch({
                            op  : DirectoryPatch.Operation.REMOVE,
                            path: '/' + connectionObject.identifier
                        }));

                        // Increment the index for the additional remove patch
                        index += 1;

                        // Add a second patch for creating the replacement connection
                        patches.push(new DirectoryPatch({
                            op  : DirectoryPatch.Operation.ADD,
                            path: '/',
                            value
                        }));

                    }

                    // Save the connection group path into the parse result
                    groupPaths[index] = connectionObject.group;

                    // Save the errors for this connection into the parse result
                    parseResult.errors[index] = connectionObject.errors;

                    // Add this connection index to the list for each user
                    forEach(connectionObject.users, identifier => {

                        // If there's an existing list, add the index to that
                        if (users[identifier])
                            users[identifier].push(index);

                        // Otherwise, create a new list with just this index
                        else
                            users[identifier] = [index];
                    });

                    // Add this connection index to the list for each group
                    forEach(connectionObject.groups, identifier => {

                        // If there's an existing list, add the index to that
                        if (groups[identifier])
                            groups[identifier].push(index);

                        // Otherwise, create a new list with just this index
                        else
                            groups[identifier] = [index];
                    });

                    // Return the existing parse result state and continue on to the
                    // next connection in the file
                    index++;
                    parseResult.connectionCount++;
                    return parseResult;

                }, new ParseResult()));
    }

    /**
     * Convert a provided CSV representation of a connection list into a JSON
     * object to be submitted to the PATCH REST endpoint, as well as a list of
     * objects containing lists of user and user group identifiers to be granted
     * to each connection.
     *
     * @param importConfig
     *     The configuration options selected by the user prior to import.
     *
     * @param csvData
     *     The CSV-encoded connection list to process.
     *
     * @return
     *     A promise resolving to ParseResult object representing the result of
     *     parsing all provided connection data.
     */
    parseCSV(importConfig: ConnectionImportConfig, csvData: string): Promise<ParseResult> {

        // Convert to an array of arrays, one per CSV row (including the header)
        // NOTE: skip_empty_lines is required, or a trailing newline will error
        let parsedData;
        try {
            parsedData = parseCSVData(csvData, { skip_empty_lines: true } as any);
        }

            // If the CSV parser throws an error, reject with that error
        catch (error: any) {

            const message = error.message;
            console.error(error);

            let deferred: Promise<ParseResult>;

            // If the error message looks like the expected (and ugly) message
            // that's thrown when a binary file is provided, throw a more
            // friendly error.
            if (trim(message).toLowerCase() == BINARY_CSV_ERROR_MESSAGE)
                deferred = Promise.reject(new ParseError({
                    message  : 'CSV binary parse attempt error: ' + error.message,
                    key      : 'IMPORT.ERROR_DETECTED_INVALID_TYPE',
                    variables: undefined
                }));

            // Otherwise, pass the error from the library through to the user
            else
                deferred = Promise.reject(new ParseError({
                    message  : 'CSV Parse Failure: ' + error.message,
                    key      : 'IMPORT.ERROR_PARSE_FAILURE_CSV',
                    variables: { ERROR: error.message }
                }));

            return deferred;
        }

        // The header row - an array of string header values
        const header = parsedData.length ? parsedData[0] : [];

        // Slice off the header row to get the data rows
        const connectionData = parsedData.slice(1);

        // Generate the CSV transform function, and apply it to every row
        // before applying all the rest of the standard transforms
        return this.connectionCSVService.getCSVTransformer(header).then(
            csvTransformer =>

                // Apply the CSV transform to every row
                this.parseConnectionData(
                    importConfig, connectionData, [csvTransformer]));

    }

    /**
     * Convert a provided YAML representation of a connection list into a JSON
     * object to be submitted to the PATCH REST endpoint, as well as a list of
     * objects containing lists of user and user group identifiers to be granted
     * to each connection.
     *
     * @param importConfig
     *     The configuration options selected by the user prior to import.
     *
     * @param yamlData
     *     The YAML-encoded connection list to process.
     *
     * @return
     *     A promise resolving to ParseResult object representing the result of
     *     parsing all provided connection data.
     */
    parseYAML(importConfig: ConnectionImportConfig, yamlData: string): Promise<ParseResult> {

        // Parse from YAML into a javascript array
        let connectionData;
        try {
            connectionData = parseYAMLData(yamlData);
        }

            // If the YAML parser throws an error, reject with that error
        catch (error: any) {
            console.error(error);
            return Promise.reject(new ParseError({
                message  : 'YAML Parse Failure: ' + error.message,
                key      : 'IMPORT.ERROR_PARSE_FAILURE_YAML',
                variables: { ERROR: error.message }
            }));
        }

        // Produce a ParseResult, making sure that each record is converted to
        // the ImportConnection type before further parsing
        return this.parseConnectionData(importConfig, connectionData,
            [(connection: any) => new ImportConnection(connection)]);
    }

    /**
     * Convert a provided JSON-encoded representation of a connection list into
     * an array of patches to be submitted to the PATCH REST endpoint, as well
     * as a list of objects containing lists of user and user group identifiers
     * to be granted to each connection.
     *
     * @param importConfig
     *     The configuration options selected by the user prior to import.
     *
     * @param jsonData
     *     The JSON-encoded connection list to process.
     *
     * @return
     *     A promise resolving to ParseResult object representing the result of
     *     parsing all provided connection data.
     */
    parseJSON(importConfig: ConnectionImportConfig, jsonData: string): Promise<ParseResult> {

        // Parse from JSON into a javascript array
        let connectionData;
        try {
            connectionData = JSON.parse(jsonData);
        }

            // If the JSON parse attempt throws an error, reject with that error
        catch (error: any) {
            console.error(error);
            return Promise.reject(new ParseError({
                message  : 'JSON Parse Failure: ' + error.message,
                key      : 'IMPORT.ERROR_PARSE_FAILURE_JSON',
                variables: { ERROR: error.message }
            }));
        }

        // Produce a ParseResult, making sure that each record is converted to
        // the ImportConnection type before further parsing
        return this.parseConnectionData(importConfig, connectionData,
            [(connection: any) => new ImportConnection(connection)]);

    }

}
