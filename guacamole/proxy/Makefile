
CFLAGS=-O2 -ansi -pedantic -Wall -Werror
LDFLAGS=-lpng -lvncclient

.PHONY: clean

all: guacd

guacd: daemon.o client.o guacio.o protocol.o
	$(CC) $(CFLAGS) $(LDFLAGS) daemon.o client.o protocol.o guacio.o -o guacd 

client.o: client.c client.h guacio.h
	$(CC) $(CFLAGS) -c client.c

protocol.o: protocol.c protocol.h guacio.h
	$(CC) $(CFLAGS) -c protocol.c

guacio.o: guacio.c guacio.h
	$(CC) $(CFLAGS) -c guacio.c

daemon.o: daemon.c client.h
	$(CC) $(CFLAGS) -c daemon.c

clean:
	$(RM) *.o guacd

distclean: clean
	$(RM) *~

