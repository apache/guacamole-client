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
angular.module('settings').factory('connectionImportParseService',
        ['$injector', function connectionImportParseService($injector) {

    // Required types
    const Connection          = $injector.get('Connection');
    const ParseError          = $injector.get('ParseError');
    const TranslatableMessage = $injector.get('TranslatableMessage');
    
    // Required services
    const $q                     = $injector.get('$q');
    const $routeParams           = $injector.get('$routeParams');
    const schemaService          = $injector.get('schemaService');
    const connectionGroupService = $injector.get('connectionGroupService');

    const service = {};
    
    /**
     * Perform basic checks, common to all file types - namely that the parsed
     * data is an array, and contains at least one connection entry.
     * 
     * @throws {ParseError}
     *     An error describing the parsing failure, if one of the basic checks
     *     fails.
     */
    function performBasicChecks(parsedData) {
        
        // Make sure that the file data parses to an array (connection list)
        if (!(parsedData instanceof Array))
            throw new ParseError({
                message: 'Import data must be a list of connections',
                translatableMessage: new TranslatableMessage({
                    key: 'SETTINGS_CONNECTION_IMPORT.ERROR_ARRAY_REQUIRED'
                })
            });

        // Make sure that the connection list is not empty - contains at least
        // one connection
        if (!parsedData.length)
            throw new ParseError({
                message: 'The provided CSV file is empty',
                translatableMessage: new TranslatableMessage({
                    key: 'SETTINGS_CONNECTION_IMPORT.ERROR_EMPTY_FILE'
                })
            });
    }

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
     *     connection in the provided CSV.
     */
    service.parseYAML = function parseYAML(yamlData) {

        // Parse from YAML into a javascript array
        const parsedData = parseYAMLData(yamlData);
        
        // Check that the data is the correct format, and not empty
        performBasicChecks(parsedData);
        
        // Convert to an array of Connection objects and return
        const deferredConnections = $q.defer();
        deferredConnections.resolve(
                parsedData.map(connection => new Connection(connection)));
        return deferredConnections.promise;

    };
    
    /**
     * Returns a promise that resolves to an object detailing the connection
     * attributes for the current data source, as well as the connection
     * paremeters for every protocol, for the current data source.
     * 
     * The object that the promise will contain an "attributes" key that maps to
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
     * @returns {Promise.<Object>}
     */
    function getFieldLookups() {
        
        // The current data source - the one that the connections will be
        // imported into
        const dataSource = $routeParams.dataSource;
        
        // Fetch connection attributes and protocols for the current data source
        return $q.all({
            attributes : schemaService.getConnectionAttributes(dataSource),
            protocols  : schemaService.getProtocols(dataSource)
        })
        .then(function connectionStructureRetrieved({attributes, protocols}) {
            
            return {
                
                // Translate the forms and fields into a flat map of attribute
                // name to `true` boolean value
                attributes: attributes.reduce(
                    (attributeMap, form) => {
                        form.fields.forEach(
                            field => attributeMap[field.name] = true); 
                        return attributeMap
                    }, {}),
                  
                // Translate the protocol definitions into a map of protocol
                // name to map of field name to `true` boolean value
                protocolParameters: _.mapValues(
                    protocols, protocol => protocol.connectionForms.reduce(
                        (protocolFieldMap, form) => {
                            form.fields.forEach(
                                field => protocolFieldMap[field.name] = true); 
                            return protocolFieldMap;
                        }, {}))
            };
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
    
/*
// Example Connection JSON
{
   "attributes": {
       "failover-only": "true",
       "guacd-encryption": "none",
       "guacd-hostname": "potato",
       "guacd-port": "1234",
       "ksm-user-config-enabled": "true",
       "max-connections": "1",
       "max-connections-per-user": "1",
       "weight": "1"
   },
   "name": "Bloatato",
   "parameters": {
       "audio-servername": "heyoooooooo",
       "clipboard-encoding": "",
       "color-depth": "",
       "create-recording-path": "",
       "cursor": "remote",
       "dest-host": "pooootato",
       "dest-port": "4444",
       "disable-copy": "",
       "disable-paste": "true",
       "enable-audio": "true",
       "enable-sftp": "true",
       "force-lossless": "true",
       "hostname": "potato",
       "password": "taste",
       "port": "4321",
       "read-only": "",
       "recording-exclude-mouse": "",
       "recording-exclude-output": "",
       "recording-include-keys": "",
       "recording-name": "heyoooooo",
       "recording-path": "/path/to/goo",
       "sftp-disable-download": "",
       "sftp-disable-upload": "",
       "sftp-hostname": "what what good sir",
       "sftp-port": "",
       "sftp-private-key": "lol i'll never tell",
       "sftp-server-alive-interval": "",
       "swap-red-blue": "true",
       "username": "test",
       "wol-send-packet": "",
       "wol-udp-port": "",
       "wol-wait-time": ""
   },
                
   // or a numeric identifier - we will probably want to offer a way to allow
   // them to specify a path like "ROOT/parent/child" or just "/parent/child" or
   // something like that
   // TODO: Call the 
   "parentIdentifier": "ROOT",
   "protocol": "vnc"
                
}
*/

    /**
     * Convert a provided JSON representation of a connection list into a JSON
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
        
        const deferredConnections = $q.defer();
        
        return $q.all({
            fieldLookups : getFieldLookups(),
            groupLookups : getGroupLookups()
        })
        .then(function lookupsReady({fieldLookups, groupLookups}) {
        
            const {attributes, protocolParameters} = fieldLookups;
    
            console.log({attributes, protocolParameters}, groupLookups);
            
            // Convert to an array of arrays, one per CSV row (including the header)
            const parsedData = parseCSVData(csvData);

            // Slice off the header row to get the data rows
            const connectionData = parsedData.slice(1);

            // Check that the provided CSV is not empty (the parser always
            // returns an array)
            performBasicChecks(connectionData);

            // The header row - an array of string header values
            const header = parsedData[0];
            
            // TODO: Connectionify this
            deferredConnections.resolve(connectionData);
        });
        
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
     *     connection in the provided CSV.
     */
    service.parseJSON = function parseJSON(jsonData) {

        // Parse from JSON into a javascript array
        const parsedData = JSON.parse(yamlData);
        
        // Check that the data is the correct format, and not empty
        performBasicChecks(parsedData);
        
        // Convert to an array of Connection objects and return
        const deferredConnections = $q.defer();
        deferredConnections.resolve(
                parsedData.map(connection => new Connection(connection)));
        return deferredConnections.promise;
        
    };

    return service;

}]);
