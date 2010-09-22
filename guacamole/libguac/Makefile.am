AUTOMAKE_OPTIONS = gnu

AM_CFLAGS = -Werror -Wall -Iinclude

libguacincdir = $(includedir)/guacamole
libguacinc_HEADERS = include/client.h include/guacio.h include/protocol.h include/uuidtree.h

lib_LTLIBRARIES = libguac.la

libguac_la_SOURCES = src/client.c src/clientreg.c src/guacio.c src/protocol.c src/uuidtree.c

libguac_la_LDFLAGS = -version-info 0:0:0

