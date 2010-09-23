
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

    /* Limit */
    unsigned int transfer_limit; /* KB/sec */

} GUACIO;

GUACIO* guac_open(int fd);
ssize_t guac_write_int(GUACIO* io, unsigned int i);
ssize_t guac_write_string(GUACIO* io, const char* str);
ssize_t guac_write_base64(GUACIO* io, const void* buf, size_t count);
ssize_t guac_flush_base64(GUACIO* io);
ssize_t guac_flush(GUACIO* io);
int guac_select(GUACIO* io, int usec_timeout);
void guac_close(GUACIO* io);
void guac_close_final(GUACIO* io);

#endif

