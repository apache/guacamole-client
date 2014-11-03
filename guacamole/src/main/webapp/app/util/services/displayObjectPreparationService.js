/*
 * Copyright (C) 2014 Glyptodon LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * A service to help prepare objects from the REST API for display.
 */
angular.module('util').factory('displayObjectPreparationService', [function displayObjectPreparationService() {
    var service = {};
    
    /**
     * Adds properties to the connection that will be useful for display.
     * 
     * @param {object} connection The connection to add display properties to.
     */
    service.prepareConnection = function prepareConnection(connection) {
        
        // This is a connection
        connection.isConnection = true;
    };
    
    /**
     * Adds properties to the connection that will be useful for display.
     * 
     * @param {object} connectionGroup The connection group to add display properties to.
     */
    service.prepareConnectionGroup = function prepareConnectionGroup(connectionGroup) {
        
        // This is not a connection
        connectionGroup.isConnection = false;
            
        connectionGroup.balancer = connectionGroup.type !== "ORGANIZATIONAL";
        connectionGroup.expanded = false;
        connectionGroup.children = [];
    };
    
    return service;
}]);
