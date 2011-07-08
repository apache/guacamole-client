
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

    // Block context menu so right-click gets sent properly
    element.oncontextmenu = function(e) {
        return false;
    };

    element.onmousemove = function(e) {

        e.stopPropagation();

        var absoluteMouseX = e.pageX;
        var absoluteMouseY = e.pageY;

        guac_mouse.currentState.x = absoluteMouseX - element.offsetLeft;
        guac_mouse.currentState.y = absoluteMouseY - element.offsetTop;

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

    };


    element.onmousedown = function(e) {

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
    function handleScroll(e) {

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
    }

    element.addEventListener('DOMMouseScroll', handleScroll, false);

    element.onmousewheel = function(e) {
        handleScroll(e);
    };

};

Guacamole.Mouse.State = function(x, y, left, middle, right, up, down) {

    this.x = x;
    this.y = y;
    this.left = left;
    this.middle = middle
    this.right = right;
    this.up = up;
    this.down = down;
    
};

