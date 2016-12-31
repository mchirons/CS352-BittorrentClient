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

public class UploadThread implements Runnable{
	
	private ClientManager manager;
	private Socket socket;
	private OutputStream out;
	private DataOutputStream dOut;
	private InputStream in;
	private ByteArrayOutputStream bOut;	
	private TorrentInfo torrentInfo;
	private byte[] pieces;
	private byte[] info_hash;				
	private boolean isChoked;				
	private boolean isInterested;
	//private ArrayList<Integer> newPieces = new ArrayList<Integer>();
	private String threadName;
	private boolean keepAlive;
	private boolean isConnected;
	private long begin;
	private Thread thread;
	private Timer timer;
	
	public UploadThread(String threadName, Socket socket, ClientManager manager){
		this.threadName = threadName;
		this.manager = manager;
		this.socket = socket;
		this.isChoked = true;
		this.isInterested = false;
		this.out = null;
		this.dOut = null;
		this.in = null;
		this.bOut = null;
		this.torrentInfo = manager.getTorrentInfo();
		this.info_hash = torrentInfo.info_hash.array();
		this.keepAlive = true;
		this.isConnected = true;
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
	
	public ClientManager getManager(){
		return manager;
	}
	
	public String getUploadThread(){
		return threadName;
	}
	
	public void checkConnection(){
		long end = System.nanoTime();
		long max = 120000000000L;
		long interval = end - begin;
		if (interval  >= max){
			System.out.println("UT " + threadName + " Timeout");
			closeConnections();
			this.thread.interrupt();
		}
	}
	
	public void setThread(Thread thread){
		this.thread = thread;
	}
	
	private void openConnection(){
		
		boolean receivedHandshake = false;
		boolean started = false;
		
		
		
		
		try {
			while (keepAlive){
				//System.out.println("in upload loop");
				out = socket.getOutputStream();
			    dOut = new DataOutputStream(out);
			    in = socket.getInputStream();
				bOut = new ByteArrayOutputStream();
				
				int readBytes = -1;
				int bytesRead = 0;
				int messageLength = 0;
				
				while((readBytes = in.read()) > -1) {
					 bOut.write(readBytes);
					 bOut.flush();
					 bytesRead++;
					 //System.out.println(bytesRead);
					 if (!receivedHandshake && bytesRead == 68){
							//System.out.println("entered right place");
							if( isValidHandshake(bOut.toByteArray())){
								System.out.println("UT " + threadName + " Valid handshake");
								byte[] handshake = createHandshake();
								receivedHandshake = true;
								dOut.write(handshake);
								System.out.println("UT " + threadName + " Sent handshake");
								dOut.write(bitfieldMessage());
								System.out.println("UT " + threadName + " sent bitfield message");
								dOut.flush();
								
								break;
							}
					}
					if (bytesRead == 4){
				    	byte[] bytes = bOut.toByteArray();
				    	messageLength = 4 + Utilities.byteArrayToInt(bytes);
					}
					//System.out.println("bytesRead: " + bytesRead + " messageLength: " + messageLength);
				    if (messageLength == bytesRead){
				    	System.out.println("\nUT " + threadName + " Received message");
				    	UploadMessageHandler ms = new UploadMessageHandler(bOut.toByteArray(), this);
				    	byte[] message = ms.getMessageAction();
				    	
				    	if(message == null){
				    		System.out.println("UT " + threadName + " Waiting for message...");
				    	}
				    	else{
				    		//System.out.println(threadName + " Sent message\n");
				    		sendMessage(message);
				    		//System.out.println("UT + sent message");
				    		begin = System.nanoTime();
				    		
				    		if (!started){
				    			Timer timer = new Timer();
								this.timer = timer;
								timer.schedule(new UpdatePeer(this), 0 , 1000);
								started = true;
				    		}
				    		/*
				    		if (!startedTimer){
				    			Timer timer2 = new Timer();
				    			timer2.schedule(new EndConnection(this), 0, 5000);
				    			startedTimer = true;
				    		}
				    		*/
				    	}
				    	break;
				    }
				   
						
				}
				 
			}
			closeConnections();
		} catch (SocketException e){
			
			System.out.println("\nUT " + threadName +  " Connection closed");
			closeConnections();
			return;
		} catch (Exception d){
			d.printStackTrace();
		}
		
		
		
	}
	
	private byte[] haveMessage(int i){
		
		byte[] message = new byte[9];
		message[0] = 0;
		message[1] = 0;
		message[2] = 0;
		message[3] = 5;
		message[4] = 4;
		
		ByteBuffer buff = ByteBuffer.allocate(4);
		buff.putInt(i);
		byte[] index = buff.array();
		
		System.arraycopy(index, 0, message, 5, 4);
		
		return message;
	}
	
	private byte[] bitfieldMessage(){
		byte[] pieces = SharedData.getInstance().getAvailablePiece(manager.getSaveFile());
		
		int len = pieces.length / 8;
		if (pieces.length % 8 > 0){
			len = len + 1;
		}
		
		byte[] payload = new byte[len];
		
		int j = 0;
		int k = 0;
		int l = 0;
		for (int i = 0; i < payload.length; i++){
			
			for( j = i * 8 , k = 0, l = 7; k < 8 && j + k < pieces.length; k++, l--){
					if (pieces[j + k] == 1){
						payload[i] = (byte)(payload[i] | (1 << l));
					}		
			}
			
		}
		/*
		for (int i = 0; i < payload.length; i++){
			int nextIndex = i * 8;
			for (int j = 0; j < 8; i++){
				payload[nextIndex] = (byte)(payload[nextIndex] )
			}
		}
		*/
		
		
		/*
		for (int i = 0; i < payload.length; i++){
			System.out.println("payload[" + i + "]: " + payload[i]);
		}
		int count = 0;
		for (int i = 0; i < pieces.length; i++){
			if (pieces[i] == 1){
				count++;
			}
		}
		for (int i = 0; i < pieces.length; i++){
			System.out.println("pieces[" + i + "]: " + pieces[i]);
		}
		System.out.println("total on bits: " + count);
		
		*/
		
		ByteBuffer buff = ByteBuffer.allocate(4);
		buff.putInt(len);
		byte[] length = buff.array();
		
		byte[] id = {5};
		
		byte[] message = new byte[len + 4];
		System.arraycopy(length, 0, message, 0, 4);
		System.arraycopy(id, 0, message, 4, 1);
		System.arraycopy(payload, 0, message, 5, len - 1);
		return message;
		 
	}
	
	private byte[] createHandshake(){
		
		//System.out.println(threadName + " In createHandshake");
		byte[] handshake = new byte[68];
		//ToolKit.print(handshake);
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
	
	private boolean isValidHandshake(byte[] buffer){
		boolean isValid = true;
		byte[] handshake = new byte[48];
		//System.out.println("handshakelength: " + );
		handshake[0] = 0x13;
		byte[] protocol = { 'B', 'i', 't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ',
				'p', 'r', 'o', 't', 'o', 'c', 'o', 'l' };
		byte[] reserved = new byte[8];
		byte[] info_hash = this.info_hash;
		System.arraycopy(protocol, 0, handshake, 1, 19);
		System.arraycopy(reserved, 0, handshake, 20, 8);
		System.arraycopy(info_hash, 0, handshake, 28, 20);
		
		for (int i = 0; i < 48; i++){
			int b = buffer[i];
			int h = handshake[i];
			//System.out.println("buffer: " + b +  " handshake: " + h);
			if (buffer[i] != handshake[i]){
				isValid = false;
				break;
			}
		}
		return isValid;
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
	
	private void updatePieces(){
		pieces = SharedData.getInstance().getAvailablePiece(manager.getSaveFile());
	}
	
	public void notifyPeer(){
		
		try {
			ArrayList<Integer> newPieces = new ArrayList<Integer>();
			
			byte[] newList = SharedData.getInstance().getAvailablePiece(manager.getSaveFile());
			
			for (int i = 0; i < newList.length; i++){
				if (pieces[i] == 0  && newList[i] == 1){
					pieces[i] = 1;
					newPieces.add(i);
				}
			}
			
			if (newPieces.size() > 0){
				out = socket.getOutputStream();
				bOut = new ByteArrayOutputStream();
	    		for (int i = 0; i < newPieces.size(); i++){
	    			//send have message message
	    			byte[] message = haveMessage(newPieces.get(i));
	    			System.out.println("UT " + threadName + " sent have message: " + newPieces.get(i));
	    			sendMessage(message);
	    		}
			}
		} catch (SocketException e){
			System.out.println("UT " + threadName + " Connection ended");
			closeConnections();
			this.timer.cancel();
			this.thread.interrupt();
			
		} catch (Exception e){
			e.printStackTrace();
		}
		
		
	}
	
	public synchronized void sendMessage(byte[] message){
		try{
			dOut.write(message);
			dOut.flush();
			
		} catch (Exception e){
			System.out.println("UT " + threadName + " connection closed");
			this.timer.cancel();
			this.thread.interrupt();
			e.printStackTrace();
		}
		
	}
	
	public void run(){
		try {
			updatePieces();
			openConnection();
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	
}



