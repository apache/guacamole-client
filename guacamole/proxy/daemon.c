
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
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>

#include "client.h"
#include "vnc_client.h"

int main(int argc, char* argv[]) {

    /* Server */
    int socket_fd;
    struct sockaddr_in server_addr;

    /* Client */
    struct sockaddr_in client_addr;
    unsigned int client_addr_len;
    int connected_socket_fd;
    pid_t client_pid ;


    fprintf(stderr, "Guacamole starting...\n");

    /* Get binding address */
    memset(&server_addr, 0, sizeof(server_addr)); /* Zero struct */
    server_addr.sin_family = AF_INET;
    server_addr.sin_addr.s_addr = INADDR_ANY;
    server_addr.sin_port = htons(1234);

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

    /* Daemon loop */
    for (;;) {

        fprintf(stderr, "Listening...\n");

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
        }

        /* In child ... */
        else if (client_pid == 0) {

            guac_client* client = guac_get_client(connected_socket_fd, vnc_guac_client_init); 
            guac_start_client(client);
            guac_free_client(client);

            /* Close socket */
            if (close(connected_socket_fd) < 0) {
                perror("Error closing connection");
                return 3;
            }

            fprintf(stderr, "Child exiting.\n");
            return 0;
        }

        else
            fprintf(stderr, "Child forked.\n");

    }

    /* Close socket */
    if (close(socket_fd) < 0) {
        perror("Error closing socket");
        return 3;
    }

    return 0;

}

