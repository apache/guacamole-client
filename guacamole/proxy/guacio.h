
#ifndef _GUACIO_H
#define _GUACIO_H

#include <unistd.h>

typedef struct GUACIO {

    int fd;
    
    int ready;
    int ready_buf[3];

    int written;
    char out_buf[8192];

} GUACIO;

GUACIO* guac_open(int fd);
ssize_t write_base64(GUACIO* io, const void* buf, size_t count);
ssize_t flush_base64(GUACIO* io);
void guac_close(GUACIO* io);

#endif

