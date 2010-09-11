
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

#ifndef __PROTOCOL_H
#define __PROTOCOL_H

#include <png.h>

#include "guacio.h"

typedef struct guac_instruction {

    char* opcode;

    int argc;
    char** argv;

} guac_instruction;


void guac_free_instruction(guac_instruction* instruction);
char* guac_escape_string(const char* str);
char* guac_unescape_string_inplace(char* str);
void guac_send_name(GUACIO* io, const char* name);
void guac_send_error(GUACIO* io, const char* error);
void guac_send_clipboard(GUACIO* io, const char* data);
void guac_send_size(GUACIO* io, int w, int h);
void guac_send_copy(GUACIO* io, int srcx, int srcy, int w, int h, int dstx, int dsty);
void guac_send_png(GUACIO* io, int x, int y, png_byte** png_rows, int w, int h);
void guac_send_cursor(GUACIO* io, int x, int y, png_byte** png_rows, int w, int h);

int guac_instructions_waiting(GUACIO* io);
int guac_read_instruction(GUACIO* io, guac_instruction* parsed_instruction);

#endif

