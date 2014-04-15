/*
 * Copyright (C) 2013 Glyptodon LLC
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
 * Maintains state across multiple Guacamole pages via HTML5 Web Storage.
 * @constructor
 */
function GuacamoleSessionState() {

    /**
     * Reference to this GuacamoleSessionState.
     * @private
     */
    var guac_state = this;

    /**
     * The last read state object.
     * @private
     */
    var state = localStorage.getItem("GUACAMOLE_STATE") || {};

    /**
     * Reloads the internal state, sending onchange events for all changed,
     * deleted, or new properties.
     */
    this.reload = function() {

        // Pull current state
        var new_state = JSON.parse(localStorage.getItem("GUACAMOLE_STATE") || "{}");
        
        // Assign new state
        var old_state = state;
        state = new_state;

        // Check if any values are different
        for (var name in new_state) {

            // If value changed, call handler
            var old = old_state[name];
            if (old != new_state[name]) {

                // Call change handler
                if (guac_state.onchange)
                    guac_state.onchange(state, new_state, name);

            }

        }

    };

    /**
     * Sets the given property to the given value.
     * 
     * @param {String} name The name of the property to change.
     * @param [value] An arbitrary value.
     */
    this.setProperty = function(name, value) {
        state[name] = value;
        localStorage.setItem("GUACAMOLE_STATE", JSON.stringify(state));
    };

    /**
     * Returns the value stored under the property having the given name.
     * 
     * @param {String} name The name of the property to read.
     * @param value The default value, if any.
     * @return The value of the given property.
     */
    this.getProperty = function(name, value) {

        var current = state[name];
        if (current === undefined)
            return value;

        return current;
    };

    /**
     * Event which is fired whenever a property value is changed externally.
     * 
     * @event
     * @param old_state An object whose properties' values are the old values
     *                  of this GuacamoleSessionState.
     * @param new_state An object whose properties' values are the new values
     *                  of this GuacamoleSessionState.
     * @param {String} name The name of the property that is being changed.
     */
    this.onchange = null;

    // Reload when modified
    window.addEventListener("storage", guac_state.reload, false);

    // Initial load
    guac_state.reload();

}
