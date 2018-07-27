package impiccato;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class serverTCPAccept extends Thread {
	ServerSocket socket = null;
	Socket clientSocket = null;
	int port;
	Server s;
	
	public serverTCPAccept(Server s, int p){
		super();
		port = p;
		this.s = s;
		try {
			socket = new ServerSocket(port);
			System.out.println("Creato il thread di accept e la server socket");
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void run() {
		boolean v  = true;
    	while(v){
    		try {
	    		clientSocket = socket.accept();
	    		Thread t = new ConnessioneTCP(clientSocket,s);
	    		t.start();
    		} catch (IOException e) {
    	        System.err.println("Accept failed.");
    	    }
    	}
	        	
	}

}
