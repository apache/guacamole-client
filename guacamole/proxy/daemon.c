
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

int main(int argc, char* argv[]) {

    /* Client registry */
    guac_client_registry_node* registry;

    /* Pluggable client */
    guac_client* client;
    void* client_plugin_handle;

    union {
        void (*client_init)(guac_client* client, const char* hostname, int port);
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
    const char* connect_host;
    int connect_port;

    if (argc < 4) {
        fprintf(stderr, "USAGE: %s LISTENPORT CONNECTHOST CONNECTPORT\n", argv[0]);
        return 1;
    }

    listen_port = atoi(argv[1]);
    connect_host = argv[2];
    connect_port = atoi(argv[3]);


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
    client_plugin_handle = dlopen("libguac_client_vnc.so", RTLD_LAZY);
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



    fprintf(stderr, "[guacamole] listening on port %i, forwarding to %s:%i\n", listen_port, connect_host, connect_port);

    /* Allocate registry */
    registry = guac_create_client_registry();

    /* Daemon loop */
    for (;;) {

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

            fprintf(stderr, "[guacamole] spawning client\n");

            /* Load and start client */
            client = guac_get_client(connected_socket_fd, registry, alias.client_init, connect_host, connect_port); 
            guac_start_client(client);

            /* FIXME: Need to free client, but only if the client is not
             * being used. This line will be reached if handoff occurs
             */
            guac_free_client(client, registry);

            /* Close socket */
            if (close(connected_socket_fd) < 0) {
                perror("Error closing connection");
                return 3;
            }

            fprintf(stderr, "[guacamole] client finished\n");
            return 0;
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

