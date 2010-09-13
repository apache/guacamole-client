
CFLAGS=-O2 -pedantic -Wall -Werror -I../libguac/include
LDFLAGS=-L../libguac -lpng -lguac

.PHONY: clean

all: guacd

guacd: daemon.o 
	$(CC) $(CFLAGS) $(LDFLAGS) daemon.o -o guacd 

daemon.o: daemon.c
	$(CC) $(CFLAGS) -c daemon.c

clean:
	$(RM) *.o guacd

distclean: clean
	$(RM) *~

