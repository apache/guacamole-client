
#include <unistd.h>
#include <stdio.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>

#include "client.h"

int main(int argc, char* argv[]) {

    /* Server */
    int socket_fd;
    struct sockaddr_in server_addr;

    /* Client */
    struct sockaddr_in client_addr;
    unsigned int client_addr_len;
    int connected_socket_fd;
    pid_t client_pid ;

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
            client(connected_socket_fd);

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

