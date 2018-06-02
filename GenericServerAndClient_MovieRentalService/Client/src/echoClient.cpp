#include <stdlib.h>
#include <connectionHandler.h>
#include <boost/thread.hpp>
#include "../include/readFromSocket.h"

int main (int argc, char *argv[]) {
    if (argc < 3) {
        std::cerr << "Usage: " << argv[0] << " host port" << std::endl << std::endl;
        return -1;
    }
    std::string host = argv[1];
    short port = atoi(argv[2]);
    
    ConnectionHandler connectionHandler(host, port);
    if (!connectionHandler.connect()) {
        std::cerr << "Cannot connect to " << host << ":" << port << std::endl;
        return 1;
    }

    boost::mutex _mutex;
    boost::condition_variable cond;
    bool closeMain=false;
    readFromSocket fromSocket(closeMain,connectionHandler,cond);
    boost::thread t2(fromSocket);

    while (closeMain==false) {
        const short bufsize = 1024;
        char buf[bufsize];
        std::cin.getline(buf, bufsize);
		std::string line(buf);
		if(line.length()==0)
			break;
        if (!connectionHandler.sendLine(line)) {
            std::cout << "Disconnected. Exiting...\n" << std::endl;
            break;
        }

            boost::unique_lock<boost::mutex> lock(_mutex);
            cond.wait(lock);
        

    }
    t2.join();
    return 0;
}

