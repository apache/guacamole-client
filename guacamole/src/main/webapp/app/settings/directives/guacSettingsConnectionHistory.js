/*
 * Copyright (C) 2015 Glyptodon LLC
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
 * A directive for viewing connection history records.
 */
angular.module('settings').directive('guacSettingsConnectionHistory', [function guacSettingsConnectionHistory() {
        
    return {
        // Element only
        restrict: 'E',
        replace: true,

        scope: {
        },

        templateUrl: 'app/settings/templates/settingsConnectionHistory.html',
        controller: ['$scope', '$injector', function settingsConnectionHistoryController($scope, $injector) {
                
            // Get required types
            var ConnectionHistoryEntryWrapper = $injector.get('ConnectionHistoryEntryWrapper');
            var FilterToken                   = $injector.get('FilterToken');
            var SortOrder                     = $injector.get('SortOrder');

            // Get required services
            var $routeParams   = $injector.get('$routeParams');
            var $translate     = $injector.get('$translate');
            var historyService = $injector.get('historyService');

            /**
             * The identifier of the currently-selected data source.
             *
             * @type String
             */
            $scope.dataSource = $routeParams.dataSource;

            /**
             * All wrapped matching connection history entries, or null if these
             * entries have not yet been retrieved.
             *
             * @type ConnectionHistoryEntryWrapper[]
             */
            $scope.historyEntryWrappers = null;

            /**
             * The search terms to use when filtering the history records.
             *
             * @type String
             */
            $scope.searchString = '';

            /**
             * The date format for use for start/end dates.
             *
             * @type String
             */
            $scope.dateFormat = null;

            /**
             * SortOrder instance which stores the sort order of the history
             * records.
             *
             * @type SortOrder
             */
            $scope.order = new SortOrder([
                '-startDate',
                '-duration',
                'username',
                'connectionName'
            ]);

            // Get session date format
            $translate('SETTINGS_CONNECTION_HISTORY.FORMAT_DATE')
            .then(function dateFormatReceived(retrievedDateFormat) {

                // Store received date format
                $scope.dateFormat = retrievedDateFormat;

            });
            
            /**
             * Returns true if the connection history records have been loaded,
             * indicating that information needed to render the page is fully 
             * loaded.
             * 
             * @returns {Boolean} 
             *     true if the history records have been loaded, false
             *     otherwise.
             * 
             */
            $scope.isLoaded = function isLoaded() {
                return $scope.historyEntryWrappers !== null
                    && $scope.dateFormat           !== null;
            };

            /**
             * Returns whether the search has completed but contains no history
             * records. This function will return false if there are history
             * records in the results OR if the search has not yet completed.
             *
             * @returns {Boolean}
             *     true if the search results have been loaded but no history
             *     records are present, false otherwise.
             */
            $scope.isHistoryEmpty = function isHistoryEmpty() {
                return $scope.isLoaded() && $scope.historyEntryWrappers.length === 0;
            };

            /**
             * Query the API for the connection record history, filtered by 
             * searchString, and ordered by order.
             */
            $scope.search = function search() {

                // Clear current results
                $scope.historyEntryWrappers = null;

                // Tokenize search string
                var tokens = FilterToken.tokenize($scope.searchString);

                // Transform tokens into list of required string contents
                var requiredContents = [];
                angular.forEach(tokens, function addRequiredContents(token) {

                    // Transform depending on token type
                    switch (token.type) {

                        // For string literals, use parsed token value
                        case 'LITERAL':
                            requiredContents.push(token.value);

                        // Ignore whitespace
                        case 'WHITESPACE':
                            break;

                        // For all other token types, use the relevant portion
                        // of the original search string
                        default:
                            requiredContents.push(token.consumed);

                    }

                });

                // Fetch history records
                historyService.getConnectionHistory(
                    $scope.dataSource,
                    requiredContents,
                    $scope.order.predicate.filter(function isSupportedPredicate(predicate) {
                        return predicate === 'startDate' || predicate === '-startDate';
                    })
                )
                .success(function historyRetrieved(historyEntries) {

                    // Wrap all history entries for sake of display
                    $scope.historyEntryWrappers = [];
                    angular.forEach(historyEntries, function wrapHistoryEntry(historyEntry) {
                       $scope.historyEntryWrappers.push(new ConnectionHistoryEntryWrapper(historyEntry)); 
                    });

                });

            };
            
            // Initialize search results
            $scope.search();
            
        }]
    };
    
}]);
