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

    "lists" : {
        "connection_list" :  document.getElementById("connection-list"),
        "user_list"       :  document.getElementById("user-list")
    },

    "buttons" : {
        "back"           :  document.getElementById("back"),
        "logout"         :  document.getElementById("logout"),
        "add_connection" :  document.getElementById("add-connection"),
        "add_user"       :  document.getElementById("add-user")
    },

    "fields" : {
        "connection_id" :  document.getElementById("connection-id"),
        "username"      :  document.getElementById("username")
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

GuacAdmin.Field._HTML_INPUT.prototype = new GuacAdmin.Field();


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
 * Adds the user with the given name to the displayed user list.
 */
GuacAdmin.addUser = function(name) {

    // Create user list item
    var item = new GuacAdmin.ListItem("user", name);
    var item_element = item.getElement();
    GuacAdmin.lists.user_list.appendChild(item_element);

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
            GuacUI.removeClass(GuacAdmin.lists.user_list, "disabled");
            GuacUI.removeClass(item_element, "selected");
            item_element.removeChild(form_element);
            GuacAdmin.selected_user = null;
        }

        // Select
        function select() {
            GuacUI.addClass(GuacAdmin.lists.user_list, "disabled");
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
 * Adds the given connection to the displayed connection list.
 */
GuacAdmin.addConnection = function(connection) {

    var item = new GuacAdmin.ListItem("connection", connection.id);
    var item_element = item.getElement();
    GuacAdmin.lists.connection_list.appendChild(item_element);

    item_element.onclick = function() {

        // Ignore clicks if any item is selected
        if (GuacAdmin.selected_connection) return;
        else GuacAdmin.selected_connection = connection.id;

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
        for (var i=0; i<GuacAdmin.cached_protocols.length; i++) {

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

        // Deselect
        function deselect() {
            GuacUI.removeClass(GuacAdmin.lists.connection_list, "disabled");
            GuacUI.removeClass(item_element, "selected");
            item_element.removeChild(form_element);
            GuacAdmin.selected_connection = null;
        }

        // Select
        function select() {
            GuacUI.addClass(GuacAdmin.lists.connection_list, "disabled");
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

    /*
     * Add readable users.
     */

    GuacAdmin.lists.user_list.innerHTML = "";
    for (var name in GuacAdmin.cached_permissions.read_user)
        GuacAdmin.addUser(name)

    /*
     * Add readable connections.
     */

    GuacAdmin.lists.connection_list.innerHTML = "";
    for (var i=0; i<GuacAdmin.cached_connections.length; i++)
        GuacAdmin.addConnection(GuacAdmin.cached_connections[i]);

};

// Initial load
GuacAdmin.reset();

