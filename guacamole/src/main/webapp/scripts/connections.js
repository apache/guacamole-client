

function Config(protocol, id) {
    this.protocol = protocol;
    this.id = id;
}

function getConfigList(parameters) {

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
    
}
