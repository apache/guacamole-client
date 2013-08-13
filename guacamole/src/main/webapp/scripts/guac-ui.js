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
    if (classname) new_element.className = classname;
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
 * Creates a new row within the given table having a single header cell
 * with the given title, and a single value cell. The value cell is returned.
 */
GuacUI.createTabulatedContainer = function(table, title) {

    // Create elements
    var row    = GuacUI.createChildElement(table, "tr");
    var header = GuacUI.createChildElement(row, "th");
    var cell   = GuacUI.createChildElement(row, "td");

    // Set title, return cell
    header.textContent = title;
    return cell;

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
    element.addEventListener("click", function(e) {

        // Prevent click from affecting parent
        e.stopPropagation();
        e.preventDefault();

        // Attempt to focus existing window
        var current = window.open(null, connection.id);

        // If window did not already exist, set up as
        // Guacamole client
        if (!current.GuacUI)
            window.open(url, connection.id);

    }, false);


    // Add icon
    protocol.appendChild(protocol_icon);

    // Set name
    name.textContent = connection.name;

    // Assemble caption
    caption.appendChild(protocol);
    caption.appendChild(name);

    // Add active usages (if any)
    var active_users = connection.currentUsage();
    if (active_users > 0) {
        var usage = GuacUI.createChildElement(caption, "span", "usage");
        usage.textContent = "Currently in use by " + active_users + " user(s)";
        GuacUI.addClass(element, "in-use");
    }

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
 * A paging component. Elements can be added via the addElement() function,
 * and will only be shown if they are on the current page, set via setPage().
 * 
 * Beware that all elements will be added to the given container element, and
 * all children of the container element will be removed when the page is
 * changed.
 */
GuacUI.Pager = function(container) {

    var guac_pager = this;

    /**
     * A container for all pager control buttons.
     */
    var element = GuacUI.createElement("div", "pager");

    /**
     * All displayable elements.
     */
    var elements = [];

    /**
     * The number of elements to display per page.
     */
    this.page_capacity = 10;

    /**
     * The number of pages to generate a window for.
     */
    this.window_size = 11;

    /**
     * The current page, where 0 is the first page.
     */
    this.current_page = 0;

    /**
     * The last existing page.
     */
    this.last_page = 0;

    function update_display() {

        var i;

        // Calculate first and last elements of page (where the last element
        // is actually the first element of the next page)
        var first_element = guac_pager.current_page * guac_pager.page_capacity;
        var last_element  = Math.min(elements.length,
                first_element + guac_pager.page_capacity);

        // Clear contents, add elements
        container.innerHTML = "";
        for (i=first_element; i < last_element; i++)
            container.appendChild(elements[i]);

        // Update buttons
        element.innerHTML = "";

        // Create first and prev buttons
        var first = GuacUI.createChildElement(element, "div", "first-page icon");
        var prev = GuacUI.createChildElement(element, "div", "prev-page icon");

        // Handle prev/first
        if (guac_pager.current_page > 0) {
            first.onclick = function() {
                guac_pager.setPage(0);
            };

            prev.onclick = function() {
                guac_pager.setPage(guac_pager.current_page - 1);
            };
        }
        else {
            GuacUI.addClass(first, "disabled");
            GuacUI.addClass(prev, "disabled");
        }

        // Calculate page jump window start/end
        var window_start = guac_pager.current_page - (guac_pager.window_size - 1) / 2;
        var window_end = window_start + guac_pager.window_size - 1;

        // Shift window as necessary
        if (window_start < 0) {
            window_end = Math.min(guac_pager.last_page, window_end - window_start);
            window_start = 0;
        }
        else if (window_end > guac_pager.last_page) {
            window_start = Math.max(0, window_start - window_end + guac_pager.last_page);
            window_end = guac_pager.last_page;
        }
        
        // Add ellipsis if window after beginning
        if (window_start != 0)
            GuacUI.createChildElement(element, "div", "more-pages").textContent = "...";
        
        // Add page jumps
        for (i=window_start; i<=window_end; i++) {

            // Create clickable element containing page number
            var jump = GuacUI.createChildElement(element, "div", "set-page");
            jump.textContent = i+1;
            
            // Mark current page
            if (i == guac_pager.current_page)
                GuacUI.addClass(jump, "current");

            // If not current, add click event
            else
                (function(page_number) {
                    jump.onclick = function() {
                        guac_pager.setPage(page_number);
                    };
                })(i);

        }

        // Add ellipsis if window before end
        if (window_end != guac_pager.last_page)
            GuacUI.createChildElement(element, "div", "more-pages").textContent = "...";
        
        // Create next and last buttons
        var next = GuacUI.createChildElement(element, "div", "next-page icon");
        var last = GuacUI.createChildElement(element, "div", "last-page icon");

        // Handle next/last
        if (guac_pager.current_page < guac_pager.last_page) {
            next.onclick = function() {
                guac_pager.setPage(guac_pager.current_page + 1);
            };
            
            last.onclick = function() {
                guac_pager.setPage(guac_pager.last_page);
            };
        }
        else {
            GuacUI.addClass(next, "disabled");
            GuacUI.addClass(last, "disabled");
        }

    }

    /**
     * Adds the given element to the set of displayable elements.
     */
    this.addElement = function(element) {
        elements.push(element);
        guac_pager.last_page = Math.max(0,
            Math.floor((elements.length - 1) / guac_pager.page_capacity));
    };

    /**
     * Sets the current page, where 0 is the first page.
     */
    this.setPage = function(number) {
        guac_pager.current_page = number;
        update_display();
    };

    /**
     * Returns the element representing the buttons of this pager.
     */
    this.getElement = function() {
        return element;
    };

};


/**
 * Interface object which displays the progress of a download, ultimately
 * becoming a download link once complete.
 * 
 * @constructor
 * @param {String} filename The name the file will have once complete.
 */
GuacUI.Download = function(filename) {

    /**
     * Reference to this GuacUI.Download.
     * @private
     */
    var guac_download = this;

    /**
     * The outer div representing the notification.
     * @private
     */
    var element = GuacUI.createElement("div", "download notification");

    /**
     * Title bar describing the notification.
     * @private
     */
    var title = GuacUI.createChildElement(element, "div", "title-bar");

    /**
     * Close button for removing the notification.
     * @private
     */
    var close_button = GuacUI.createChildElement(title, "div", "close");
    close_button.onclick = function() {
        if (guac_download.onclose)
            guac_download.onclose();
    };

    GuacUI.createChildElement(title, "div", "title").textContent =
        "File Transfer";

    GuacUI.createChildElement(element, "div", "caption").textContent =
        filename + " ";

    /**
     * Progress bar and status.
     * @private
     */
    var progress = GuacUI.createChildElement(element, "div", "progress");

    /**
     * Updates the content of the progress indicator with the given text.
     * 
     * @param {String} text The text to assign to the progress indicator.
     */
    this.updateProgress = function(text) {
        progress.textContent = text;
    };

    /**
     * Removes the progress indicator and replaces it with a download button.
     */
    this.complete = function() {

        element.removeChild(progress);
        GuacUI.addClass(element, "complete");

        var download = GuacUI.createChildElement(element, "div", "download");
        download.textContent = "Download";
        download.onclick = function() {
            if (guac_download.ondownload)
                guac_download.ondownload();
        };

    };

    /**
     * Returns the element representing this notification.
     */
    this.getElement = function() {
        return element;
    };

    /**
     * Called when the close button of this notification is clicked.
     * @event
     */
    this.onclose = null;

    /**
     * Called when the download button of this notification is clicked.
     * @event
     */
    this.ondownload = null;

};

/**
 * A grouping component. Child elements can be added via the addElement()
 * function. By default, groups display as collapsed.
 */
GuacUI.ListGroup = function(caption) {

    /**
     * Reference to this group.
     * @private
     */
    var guac_group = this;

    /**
     * A container for for the list group itself.
     */
    var element = GuacUI.createElement("div", "group");

    // Create connection display elements
    var caption_element = GuacUI.createChildElement(element, "div",  "caption");
    var caption_icon    = GuacUI.createChildElement(caption_element, "div",  "icon group");
    GuacUI.createChildElement(caption_element, "span", "name").textContent = caption;

    /**
     * A container for all children of this list group.
     */
    var elements = GuacUI.createChildElement(element, "div", "children");

    // Toggle by default
    element.addEventListener("click", function(e) {

        // Prevent click from affecting parent
        e.stopPropagation();
        e.preventDefault();

        if (guac_group.expanded)
            guac_group.collapse();
        else
            guac_group.expand();

    }, false);

    /**
     * Whether this group is expanded.
     * 
     * @type Boolean
     */
    this.expanded = false;

    /**
     * Returns the element representing this notification.
     */
    this.getElement = function() {
        return element;
    };

    /**
     * Adds an element as a child of this group.
     * 
     * @param {
     */
    this.addElement = function(element) {
        elements.appendChild(element);
    };

    /**
     * Expands the list group, revealing all children of the group. This
     * functionality requires supporting CSS.
     */
    this.expand = function() {
        GuacUI.addClass(element, "expanded");
        guac_group.expanded = true;
    };

    /**
     * Collapses the list group, hiding all children of the group. This
     * functionality requires supporting CSS.
     */
    this.collapse = function() {
        GuacUI.removeClass(element, "expanded");
        guac_group.expanded = false;
    };

}

/**
 * Component which displays a paginated tree view of all groups and their
 * connections.
 * 
 * @constructor
 * @param {GuacamoleService.ConnectionGroup} root_group The group to display
 *                                                      within the view.
 * @param {Boolean} multiselect Whether multiple objects are selectable.
 */
GuacUI.GroupView = function(root_group, multiselect) {

    /**
     * Reference to this GroupView.
     * @private
     */
    var group_view = this;

    // Group view components
    var element = GuacUI.createElement("div", "group-view");
    var list = GuacUI.createChildElement(element, "div", "list");

    /**
     * Set of all connections, indexed by ID.
     */
    this.connections = {};

    /**
     * Fired when a connection is clicked.
     *
     * @event
     * @param {GuacamolService.Connection} connection The connection which was
     *                                                clicked.
     */
    this.onconnectionclick = null;

    /**
     * Fired when a connection group is clicked.
     *
     * @event
     * @param {GuacamolService.ConnectionGroup} group The connection group which 
     *                                                was clicked.
     */
    this.ongroupclick = null;

    /**
     * Fired when a connection's selected status changes.
     *
     * @event
     * @param {GuacamolService.Connection} connection The connection whose
     *                                                status changed.
     * @param {Boolean} selected The new status of the connection.
     */
    this.onconnectionchange = null;

    /**
     * Fired when a connection group's selected status changes.
     *
     * @event
     * @param {GuacamolService.ConnectionGroup} group The connection group whose
     *                                                status changed.
     * @param {Boolean} selected The new status of the connection group.
     */
    this.ongroupchange = null;

    /**
     * Returns the element representing this group view.
     */
    this.getElement = function() {
        return element;
    };

    // Create pager for contents 
    var pager = new GuacUI.Pager(list);
    pager.page_capacity = 20;

    /**
     * Adds the given group to the given display parent object. This object
     * must have an addElement() function, which will be used for adding all
     * child elements representing child connections and groups.
     * 
     * @param {GuacamoleService.ConnectionGroup} group The group to add.
     * @param {Function} appendChild A function which, given an element, will add that
     *                               element the the display as desired.
     */
    function addGroup(group, appendChild) {

        var i;

        // Add all contained connections
        for (i=0; i<group.connections.length; i++) {

            // Add connection to set
            var connection = group.connections[i];
            group_view.connections[connection.id] = connection;

            // Add connection to connection list or parent group
            var guacui_connection = new GuacUI.Connection(connection);
            GuacUI.addClass(guacui_connection.getElement(), "list-item");
            appendChild(guacui_connection.getElement());

        } // end for each connection

        // Add all contained groups 
        for (i=0; i<group.groups.length; i++) {

            // Create display element for group
            var child_group = group.groups[i];
            var list_group = new GuacUI.ListGroup(child_group.name);

            // Recursively add all children to the new element
            addGroup(child_group, list_group.addElement);

            // Add element to display
            GuacUI.addClass(list_group.getElement(), "list-item");
            appendChild(list_group.getElement());

        } // end for each gorup

    }

    // Add root group directly to pager
    addGroup(root_group, pager.addElement);

    // Add buttons if more than one page
    if (pager.last_page !== 0) {
        var list_buttons = GuacUI.createChildElement(element, "div", "buttons");
        list_buttons.appendChild(pager.getElement());
    }

    // Start at page 0
    pager.setPage(0);

};

/**
 * Simple modal dialog providing a header, body, and footer. No other
 * functionality is provided other than a reasonable hierarchy of divs and
 * easy access to their corresponding elements.
 */
GuacUI.Dialog = function() {

    /**
     * The container of the entire dialog. Adding this element to the DOM
     * displays the dialog, while removing this element hides the dialog.
     * 
     * @private
     * @type Element
     */
    var element = GuacUI.createElement("div", "dialog-container");

    /**
     * The dialog itself. This element is not exposed outside this object,
     * but rather contains the header, body, and footer sections which are
     * exposed.
     * 
     * @private
     * @type Element
     */
    var dialog = GuacUI.createChildElement(element, "div", "dialog");

    /**
     * The header section of the dialog. This section would normally contain
     * the title.
     * 
     * @private
     * @type Element
     */
    var header = GuacUI.createChildElement(dialog, "div", "header");

    /**
     * The body section of the dialog. This section would normally contain any
     * form fields and content.
     * 
     * @private
     * @type Element
     */
    var body = GuacUI.createChildElement(dialog, "div", "body");

    /**
     * The footer section of the dialog. This section would normally contain
     * the buttons.
     * 
     * @private
     * @type Element
     */
    var footer = GuacUI.createChildElement(dialog, "div", "footer");

    /**
     * Returns the header section of this dialog. This section normally
     * contains the title of the dialog.
     * 
     * @return {Element} The header section of this dialog.
     */
    this.getHeader = function() {
        return header;
    };

    /**
     * Returns the body section of this dialog. This section normally contains
     * the form fields, etc. of a dialog.
     * 
     * @return {Element} The body section of this dialog.
     */
    this.getBody = function() {
        return body;
    };

    /**
     * Returns the footer section of this dialog. This section is normally
     * used to contain the buttons of the dialog.
     * 
     * @return {Element} The footer section of this dialog.
     */
    this.getFooter = function() {
        return footer;
    };

    /**
     * Returns the element representing this dialog. Adding this element to
     * the DOM shows the dialog, while removing this element hides the dialog.
     * 
     * @return {Element} The element representing this dialog.
     */
    this.getElement = function() {
        return element;
    };

};
