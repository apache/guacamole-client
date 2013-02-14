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
        "protocol"      :  document.getElementById("protocol"),
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
 * @param {String} title A human-readable title for the field.
 * @param {String[]} available The allowed value(s), if any.
 * @param {String[]} selected The selected value(s), if any.
 */
GuacAdmin.Field = function(title, available, selected) {

    /**
     * A human-readable title describing this field.
     */
    this.title = title;

    /**
     * All available values, if any.
     */
    this.available = available || [];

    /**
     * All selected values, if any.
     */
    this.selected = selected || [];

    /**
     * Returns the DOM Element representing this field.
     * 
     * @return {Element} The DOM Element representing this field.
     */
    this.getElement = function() {};

    /**
     * Returns the selected values of this field.
     * 
     * @return {String[]} All selected values.
     */
    this.getSelected = function() {};

};


/**
 * Simple HTML input field.
 * 
 * @augments GuacAdmin.Field
 */
GuacAdmin.Field._HTML_INPUT = function(type, title, available, selected) {

    // Call parent constructor
    GuacAdmin.Field.apply(this, [title, available, selected]);

    // Create backing element
    var element = GuacUI.createElement("input");
    element.setAttribute("type", type);
    if (selected && selected.length == 1)
        element.setAttribute("value", selected[0]);

    this.getSelected = function() {
        return [element.value];
    };

    this.getElement = function() {
        return element;
    };

};

GuacAdmin.Field._HTML_INPUT.prototype = new GuacAdmin.Field();


/**
 * A basic text field.
 * 
 * @augments GuacAdmin.Field._HTML_INPUT
 */
GuacAdmin.Field.TEXT = function(title, available, selected) {
    GuacAdmin.Field._HTML_INPUT.apply(this, ["text", title, available, selected]);
};

GuacAdmin.Field.TEXT.prototype = new GuacAdmin.Field._HTML_INPUT();


/**
 * A basic password field.
 * 
 * @augments GuacAdmin.Field._HTML_INPUT
 */
GuacAdmin.Field.PASSWORD = function(title, available, selected) {
    GuacAdmin.Field._HTML_INPUT.apply(this, ["password", title, available, selected]);
};

GuacAdmin.Field.PASSWORD.prototype = new GuacAdmin.Field._HTML_INPUT();


/**
 * Multi-select list where each element has a corresponding checkbox.
 * 
 * @augments GuacAdmin.Field
 */
GuacAdmin.Field.LIST = function(title, available, selected) {

    // Call parent constructor
    GuacAdmin.Field.apply(this, [title, available, selected]);

    var i;

    // All selected connections 
    var is_selected = {};
    for (i=0; i<selected.length; i++)
        is_selected[selected[i]] = true;

    // Add elements for all list items
    var element = GuacUI.createElement("div", "list");
    for (i=0; i<available.length; i++) {

        (function() {

            // Get name 
            var name = available[i];

            // Containing div
            var list_item = GuacUI.createChildElement(element, "div", "connection");

            // Checkbox
            var checkbox = GuacUI.createChildElement(list_item, "input");
            checkbox.setAttribute("type", "checkbox");
            if (is_selected[name])
                checkbox.checked = true;

            // Update selected set when changed
            checkbox.onclick =
            checkbox.onchange = function() {

                if (checkbox.checked)
                    is_selected[name] = true;
                else if (is_selected[name])
                    delete is_selected[name];

            };

            // Connection name
            var name_element = GuacUI.createChildElement(list_item, "span", "name");
            name_element.textContent = name;

        })();

    }

    this.getElement = function() {
        return element;
    };

    this.getSelected = function() {
        return Object.keys(is_selected);
    };

};

GuacAdmin.Field.LIST.prototype = new GuacAdmin.Field();


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
 * An arbitrary table-based form. Given an array of fields and an array
 * of buttons, GuacAdmin.Form constructs a clean HTML form using DOM Elements.
 * 
 * @constructor
 * @param {GuacAdmin.Field[]} fields An array of all fields to include in the
 *                                   form.
 * @param {GuacAdmin.Button[]} buttons An array of all buttons to include in the
 *                                     form.
 */
GuacAdmin.Form = function(fields, buttons) {

    /**
     * Reference to this form.
     */
    var guac_form = this;

    // Main div and fields
    var element     = GuacUI.createElement("div", "form");
    var field_table = GuacUI.createChildElement(element, "table", "fields");

    // Buttons
    var button_div = GuacUI.createChildElement(element, "div", "object-buttons");
   
    /**
     * Returns the DOM element representing this form.
     */
    this.getElement = function() {
        return element;
    };

    /**
     * Event called when a button is clicked.
     * 
     * @event
     * @param {String} title The title of the button clicked.
     */
    this.onaction = null;

    /*
     * Add all fields
     */

    var i;
    for (i=0; i<fields.length; i++) {

        // Get field
        var field = fields[i];

        // Add elements
        var row    = GuacUI.createChildElement(field_table, "tr");
        var header = GuacUI.createChildElement(row, "th");
        var cell   = GuacUI.createChildElement(row, "td");

        // Set title
        header.textContent = field.title;

        // Add to cell
        cell.appendChild(field.getElement());

    }
    
    /*
     * Add all buttons
     */

    for (i=0; i<buttons.length; i++) {

        (function() {

            // Get title and element
            var title = buttons[i].title;
            var button_element = buttons[i].getElement();

            // Trigger onaction event when clicked
            button_element.addEventListener("click", function(e) {

                if (guac_form.onaction) {

                    // Build array of field values
                    var field_values = [];
                    for (var j=0; j<fields.length; j++)
                        field_values.push(fields[j].getSelected());

                    guac_form.onaction(title, field_values);
                    e.stopPropagation();
                    
                }

            });

            // Add to cell
            button_div.appendChild(button_element);

        })();

    }

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

        /* STUB */

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
                    GuacAdmin.fields.protocol.value, GuacAdmin.fields.connection_id.value);
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

