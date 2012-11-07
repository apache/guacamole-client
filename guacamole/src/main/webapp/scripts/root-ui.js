
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
 * General set of UI elements and UI-related functions regarding user login and
 * connection management.
 */
var GuacamoleRootUI = {

    "sections": {
        "login_form"         : document.getElementById("login-form"),
        "recent_connections" : document.getElementById("recent-connections"),
        "all_connections"  : document.getElementById("all-connections")
    },

    "messages": {
        "login_error"           : document.getElementById("login-error"),
        "no_recent_connections" : document.getElementById("no-recent")
    },

    "fields": {
        "username"  : document.getElementById("username"),
        "password"  : document.getElementById("password"),
        "clipboard" : document.getElementById("clipboard")
    },
    
    "buttons": {
        "login"  : document.getElementById("login"),
        "logout" : document.getElementById("logout")
    },

    "settings": {
        "auto_fit"      : document.getElementById("auto-fit"),
        "disable_sound" : document.getElementById("disable-sound")
    },

    "views": {
        "login"       : document.getElementById("login-ui"),
        "connections" : document.getElementById("connection-list-ui")
    },

    "session_state" :  new GuacamoleSessionState()

};

/**
 * Attempts to login the given user using the given password, throwing an
 * error if the process fails.
 * 
 * @param {String} username The name of the user to login as.
 * @param {String} password The password to use to authenticate the user.
 */
GuacamoleRootUI.login = function(username, password) {

    // Get parameters from query string
    var parameters = window.location.search.substring(1);

    // Get username and password from form
    var data =
           "username=" + encodeURIComponent(username)
        + "&password=" + encodeURIComponent(password)

    // Include query parameters in submission data
    if (parameters) data += "&" + parameters;

    // Log in
    var xhr = new XMLHttpRequest();
    xhr.open("POST", "login", false);
    xhr.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
    xhr.send(data);

    // Handle failures
    if (xhr.status != 200)
        throw new Error("Invalid login");

};

/**
 * An arbitrary Guacamole configuration, consisting of an ID/protocol pair.
 * 
 * @constructor
 * @param {String} protocol The protocol used by this configuration.
 * @param {String} id The ID associated with this configuration.
 */
GuacamoleRootUI.Configuration = function(protocol, id) {

    /**
     * The protocol associated with this configuration.
     */
    this.protocol = protocol;

    /**
     * The ID associated with this configuration.
     */
    this.id = id;

};

GuacamoleRootUI.getConfigurations = function(parameters) {

    // Construct request URL
    var configs_url = "configs";
    if (parameters) configs_url += "?" + parameters;

    // Get config list
    var xhr = new XMLHttpRequest();
    xhr.open("GET", configs_url, false);
    xhr.send(null);

    // If fail, throw error
    if (xhr.status != 200)
        throw new Error(xhr.statusText);

    // Otherwise, get list
    var configs = new Array();

    var configElements = xhr.responseXML.getElementsByTagName("config");
    for (var i=0; i<configElements.length; i++) {
        configs.push(new Config(
            configElements[i].getAttribute("protocol"),
            configElements[i].getAttribute("id")
        ));
    }

    return configs;
 
};

/**
 * A connection UI object which can be easily added to a list of connections
 * for sake of display.
 */
GuacamoleRootUI.Connection = function(config) {

    /**
     * The configuration associated with this connection.
     */
    this.configuration = config;

    function element(tagname, classname) {
        var new_element = document.createElement(tagname);
        new_element.className = classname;
        return new_element;
    }

    // Create connection display elements
    var connection    = element("div",  "connection");
    var caption       = element("div",  "caption");
    var protocol      = element("div",  "protocol");
    var name          = element("span", "name");
    var protocol_icon = element("div",  "icon " + config.protocol);
    var thumbnail     = element("div",  "thumbnail");
    var thumb_img;

    // Get URL
    var url = "client.xhtml?id=" + encodeURIComponent(config.id);

    // Create link to client
    connection.onclick = function() {

        // Attempt to focus existing window
        var current = window.open(null, config.id);

        // If window did not already exist, set up as
        // Guacamole client
        if (!current.GuacamoleUI)
            window.open(url, config.id);

    };

    // Add icon
    protocol.appendChild(protocol_icon);

    // Set name
    name.textContent = config.id;

    // Assemble caption
    caption.appendChild(protocol);
    caption.appendChild(name);

    // Assemble connection icon
    connection.appendChild(thumbnail);
    connection.appendChild(caption);

    // Add screenshot if available
    var thumbnail_url = GuacamoleHistory.get(config.id).thumbnail;
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
        return connection;
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
 * Set of all thumbnailed connections, indexed by ID.
 */
GuacamoleRootUI.thumbnailConnections = {};

/**
 * Set of all configurations, indexed by ID.
 */
GuacamoleRootUI.configurations = {};

/**
 * Adds the given connection to the recent connections list.
 */
GuacamoleRootUI.addRecentConnection = function(connection) {

    // Add connection object to list of thumbnailed connections
    GuacamoleRootUI.thumbnailConnections[connection.configuration.id] =
        connection;
    
    // Add connection to recent list
    GuacamoleRootUI.sections.recent_connections.appendChild(
        connection.getElement());

    // Hide "No recent connections" message
    GuacamoleRootUI.messages.no_recent_connections.style.display = "none";

};


/**
 * Resets the interface such that the login UI is displayed if
 * the user is not authenticated (or authentication fails) and
 * the connection list UI (or the client for the only available
 * connection, if there is only one) is displayed if the user is
 * authenticated.
 */
GuacamoleRootUI.reset = function() {

    // Get parameters from query string
    var parameters = window.location.search.substring(1);

    // Read configs
    var configs;
    try {
        configs = GuacamoleRootUI.getConfigurations(parameters);
    }
    catch (e) {

        // Show login UI if unable to get configs
        GuacamoleRootUI.views.login.style.display = "";
        GuacamoleRootUI.views.connections.style.display = "none";

        return;

    }

    // Add connection icons
    for (var i=0; i<configs.length; i++) {

        // Add configuration to set
        GuacamoleRootUI.configurations[configs[i].id] = configs[i];

        // Get connection element
        var connection = new GuacamoleRootUI.Connection(configs[i]);

        // If screenshot present, add to recent connections
        if (connection.hasThumbnail())
            GuacamoleRootUI.addRecentConnection(connection);

        // Add connection to connection list
        GuacamoleRootUI.sections.all_connections.appendChild(
            new GuacamoleRootUI.Connection(configs[i]).getElement());

    }

    // If configs could be retrieved, display list
    GuacamoleRootUI.views.login.style.display = "none";
    GuacamoleRootUI.views.connections.style.display = "";

    // Reload history every 5 seconds
    window.setInterval(GuacamoleHistory.reload, 5000);

    // Reload history when focus gained
    window.onfocus = GuacamoleHistory.reload;

};

GuacamoleHistory.onchange = function(id, old_entry, new_entry) {

    // Get existing connection, if any
    var connection = GuacamoleRootUI.thumbnailConnections[id];

    // If we are adding or updating a connection
    if (new_entry) {

        // Ensure connection is added
        if (!connection) {

            // Create new connection
            connection = new GuacamoleRootUI.Connection(
                GuacamoleRootUI.configurations[id]
            );

            GuacamoleRootUI.addRecentConnection(connection);

        }

        // Set new thumbnail 
        connection.setThumbnail(new_entry.thumbnail);

    }

    // Otherwise, delete existing connection
    else {

        GuacamoleRootUI.sections.recent_connections.removeChild(
            connection.getElement());

        delete GuacamoleRootUI.thumbnailConnections[id];

        // Display "No recent connections" message if none left
        if (GuacamoleRootUI.thumbnailConnections.length == 0)
            GuacamoleRootUI.messages.no_recent_connections.style.display = "";

    }
    
};

/*
 * Update session state when auto-fit checkbox is changed
 */

GuacamoleRootUI.settings.auto_fit.onchange =
GuacamoleRootUI.settings.auto_fit.onclick  = function() {

    GuacamoleRootUI.session_state.setProperty(
        "auto-fit", GuacamoleRootUI.settings.auto_fit.checked);

};

/*
 * Update session state when disable-sound checkbox is changed
 */

GuacamoleRootUI.settings.disable_sound.onchange =
GuacamoleRootUI.settings.disable_sound.onclick  = function() {

    GuacamoleRootUI.session_state.setProperty(
        "disable-sound", GuacamoleRootUI.settings.disable_sound.checked);

};

/*
 * Update clipboard contents when changed
 */

GuacamoleRootUI.fields.clipboard.onchange = function() {

    GuacamoleRootUI.session_state.setProperty(
        "clipboard", GuacamoleRootUI.fields.clipboard.value);

};

/*
 * Update element states when session state changes
 */

GuacamoleRootUI.session_state.onchange =
function(old_state, new_state, name) {

    // Clipboard
    if (name == "clipboard")
        GuacamoleRootUI.fields.clipboard.value = new_state[name];

    // Auto-fit display
    else if (name == "auto-fit")
        GuacamoleRootUI.fields.auto_fit.checked = new_state[name];

    // Disable Sound
    else if (name == "disable-sound")
        GuacamoleRootUI.fields.disable_sound.checked = new_state[name];

};

/*
 * Initialize clipboard with current data
 */

if (GuacamoleRootUI.session_state.getProperty("clipboard"))
    GuacamoleRootUI.fields.clipboard.value =
        GuacamoleRootUI.session_state.getProperty("clipboard");

/*
 * Default to true if auto-fit not specified
 */

if (GuacamoleRootUI.session_state.getProperty("auto-fit") === undefined)
    GuacamoleRootUI.session_state.setProperty("auto-fit", true);

/*
 * Initialize auto-fit setting in UI
 */

GuacamoleRootUI.settings.auto_fit.checked =
    GuacamoleRootUI.session_state.getProperty("auto-fit");

/*
 * Initialize disable-sound setting in UI
 */
GuacamoleRootUI.settings.disable_sound.checked =
    GuacamoleRootUI.session_state.getProperty("disable-sound");

/*
 * Set handler for logout
 */

GuacamoleRootUI.buttons.logout.onclick = function() {
    window.location.href = "logout";
};

/*
 * Set handler for login
 */

GuacamoleRootUI.sections.login_form.onsubmit = function() {

    try {

        // Attempt login
        GuacamoleRootUI.login(
            GuacamoleRootUI.fields.username.value,
            GuacamoleRootUI.fields.password.value
        );

        // Ensure username/password fields are blurred after login attempt
        GuacamoleRootUI.fields.username.blur();
        GuacamoleRootUI.fields.password.blur();

        // Reset UI
        GuacamoleRootUI.reset();

    }
    catch (e) {

        // Display error, reset and refocus password field
        GuacamoleRootUI.messages.login_error.textContent = e.message;

        // Reset and recofus password field
        GuacamoleRootUI.fields.password.value = "";
        GuacamoleRootUI.fields.password.focus();

    }

    // Always cancel submit
    return false;

};

/*
 * Turn off autocorrect and autocapitalization on usename 
 */

GuacamoleRootUI.fields.username.setAttribute("autocorrect", "off");
GuacamoleRootUI.fields.username.setAttribute("autocapitalize", "off");

/*
 * Initialize UI
 */

GuacamoleRootUI.reset();
