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
 * Provides the ManagedClientMessage class used for messages displayed in
 * a ManagedClient.
 */
angular.module('client').factory('ManagedClientMessage', [function defineManagedClientMessage() {

    /**
     * Object which represents a message to be displayed to a Guacamole client.
     *
     * @constructor
     * @param {ManagedClientMessage|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ManagedClientMessage.
     */
    var ManagedClientMessage = function ManagedClientMessage(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The message code sent by the server that will be used to locate the
         * message within the Guacamole translation framework.
         * 
         * @type Number
         */
        this.msgcode = template.msgcode;
        
        /**
         * Any arguments that should be passed through the translation system
         * and displayed as part of the message.
         * 
         * @type String[]
         */
        this.args = template.args;
        
    };

    return ManagedClientMessage;

}]);