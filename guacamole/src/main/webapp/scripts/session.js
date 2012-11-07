
/*
 *  Guacamole - Clientless Remote Desktop
 *  Copyright (C) 2010  Michael Jumper
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
     * @param value An arbitrary value.
     */
    this.setProperty = function(name, value) {
        state[name] = value;
        localStorage.setItem("GUACAMOLE_STATE", JSON.stringify(state));
    };

    /**
     * Returns the value stored under the property having the given name.
     * 
     * @param {String} name The name of the property to read.
     * @return The value of the given property.
     */
    this.getProperty = function(name) {
        return state[name];
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
    window.addEventListener("storage", guac_state.reload);

    // Initial load
    guac_state.reload();

}
