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
 * Provides the ManagedDisplay class used by the guacClientManager service.
 */
angular.module('client').factory('ManagedDisplay', ['$rootScope',
    function defineManagedDisplay($rootScope) {

    /**
     * Object which serves as a surrogate interface, encapsulating a Guacamole
     * display while it is active, allowing it to be detached and reattached
     * from different client views.
     * 
     * @constructor
     * @param {ManagedDisplay|Object} [template={}]
     *     The object whose properties should be copied within the new
     *     ManagedDisplay.
     */
    var ManagedDisplay = function ManagedDisplay(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The underlying Guacamole display.
         * 
         * @type Guacamole.Display
         */
        this.display = template.display;

        /**
         * The current size of the Guacamole display.
         *
         * @type ManagedDisplay.Dimensions
         */
        this.size = new ManagedDisplay.Dimensions(template.size);

        /**
         * The current mouse cursor, if any.
         * 
         * @type ManagedDisplay.Cursor
         */
        this.cursor = template.cursor;

    };

    /**
     * Object which represents the size of the Guacamole display.
     *
     * @constructor
     * @param {ManagedDisplay.Dimensions|Object} template
     *     The object whose properties should be copied within the new
     *     ManagedDisplay.Dimensions.
     */
    ManagedDisplay.Dimensions = function Dimensions(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The current width of the Guacamole display, in pixels.
         *
         * @type Number
         */
        this.width = template.width || 0;

        /**
         * The current width of the Guacamole display, in pixels.
         *
         * @type Number
         */
        this.height = template.height || 0;

    };

    /**
     * Object which represents a mouse cursor used by the Guacamole display.
     *
     * @constructor
     * @param {ManagedDisplay.Cursor|Object} template
     *     The object whose properties should be copied within the new
     *     ManagedDisplay.Cursor.
     */
    ManagedDisplay.Cursor = function Cursor(template) {

        // Use empty object by default
        template = template || {};

        /**
         * The actual mouse cursor image.
         * 
         * @type HTMLCanvasElement
         */
        this.canvas = template.canvas;

        /**
         * The X coordinate of the cursor hotspot.
         * 
         * @type Number
         */
        this.x = template.x;

        /**
         * The Y coordinate of the cursor hotspot.
         * 
         * @type Number
         */
        this.y = template.y;

    };

    /**
     * Creates a new ManagedDisplay which represents the current state of the
     * given Guacamole display.
     * 
     * @param {Guacamole.Display} display
     *     The Guacamole display to represent. Changes to this display will
     *     affect this ManagedDisplay.
     *
     * @returns {ManagedDisplay}
     *     A new ManagedDisplay which represents the current state of the
     *     given Guacamole display.
     */
    ManagedDisplay.getInstance = function getInstance(display) {

        var managedDisplay = new ManagedDisplay({
            display : display
        });

        // Store changes to display size
        display.onresize = function() {
            $rootScope.$apply(function updateClientSize() {
                managedDisplay.size = new ManagedDisplay.Dimensions({
                    width  : display.getWidth(),
                    height : display.getHeight()
                });
            });
        };

        // Store changes to display cursor
        display.oncursor = function(canvas, x, y) {
            $rootScope.$apply(function updateClientCursor() {
                managedDisplay.cursor = new ManagedDisplay.Cursor({
                    canvas : canvas,
                    x      : x,
                    y      : y
                });
            });
        };

        // Do nothing when the display element is clicked on
        display.getElement().onclick = function(e) {
            e.preventDefault();
            return false;
        };

        return managedDisplay;

    };

    return ManagedDisplay;

}]);