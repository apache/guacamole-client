
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

    int argc;
    char** argv;

} client_thread_data;


void* start_client_thread(void* data) {

    guac_client* client;
    client_thread_data* thread_data = (client_thread_data*) data;

    syslog(LOG_INFO, "Spawning client");

    /* Load and start client */
    client = guac_get_client(thread_data->fd, thread_data->client_init, thread_data->argc, thread_data->argv); 

    if (client == NULL) {
        syslog(LOG_ERR, "Client retrieval failed");
        return NULL;
    }

    guac_start_client(client);

    guac_free_client(client);

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

    int listen_port = -1;
    char* protocol = NULL;

    int client_argc;
    char** client_argv;

    char protocol_lib[256] = "libguac_client_";

    int opt;

    /* Parse arguments */
    while ((opt = getopt(argc, argv, "l:p:")) != -1) {
        if (opt == 'l') {
            listen_port = atoi(optarg);
            if (listen_port <= 0) {
                fprintf(stderr, "Invalid port: %s\n", optarg);
                exit(EXIT_FAILURE);
            }
        }
        else if (opt == 'p') {
            protocol = optarg;
            break;
        }
        else {
            fprintf(stderr, "USAGE: %s [-l LISTENPORT] [-p PROTOCOL [PROTOCOL OPTIONS ...]]\n", argv[0]);
            exit(EXIT_FAILURE);
        }
    }

    /* Validate arguments */

    if (listen_port < 0) {
        fprintf(stderr, "The port to listen on must be specified.\n");
        exit(EXIT_FAILURE);
    }

    if (protocol == NULL) {
        fprintf(stderr, "The protocol must be specified.\n");
        exit(EXIT_FAILURE);
    }

    strcat(protocol_lib, protocol);
    strcat(protocol_lib, ".so");

    client_argc = argc - optind;
    client_argv = &(argv[optind]);

    /* Get binding address */
    memset(&server_addr, 0, sizeof(server_addr)); /* Zero struct */
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = INADDR_ANY;
    server_addr.sin_port = htons(listen_port);

    /* Get socket */
    socket_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (socket_fd < 0) {
        fprintf(stderr, "Error opening socket: %s\n", strerror(errno));
        exit(EXIT_FAILURE);
    }

    /* Bind socket to address */
    if (bind(socket_fd, (struct sockaddr*) &server_addr,
                sizeof(server_addr)) < 0) {
        fprintf(stderr, "Error binding socket: %s\n", strerror(errno));
        exit(EXIT_FAILURE);
    } 

    /* Load client plugin */
    client_plugin_handle = dlopen(protocol_lib, RTLD_LAZY);
    if (!client_plugin_handle) {
        fprintf(stderr, "Could not open client plugin for protocol \"%s\": %s\n", protocol, dlerror());
        exit(EXIT_FAILURE);
    }

    dlerror(); /* Clear errors */

    /* Get init function */
    alias.obj = dlsym(client_plugin_handle, "guac_client_init");

    if ((error = dlerror()) != NULL) {
        fprintf(stderr, "Could not get guac_client_init in  plugin: %s\n", error);
        exit(EXIT_FAILURE);
    }


    syslog(LOG_INFO, "Started, listening on port %i", listen_port);


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
        data->argc = client_argc;
        data->argv = client_argv;

        if (pthread_create(&thread, NULL, start_client_thread, (void*) data)) {
            syslog(LOG_ERR, "Could not create client thread: %s", strerror(errno));
            return 3;
        }

    }

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

