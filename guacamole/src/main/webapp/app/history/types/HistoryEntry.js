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
 * Provides the HistoryEntry class used by the guacHistory service.
 */
angular.module('history').factory('HistoryEntry', [function defineHistoryEntry() {

    /**
     * A single entry in the connection history.
     * 
     * @constructor
     * @param {String} id The ID of the connection.
     * 
     * @param {String} thumbnail
     *     The URL of the thumbnail to use to represent the connection.
     */
    var HistoryEntry = function HistoryEntry(id, thumbnail) {

        /**
         * The ID of the connection associated with this history entry,
         * including type prefix.
         */
        this.id = id;

        /**
         * The thumbnail associated with the connection associated with this
         * history entry.
         */
        this.thumbnail = thumbnail;

    };

    return HistoryEntry;

}]);
