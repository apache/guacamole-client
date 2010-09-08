
CFLAGS=-O2 -pedantic -Wall -Werror
LDFLAGS=-lpng -lvncclient

.PHONY: clean

all: guacd

guacd: daemon.o client.o guacio.o protocol.o vnc_client.o
	$(CC) $(CFLAGS) $(LDFLAGS) daemon.o client.o protocol.o guacio.o vnc_client.o -o guacd 

client.o: client.c client.h guacio.h
	$(CC) $(CFLAGS) -c client.c

vnc_client.o: vnc_client.c vnc_client.h guacio.h 
	$(CC) $(CFLAGS) -c vnc_client.c

protocol.o: protocol.c protocol.h guacio.h
	$(CC) $(CFLAGS) -c protocol.c

guacio.o: guacio.c guacio.h
	$(CC) $(CFLAGS) -c guacio.c

daemon.o: daemon.c client.h vnc_client.h
	$(CC) $(CFLAGS) -c daemon.c

clean:
	$(RM) *.o guacd

distclean: clean
	$(RM) *~

