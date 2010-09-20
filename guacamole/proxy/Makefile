
CFLAGS+=-O2 -pedantic -Wall -Werror -I../libguac/include
LDFLAGS=-L../libguac -lpng -lguac -ldl -lpthread

.PHONY: clean

all: guacd

guacd: daemon.o 
	$(CC) $(LDFLAGS) daemon.o -o guacd 

daemon.o: daemon.c
	$(CC) $(CFLAGS) -c daemon.c

clean:
	$(RM) *.o guacd

distclean: clean
	$(RM) *~

