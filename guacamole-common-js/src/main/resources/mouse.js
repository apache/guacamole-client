
/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is guacamole-common-js.
 *
 * The Initial Developer of the Original Code is
 * Michael Jumper.
 * Portions created by the Initial Developer are Copyright (C) 2010
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

// Guacamole namespace
var Guacamole = Guacamole || {};

/**
 * Provides cross-browser mouse events for a given element. The events of
 * the given element are automatically populated with handlers that translate
 * mouse events into a non-browser-specific event provided by the
 * Guacamole.Mouse instance.
 * 
 * Touch events are translated into mouse events as if the touches occurred
 * on a touchpad (drag to push the mouse pointer, tap to click).
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

    function cancelEvent(e) {
        e.stopPropagation();
        if (e.preventDefault) e.preventDefault();
        e.returnValue = false;
    }

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
    element.addEventListener("contextmenu", function(e) {
        cancelEvent(e);
    }, false);

    element.addEventListener("mousemove", function(e) {

        // Don't handle if we aren't supposed to
        if (gesture_in_progress) return;

        cancelEvent(e);

        moveMouse(e.pageX, e.pageY);

    }, false);

    var last_touch_x = 0;
    var last_touch_y = 0;
    var last_touch_time = 0;
    var pixels_moved = 0;

    var gesture_in_progress = false;
    var click_release_timeout = null;

    element.addEventListener("touchend", function(e) {
        
        // If we're handling a gesture
        if (gesture_in_progress) {
            
            cancelEvent(e);
            
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

    }, false);

    element.addEventListener("touchstart", function(e) {

        // Record initial touch location and time for single-touch movement
        // and tap gestures
        if (e.touches.length == 1) {

            cancelEvent(e);

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

    }, false);

    element.addEventListener("touchmove", function(e) {

        // Handle single-touch movement gesture (touchpad mouse move)
        if (e.touches.length == 1) {

            cancelEvent(e);

            // Get change in touch location
            var touch = e.touches[0];
            var delta_x = touch.pageX - last_touch_x;
            var delta_y = touch.pageY - last_touch_y;

            // Track pixels moved
            pixels_moved += Math.abs(delta_x) + Math.abs(delta_y);

            // Update mouse location
            guac_mouse.currentState.x += delta_x;
            guac_mouse.currentState.y += delta_y;

            // Prevent mouse from leaving screen

            if (guac_mouse.currentState.x < 0)
                guac_mouse.currentState.x = 0;
            else if (guac_mouse.currentState.x >= element.offsetWidth)
                guac_mouse.currentState.x = element.offsetWidth - 1;

            if (guac_mouse.currentState.y < 0)
                guac_mouse.currentState.y = 0;
            else if (guac_mouse.currentState.y >= element.offsetHeight)
                guac_mouse.currentState.y = element.offsetHeight - 1;

            // Fire movement event, if defined
            if (guac_mouse.onmousemove)
                guac_mouse.onmousemove(guac_mouse.currentState);

            // Update touch location
            last_touch_x = touch.pageX;
            last_touch_y = touch.pageY;

        }

    }, false);


    element.addEventListener("mousedown", function(e) {

        // Don't handle if we aren't supposed to
        if (gesture_in_progress) return;

        cancelEvent(e);

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

    }, false);


    element.addEventListener("mouseup", function(e) {

        // Don't handle if we aren't supposed to
        if (gesture_in_progress) return;

        cancelEvent(e);

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

    }, false);

    element.addEventListener("mouseout", function(e) {

        // Don't handle if we aren't supposed to
        if (gesture_in_progress) return;

        // Get parent of the element the mouse pointer is leaving
       	if (!e) e = window.event;

        // Check that mouseout is due to actually LEAVING the element
        var target = e.relatedTarget || e.toElement;
        while (target != null) {
            if (target === element)
                return;
            target = target.parentNode;
        }

        cancelEvent(e);

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

    }, true);

    // Override selection on mouse event element.
    element.addEventListener("selectstart", function(e) {
        cancelEvent(e);
    }, false);

    // Scroll wheel support
    element.addEventListener('DOMMouseScroll', function(e) {

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

        cancelEvent(e);

    }, false);

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

