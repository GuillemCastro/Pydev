'''
@author Fabio Zadrozny 
'''
import threading
import time
import simpleTipper
import refactoring
import sys

HOST = '127.0.0.1'               # Symbolic name meaning the local host

MSG_KILL_SERVER     = '@@KILL_SERVER_END@@'
MSG_COMPLETIONS     = '@@COMPLETIONS'
MSG_END             = 'END@@'
MSG_GLOBALS         = '@@GLOBALS:'
MSG_TOKEN_GLOBALS   = '@@TOKEN_GLOBALS('
MSG_CLASS_GLOBALS   = '@@CLASS_GLOBALS('
MSG_INVALID_REQUEST = '@@INVALID_REQUEST'
MSG_RELOAD_MODULES  = '@@RELOAD_MODULES_END@@'
MSG_CHANGE_DIR      = '@@CHANGE_DIR:'
MSG_OK              = '@@MSG_OK_END@@'
MSG_REFACTOR        = '@@REFACTOR'
MSG_PROCESSING      = '@@PROCESSING_END@@'

BUFFER_SIZE = 1024

class KeepAliveThread(threading.Thread):
    def __init__(self, socket):
        threading.Thread.__init__(self)
        self.socket = socket
        self.lastMsg = None
    
    def run(self):
        time.sleep(0.1)
        while self.lastMsg == None:
            print 'sending', MSG_PROCESSING
            self.socket.send(MSG_PROCESSING)
            time.sleep(0.1)
        print 'sending', self.lastMsg
        self.socket.send(self.lastMsg)
        
class T(threading.Thread):

    def __init__(self, thisPort, serverPort):
        threading.Thread.__init__(self)
        self.thisPort   = thisPort
        self.serverPort = serverPort
        self.socket = None #socket to send messages.
        

    def connectToServer(self):
        import socket
        
        self.socket = s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.connect((HOST, self.serverPort))

    def removeInvalidChars(self, msg):
        if msg:
            return msg.replace(',','').replace('(','').replace(')','')
        return ' '
    
    def formatCompletionMessage(self, completionsList):
        '''
        Format the completions suggestions in the following format:
        @@COMPLETIONS((token,description),(token,description),(token,description))END@@
        '''
        compMsg = ''
        for tup in completionsList:
            if compMsg != '':
                compMsg += ','
                
            compMsg += '(%s,%s)' % (self.removeInvalidChars(tup[0]),self.removeInvalidChars(tup[1]))
            
        return '%s(%s)%s'%(MSG_COMPLETIONS, compMsg, MSG_END)
    
    def getCompletionsMessage(self, completionsList):
        '''
        get message with completions.
        '''
        return self.formatCompletionMessage(completionsList)
    
    def getTokenAndData(self, data):
        '''
        When we receive this, we have 'token):data'
        '''
        token = ''
        for c in data:
            if c != ')':
                token += c
            else:
                break;
        
        return token, data.lstrip(token+'):')

    
    def run(self):
        # Echo server program
        import socket
        
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.bind((HOST, self.thisPort))
        s.listen(1) #socket to receive messages.
        

        #we stay here until we are connected.
        #we only accept 1 client. 
        #the exit message for the server is @@KILL_SERVER_END@@
        conn, addr = s.accept()

        #after being connected, create a socket as a client.
        self.connectToServer()
        
#        print 'pycompletionserver Connected by', addr
        
        
        while 1:
            data = ''
            returnMsg = ''
            keepAliveThread = KeepAliveThread(self.socket)
            
            while not data.endswith(MSG_END):
                data += conn.recv(BUFFER_SIZE)
            
            try:
                if MSG_KILL_SERVER in data:
                    #break if we received kill message.
                    break;
    
                keepAliveThread.start()
                
                if MSG_RELOAD_MODULES in data:
                    simpleTipper.ReloadModules()
                    returnMsg = MSG_OK
                
                else:
                    data = data[:data.find(MSG_END)]
                
                    if data.startswith(MSG_GLOBALS):
                        data = data.replace(MSG_GLOBALS, '')
                        comps = simpleTipper.GenerateTip(data, None, False)
                        returnMsg = self.getCompletionsMessage(comps)
                    
                    elif data.startswith(MSG_TOKEN_GLOBALS ):
                        data = data.replace(MSG_TOKEN_GLOBALS, '')
                        token, data = self.getTokenAndData(data)                
                        comps = simpleTipper.GenerateTip(data, token, False)
                        returnMsg = self.getCompletionsMessage(comps)
        
                    elif data.startswith(MSG_CLASS_GLOBALS ):
                        data = data.replace(MSG_CLASS_GLOBALS, '')
                        token, data = self.getTokenAndData(data)                
                        comps = simpleTipper.GenerateTip(data, token, True)
                        returnMsg = self.getCompletionsMessage(comps)
                    
                    elif data.startswith(MSG_CHANGE_DIR ):
                        data = data.replace(MSG_CHANGE_DIR, '')
                        simpleTipper.CompleteFromDir(data)
                        returnMsg = MSG_OK
                        
                    elif data.startswith(MSG_REFACTOR):
                        data = data.replace(MSG_REFACTOR, '')
                        returnMsg = refactoring.HandleRefactorMessage(data)
                        
                    else:
                        returnMsg = MSG_INVALID_REQUEST
            finally:
                keepAliveThread.lastMsg = returnMsg
            
        conn.close()
        self.ended = True

if __name__ == '__main__':
    #let's log this!!
    out = open('c:/temp/pydev.log', 'w')
    sys.stdout = out
    sys.stderr = out
    
    thisPort = int(sys.argv[1])  #this is from where we want to receive messages.
    serverPort = int(sys.argv[2])#this is where we want to write messages.
    
    t = T(thisPort, serverPort)
    print 'will start'
    t.start()

