
#ifndef INCLUDE_READFROMSOCKET_H_
#define INCLUDE_READFROMSOCKET_H_
#include "../include/connectionHandler.h"

class readFromSocket {
public:
	readFromSocket(bool& close,ConnectionHandler &handle,boost::condition_variable& cond);
	void operator()();

private:
    bool& _close;
	ConnectionHandler &_handle;
	boost::condition_variable& _cond;

};

#endif
