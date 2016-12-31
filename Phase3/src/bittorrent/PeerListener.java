package bittorrent;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class PeerListener implements Runnable {
	
	private ClientManager manager;
	
	public PeerListener(ClientManager manager){
		this.manager = manager;
	}
	
	@SuppressWarnings("resource")
	public void run(){
		int i = 0;
		
		manager.uploadThreads = new ArrayList<Thread>();
		
		ServerSocket serverSocket = null;
        try{
        	serverSocket = new ServerSocket(6881);
        } catch (Exception e){
        	e.printStackTrace();
        }
        
        ArrayList<String> clients = new ArrayList<String>();
		
        while (true) {

        	try{
        		
        			System.out.println("\nWaiting for connection request...");
            		// a "blocking" call which waits until a connection is requested
    	        	Socket clientSocket = serverSocket.accept();
    	        	String address = clientSocket.getInetAddress().toString();
  
    	        	
    	        	if (!clients.contains(address) && ( !address.equals("/172.16.97.10") && !address.equals("/172.16.97.11") && !address.equals("/172.16.97.12") && !address.equals("/172.16.97.13"))){
    	        		clients.add(clientSocket.getInetAddress().toString());
    	        		System.out.println("UT " + i + " Connection made to: " + clientSocket.getInetAddress().toString());
        	        	//spawn upload thread
        	        	Thread uThread;
        	        	UploadThread uploadThread = new UploadThread(Integer.toString(i), clientSocket, manager);
        	        	uThread = new Thread(uploadThread);
        	        	uploadThread.setThread(uThread);
        	        	manager.uploadThreads.add(uThread);
        	        	uThread.start();
        	        	i++;
    	        	}
    	        	
        		
 
        		
	        	
	        	
	      
        	} catch (Exception e){
        		e.printStackTrace();
        	}
            
        }
        
		
	}

}
