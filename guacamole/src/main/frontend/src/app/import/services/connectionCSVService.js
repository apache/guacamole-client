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

/**
 * A service for parsing user-provided CSV connection data for bulk import.
 */
angular.module('import').factory('connectionCSVService',
        ['$injector', function connectionCSVService($injector) {
    
    // Required services
    const $q                     = $injector.get('$q');
    const $routeParams           = $injector.get('$routeParams');
    const schemaService          = $injector.get('schemaService');
    
    const service = {};
    
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
     * 
     * 
     * @returns {Promise.<Function.<String[], Object>>}
     *     A promise that will resolve to a function that translates a CSV data
     *     row (array of strings) to a connection object.
     */
    service.getCSVTransformer = function getCSVTransformer(headerRow) {
        
    };
    
    return service;
    
}]);