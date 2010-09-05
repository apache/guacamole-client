
#include <stdio.h>

#include "proxy.h"

void proxy(int client_fd) {

    write(client_fd, "name:hello;size:1024,768;error:Test finished.;", 46);

}

