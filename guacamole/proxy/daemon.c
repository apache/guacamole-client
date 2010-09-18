
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

#include "client.h"


typedef struct client_thread_data {

    int fd;
    guac_client_init_handler* client_init;
    guac_client_registry_node* registry;

    int argc;
    char** argv;

} client_thread_data;


void* start_client_thread(void* data) {

    guac_client* client;
    client_thread_data* thread_data = (client_thread_data*) data;

    fprintf(stderr, "[guacamole] spawning client\n");

    /* Load and start client */
    client = guac_get_client(thread_data->fd, thread_data->registry, thread_data->client_init, thread_data->argc, thread_data->argv); 
    guac_start_client(client);

    /* FIXME: Need to free client, but only if the client is not
     * being used. This line will be reached if handoff occurs
     */
    guac_free_client(client, thread_data->registry);

    /* Close socket */
    if (close(thread_data->fd) < 0) {
        perror("Error closing connection");
        free(data);
        return NULL;
    }

    fprintf(stderr, "[guacamole] client finished\n");
    free(data);
    return NULL;

}

int main(int argc, char* argv[]) {

    /* Client registry */
    guac_client_registry_node* registry;

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
    pid_t client_pid ;

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
        perror("Error opening socket");
        return 1;
    }

    /* Bind socket to address */
    if (bind(socket_fd, (struct sockaddr*) &server_addr,
                sizeof(server_addr)) < 0) {
        perror("Error binding socket");
        return 2;
    } 

    fprintf(stderr, "[guacamole] loading pluggable client\n");

    /* Load client plugin */
    client_plugin_handle = dlopen(protocol_lib, RTLD_LAZY);
    if (!client_plugin_handle) {
        fprintf(stderr, "[guacamole] could not open client plugin: %s\n", dlerror());
        return 2;
    }

    dlerror(); /* Clear errors */

    /* Get init function */
    alias.obj = dlsym(client_plugin_handle, "guac_client_init");

    if ((error = dlerror()) != NULL) {
        fprintf(stderr, "[guacamole] could not get guac_client_init in plugin: %s\n", error);
        return 2;
    }



    fprintf(stderr, "[guacamole] listening on port %i\n", listen_port);

    /* Allocate registry */
    registry = guac_create_client_registry();

    /* Daemon loop */
    for (;;) {

        client_thread_data* data;

        /* Listen for connections */
        if (listen(socket_fd, 5) < 0) {
            perror("Error listening on socket");
            return 3;
        }

        /* Accept connection */
        client_addr_len = sizeof(client_addr);
        connected_socket_fd = accept(socket_fd, (struct sockaddr*) &client_addr, &client_addr_len);
        if (connected_socket_fd < 0) {
            perror("Error accepting client");
            return 3;
        }

        /* Fork client */
        client_pid = fork();
        if (client_pid < 0) {
            perror("Could not fork child");
            return 4;
        }

        /* In child ... */
        else if (client_pid == 0) {

            data = malloc(sizeof(client_thread_data));

            data->fd = connected_socket_fd;
            data->client_init = alias.client_init;
            data->registry = registry;
            data->argc = client_argc;
            data->argv = client_argv;

            start_client_thread(data);

        }

    }

    /* FIXME: Cleanup client registry (and all other objects) on exit */

    /* Close socket */
    if (close(socket_fd) < 0) {
        perror("Error closing socket");
        return 3;
    }

    /* Load client plugin */
    if (dlclose(client_plugin_handle)) {
        fprintf(stderr, "[guacamole] could not close client plugin: %s\n", dlerror());
        return 2;
    }


    return 0;

}

