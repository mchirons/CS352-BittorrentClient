package bittorrent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Timer;

import GivenTools.ToolKit;
import GivenTools.TorrentInfo;

public class DownloadThread implements Runnable{
	
	private Thread thread;
	private String threadName;
	private ClientManager manager;
	private TorrentInfo torrentInfo;			
	private Peer peer;	
	private byte[] info_hash;				
	private boolean isChoked;				
	private boolean isInterested;						
	private boolean downloadCompleted;
	private OutputStream out;
	private Socket socket;
	private DataOutputStream dOut;
	private InputStream in;
	private ByteArrayOutputStream bOut;	
	private int currentPiece;
	public byte[] peerPieces;
	private Timer timer;
	
	
	public DownloadThread(String name, ClientManager manager, Peer peer){
		this.threadName = name;
		this.torrentInfo = manager.getTorrentInfo();
		this.isChoked = true;
		this.isInterested = false;
		this.downloadCompleted = false;
		this.out = null;
		this.dOut = null;
		this.in = null;
		this.bOut = null;
		this.peer = peer;
		this.manager = manager;
		try{
			socket = new Socket(peer.ipAddress, peer.port);
		} catch (Exception e){
			e.printStackTrace();
		}
		peerPieces = new byte[manager.getTotalPieces()];
	}
	
	public void setPeer(Peer peer){
		this.peer = peer;
	}
		
	public void setInfoHash(byte[] info_hash){
		this.info_hash = info_hash;
	}
	
	public void switchIsChoked(){
		isChoked = !isChoked;
	}
	
	public void switchIsInterested(){
		isInterested = !isInterested;
	}
	
	public boolean getIsChoked(){
		return isChoked;
	}
	
	public boolean getIsInterested(){
		return isInterested;
	}
	
	public TorrentInfo getTorrentInfo(){
		return torrentInfo;
	}
	
	
	public boolean getDownloadCompleted(){
		return downloadCompleted;
	}
	
	public ClientManager getManager(){
		return manager;
	}
	
	public void setCurrentPiece(int piece){
		this.currentPiece = piece;
	}
	
	public int getCurrentPiece(){
		return currentPiece;
	}
	
	public void setThread(Thread thread){
		this.thread = thread;
	}
	
	
	
	private byte[] createHandshake(){
		//System.out.println(threadName + " In createHandshake");
		byte[] handshake = new byte[68];
		ToolKit.print(handshake);
		handshake[0] = 0x13;
		byte[] protocol = { 'B', 'i', 't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ',
				'p', 'r', 'o', 't', 'o', 'c', 'o', 'l' };
		ToolKit.print(protocol);
		byte[] reserved = new byte[8];
		ToolKit.print(reserved);
		ByteBuffer buf = torrentInfo.info_hash;
		byte[] i_hash = buf.array();
		ToolKit.print(i_hash);
		setInfoHash(i_hash);
		byte[] peerid = manager.getId().getBytes(Charset.forName("ASCII"));
		ToolKit.print(peerid);
		System.arraycopy(protocol, 0, handshake, 1, 19);
		System.arraycopy(reserved, 0, handshake, 20, 8);
		System.arraycopy(i_hash, 0, handshake, 28, 20);
		System.arraycopy(peerid, 0, handshake, 48, 20);
		buf = null;
		return handshake;
	}
	
	private boolean isValidHandShake(byte[] buffer){
		boolean isValid = true;
		byte[] handshake = new byte[68];
		//System.out.println("handshakelength: " + );
		handshake[0] = 0x13;
		byte[] protocol = { 'B', 'i', 't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ',
				'p', 'r', 'o', 't', 'o', 'c', 'o', 'l' };
		byte[] reserved = new byte[8];
		byte[] info_hash = this.info_hash;
		byte[] peerid = this.peer.peer_id;
		System.arraycopy(protocol, 0, handshake, 1, 19);
		System.arraycopy(reserved, 0, handshake, 20, 8);
		System.arraycopy(info_hash, 0, handshake, 28, 20);
		System.arraycopy(peerid, 0, handshake, 48, 20);
		
		for (int i = 0; i < 68; i++){
			//System.out.println("buffer: " + b +  " handshake: " + h);
			if (buffer[i] != handshake[i]){
				isValid = false;
				break;
			}
		}
		return isValid;
	}
	
	public void checkDone(){
		if (manager.haveAllPieces()){
			System.out.println("DT " + threadName + " ending");
			 closeConnections();System.out.println("DT " + threadName + " ending on timer");
			 this.timer.cancel();
			this.thread.interrupt();
			
		}
	}
	private void connectToPeer(){
		boolean receivedHandshake = false;
		boolean handshakeSent = false;
		boolean closeConnection = false;
		byte[] handshake = createHandshake();
		while (!closeConnection){	
			 try {
				 
				
			 	out = socket.getOutputStream();
			    dOut = new DataOutputStream(out);
			    in = socket.getInputStream();
				bOut = new ByteArrayOutputStream();
			    if (!handshakeSent){
			    	dOut.write(handshake);
				    dOut.flush();
				    handshakeSent = true;
			    }
				int readBytes = -1;
				int bytesRead = 0;
				int messageLength = 0;
				while((readBytes = in.read()) > -1) {
				    bOut.write(readBytes);
				    bOut.flush();
				    bytesRead++;
				    if (!receivedHandshake && bytesRead == 68){
				    	//check for response handshake	
				    	if (!isValidHandShake(bOut.toByteArray())){
				    		closeConnection = true;
				    	}
				    	else{
				    		receivedHandshake = true;
				    	}
				    	break;
				    }
				    if (bytesRead == 4){
				    	byte[] bytes = bOut.toByteArray();
				    	messageLength = 4 + Utilities.byteArrayToInt(bytes);
				    }
				    if (messageLength == bytesRead){
				    	//System.out.println("\nDT " + threadName +  " Received message");
				    	DownLoadMessageHandler ms = new DownLoadMessageHandler(bOut.toByteArray(), this);
				    	byte[] message = ms.getMessageAction();
				    	if (currentPiece == -1){
					    	closeConnection = true;
					    }
				    	else if(message == null){
				    		
				    		if (SharedData.getInstance().allPiecesRequested() || manager.haveAllPieces()){
				    			closeConnection = true;
				    		}
				    		else {
				    			System.out.println("Waiting for message...");
				    		}
				    	
				    		
				    	}
				    	else{
				    		System.out.println("\nDT " + threadName + " Sent message\n");
				    		dOut.write(message);
						    dOut.flush();
				    	}
				    	break;
				    }
				}
				
			}catch (SocketException e){
				System.out.println("DT " + threadName + " connection closed");
				closeConnections();
					
			}catch (Exception e){
				e.printStackTrace();
			}
			
			 
		}
		//System.out.println("DT " + threadName + " ending");
		 closeConnections();
	}
	
	private void closeConnections(){
		try {
			dOut.close();
			out.close();
			bOut.close();
			in.close();
			socket.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	public String getThreadName(){
		return threadName;
	}
	
	public void run(){
		
		
		
		try {
			connectToPeer();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	
}



