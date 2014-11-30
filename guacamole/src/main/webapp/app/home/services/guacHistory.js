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
 * A service for creating Guacamole clients.
 */
angular.module('home').factory('guacHistory', [function guacHistory() {

    var service = {};

    // The parameter name for getting the history from local storage
    var GUAC_HISTORY_STORAGE_KEY = "GUAC_HISTORY";
                                    
    /**
     * The number of entries to allow before removing old entries based on the
     * cutoff.
     */
    var IDEAL_LENGTH = 6;

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
         * The ID of the connection associated with this history entry.
         */
        this.id = id;

        /**
         * The thumbnail associated with the connection associated with this
         * history entry.
         */
        this.thumbnail = thumbnail;

    };

    /**
     * The top few recent connections, sorted in order of most recent access.
     * 
     * @type HistoryEntry[]
     */
    service.recentConnections = [];

    /**
     * Updates the thumbnail and access time of the history entry for the
     * connection with the given ID.
     * 
     * @param {String} id
     *     The ID of the connection whose history entry should be updated.
     * 
     * @param {String} thumbnail
     *     The URL of the thumbnail image to associate with the history entry.
     */
    service.updateThumbnail = function(id, thumbnail) {

        var i;

        // Remove any existing entry for this connection
        for (i=0; i < service.recentConnections.length; i++) {
            if (service.recentConnections[i].id === id) {
                service.recentConnections.splice(i, 1);
                break;
            }
        }

        // Store new entry in history
        service.recentConnections.unshift(new HistoryEntry(
            id,
            thumbnail,
            new Date().getTime()
        ));

        // Truncate history to ideal length
        if (service.recentConnections.length > IDEAL_LENGTH)
            service.recentConnections.length = IDEAL_LENGTH;

        // Save updated history, ignore inability to use localStorage
        try {
            if (localStorage)
                localStorage.setItem(GUAC_HISTORY_STORAGE_KEY, JSON.stringify(service.recentConnections));
        }
        catch (ignore) {}

    };

    // Get stored connection history, ignore inability to use localStorage
    try {

        if (localStorage) {
            var storedHistory = JSON.parse(localStorage.getItem(GUAC_HISTORY_STORAGE_KEY) || "[]");
            if (storedHistory instanceof Array)
                service.recentConnections = storedHistory;

        }

    }
    catch (ignore) {}

    return service;

}]);
