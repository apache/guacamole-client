
.PHONY: clean

all: guacd

guacd: daemon.o proxy.o guacio.o
	$(CC) -g daemon.o proxy.o guacio.o -l png -l vncclient -o guacd 

proxy.o: proxy.c proxy.h guacio.h
	$(CC) -g -c -ansi -pedantic proxy.c

guacio.o: guacio.c guacio.h
	$(CC) -g -c -ansi -pedantic guacio.c

daemon.o: daemon.c proxy.h
	$(CC) -g -c -ansi -pedantic daemon.c

clean:
	$(RM) *.o guacd

distclean: clean
	$(RM) *~

