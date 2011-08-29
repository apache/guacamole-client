
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

// Guacamole namespace
var Guacamole = Guacamole || {};

/**
 * Provides cross-browser mouse events for a given element. The events of
 * the given element are automatically populated with handlers that translate
 * mouse events into a non-browser-specific event provided by the
 * Guacamole.Mouse instance.
 * 
 * Touch event support is planned, but currently only in testing (translate
 * touch events into mouse events).
 * 
 * @constructor
 * @param {Element} element The Element to use to provide mouse events.
 */
Guacamole.Mouse = function(element) {

    /**
     * Reference to this Guacamole.Mouse.
     * @private
     */
    var guac_mouse = this;

    /**
     * The current mouse state. The properties of this state are updated when
     * mouse events fire. This state object is also passed in as a parameter to
     * the handler of any mouse events.
     * 
     * @type Guacamole.Mouse.State
     */
    this.currentState = new Guacamole.Mouse.State(
        0, 0, 
        false, false, false, false, false
    );

    /**
     * Fired whenever the user presses a mouse button down over the element
     * associated with this Guacamole.Mouse.
     * 
     * @event
     * @param {Guacamole.Mouse.State} state The current mouse state.
     */
	this.onmousedown = null;

    /**
     * Fired whenever the user releases a mouse button down over the element
     * associated with this Guacamole.Mouse.
     * 
     * @event
     * @param {Guacamole.Mouse.State} state The current mouse state.
     */
	this.onmouseup = null;

    /**
     * Fired whenever the user moves the mouse over the element associated with
     * this Guacamole.Mouse.
     * 
     * @event
     * @param {Guacamole.Mouse.State} state The current mouse state.
     */
	this.onmousemove = null;

    function moveMouse(pageX, pageY) {

        guac_mouse.currentState.x = pageX - element.offsetLeft;
        guac_mouse.currentState.y = pageY - element.offsetTop;

        // This is all JUST so we can get the mouse position within the element
        var parent = element.offsetParent;
        while (parent) {
            if (parent.offsetLeft && parent.offsetTop) {
                guac_mouse.currentState.x -= parent.offsetLeft;
                guac_mouse.currentState.y -= parent.offsetTop;
            }
            parent = parent.offsetParent;
        }

        if (guac_mouse.onmousemove)
            guac_mouse.onmousemove(guac_mouse.currentState);

    }


    // Block context menu so right-click gets sent properly
    element.oncontextmenu = function(e) {
        return false;
    };

    element.onmousemove = function(e) {

        // Don't handle if we aren't supposed to
        if (gesture_in_progress) return;

        e.stopPropagation();

        moveMouse(e.pageX, e.pageY);

    };

    var last_touch_x = 0;
    var last_touch_y = 0;
    var last_touch_time = 0;
    var pixels_moved = 0;

    var gesture_in_progress = false;
    var click_release_timeout = null;

    element.ontouchend = function(e) {
        
        // If we're handling a gesture
        if (gesture_in_progress) {
            
            e.stopPropagation();
            e.preventDefault();
            
            var time = new Date().getTime();

            // If mouse already down, release anad clear timeout
            if (guac_mouse.currentState.left) {

                // Fire left button up event
                guac_mouse.currentState.left = false;
                if (guac_mouse.onmouseup)
                    guac_mouse.onmouseup(guac_mouse.currentState);

                // Clear timeout, if set
                if (click_release_timeout) {
                    window.clearTimeout(click_release_timeout);
                    click_release_timeout = null;
                }

            }

            // If single tap detected (based on time and distance)
            if (time - last_touch_time <= 250 && pixels_moved < 10) {

                // Fire left button down event
                guac_mouse.currentState.left = true;
                if (guac_mouse.onmousedown)
                    guac_mouse.onmousedown(guac_mouse.currentState);

                // Delay mouse up - mouse up should be canceled if
                // touchstart within timeout.
                click_release_timeout = window.setTimeout(function() {
                    
                    // Fire left button up event
                    guac_mouse.currentState.left = false;
                    if (guac_mouse.onmouseup)
                        guac_mouse.onmouseup(guac_mouse.currentState);
                    
                    // Allow mouse events now that touching is over
                    gesture_in_progress = false;
            
                }, 250);

            }

        }

    };

    element.ontouchstart = function(e) {

        // Record initial touch location and time for single-touch movement
        // and tap gestures
        if (e.touches.length == 1) {

            e.stopPropagation();
            e.preventDefault();

            // Stop mouse events while touching
            gesture_in_progress = true;

            // Clear timeout, if set
            if (click_release_timeout) {
                window.clearTimeout(click_release_timeout);
                click_release_timeout = null;
            }

            // Record touch location and time
            var starting_touch = e.touches[0];
            last_touch_x = starting_touch.pageX;
            last_touch_y = starting_touch.pageY;
            last_touch_time = new Date().getTime();
            pixels_moved = 0;

            // TODO: Handle different buttons

        }

    };

    element.ontouchmove = function(e) {

        // Handle single-touch movement gesture (touchpad mouse move)
        if (e.touches.length == 1) {

            e.stopPropagation();
            e.preventDefault();

            // Get change in touch location
            var touch = e.touches[0];
            var delta_x = touch.pageX - last_touch_x;
            var delta_y = touch.pageY - last_touch_y;

            // Track pixels moved
            pixels_moved += Math.abs(delta_x) + Math.abs(delta_y);

            // Update mouse location
            guac_mouse.currentState.x += delta_x;
            guac_mouse.currentState.y += delta_y;

            // FIXME: Prevent mouse from leaving screen

            // Fire movement event, if defined
            if (guac_mouse.onmousemove)
                guac_mouse.onmousemove(guac_mouse.currentState);

            // Update touch location
            last_touch_x = touch.pageX;
            last_touch_y = touch.pageY;

        }

    };


    element.onmousedown = function(e) {

        // Don't handle if we aren't supposed to
        if (gesture_in_progress) return;

        e.stopPropagation();

        switch (e.button) {
            case 0:
                guac_mouse.currentState.left = true;
                break;
            case 1:
                guac_mouse.currentState.middle = true;
                break;
            case 2:
                guac_mouse.currentState.right = true;
                break;
        }

        if (guac_mouse.onmousedown)
            guac_mouse.onmousedown(guac_mouse.currentState);

    };


    element.onmouseup = function(e) {

        // Don't handle if we aren't supposed to
        if (gesture_in_progress) return;

        e.stopPropagation();

        switch (e.button) {
            case 0:
                guac_mouse.currentState.left = false;
                break;
            case 1:
                guac_mouse.currentState.middle = false;
                break;
            case 2:
                guac_mouse.currentState.right = false;
                break;
        }

        if (guac_mouse.onmouseup)
            guac_mouse.onmouseup(guac_mouse.currentState);

    };

    element.onmouseout = function(e) {

        // Don't handle if we aren't supposed to
        if (gesture_in_progress) return;

        e.stopPropagation();

        // Release all buttons
        if (guac_mouse.currentState.left
            || guac_mouse.currentState.middle
            || guac_mouse.currentState.right) {

            guac_mouse.currentState.left = false;
            guac_mouse.currentState.middle = false;
            guac_mouse.currentState.right = false;

            if (guac_mouse.onmouseup)
                guac_mouse.onmouseup(guac_mouse.currentState);
        }

    };

    // Override selection on mouse event element.
    element.onselectstart = function() {
        return false;
    };

    // Scroll wheel support
    element.onmousewheel = function(e) {

        // Don't handle if we aren't supposed to
        if (gesture_in_progress) return;

        var delta = 0;
        if (e.detail)
            delta = e.detail;
        else if (e.wheelDelta)
            delta = -event.wheelDelta;

        // Up
        if (delta < 0) {
            if (guac_mouse.onmousedown) {
                guac_mouse.currentState.up = true;
                guac_mouse.onmousedown(guac_mouse.currentState);
            }

            if (guac_mouse.onmouseup) {
                guac_mouse.currentState.up = false;
                guac_mouse.onmouseup(guac_mouse.currentState);
            }
        }

        // Down
        if (delta > 0) {
            if (guac_mouse.onmousedown) {
                guac_mouse.currentState.down = true;
                guac_mouse.onmousedown(guac_mouse.currentState);
            }

            if (guac_mouse.onmouseup) {
                guac_mouse.currentState.down = false;
                guac_mouse.onmouseup(guac_mouse.currentState);
            }
        }

        if (e.preventDefault)
            e.preventDefault();

        e.returnValue = false;

    };

    element.addEventListener('DOMMouseScroll', element.onmousewheel, false);

};

/**
 * Simple container for properties describing the state of a mouse.
 * 
 * @constructor
 * @param {Number} x The X position of the mouse pointer in pixels.
 * @param {Number} y The Y position of the mouse pointer in pixels.
 * @param {Boolean} left Whether the left mouse button is pressed. 
 * @param {Boolean} middle Whether the middle mouse button is pressed. 
 * @param {Boolean} right Whether the right mouse button is pressed. 
 * @param {Boolean} up Whether the up mouse button is pressed (the fourth
 *                     button, usually part of a scroll wheel). 
 * @param {Boolean} down Whether the down mouse button is pressed (the fifth
 *                       button, usually part of a scroll wheel). 
 */
Guacamole.Mouse.State = function(x, y, left, middle, right, up, down) {

    /**
     * The current X position of the mouse pointer.
     * @type Number
     */
    this.x = x;

    /**
     * The current Y position of the mouse pointer.
     * @type Number
     */
    this.y = y;

    /**
     * Whether the left mouse button is currently pressed.
     * @type Boolean
     */
    this.left = left;

    /**
     * Whether the middle mouse button is currently pressed.
     * @type Boolean
     */
    this.middle = middle

    /**
     * Whether the right mouse button is currently pressed.
     * @type Boolean
     */
    this.right = right;

    /**
     * Whether the up mouse button is currently pressed. This is the fourth
     * mouse button, associated with upward scrolling of the mouse scroll
     * wheel.
     * @type Boolean
     */
    this.up = up;

    /**
     * Whether the down mouse button is currently pressed. This is the fifth 
     * mouse button, associated with downward scrolling of the mouse scroll
     * wheel.
     * @type Boolean
     */
    this.down = down;
    
};

