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
 * General set of UI elements and UI-related functions regarding
 * administration.
 */
var GuacAdmin = {

    "containers" : {
        "connection_list"         : document.getElementById("connection-list"),
        "user_list"               : document.getElementById("user-list"),
        "user_list_buttons"       : document.getElementById("user-list-buttons"),
        "connection_list_buttons" : document.getElementById("connection-list-buttons")
    },

    "buttons" : {
        "back"           : document.getElementById("back"),
        "logout"         : document.getElementById("logout"),
        "add_connection" : document.getElementById("add-connection"),
        "add_user"       : document.getElementById("add-user")
    },

    "fields" : {
        "connection_id" : document.getElementById("connection-id"),
        "username"      : document.getElementById("username")
    },

    "cached_permissions" : null,
    "cached_protocols"   : null,
    "cached_connections" : null,

    "selected_user"       : null,
    "selected_connection" : null

};

/**
 * An arbitrary input field.
 * 
 * @constructor
 */
GuacAdmin.Field = function() {

    /**
     * Returns the DOM Element representing this field.
     * 
     * @return {Element} The DOM Element representing this field.
     */
    this.getElement = function() {};

    /**
     * Returns the value of this field.
     * 
     * @return {String} The value of this field.
     */
    this.getValue = function() {};

    /**
     * Sets the value of this field.
     * 
     * @param {String} value The value of this field.
     */
    this.setValue = function(value) {};

};


/**
 * Simple HTML input field.
 * 
 * @augments GuacAdmin.Field
 * @param {String} type The type of HTML field.
 */
GuacAdmin.Field._HTML_INPUT = function(type) {

    // Call parent constructor
    GuacAdmin.Field.apply(this);

    // Create backing element
    var element = GuacUI.createElement("input");
    element.setAttribute("type", type);

    this.getValue = function() {
        return element.value;
    };

    this.getElement = function() {
        return element;
    };

    this.setValue = function(value) {
        element.value = value;
    };

};

GuacAdmin.Field._HTML_INPUT.prototype = new GuacAdmin.Field();


/**
 * A basic text field.
 * 
 * @augments GuacAdmin.Field._HTML_INPUT
 */
GuacAdmin.Field.TEXT = function() {
    GuacAdmin.Field._HTML_INPUT.apply(this, ["text"]);
};

GuacAdmin.Field.TEXT.prototype = new GuacAdmin.Field._HTML_INPUT();


/**
 * A basic password field.
 * 
 * @augments GuacAdmin.Field._HTML_INPUT
 */
GuacAdmin.Field.PASSWORD = function() {
    GuacAdmin.Field._HTML_INPUT.apply(this, ["password"]);
};

GuacAdmin.Field.PASSWORD.prototype = new GuacAdmin.Field._HTML_INPUT();

/**
 * Simple checkbox.
 * 
 * @augments GuacAdmin.Field
 */
GuacAdmin.Field.CHECKBOX = function(value) {

    // Call parent constructor
    GuacAdmin.Field.apply(this);

    // Create backing element
    var element = GuacUI.createElement("input");
    element.setAttribute("type", "checkbox");
    element.setAttribute("value", value);

    this.getValue = function() {
        if (element.checked)
            return value;
        else
            return "";
    };

    this.getElement = function() {
        return element;
    };

    this.setValue = function(new_value) {
        if (new_value == value)
            element.checked = true;
        else
            element.checked = false;
    };

};

GuacAdmin.Field.CHECKBOX.prototype = new GuacAdmin.Field();

/**
 * Enumerated field type.
 * 
 * @augments GuacAdmin.Field
 */
GuacAdmin.Field.ENUM = function(values) {

    // Call parent constructor
    GuacAdmin.Field.apply(this);

    // Create backing element
    var element = GuacUI.createElement("select");
    for (var name in values) {
        var option = GuacUI.createChildElement(element, "option");
        option.textContent = values[name];
        option.value = name;
    }

    this.getValue = function() {
        return element.value;
    };

    this.getElement = function() {
        return element;
    };

    this.setValue = function(value) {
        element.value = value;
    };

};

GuacAdmin.Field.ENUM.prototype = new GuacAdmin.Field();


/**
 * An arbitrary button.
 * 
 * @constructor
 * @param {String} title A human-readable title for the button.
 */
GuacAdmin.Button = function(title) {

    /**
     * A human-readable title describing this button.
     */
    this.title = title;

    // Button element
    var element = GuacUI.createElement("button");
    element.textContent = title;

    /**
     * Returns the DOM element associated with this button.
     */
    this.getElement = function() {
        return element;
    };

};

/**
 * An arbitrary list item with an icon and caption.
 */
GuacAdmin.ListItem = function(type, title) {

    // Create connection display elements
    var element = GuacUI.createElement("div",  "list-item");
    var icon    = GuacUI.createChildElement(element, "div",  "icon");
    var name    = GuacUI.createChildElement(element, "span", "name");
    GuacUI.addClass(icon, type);

    // Set name
    name.textContent = title;

    /**
     * Returns the DOM element representing this connection.
     */
    this.getElement = function() {
        return element;
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
GuacAdmin.Pager = function(container) {

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
    this.window_size = 5;

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

/*
 * Set handler for logout
 */

GuacAdmin.buttons.logout.onclick = function() {
    window.location.href = "logout";
};

/*
 * Set handler for back button 
 */

GuacAdmin.buttons.back.onclick = function() {
    window.location.href = "index.xhtml";
};

/**
 * Returns whether the given object has at least one property.
 */
GuacAdmin.hasEntry = function(object) {
    for (var name in object)
        return true;
    return false;
};

/**
 * Given a Date, returns a formatted String.
 * 
 * @param {Date} date The date tor format.
 * @return {String} A formatted String.
 */
GuacAdmin.formatDate = function(date) {

    var month = date.getMonth() + 1;
    var day   = date.getDate();
    var year  = date.getFullYear();

    var hour   = date.getHours();
    var minute = date.getMinutes();
    var second = date.getSeconds();

    return      ("00" +  month).slice(-2)
        + "/" + ("00" +    day).slice(-2)
        + "/" + year
        + " " + ("00" +   hour).slice(-2)
        + ":" + ("00" + minute).slice(-2)
        + ":" + ("00" + second).slice(-2);

};

/**
 * Given a number of seconds, returns a String representing that length
 * of time in a human-readable format.
 * 
 * @param {Number} seconds The number of seconds.
 * @return {String} A human-readable description of the duration specified.
 */
GuacAdmin.formatSeconds = function(seconds) {

    function round(value) {
        return Math.round(value * 10) / 10;
    }

    if (seconds < 60)    return round(seconds)        + " seconds";
    if (seconds < 3600)  return round(seconds / 60)   + " minutes";
    if (seconds < 86400) return round(seconds / 3600) + " hours";
    return round(seconds / 86400) + " days";

};

/**
 * Currently-defined pager for users, if any.
 */
GuacAdmin.userPager = null;

/**
 * Adds the user with the given name to the displayed user list.
 */
GuacAdmin.addUser = function(name) {

    // Create user list item
    var item = new GuacAdmin.ListItem("user", name);
    var item_element = item.getElement();
    GuacAdmin.userPager.addElement(item_element);

    // When clicked, build and display property form
    item_element.onclick = function() {

        // Ignore clicks if any item is selected
        if (GuacAdmin.selected_user) return;
        else GuacAdmin.selected_user = name;

        // Get user permissions
        var user_perms = GuacamoleService.Permissions.list(name);

        // Create form base elements
        var form_element = GuacUI.createElement("div", "form");
        var user_header = GuacUI.createChildElement(form_element, "h2");
        var sections = GuacUI.createChildElement(
            GuacUI.createChildElement(form_element, "div", "settings section"),
            "dl");

        var field_header = GuacUI.createChildElement(sections, "dt");
        var field_table  = GuacUI.createChildElement(
            GuacUI.createChildElement(sections, "dd"),
            "table", "fields section");

        user_header.textContent = name;
        field_header.textContent = "Properties:";

        // Deselect
        function deselect() {
            GuacUI.removeClass(GuacAdmin.containers.user_list, "disabled");
            GuacUI.removeClass(item_element, "selected");
            item_element.removeChild(form_element);
            GuacAdmin.selected_user = null;
        }

        // Select
        function select() {
            GuacUI.addClass(GuacAdmin.containers.user_list, "disabled");
            GuacUI.addClass(item_element, "selected");
            item_element.appendChild(form_element);
        }

        // Add password field
        var password_field = GuacUI.createChildElement(
                GuacUI.createTabulatedContainer(field_table, "Password:"),
                "input");
        password_field.setAttribute("type",  "password");
        password_field.setAttribute("value", "password");
            
        // Add password re-entry field
        var reenter_password_field = GuacUI.createChildElement(
                GuacUI.createTabulatedContainer(field_table, "Re-enter Password:"),
                "input");
        reenter_password_field.setAttribute("type",  "password");
        reenter_password_field.setAttribute("value", "password");

        // Update password if changed
        var password_modified = false;
        password_field.onchange =
        reenter_password_field.onchange = function() {
            password_modified = true;
        };

        // If readable connections exist, list them
        var selected_connections = {};
        if (GuacAdmin.hasEntry(GuacAdmin.cached_permissions.administer_connection)) {

            // Add fields for per-connection checkboxes
            var connections_header = GuacUI.createChildElement(sections, "dt");
            connections_header.textContent = "Connections:";
            var connections = GuacUI.createChildElement(
                GuacUI.createChildElement(sections, "dd"),
                "div", "list");

            for (var conn in GuacAdmin.cached_permissions.administer_connection) {

                var connection       = GuacUI.createChildElement(connections, "div", "connection");
                var connection_field = GuacUI.createChildElement(connection, "input");
                var connection_name  = GuacUI.createChildElement(connection, "span", "name");

                connection_field.setAttribute("type", "checkbox");
                connection_field.setAttribute("value", conn);

                // Check checkbox if connection readable by selected user
                if (conn in user_perms.read_connection) {
                    selected_connections[conn] = true;
                    connection_field.checked = true;
                }

                // Update selected connections when changed
                connection_field.onclick = connection_field.onchange = function() {
                    if (this.checked)
                        selected_connections[this.value] = true;
                    else if (selected_connections[this.value])
                        delete selected_connections[this.value];
                };

                connection_name.textContent = conn;

            }

        }

        // Add buttons
        var button_div = GuacUI.createChildElement(form_element, "div", "object-buttons");

        // Add save button
        var save_button = GuacUI.createChildElement(button_div, "button");
        save_button.textContent = "Save";
        save_button.onclick = function(e) {

            e.stopPropagation();

            try {

                // If password modified, use password given
                var password;
                if (password_modified) {

                    // Get passwords
                    password = password_field.value;
                    var reentered_password = reenter_password_field.value;

                    // Check that passwords match
                    if (password != reentered_password)
                        throw new Error("Passwords do not match.");

                }

                // Otherwise, do not change password
                else
                    password = null;

                // Set user permissions
                user_perms.read_connection = selected_connections;

                // Save user
                GuacamoleService.Users.update(
                    GuacAdmin.selected_user, password, user_perms);
                deselect();
                GuacAdmin.reset();

            }
            catch (e) {
                alert(e.message);
            }

        };

        // Add cancel button
        var cancel_button = GuacUI.createChildElement(button_div, "button");
        cancel_button.textContent = "Cancel";
        cancel_button.onclick = function(e) {
            e.stopPropagation();
            deselect();
        };

        // Add delete button if permission available
        if (name in GuacAdmin.cached_permissions.remove_user) {
            
            // Create button
            var delete_button = GuacUI.createChildElement(button_div, "button");
            delete_button.textContent = "Delete";
            
            // Remove selected user when clicked
            delete_button.onclick = function(e) {

                e.stopPropagation();

                GuacamoleService.Users.remove(GuacAdmin.selected_user);
                deselect();
                GuacAdmin.reset();

            };

        }

        // Select item
        select();

    };

};

/**
 * Currently-defined pager for connections, if any.
 */
GuacAdmin.connectionPager = null;

/**
 * Adds the given connection to the displayed connection list.
 */
GuacAdmin.addConnection = function(connection) {

    var item = new GuacAdmin.ListItem("connection", connection.id);
    var item_element = item.getElement();
    GuacAdmin.connectionPager.addElement(item_element);

    item_element.onclick = function() {

        // Ignore clicks if any item is selected
        if (GuacAdmin.selected_connection) return;
        else GuacAdmin.selected_connection = connection.id;

        var i;

        // Create form base elements
        var form_element = GuacUI.createElement("div", "form");
        var connection_header = GuacUI.createChildElement(form_element, "h2");
        connection_header.textContent = connection.id;

        var sections = GuacUI.createChildElement(
            GuacUI.createChildElement(form_element, "div", "settings section"),
            "dl");

        // Parameter header
        var protocol_header = GuacUI.createChildElement(sections, "dt")
        protocol_header.textContent = "Protocol:";
        
        var protocol_field = GuacUI.createChildElement(protocol_header, "select");

        // Associative set of protocols
        var available_protocols = {};

        // All form fields by parameter name
        var fields = {};

        // Add protocols
        for (i=0; i<GuacAdmin.cached_protocols.length; i++) {

            // Get protocol and store in associative set
            var protocol = GuacAdmin.cached_protocols[i];
            available_protocols[protocol.name] = protocol;

            // List protocol in select
            var protocol_title = GuacUI.createChildElement(protocol_field, "option");
            protocol_title.textContent = protocol.title;
            protocol_title.value = protocol.name;

        }

        // Parameter section
        var field_table  = GuacUI.createChildElement(
            GuacUI.createChildElement(sections, "dd"),
            "table", "fields section");

        // History header
        var history_header = GuacUI.createChildElement(sections, "dt")
        history_header.textContent = "Usage History:";

        // If history present, display as table
        if (connection.history.length > 0) {

            // History section
            var history_table  = GuacUI.createChildElement(
                GuacUI.createChildElement(sections, "dd"),
                "table", "history section");

            var history_table_header = GuacUI.createChildElement(
                history_table, "tr");

            GuacUI.createChildElement(history_table_header, "th").textContent =
                "Username";

            GuacUI.createChildElement(history_table_header, "th").textContent =
                "Start Time";

            GuacUI.createChildElement(history_table_header, "th").textContent =
                "Duration";

            // Add history
            for (i=0; i<connection.history.length; i++) {

                // Get record
                var record = connection.history[i];

                // Create record elements
                var row = GuacUI.createChildElement(history_table, "tr");
                var user = GuacUI.createChildElement(row, "td", "username");
                var start = GuacUI.createChildElement(row, "td", "start");
                var duration = GuacUI.createChildElement(row, "td", "duration");

                // Display record
                user.textContent = record.username;
                start.textContent = GuacAdmin.formatDate(record.start);
                if (record.duration)
                    duration.textContent = GuacAdmin.formatSeconds(record.duration);
                else
                    duration.textContent = "Active now";

            }

        }
        else
            GuacUI.createChildElement(
                GuacUI.createChildElement(sections, "dd"), "p").textContent =
                    "This connection has not yet been used.";

        // Deselect
        function deselect() {
            GuacUI.removeClass(GuacAdmin.containers.connection_list, "disabled");
            GuacUI.removeClass(item_element, "selected");
            item_element.removeChild(form_element);
            GuacAdmin.selected_connection = null;
        }

        // Select
        function select() {
            GuacUI.addClass(GuacAdmin.containers.connection_list, "disabled");
            GuacUI.addClass(item_element, "selected");
            item_element.appendChild(form_element);
        }

        // Display fields for the given protocol name 
        function setFields(protocol_name) {

            // Clear fields
            field_table.innerHTML = "";

            // Get protocol
            var protocol = available_protocols[protocol_name];

            // For each parameter
            for (var name in protocol.parameters) {

                // Get parameter
                var parameter = protocol.parameters[name];

                // Create corresponding field
                var field;
                switch (parameter.type) {

                    case "text":
                        field = new GuacAdmin.Field.TEXT();
                        break;

                    case "password":
                        field = new GuacAdmin.Field.PASSWORD();
                        break;

                    case "boolean":
                        field = new GuacAdmin.Field.CHECKBOX(parameter.value);
                        break;

                    case "enum":
                        field = new GuacAdmin.Field.ENUM(parameter.options);
                        break;

                    default:
                        continue;

                }

                // Create container for field
                var container = 
                    GuacUI.createTabulatedContainer(field_table, parameter.title + ":");

                // Set initial value
                if (connection.parameters[name])
                    field.setValue(connection.parameters[name]);

                // Add field
                container.appendChild(field.getElement());
                fields[name] = field;

            } // end foreach parameter

        }

        // Set initially selected protocol
        protocol_field.value = connection.protocol;
        setFields(connection.protocol);

        protocol_field.onchange = protocol_field.onclick = function() {
            setFields(this.value);
        };

        // Add buttons
        var button_div = GuacUI.createChildElement(form_element, "div", "object-buttons");

        // Add save button
        var save_button = GuacUI.createChildElement(button_div, "button");
        save_button.textContent = "Save";
        save_button.onclick = function(e) {

            e.stopPropagation();

            try {

                // Build connection
                var updated_connection = new GuacamoleService.Connection(
                    protocol_field.value,
                    connection.id
                );

                // Populate parameters
                var protocol = available_protocols[updated_connection.protocol];
                for (var name in protocol.parameters) {
                    var field = fields[name];
                    if (field)
                        updated_connection.parameters[name] = field.getValue();
                }

                // Update connection
                GuacamoleService.Connections.update(updated_connection);
                deselect();
                GuacAdmin.reset();

            }
            catch (e) {
                alert(e.message);
            }

        };

        // Add cancel button
        var cancel_button = GuacUI.createChildElement(button_div, "button");
        cancel_button.textContent = "Cancel";
        cancel_button.onclick = function(e) {
            e.stopPropagation();
            deselect();
        };

        // Add delete button if permission available
        if (connection.id in GuacAdmin.cached_permissions.remove_connection) {
            
            // Create button
            var delete_button = GuacUI.createChildElement(button_div, "button");
            delete_button.textContent = "Delete";
            
            // Remove selected connection when clicked
            delete_button.onclick = function(e) {

                e.stopPropagation();

                GuacamoleService.Connections.remove(GuacAdmin.selected_connection);
                deselect();
                GuacAdmin.reset();

            };

        }

        // Select item
        select();

    };

};

GuacAdmin.reset = function() {

    /*
     * Show admin elements if admin permissions available
     */

    // Query service for permissions, protocols, and connections
    GuacAdmin.cached_permissions = GuacamoleService.Permissions.list();
    GuacAdmin.cached_protocols   = GuacamoleService.Protocols.list();
    GuacAdmin.cached_connections = GuacamoleService.Connections.list();

    // Sort connections by ID
    GuacAdmin.cached_connections.sort(function(a, b) {
        return a.id.localeCompare(b.id);
    });

    // Connection management
    if (GuacAdmin.cached_permissions.create_connection
        || GuacAdmin.hasEntry(GuacAdmin.cached_permissions.update_connection)
        || GuacAdmin.hasEntry(GuacAdmin.cached_permissions.remove_connection)
        || GuacAdmin.hasEntry(GuacAdmin.cached_permissions.administer_connection))
            GuacUI.addClass(document.body, "manage-connections");
        else
            GuacUI.removeClass(document.body, "manage-connections");

    // User management
    if (GuacAdmin.cached_permissions.create_user
        || GuacAdmin.hasEntry(GuacAdmin.cached_permissions.update_user)
        || GuacAdmin.hasEntry(GuacAdmin.cached_permissions.remove_user)
        || GuacAdmin.hasEntry(GuacAdmin.cached_permissions.administer_user))
            GuacUI.addClass(document.body, "manage-users");
        else
            GuacUI.removeClass(document.body, "manage-users");

    // Connection creation 
    if (GuacAdmin.cached_permissions.create_connection) {
        GuacUI.addClass(document.body, "add-connections");

        GuacAdmin.buttons.add_connection.onclick = function() {

            // Try to create connection
            try {
                var connection = new GuacamoleService.Connection(
                    GuacAdmin.cached_protocols[0].name, GuacAdmin.fields.connection_id.value);
                GuacamoleService.Connections.create(connection);
                GuacAdmin.fields.connection_id.value = "";
                GuacAdmin.reset();
            }

            // Alert on failure
            catch (e) {
                alert(e.message);
            }

        };

    }

    // User creation
    if (GuacAdmin.cached_permissions.create_user) {
        GuacUI.addClass(document.body, "add-users");

        GuacAdmin.buttons.add_user.onclick = function() {

            // Attempt to create user
            try {
                GuacamoleService.Users.create(GuacAdmin.fields.username.value);
                GuacAdmin.fields.username.value = "";
                GuacAdmin.reset();
            }

            // Alert on failure
            catch (e) {
                alert(e.message);
            }

        };

    }

    var i;

    /*
     * Add readable users.
     */

    // Get previous page, if any
    var user_previous_page = 0;
    if (GuacAdmin.userPager)
        user_previous_page = GuacAdmin.userPager.current_page;

    // Add new pager
    GuacAdmin.containers.user_list.innerHTML = "";
    GuacAdmin.userPager = new GuacAdmin.Pager(GuacAdmin.containers.user_list);

    // Add users to pager
    var usernames = Object.keys(GuacAdmin.cached_permissions.read_user).sort();
    for (i=0; i<usernames.length; i++)
        GuacAdmin.addUser(usernames[i]);

    // If more than one page, add navigation buttons
    GuacAdmin.containers.user_list_buttons.innerHTML = "";
    if (GuacAdmin.userPager.last_page != 0)
        GuacAdmin.containers.user_list_buttons.appendChild(GuacAdmin.userPager.getElement());

    // Set starting page
    GuacAdmin.userPager.setPage(Math.min(GuacAdmin.userPager.last_page,
            user_previous_page));

    /*
     * Add readable connections.
     */

    // Get previous page, if any
    var connection_previous_page = 0;
    if (GuacAdmin.connectionPager)
        connection_previous_page = GuacAdmin.connectionPager.current_page;

    // Add new pager
    GuacAdmin.containers.connection_list.innerHTML = "";
    GuacAdmin.connectionPager = new GuacAdmin.Pager(GuacAdmin.containers.connection_list);

    // Add connections to pager
    for (i=0; i<GuacAdmin.cached_connections.length; i++)
        GuacAdmin.addConnection(GuacAdmin.cached_connections[i]);

    // If more than one page, add navigation buttons
    GuacAdmin.containers.connection_list_buttons.innerHTML = "";
    if (GuacAdmin.connectionPager.last_page != 0)
        GuacAdmin.containers.connection_list_buttons.appendChild(
            GuacAdmin.connectionPager.getElement());

    // Set starting page
    GuacAdmin.connectionPager.setPage(Math.min(GuacAdmin.connectionPager.last_page,
            connection_previous_page));

};

// Initial load
GuacAdmin.reset();

