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
 * Main Guacamole UI namespace.
 * @namespace
 */
var GuacUI = GuacUI || {};

/**
 * Creates a new element having the given tagname and CSS class.
 */
GuacUI.createElement = function(tagname, classname) {
    var new_element = document.createElement(tagname);
    new_element.className = classname;
    return new_element;
};

GuacUI.createChildElement = function(parent, tagname, classname) {
    var element = GuacUI.createElement(tagname, classname);
    parent.appendChild(element);
    return element;
};

/**
 * Central registry of all components for all states.
 */
GuacUI.StateManager = new (function() {

    /**
     * The current state.
     */
    var current_state = null;

    /**
     * Array of arrays of components, indexed by the states they are in.
     */
    var components = [];

    /**
     * Registers the given component with this state manager, to be shown
     * during the given states.
     * 
     * @param {GuacUI.Component} component The component to register.
     * @param {Number} [...] The list of states this component should be
     *                       visible during.
     */
    this.registerComponent = function(component) {

        // For each state specified, add the given component
        for (var i=1; i<arguments.length; i++) {

            // Get specified state
            var state = arguments[i];

            // Get array of components in that state
            var component_array = components[state];
            if (!component_array)
                component_array = components[state] = [];

            // Add component
            component_array.push(component);

        }

    };

    function allComponents(components, name) {

        // Invoke given function on all components in array
        for (var i=0; i<components.length; i++)
            components[i][name]();

    }

    /**
     * Sets the current visible state.
     */
    this.setState = function(state) {

        // Hide components in current state
        if (current_state && components[current_state])
            allComponents(components[current_state], "hide");

        // Show all components in new state
        current_state = state;
        if (components[state])
            allComponents(components[state], "show");

    };

    /**
     * Returns the current visible state.
     */
    this.getState = function() {
        return current_state;
    };

})();


/**
 * Abstract component which can be registered with GuacUI and shown or hidden
 * dynamically based on interface mode.
 * 
 * @constructor
 */
GuacUI.Component = function() {

    /**
     * Called whenever this component needs to be shown and activated.
     * @event
     */
    this.onshow = null;

    /**
     * Called whenever this component needs to be hidden and deactivated.
     * @event
     */
    this.onhide = null;

};

/**
 * A Guacamole UI component which can be repositioned by dragging.
 * 
 * @constructor
 * @augments GuacUI.Component
 */
GuacUI.DraggableComponent = function(element) {

    var draggable_component = this;

    var position_x = 0;
    var position_y = 0;

    var start_x = 0;
    var start_y = 0;

    /*
     * Record drag start when finger hits element
     */
    if (element)
        element.addEventListener("touchstart", function(e) {
            
            if (e.touches.length == 1) {

                start_x = e.touches[0].screenX;
                start_y = e.touches[0].screenY;

            }
       
            e.stopPropagation();
       
        }, true);

    /*
     * Update position based on last touch
     */
    if (element)
        element.addEventListener("touchmove", function(e) {
            
            if (e.touches.length == 1) {
                
                var new_x = e.touches[0].screenX;
                var new_y = e.touches[0].screenY;

                position_x += new_x - start_x;
                position_y += new_y - start_y;

                start_x = new_x;
                start_y = new_y;

                // Move magnifier to new position
                draggable_component.move(position_x, position_y);

            }
            
            e.preventDefault();
            e.stopPropagation();

        }, true);

    if (element)
        element.addEventListener("touchend", function(e) {
            e.stopPropagation();
        }, true);
            
    /**
     * Moves this component to the specified location relative to its normal
     * position.
     * 
     * @param {Number} x The X coordinate in pixels.
     * @param {Number} y The Y coordinate in pixels.
     */
    this.move = function(x, y) {

        element.style.WebkitTransform =
        element.style.MozTransform =
        element.style.OTransform =
        element.style.msTransform =
        element.style.transform = "translate("
            + x + "px, " + y + "px)";

        if (draggable_component.onmove)
            draggable_component.onmove(x, y);

    };

    /**
     * Trigered whenever this element is moved.
     * 
     * @event
     * @param {Number} x The new X coordinate.
     * @param {Number} y The new Y coordinate.
     */
    this.onmove = null;

};