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

/* global _ */

/**
 * A directive that displays errors that occurred during parsing of a connection
 * import file, or errors that were returned from the API during the connection
 * batch creation attempt.
 */
angular.module('import').directive('connectionImportErrors', [
        function connectionImportErrors() {

    const directive = {
        restrict: 'E',
        replace: true,
        templateUrl: 'app/import/templates/connectionErrors.html',
        scope: {

            /**
             * The result of parsing the import file. Any errors in this file
             * will be displayed to the user.
             *
             * @type ParseResult
             */
            parseResult : '=',

            /**
             * The error associated with an attempt to batch create the
             * connections represented by the ParseResult, if the ParseResult
             * had no errors. If the provided ParseResult has errors, no request
             * should have been made, and any provided patch error will be
             * ignored.
             *
             * @type Error
             */
            patchFailure : '=',

        }
    };

    directive.controller = ['$scope', '$injector',
            function connectionImportErrorsController($scope, $injector) {

        // Required types
        const DisplayErrorList      = $injector.get('DisplayErrorList');
        const ImportConnectionError = $injector.get('ImportConnectionError');
        const ParseError            = $injector.get('ParseError');
        const SortOrder             = $injector.get('SortOrder');

        // Required services
        const $q                    = $injector.get('$q');
        const $translate            = $injector.get('$translate');

        // There are errors to display if the parse result generated errors, or
        // if the patch request failed
        $scope.hasErrors = () =>
            !!_.get($scope, 'parseResult.hasErrors') || !!$scope.patchFailure;

        /**
         * All connections with their associated errors for display. These may
         * be either parsing failures, or errors returned from the API. Both
         * error types will be adapted to a common display format, though the
         * error types will never be mixed, because no REST request should ever
         * be made if there are client-side parse errors.
         *
         * @type {ImportConnectionError[]}
         */
        $scope.connectionErrors = [];

        /**
         * SortOrder instance which maintains the sort order of the visible
         * connection errors.
         *
         * @type SortOrder
         */
        $scope.errorOrder = new SortOrder([
            'rowNumber',
            'name',
            'protocol',
            'errors',
        ]);

        /**
         * Array of all connection error properties that are filterable.
         *
         * @type String[]
         */
        $scope.filteredErrorProperties = [
            'rowNumber',
            'name',
            'protocol',
            'errors',
        ];

        /**
         * Generate a ImportConnectionError representing any errors associated
         * with the row at the given index within the given parse result.
         *
         * @param {ParseResult} parseResult
         *     The result of parsing the connection import file.
         *
         * @param {Integer} index
         *     The current row within the import file, 0-indexed.
         *
         * @returns {ImportConnectionError}
         *     The connection error object associated with the given row in the
         *     given parse result.
         */
        const generateConnectionError = (parseResult, index) => {

            // Get the patch associated with the current row
            const patch = parseResult.patches[index];

            // The value of a patch is just the Connection object
            const connection = patch.value;

            return new ImportConnectionError({

                // Add 1 to the index to get the position in the file
                rowNumber: index + 1,

                // Basic connection information - name and protocol.
                name: connection.name,
                protocol: connection.protocol,

                // The human-readable error messages
                errors: new DisplayErrorList(
                        [ ...(parseResult.errors[index] || []) ])
            });
        };

        // If a new connection patch failure is seen, update the display list
        $scope.$watch('patchFailure', function patchFailureChanged(patchFailure) {

            const { parseResult } = $scope;

            // Do not attempt to process anything before the data has loaded
            if (!patchFailure || !parseResult)
                return;

            // Set up the list of connection errors based on the existing parse
            // result, with error messages fetched from the patch failure
            $scope.connectionErrors = parseResult.patches.map(
                    (patch, index) => {

                // Generate a connection error for display
                const connectionError = generateConnectionError(parseResult, index);

                // Set the error from the PATCH request, if there is one
                const error = _.get(patchFailure, ['patches', index, 'error']);
                if (error)
                    connectionError.errors = new DisplayErrorList([error]);

                return connectionError;
            });
        });

        // If a new parse result with errors is seen, update the display list
        $scope.$watch('parseResult', function parseResultChanged(parseResult) {

            // Do not process if there are no errors in the provided result
            if (!parseResult || !parseResult.hasErrors)
                return;

            // All promises from all translation requests. The scope will not be
            // updated until all translations are ready.
            const translationPromises = [];

            // The parse result should only be updated on a fresh file import;
            // therefore it should be safe to skip checking the patch errors
            // entirely - if set, they will be from the previous file and no
            // longer relevant.

            // Set up the list of connection errors based on the updated parse
            // result
            const connectionErrors = parseResult.patches.map(
                    (patch, index) => {

                // Generate a connection error for display
                const connectionError = generateConnectionError(parseResult, index);

                // Go through the errors and check if any are translateable
                connectionError.errors.getArray().forEach(
                        (error, errorIndex) => {

                    // If this error is a ParseError, it can be translated.
                    // NOTE: Generally one would translate error messages in the
                    // template, but in this case, the connection errors need to
                    // be raw strings in order to enable sorting and filtering.
                    if (error instanceof ParseError)

                        // Fetch the translation and update it when it's ready
                        translationPromises.push($translate(
                            error.key, error.variables)
                            .then(translatedError => {
                                connectionError.errors.getArray()[errorIndex] = translatedError;
                            }));

                });

                return connectionError;

            });

            // Once all the translations have been completed, update the
            // connectionErrors all in one go, to ensure no excessive reloading
            $q.all(translationPromises).then(() => {
                $scope.connectionErrors = connectionErrors;
            });

        });

    }];

    return directive;

}]);
