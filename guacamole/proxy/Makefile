
.PHONY: clean

all: guacd

guacd: daemon.o proxy.o guacio.o protocol.o
	$(CC) -O2 daemon.o proxy.o protocol.o guacio.o -l png -l vncclient -o guacd 

proxy.o: proxy.c proxy.h guacio.h
	$(CC) -O2 -c -ansi -pedantic proxy.c

protocol.o: protocol.c protocol.h guacio.h
	$(CC) -O2 -c -ansi -pedantic protocol.c

guacio.o: guacio.c guacio.h
	$(CC) -O2 -c -ansi -pedantic guacio.c

daemon.o: daemon.c proxy.h
	$(CC) -O2 -c -ansi -pedantic daemon.c

clean:
	$(RM) *.o guacd

distclean: clean
	$(RM) *~

