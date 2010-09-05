
#include <unistd.h>
#include <stdio.h>

#include "guacio.h"

char characters[64] = {
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
    'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
    'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
    'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '+', '/', 
};

int ready_buf[3];
int ready = 0;

int written = 0;
char out_buf[8192];

ssize_t __write_base64_triplet(int fd, int a, int b, int c) {

    int retval;

    /* Byte 1 */
    out_buf[written++] = characters[(a & 0xFC) >> 2]; /* [AAAAAA]AABBBB BBBBCC CCCCCC */

    if (b >= 0) {
        out_buf[written++] = characters[((a & 0x03) << 4) | ((b & 0xF0) >> 4)]; /* AAAAAA[AABBBB]BBBBCC CCCCCC */

        if (c >= 0) {
            out_buf[written++] = characters[((b & 0x0F) << 2) | ((c & 0xC0) >> 6)]; /* AAAAAA AABBBB[BBBBCC]CCCCCC */
            out_buf[written++] = characters[c & 0x3F]; /* AAAAAA AABBBB BBBBCC[CCCCCC] */
        }
        else { 
            out_buf[written++] = characters[((b & 0x0F) << 2)]; /* AAAAAA AABBBB[BBBB--]------ */
            out_buf[written++] = '='; /* AAAAAA AABBBB BBBB--[------] */
        }
    }
    else {
        out_buf[written++] = characters[((a & 0x03) << 4)]; /* AAAAAA[AA----]------ ------ */
        out_buf[written++] = '='; /* AAAAAA AA----[------]------ */
        out_buf[written++] = '='; /* AAAAAA AA---- ------[------] */
    }

    /* At this point, 4 bytes have been written */

    /* Flush when necessary, return on error */
    if (written > 8188 /* sizeof(out_buf) - 4 */) {
        retval = write(fd, out_buf, written);
        if (retval < 0)
            return retval;

        written = 0;
    }

    if (b < 0)
        return 1;

    if (c < 0)
        return 2;

    return 3;

}

ssize_t __write_base64_byte(int fd, char buf) {

    int retval;

    ready_buf[ready++] = buf & 0xFF;

    /* Flush triplet */
    if (ready == 3) {
        retval = __write_base64_triplet(fd, ready_buf[0], ready_buf[1], ready_buf[2]);
        if (retval < 0)
            return retval;

        ready = 0;
    }

    return 1;
}

ssize_t write_base64(int fd, const void* buf, size_t count) {

    int retval;

    const char* char_buf = (const char*) buf;
    const char* end = char_buf + count;

    while (char_buf < end) {

        retval = __write_base64_byte(fd, *(char_buf++));
        if (retval < 0)
            return retval;

    }

    return count;

}

ssize_t flush_base64(int fd) {

    int retval;

    /* Flush triplet to output buffer */
    while (ready > 0) {
        retval = __write_base64_byte(fd, -1);
        if (retval < 0)
            return retval;
    }

    /* Flush remaining bytes in buffer */
    if (written > 0) {
        retval = write(fd, out_buf, written);
        if (retval < 0)
            return retval;

        written = 0;
    }

    return 0;

}

