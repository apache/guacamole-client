/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * 'License'); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/* global _ */

import { parse as parseCSVData } from 'csv-parse/lib/sync'
import { parse as parseYAMLData } from 'yaml'

/**
 * A service for parsing user-provided JSON, YAML, or JSON connection data into
 * an appropriate format for bulk uploading using the PATCH REST endpoint.
 */
angular.module('import').factory('connectionParseService',
        ['$injector', function connectionParseService($injector) {

    // Required types
    const Connection          = $injector.get('Connection');
    const ParseError          = $injector.get('ParseError');
    const TranslatableMessage = $injector.get('TranslatableMessage');
    
    // Required services
    const $q                     = $injector.get('$q');
    const $routeParams           = $injector.get('$routeParams');
    const schemaService          = $injector.get('schemaService');
    const connectionCSVService   = $injector.get('connectionCSVService');
    const connectionGroupService = $injector.get('connectionGroupService');

    const service = {};
    
    /**
     * Perform basic checks, common to all file types - namely that the parsed
     * data is an array, and contains at least one connection entry. Returns an 
     * error if any of these basic checks fails.
     * 
     * returns {ParseError}
     *     An error describing the parsing failure, if one of the basic checks
     *     fails.
     */
    function performBasicChecks(parsedData) {
        
        // Make sure that the file data parses to an array (connection list)
        if (!(parsedData instanceof Array))
            return new ParseError({
                message: 'Import data must be a list of connections',
                key: 'CONNECTION_IMPORT.ERROR_ARRAY_REQUIRED'
            });

        // Make sure that the connection list is not empty - contains at least
        // one connection
        if (!parsedData.length)
            return new ParseError({
                message: 'The provided file is empty',
                key: 'CONNECTION_IMPORT.ERROR_EMPTY_FILE'
            });
    }
    
    
    /**
     * Returns a promise that resolves to an object mapping potential groups
     * that might be encountered in an imported connection to group identifiers.
     * 
     * The idea is that a user-provided import file might directly specify a
     * parentIdentifier, or it might specify a named group path like "ROOT",
     * "ROOT/parent", or "ROOT/parent/child". This object resolved by the 
     * promise returned from this function will map all of the above to the 
     * identifier of the appropriate group, if defined.
     * 
     * @returns {Promise.<Object>}
     */
    function getGroupLookups() {
        
        // The current data source - defines all the groups that the connections
        // might be imported into
        const dataSource = $routeParams.dataSource;
        
        const deferredGroupLookups = $q.defer();
        
        connectionGroupService.getConnectionGroupTree(dataSource).then(
                rootGroup => {
            
            const groupLookup = {};
            
            // Add the specified group to the lookup, appending all specified
            // prefixes, and then recursively call saveLookups for all children
            // of the group, appending to the prefix for each level
            function saveLookups(prefix, group) {
                
                // To get the path for the current group, add the name
                const currentPath = prefix + group.name;
                
                // Add the current path to the lookup
                groupLookup[currentPath] = group.identifier;
                
                // Add each child group to the lookup
                const nextPrefix = currentPath + "/";
                _.forEach(group.childConnectionGroups,
                        childGroup => saveLookups(nextPrefix, childGroup));
            }
            
            // Start at the root group
            saveLookups("", rootGroup);
            
            // Resolve with the now fully-populated lookups
            deferredGroupLookups.resolve(groupLookup);
            
        });
        
        return deferredGroupLookups.promise;
    }

    /**
     * Convert a provided CSV representation of a connection list into a JSON
     * string to be submitted to the PATCH REST endpoint. The returned JSON
     * string will contain a PATCH operation to create each connection in the
     * provided list.
     *
     * TODO: Describe disambiguation suffixes, e.g. hostname (parameter), and
     * that we will accept without the suffix if it's unambigous. (or not? how about not?)
     *
     * @param {String} csvData
     *     The JSON-encoded connection list to convert to a PATCH request body.
     *
     * @return {Promise.<Connection[]>}
     *     A promise resolving to an array of Connection objects, one for each 
     *     connection in the provided CSV.
     */
    service.parseCSV = function parseCSV(csvData) {
        
        // Convert to an array of arrays, one per CSV row (including the header)
        // NOTE: skip_empty_lines is required, or a trailing newline will error
        let parsedData;
        try {
            parsedData = parseCSVData(csvData, {skip_empty_lines: true});
        } 
        
        // If the CSV parser throws an error, reject with that error. No
        // translation key will be available here.
        catch(error) {
            console.error(error);
            const deferred = $q.defer();
            deferred.reject(error);
            return deferred.promise;
        }

        // Slice off the header row to get the data rows
        const connectionData = parsedData.slice(1);

        // Check that the provided CSV is not empty (the parser always
        // returns an array)
        const checkError = performBasicChecks(connectionData);
        if (checkError) {
            const deferred = $q.defer();
            deferred.reject(checkError);
            return deferred.promise;
        }
        
        // The header row - an array of string header values
        const header = parsedData[0];
        
        return connectionCSVService.getCSVTransformer(header).then(
                
            // If the transformer was successfully generated, apply it to the
            // data rows
            // TODO: Also apply the group -> parentIdentifier transform
            csvTransformer => connectionData.map(csvTransformer)
        );

    };

    /**
     * Convert a provided YAML representation of a connection list into a JSON
     * string to be submitted to the PATCH REST endpoint. The returned JSON
     * string will contain a PATCH operation to create each connection in the
     * provided list.
     *
     * @param {String} yamlData
     *     The YAML-encoded connection list to convert to a PATCH request body.
     *
     * @return {Promise.<Connection[]>}
     *     A promise resolving to an array of Connection objects, one for each 
     *     connection in the provided YAML.
     */
    service.parseYAML = function parseYAML(yamlData) {

        // Parse from YAML into a javascript array
        const parsedData = parseYAMLData(yamlData);
        
        // Check that the data is the correct format, and not empty
        const checkError = performBasicChecks(connectionData);
        if (checkError)
            return $q.defer().reject(checkError);
        
        // Convert to an array of Connection objects and return
        const deferredConnections = $q.defer();
        deferredConnections.resolve(
                parsedData.map(connection => new Connection(connection)));
        return deferredConnections.promise;

    };

    /**
     * Convert a provided JSON representation of a connection list into a JSON
     * string to be submitted to the PATCH REST endpoint. The returned JSON
     * string will contain a PATCH operation to create each connection in the
     * provided list.
     *
     * @param {String} jsonData
     *     The JSON-encoded connection list to convert to a PATCH request body.
     *
     * @return {Promise.<Connection[]>}
     *     A promise resolving to an array of Connection objects, one for each 
     *     connection in the provided JSON.
     */
    service.parseJSON = function parseJSON(jsonData) {

        // Parse from JSON into a javascript array
        const parsedData = JSON.parse(yamlData);
        
        // Check that the data is the correct format, and not empty
        const checkError = performBasicChecks(connectionData);
        if (checkError)
            return $q.defer().reject(checkError);
        
        // Convert to an array of Connection objects and return
        const deferredConnections = $q.defer();
        deferredConnections.resolve(
                parsedData.map(connection => new Connection(connection)));
        return deferredConnections.promise;
        
    };

    return service;

}]);
