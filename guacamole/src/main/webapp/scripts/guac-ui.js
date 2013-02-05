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
 * Current session state, including settings.
 */
GuacUI.sessionState = new GuacamoleSessionState();

/**
 * Creates a new element having the given tagname and CSS class.
 */
GuacUI.createElement = function(tagname, classname) {
    var new_element = document.createElement(tagname);
    new_element.className = classname;
    return new_element;
};

/**
 * Creates a new element having the given tagname, CSS class, and specified
 * parent element.
 */
GuacUI.createChildElement = function(parent, tagname, classname) {
    var element = GuacUI.createElement(tagname, classname);
    parent.appendChild(element);
    return element;
};

/**
 * Adds the given CSS class to the given element.
 */
GuacUI.addClass = function(element, classname) {

    // If supported, use native classlist for addClass()
    if (Node.classlist)
        element.classList.add(classname);

    // Otherwise, simply add new class via string manipulation
    else
        element.className += " " + classname;

};

/**
 * Removes the given CSS class from the given element.
 */
GuacUI.removeClass = function(element, classname) {

    // If supported, use native classlist for removeClass()
    if (Node.classlist)
        element.classList.remove(classname);

    // Otherwise, remove class via string manipulation
    else {

        // Filter out classes with given name
        element.className = element.className.replace(/([^ ]+)[ ]*/g,
            function(match, testClassname, spaces, offset, string) {

                // If same class, remove
                if (testClassname == classname)
                    return "";

                // Otherwise, allow
                return match;
                
            }
        );

    } // end if no classlist support

};
   
/**
 * Object describing the UI's level of audio support. If the user has request
 * that audio be disabled, this object will pretend that audio is not
 * supported.
 */
GuacUI.Audio = new (function() {

    var codecs = [
        'audio/ogg; codecs="vorbis"',
        'audio/mp4; codecs="mp4a.40.5"',
        'audio/mpeg; codecs="mp3"',
        'audio/webm; codecs="vorbis"',
        'audio/wav; codecs=1'
    ];

    var probably_supported = [];
    var maybe_supported = [];

    /**
     * Array of all supported audio mimetypes, ordered by liklihood of
     * working.
     */
    this.supported = [];

    // If sound disabled, we're done now.
    if (GuacUI.sessionState.getProperty("disable-sound"))
        return;
    
    // Build array of supported audio formats
    codecs.forEach(function(mimetype) {

        var audio = new Audio();
        var support_level = audio.canPlayType(mimetype);

        // Trim semicolon and trailer
        var semicolon = mimetype.indexOf(";");
        if (semicolon != -1)
            mimetype = mimetype.substring(0, semicolon);

        // Partition by probably/maybe
        if (support_level == "probably")
            probably_supported.push(mimetype);
        else if (support_level == "maybe")
            maybe_supported.push(mimetype);

    });

    // Add probably supported types first
    Array.prototype.push.apply(
        this.supported, probably_supported);

    // Prioritize "maybe" supported types second
    Array.prototype.push.apply(
        this.supported, maybe_supported);

})();

/**
 * Object describing the UI's level of video support.
 */
GuacUI.Video = new (function() {

    var codecs = [
        'video/ogg; codecs="theora, vorbis"',
        'video/mp4; codecs="avc1.4D401E, mp4a.40.5"',
        'video/webm; codecs="vp8.0, vorbis"'
    ];

    var probably_supported = [];
    var maybe_supported = [];

    /**
     * Array of all supported video mimetypes, ordered by liklihood of
     * working.
     */
    this.supported = [];
    
    // Build array of supported audio formats
    codecs.forEach(function(mimetype) {

        var video = document.createElement("video");
        var support_level = video.canPlayType(mimetype);

        // Trim semicolon and trailer
        var semicolon = mimetype.indexOf(";");
        if (semicolon != -1)
            mimetype = mimetype.substring(0, semicolon);

        // Partition by probably/maybe
        if (support_level == "probably")
            probably_supported.push(mimetype);
        else if (support_level == "maybe")
            maybe_supported.push(mimetype);

    });

    // Add probably supported types first
    Array.prototype.push.apply(
        this.supported, probably_supported);

    // Prioritize "maybe" supported types second
    Array.prototype.push.apply(
        this.supported, maybe_supported);

})();


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

/**
 * A connection UI object which can be easily added to a list of connections
 * for sake of display.
 */
GuacUI.Connection = function(connection) {

    /**
     * The actual connection associated with this connection UI element.
     */
    this.connection = connection;

    function createElement(tagname, classname) {
        var new_element = document.createElement(tagname);
        new_element.className = classname;
        return new_element;
    }

    // Create connection display elements
    var element       = createElement("div",  "connection");
    var caption       = createElement("div",  "caption");
    var protocol      = createElement("div",  "protocol");
    var name          = createElement("span", "name");
    var protocol_icon = createElement("div",  "icon " + connection.protocol);
    var thumbnail     = createElement("div",  "thumbnail");
    var thumb_img;

    // Get URL
    var url = "client.xhtml?id=" + encodeURIComponent(connection.id);

    // Create link to client
    element.onclick = function() {

        // Attempt to focus existing window
        var current = window.open(null, connection.id);

        // If window did not already exist, set up as
        // Guacamole client
        if (!current.GuacUI)
            window.open(url, connection.id);

    };

    // Add icon
    protocol.appendChild(protocol_icon);

    // Set name
    name.textContent = connection.id;

    // Assemble caption
    caption.appendChild(protocol);
    caption.appendChild(name);

    // Assemble connection icon
    element.appendChild(thumbnail);
    element.appendChild(caption);

    // Add screenshot if available
    var thumbnail_url = GuacamoleHistory.get(connection.id).thumbnail;
    if (thumbnail_url) {

        // Create thumbnail element
        thumb_img = document.createElement("img");
        thumb_img.src = thumbnail_url;
        thumbnail.appendChild(thumb_img);

    }

    /**
     * Returns the DOM element representing this connection.
     */
    this.getElement = function() {
        return element;
    };

    /**
     * Returns whether this connection has an associated thumbnail.
     */
    this.hasThumbnail = function() {
        return thumb_img && true;
    };

    /**
     * Sets the thumbnail URL of this existing connection. Note that this will
     * only work if the connection already had a thumbnail associated with it.
     */
    this.setThumbnail = function(url) {

        // If no image element, create it
        if (!thumb_img) {
            thumb_img = document.createElement("img");
            thumb_img.src = url;
            thumbnail.appendChild(thumb_img);
        }

        // Otherwise, set source of existing
        else
            thumb_img.src = url;

    };

};

/**
 * An arbitrary table-based form.
 */
GuacUI.Form = function() {

    var element = GuacUI.createElement("table");

    /**
     * Returns the DOM element representing this form.
     */
    this.getElement = function() {
        return element;
    };

    this.addField = function(title, type, value) {

        // Add elements
        var row    = GuacUI.createChildElement(element, "tr");
        var header = GuacUI.createChildElement(row, "th");
        var cell   = GuacUI.createChildElement(row, "td");
        var input  = GuacUI.createChildElement(cell, "input");

        // Set title
        header.textContent = title;

        // Set type and value
        input.setAttribute("type", type);
        if (value) input.setAttribute("value", value);

    };

};

/**
 * A user UI object.
 */
GuacUI.User = function(username) {

    /**
     * This user's username.
     */
    this.username = username;

    var user_type = "normal";

    // Create connection display elements
    var element       = GuacUI.createElement("div",  "user");
    var caption       = GuacUI.createChildElement(element ,"div",  "caption");
    var type          = GuacUI.createChildElement(caption, "div",  "type");
    var name          = GuacUI.createChildElement(caption, "span", "name");
    GuacUI.createChildElement(type, "div",  "icon " + user_type);

    // Get URL
    var url = "user.xhtml?name=" + encodeURIComponent(username);

    // Create link to client
    element.onclick = function() {

        // Attempt to focus existing window
        var current = window.open(null, username);

        // If window did not already exist, set up as
        // Guacamole client
        if (!current.GuacUI)
            window.open(url, username);

    };

    // Set name
    name.textContent = username;

    /**
     * Returns the DOM element representing this connection.
     */
    this.getElement = function() {
        return element;
    };

};


/**
 * An editable user UI object.
 */
GuacUI.EditableUser = function(username) {

    /**
     * This user's username.
     */
    this.username = username;

    /**
     * Current status of edit mode.
     */
    this.edit = false;

    // Create contained user
    var user = new GuacUI.User(username);
    var element = user.getElement();
    GuacUI.addClass(element, "editable");

    // Fields
    var fields = GuacUI.createChildElement(element, "div", "fields");
    var form = new GuacUI.Form();

    // Add form
    fields.appendChild(form.getElement());

    // Add fields
    form.addField("Password:",          "password", "123412341234");
    form.addField("Re-enter Password:", "password", "123412341234");

    /**
     * Returns the DOM element representing this connection.
     */
    this.getElement = function() {
        return element;
    };

    /**
     * Sets/unsets edit mode. When edit mode is on, the user's properties
     * will be visible and editable.
     */
    this.setEditMode = function(enabled) {

        // Set edit mode
        this.edit = enabled;

        // Alter class accordingly
        if (enabled)
            GuacUI.addClass(element, "edit");
        else
            GuacUI.removeClass(element, "edit");

    };

};

