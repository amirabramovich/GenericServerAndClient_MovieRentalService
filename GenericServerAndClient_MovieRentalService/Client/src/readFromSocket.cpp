#include <iostream>
#include <boost/thread.hpp>
#include "../include/readFromSocket.h"


readFromSocket::readFromSocket(bool& close,ConnectionHandler &handle,boost::condition_variable& cond):_close(close),_handle(handle),_cond(cond){};

void readFromSocket::operator()(){
	while (_close==false) {
			std::string answer;
			// Get back an answer: by using the expected number of bytes (len bytes + newline delimiter)
			// We could also use: connectionHandler.getline(answer) and then get the answer without the newline char at the end
			if (!_handle.getLine(answer)) {
				std::cout << "Disconnected. Exiting...\n" << std::endl;
				break;
			}
			int len=answer.length();
			//A string must end with a 0 char delimiter.  When we filled the answer buffer from the socket
			// we filled up to the \n char - we must make sure now that a 0 char is also present. So we truncate last character.
			answer.resize(len-1);
		if (answer == "ACK signout succeeded") {
              _close=true;

		}
			std::cout << answer << std::endl;
		    _cond.notify_all();
		 }
}


