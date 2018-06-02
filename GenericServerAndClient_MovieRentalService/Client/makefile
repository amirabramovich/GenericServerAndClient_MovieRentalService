CFLAGS:=-c -Wall -Weffc++ -g -std=c++11 -Iinclude
LDFLAGS:=-lboost_system -lboost_thread

all: Client
	g++ -o bin/BBclient bin/connectionHandler.o bin/echoClient.o bin/readFromSocket.o $(LDFLAGS) 

Client: bin/connectionHandler.o bin/echoClient.o bin/readFromSocket.o
	
bin/connectionHandler.o: src/connectionHandler.cpp
	g++ $(CFLAGS) -o bin/connectionHandler.o src/connectionHandler.cpp
	
bin/readFromSocket.o: src/readFromSocket.cpp
	g++ $(CFLAGS) -o bin/readFromSocket.o src/readFromSocket.cpp

bin/echoClient.o: src/echoClient.cpp
	g++ $(CFLAGS) -o bin/echoClient.o src/echoClient.cpp
	
.PHONY: clean
clean:
	rm -f bin/*
