
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

/**
 * Namespace for all Guacamole JavaScript objects.
 * @namespace
 */
var Guacamole = Guacamole || {};

/**
 * Provides cross-browser mouse events for a given element. The events of
 * the given element are automatically populated with handlers that translate
 * mouse events into a non-browser-specific event provided by the
 * Guacamole.Mouse instance.
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
     * The number of mousemove events to require before re-enabling mouse
     * event handling after receiving a touch event.
     */
    this.touchMouseThreshold = 3;

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

    /**
     * Counter of mouse events to ignore. This decremented by mousemove, and
     * while non-zero, mouse events will have no effect.
     * @private
     */
    var ignore_mouse = 0;

    function cancelEvent(e) {
        e.stopPropagation();
        if (e.preventDefault) e.preventDefault();
        e.returnValue = false;
    }

    // Block context menu so right-click gets sent properly
    element.addEventListener("contextmenu", function(e) {
        cancelEvent(e);
    }, false);

    element.addEventListener("mousemove", function(e) {

        cancelEvent(e);

        // If ignoring events, decrement counter
        if (ignore_mouse) {
            ignore_mouse--;
            return;
        }

        guac_mouse.currentState.fromClientPosition(element, e.clientX, e.clientY);

        if (guac_mouse.onmousemove)
            guac_mouse.onmousemove(guac_mouse.currentState);

    }, false);

    element.addEventListener("mousedown", function(e) {

        cancelEvent(e);

        // Do not handle if ignoring events
        if (ignore_mouse)
            return;

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

        cancelEvent(e);

        // Do not handle if ignoring events
        if (ignore_mouse)
            return;

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

    }, false);

    // Override selection on mouse event element.
    element.addEventListener("selectstart", function(e) {
        cancelEvent(e);
    }, false);

    // Ignore all pending mouse events when touch events are the apparent source
    function ignorePendingMouseEvents() { ignore_mouse = guac_mouse.touchMouseThreshold; }

    element.addEventListener("touchmove",  ignorePendingMouseEvents, false);
    element.addEventListener("touchstart", ignorePendingMouseEvents, false);
    element.addEventListener("touchend",   ignorePendingMouseEvents, false);

    // Scroll wheel support
    function mousewheel_handler(e) {

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

    }
    element.addEventListener('DOMMouseScroll', mousewheel_handler, false);
    element.addEventListener('mousewheel',     mousewheel_handler, false);

};


/**
 * Provides cross-browser relative touch event translation for a given element.
 * 
 * Touch events are translated into mouse events as if the touches occurred
 * on a touchpad (drag to push the mouse pointer, tap to click).
 * 
 * @constructor
 * @param {Element} element The Element to use to provide touch events.
 */
Guacamole.Mouse.Touchpad = function(element) {

    /**
     * Reference to this Guacamole.Mouse.Touchpad.
     * @private
     */
    var guac_touchpad = this;

    /**
     * The distance a two-finger touch must move per scrollwheel event, in
     * pixels.
     */
    this.scrollThreshold = 20 * (window.devicePixelRatio || 1);

    /**
     * The maximum number of milliseconds to wait for a touch to end for the
     * gesture to be considered a click.
     */
    this.clickTimingThreshold = 250;

    /**
     * The maximum number of pixels to allow a touch to move for the gesture to
     * be considered a click.
     */
    this.clickMoveThreshold = 10 * (window.devicePixelRatio || 1);

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
     * Fired whenever a mouse button is effectively pressed. This can happen
     * as part of a "click" gesture initiated by the user by tapping one
     * or more fingers over the touchpad element, as part of a "scroll"
     * gesture initiated by dragging two fingers up or down, etc.
     * 
     * @event
     * @param {Guacamole.Mouse.State} state The current mouse state.
     */
	this.onmousedown = null;

    /**
     * Fired whenever a mouse button is effectively released. This can happen
     * as part of a "click" gesture initiated by the user by tapping one
     * or more fingers over the touchpad element, as part of a "scroll"
     * gesture initiated by dragging two fingers up or down, etc.
     * 
     * @event
     * @param {Guacamole.Mouse.State} state The current mouse state.
     */
	this.onmouseup = null;

    /**
     * Fired whenever the user moves the mouse by dragging their finger over
     * the touchpad element.
     * 
     * @event
     * @param {Guacamole.Mouse.State} state The current mouse state.
     */
	this.onmousemove = null;

    var touch_count = 0;
    var last_touch_x = 0;
    var last_touch_y = 0;
    var last_touch_time = 0;
    var pixels_moved = 0;

    var touch_buttons = {
        1: "left",
        2: "right",
        3: "middle"
    };

    var gesture_in_progress = false;
    var click_release_timeout = null;

    element.addEventListener("touchend", function(e) {
        
        e.stopPropagation();
        e.preventDefault();
            
        // If we're handling a gesture AND this is the last touch
        if (gesture_in_progress && e.touches.length == 0) {
            
            var time = new Date().getTime();

            // Get corresponding mouse button
            var button = touch_buttons[touch_count];

            // If mouse already down, release anad clear timeout
            if (guac_touchpad.currentState[button]) {

                // Fire button up event
                guac_touchpad.currentState[button] = false;
                if (guac_touchpad.onmouseup)
                    guac_touchpad.onmouseup(guac_touchpad.currentState);

                // Clear timeout, if set
                if (click_release_timeout) {
                    window.clearTimeout(click_release_timeout);
                    click_release_timeout = null;
                }

            }

            // If single tap detected (based on time and distance)
            if (time - last_touch_time <= guac_touchpad.clickTimingThreshold
                    && pixels_moved < guac_touchpad.clickMoveThreshold) {

                // Fire button down event
                guac_touchpad.currentState[button] = true;
                if (guac_touchpad.onmousedown)
                    guac_touchpad.onmousedown(guac_touchpad.currentState);

                // Delay mouse up - mouse up should be canceled if
                // touchstart within timeout.
                click_release_timeout = window.setTimeout(function() {
                    
                    // Fire button up event
                    guac_touchpad.currentState[button] = false;
                    if (guac_touchpad.onmouseup)
                        guac_touchpad.onmouseup(guac_touchpad.currentState);
                    
                    // Gesture now over
                    gesture_in_progress = false;

                }, guac_touchpad.clickTimingThreshold);

            }

            // If we're not waiting to see if this is a click, stop gesture
            if (!click_release_timeout)
                gesture_in_progress = false;

        }

    }, false);

    element.addEventListener("touchstart", function(e) {

        e.stopPropagation();
        e.preventDefault();

        // Track number of touches, but no more than three
        touch_count = Math.min(e.touches.length, 3);

        // Clear timeout, if set
        if (click_release_timeout) {
            window.clearTimeout(click_release_timeout);
            click_release_timeout = null;
        }

        // Record initial touch location and time for touch movement
        // and tap gestures
        if (!gesture_in_progress) {

            // Stop mouse events while touching
            gesture_in_progress = true;

            // Record touch location and time
            var starting_touch = e.touches[0];
            last_touch_x = starting_touch.clientX;
            last_touch_y = starting_touch.clientY;
            last_touch_time = new Date().getTime();
            pixels_moved = 0;

        }

    }, false);

    element.addEventListener("touchmove", function(e) {

        e.stopPropagation();
        e.preventDefault();

        // Get change in touch location
        var touch = e.touches[0];
        var delta_x = touch.clientX - last_touch_x;
        var delta_y = touch.clientY - last_touch_y;

        // Track pixels moved
        pixels_moved += Math.abs(delta_x) + Math.abs(delta_y);

        // If only one touch involved, this is mouse move
        if (touch_count == 1) {

            // Calculate average velocity in Manhatten pixels per millisecond
            var velocity = pixels_moved / (new Date().getTime() - last_touch_time);

            // Scale mouse movement relative to velocity
            var scale = 1 + velocity;

            // Update mouse location
            guac_touchpad.currentState.x += delta_x*scale;
            guac_touchpad.currentState.y += delta_y*scale;

            // Prevent mouse from leaving screen

            if (guac_touchpad.currentState.x < 0)
                guac_touchpad.currentState.x = 0;
            else if (guac_touchpad.currentState.x >= element.offsetWidth)
                guac_touchpad.currentState.x = element.offsetWidth - 1;

            if (guac_touchpad.currentState.y < 0)
                guac_touchpad.currentState.y = 0;
            else if (guac_touchpad.currentState.y >= element.offsetHeight)
                guac_touchpad.currentState.y = element.offsetHeight - 1;

            // Fire movement event, if defined
            if (guac_touchpad.onmousemove)
                guac_touchpad.onmousemove(guac_touchpad.currentState);

            // Update touch location
            last_touch_x = touch.clientX;
            last_touch_y = touch.clientY;

        }

        // Interpret two-finger swipe as scrollwheel
        else if (touch_count == 2) {

            // If change in location passes threshold for scroll
            if (Math.abs(delta_y) >= guac_touchpad.scrollThreshold) {

                // Decide button based on Y movement direction
                var button;
                if (delta_y > 0) button = "down";
                else             button = "up";

                // Fire button down event
                guac_touchpad.currentState[button] = true;
                if (guac_touchpad.onmousedown)
                    guac_touchpad.onmousedown(guac_touchpad.currentState);

                // Fire button up event
                guac_touchpad.currentState[button] = false;
                if (guac_touchpad.onmouseup)
                    guac_touchpad.onmouseup(guac_touchpad.currentState);

                // Only update touch location after a scroll has been
                // detected
                last_touch_x = touch.clientX;
                last_touch_y = touch.clientY;

            }

        }

    }, false);

};

/**
 * Provides cross-browser absolute touch event translation for a given element.
 * 
 * Touch events are translated into mouse events as if the touches occurred
 * on a touchscreen (tapping anywhere on the screen clicks at that point,
 * long-press to right-click).
 * 
 * @constructor
 * @param {Element} element The Element to use to provide touch events.
 */
Guacamole.Mouse.Touchscreen = function(element) {

    /**
     * Reference to this Guacamole.Mouse.Touchscreen.
     * @private
     */
    var guac_touchscreen = this;

    /**
     * The distance a two-finger touch must move per scrollwheel event, in
     * pixels.
     */
    this.scrollThreshold = 20 * (window.devicePixelRatio || 1);

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
     * Fired whenever a mouse button is effectively pressed. This can happen
     * as part of a "mousedown" gesture initiated by the user by pressing one
     * finger over the touchscreen element, as part of a "scroll" gesture
     * initiated by dragging two fingers up or down, etc.
     * 
     * @event
     * @param {Guacamole.Mouse.State} state The current mouse state.
     */
	this.onmousedown = null;

    /**
     * Fired whenever a mouse button is effectively released. This can happen
     * as part of a "mouseup" gesture initiated by the user by removing the
     * finger pressed against the touchscreen element, or as part of a "scroll"
     * gesture initiated by dragging two fingers up or down, etc.
     * 
     * @event
     * @param {Guacamole.Mouse.State} state The current mouse state.
     */
	this.onmouseup = null;

    /**
     * Fired whenever the user moves the mouse by dragging their finger over
     * the touchscreen element. Note that unlike Guacamole.Mouse.Touchpad,
     * dragging a finger over the touchscreen element will always cause
     * the mouse button to be effectively down, as if clicking-and-dragging.
     * 
     * @event
     * @param {Guacamole.Mouse.State} state The current mouse state.
     */
	this.onmousemove = null;

    element.addEventListener("touchend", function(e) {
        
        // Ignore if more than one touch
        if (e.touches.length + e.changedTouches.length != 1)
            return;

        e.stopPropagation();
        e.preventDefault();

        // Release button
        guac_touchscreen.currentState.left = false;

        // Fire release event when the last touch is released, if event defined
        if (e.touches.length == 0 && guac_touchscreen.onmouseup)
            guac_touchscreen.onmouseup(guac_touchscreen.currentState);

    }, false);

    element.addEventListener("touchstart", function(e) {

        // Ignore if more than one touch
        if (e.touches.length != 1)
            return;

        e.stopPropagation();
        e.preventDefault();

        // Get touch
        var touch = e.touches[0];

        // Update state
        guac_touchscreen.currentState.left = true;
        guac_touchscreen.currentState.fromClientPosition(element, touch.clientX, touch.clientY);

        // Fire press event, if defined
        if (guac_touchscreen.onmousedown)
            guac_touchscreen.onmousedown(guac_touchscreen.currentState);


    }, false);

    element.addEventListener("touchmove", function(e) {

        // Ignore if more than one touch
        if (e.touches.length != 1)
            return;

        e.stopPropagation();
        e.preventDefault();

        // Get touch
        var touch = e.touches[0];

        // Update state
        guac_touchscreen.currentState.fromClientPosition(element, touch.clientX, touch.clientY);

        // Fire movement event, if defined
        if (guac_touchscreen.onmousemove)
            guac_touchscreen.onmousemove(guac_touchscreen.currentState);

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
     * Reference to this Guacamole.Mouse.State.
     * @private
     */
    var guac_state = this;

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

    /**
     * Updates the position represented within this state object by the given
     * element and clientX/clientY coordinates (commonly available within event
     * objects). Position is translated from clientX/clientY (relative to
     * viewport) to element-relative coordinates.
     * 
     * @param {Element} element The element the coordinates should be relative
     *                          to.
     * @param {Number} clientX The X coordinate to translate, viewport-relative.
     * @param {Number} clientY The Y coordinate to translate, viewport-relative.
     */
    this.fromClientPosition = function(element, clientX, clientY) {
    
        guac_state.x = clientX - element.offsetLeft;
        guac_state.y = clientY - element.offsetTop;

        // This is all JUST so we can get the mouse position within the element
        var parent = element.offsetParent;
        while (parent && !(parent === document.body)) {
            guac_state.x -= parent.offsetLeft - parent.scrollLeft;
            guac_state.y -= parent.offsetTop  - parent.scrollTop;

            parent = parent.offsetParent;
        }

        // Element ultimately depends on positioning within document body,
        // take document scroll into account. 
        if (parent) {
            var documentScrollLeft = document.body.scrollLeft || document.documentElement.scrollLeft;
            var documentScrollTop = document.body.scrollTop || document.documentElement.scrollTop;

            guac_state.x -= parent.offsetLeft - documentScrollLeft;
            guac_state.y -= parent.offsetTop  - documentScrollTop;
        }

    };

};

