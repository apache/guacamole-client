

function Config(protocol, id) {
    this.protocol = protocol;
    this.id = id;
}

function getConfigList() {

    // Get config list
    var xhr = new XMLHttpRequest();
    xhr.open("GET", "configs", false);
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
