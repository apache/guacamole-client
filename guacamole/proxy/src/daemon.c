
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

#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <dlfcn.h>
#include <pthread.h>

#include <errno.h>
#include <syslog.h>

#include <guacamole/client.h>

typedef struct client_thread_data {

    int fd;
    guac_client_init_handler* client_init;
    guac_client_registry* registry;

    int argc;
    char** argv;

} client_thread_data;


void* start_client_thread(void* data) {

    guac_client* client;
    client_thread_data* thread_data = (client_thread_data*) data;

    syslog(LOG_INFO, "Spawning client");

    /* Load and start client */
    client = guac_get_client(thread_data->fd, thread_data->registry, thread_data->client_init, thread_data->argc, thread_data->argv); 

    if (client == NULL) {
        syslog(LOG_ERR, "Client retrieval failed");
        return NULL;
    }

    guac_start_client(client);

    /* FIXME: Need to free client, but only if the client is not
     * being used. This line will be reached if handoff occurs
     */
    guac_free_client(client, thread_data->registry);

    /* Close socket */
    if (close(thread_data->fd) < 0) {
        syslog(LOG_ERR, "Error closing connection: %s", strerror(errno));
        free(data);
        return NULL;
    }

    syslog(LOG_INFO, "Client finished");
    free(data);
    return NULL;

}

int main(int argc, char* argv[]) {

    /* Client registry */
    guac_client_registry* registry;

    /* Pluggable client */
    void* client_plugin_handle;

    union {
        guac_client_init_handler* client_init;
        void* obj;
    } alias;

    char* error;

    /* Server */
    int socket_fd;
    struct sockaddr_in server_addr;

    /* Client */
    struct sockaddr_in client_addr;
    unsigned int client_addr_len;
    int connected_socket_fd;

    int listen_port;

    int client_argc;
    char** client_argv;

    char protocol_lib[256] = "libguac_client_";

    if (argc < 3) {
        fprintf(stderr, "USAGE: %s LISTENPORT PROTOCOL [PROTOCOL OPTIONS]\n", argv[0]);
        return 1;
    }

    listen_port = atoi(argv[1]);
    strcat(protocol_lib, argv[2]);
    strcat(protocol_lib, ".so");

    client_argc = argc - 3;
    client_argv = &(argv[3]);

    /* Get binding address */
    memset(&server_addr, 0, sizeof(server_addr)); /* Zero struct */
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = INADDR_ANY;
    server_addr.sin_port = htons(listen_port);

    /* Get socket */
    socket_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (socket_fd < 0) {
        syslog(LOG_ERR, "Error opening socket: %s", strerror(errno));
        return 1;
    }

    /* Bind socket to address */
    if (bind(socket_fd, (struct sockaddr*) &server_addr,
                sizeof(server_addr)) < 0) {
        syslog(LOG_ERR, "Error binding socket: %s", strerror(errno));
        return 2;
    } 

    syslog(LOG_INFO, "Loading client plugin");

    /* Load client plugin */
    client_plugin_handle = dlopen(protocol_lib, RTLD_LAZY);
    if (!client_plugin_handle) {
        syslog(LOG_ERR, "Could not open client plugin: %s", dlerror());
        return 2;
    }

    dlerror(); /* Clear errors */

    /* Get init function */
    alias.obj = dlsym(client_plugin_handle, "guac_client_init");

    if ((error = dlerror()) != NULL) {
        syslog(LOG_ERR, "Could not get guac_client_init in  plugin: %s", error);
        return 2;
    }


    syslog(LOG_INFO, "Listening on port %i", listen_port);


    /* Allocate registry */
    registry = guac_create_client_registry();

    /* Daemon loop */
    for (;;) {

        pthread_t thread;
        client_thread_data* data;

        /* Listen for connections */
        if (listen(socket_fd, 5) < 0) {
            syslog(LOG_ERR, "Could not listen on socket: %s", strerror(errno));
            return 3;
        }

        /* Accept connection */
        client_addr_len = sizeof(client_addr);
        connected_socket_fd = accept(socket_fd, (struct sockaddr*) &client_addr, &client_addr_len);
        if (connected_socket_fd < 0) {
            syslog(LOG_ERR, "Could not accept client connection: %s", strerror(errno));
            return 3;
        }

        data = malloc(sizeof(client_thread_data));

        data->fd = connected_socket_fd;
        data->client_init = alias.client_init;
        data->registry = registry;
        data->argc = client_argc;
        data->argv = client_argv;

        if (pthread_create(&thread, NULL, start_client_thread, (void*) data)) {
            syslog(LOG_ERR, "Could not create client thread: %s", strerror(errno));
            return 3;
        }

    }

    /* FIXME: Cleanup client registry (and all other objects) on exit */

    /* Close socket */
    if (close(socket_fd) < 0) {
        syslog(LOG_ERR, "Could not close socket: %s", strerror(errno));
        return 3;
    }

    /* Load client plugin */
    if (dlclose(client_plugin_handle)) {
        syslog(LOG_ERR, "Could not close client plugin: %s", dlerror());
        return 2;
    }


    return 0;

}

