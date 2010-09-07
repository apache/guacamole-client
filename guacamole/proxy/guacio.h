
#ifndef _GUACIO_H
#define _GUACIO_H

#include <unistd.h>

typedef struct GUACIO {

    int fd; 
    
    int ready;
    int ready_buf[3];

    int written;
    char out_buf[8192];

    int instructionbuf_size;
    int instructionbuf_used_length;
    char* instructionbuf;

} GUACIO;

GUACIO* guac_open(int fd);
ssize_t guac_write_int(GUACIO* io, unsigned int i);
ssize_t guac_write_string(GUACIO* io, const char* str);
ssize_t guac_write_base64(GUACIO* io, const void* buf, size_t count);
ssize_t guac_flush_base64(GUACIO* io);
ssize_t guac_flush(GUACIO* io);
int guac_select(GUACIO* io, int usec_timeout);
void guac_close(GUACIO* io);

#endif

