
#ifndef _GUACIO_H
#define _GUACIO_H

#include <unistd.h>

ssize_t write_base64(int fd, const void* buf, size_t count);
ssize_t flush_base64(int fd);

#endif

