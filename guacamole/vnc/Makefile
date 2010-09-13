
CFLAGS=-O2 -fPIC -pedantic -Wall -Werror -I../libguac/include
LDFLAGS=-L../libguac -lpng -lguac -lvncclient

.PHONY: clean

all: libguac_client_vnc.so

libguac_client_vnc.so: vnc_client.o
	$(CC) $(CFLAGS) -shared $(LDFLAGS) -Wl,-soname,libguac_client_vnc.so.0 -o libguac_client_vnc.so vnc_client.o

vnc_client.o: vnc_client.c
	$(CC) $(CFLAGS) -c vnc_client.c

clean:
	$(RM) *.o *.so

distclean: clean
	$(RM) *~

