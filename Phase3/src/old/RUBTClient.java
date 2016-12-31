/*
package old;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;


import GivenTools.Bencoder2;
import GivenTools.ToolKit;
import GivenTools.TorrentInfo;
import bittorrent.DownLoadMessageHandler;
import bittorrent.Peer;
import bittorrent.Utilities;

public class RUBTClient {
	
	public final static ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[]
		    { 'p', 'e', 'e', 'r' , 's'});
	public final static ByteBuffer KEY_PEERID = ByteBuffer.wrap(new byte[]
		    { 'p', 'e', 'e', 'r' , ' ', 'i', 'd'});
	public final static ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[]
		    { 'p', 'o', 'r', 't'});
	public final static ByteBuffer KEY_IP = ByteBuffer.wrap(new byte[]
		    { 'i', 'p'});
	
	static int piecesSaved = 0;				//manager
	private TorrentInfo torrentInfo;		//manager
	private Peer peer;						//download thread
	private String mypeer_id;				//download/upload thread
	private byte[] info_hash;				//download/upload thread
	private boolean isChoked;				//download/upload thread
	private boolean isInterested;			//download/upload thread
	private int currentPiece;				//download/upload thread
	private int currentBlock;				//download/upload thread
	private byte[][] pieces; 				//manager
	private byte[] purgatory;				//manager
	private int fPieceLength;
	private int numPieces;
	private boolean downloadCompleted;
	private String savePath;
	private OutputStream out;
	private Socket socket;
	private DataOutputStream dOut;
	private InputStream in;
	private ByteArrayOutputStream bOut;
	private ArrayList<Peer> peerList;

	public RUBTClient(TorrentInfo torrentInfo){
		this.torrentInfo = torrentInfo;
		this.isChoked = true;
		this.isInterested = false;
		this.currentPiece = 0;
		this.currentBlock = 0;
		this.downloadCompleted = false;
		this.socket = null;
		this.out = null;
		this.dOut = null;
		this.in = null;
		this.bOut = null;
		if (torrentInfo.file_length % torrentInfo.piece_length > 0){
			this.pieces = new byte[torrentInfo.file_length / torrentInfo.piece_length + 1][torrentInfo.piece_length];
			this.fPieceLength = torrentInfo.file_length % torrentInfo.piece_length;
			this.numPieces = torrentInfo.file_length / torrentInfo.piece_length + 1;
		}
		else{
			this.pieces = new byte[torrentInfo.file_length / torrentInfo.piece_length][torrentInfo.piece_length];
			this.fPieceLength = torrentInfo.piece_length;
			this.numPieces = torrentInfo.file_length / torrentInfo.piece_length;
		}
		//System.out.println("numPieces: " +  numPieces);
		//System.out.println("fPieceLength: " + fPieceLength);
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
	
	public int getCurrentPiece(){
		return currentPiece;
	}
	
	public int getCurrentBlock(){
		return currentBlock;
	}
	
	public void setCurrentPiece(int index){
		this.currentPiece = index;
	}
	
	public void setCurrentBlock(int offset){
		this.currentBlock = offset;
	}
	
	public TorrentInfo getTorrentInfo(){
		return torrentInfo;
	}
	
	public byte[] getPurgatory(){
		return purgatory;
	}
	
	public void setPurgatory(byte[] payload){
		this.purgatory =  payload;
	}
	
	public void setPieces(byte[] piece){
		if (currentPiece == numPieces - 1){
			System.arraycopy(piece, 0, pieces[currentPiece], 0, piece.length);
			downloadCompleted = true;
		}
		else {
			pieces[currentPiece] = piece;
			piecesSaved++;
		}
		
	}
	
	public boolean getDownloadCompleted(){
		return downloadCompleted;
	}
	
	public void setSavePath(String path){
		this.savePath = path;
	}
	
	public int getNumPieces(){
		return numPieces;
	}
	
	public int getFPieceLength(){
		return fPieceLength;
	}
	
	private byte[] getPeersList(){
		
		byte[] responseBytes = null;
		
		try {
			String  info_hash, port, left;
			//properly encode info_hash
			info_hash = new String(torrentInfo.info_hash.array(), "ISO-8859-1");
			info_hash = URLEncoder.encode(info_hash, "ISO-8859-1");
			
			mypeer_id= Utilities.generatePeerID();
			port = Integer.toString(6881);
			left = Integer.toString(torrentInfo.file_length);
			//Build URL
			String url = torrentInfo.announce_url.toString();
			url = url + "?" + "info_hash=" + info_hash + "&peer_id=" + mypeer_id + "&port=" + port + "&uploaded=0&downloaded=0&left=" + left + "&event=started";
			URL trackerURL = new URL(url);
			//Open connection with tracker and intercept response
			URLConnection trackerConnection = trackerURL.openConnection();
			BufferedInputStream responseStream = new BufferedInputStream(trackerConnection.getInputStream());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int read;
			while((read = responseStream.read()) != -1) {
			    baos.write(read);
			}
			responseBytes = baos.toByteArray();	
		} catch (Exception e){
			e.printStackTrace();
		}
		return responseBytes;
		
	}
	
	private byte[] createHandshake(){
		byte[] handshake = new byte[68];
		handshake[0] = 0x13;
		byte[] protocol = { 'B', 'i', 't', 'T', 'o', 'r', 'r', 'e', 'n', 't', ' ',
				'p', 'r', 'o', 't', 'o', 'c', 'o', 'l' };
		byte[] reserved = new byte[8];
		ByteBuffer buf = torrentInfo.info_hash;
		byte[] i_hash = new byte[buf.remaining()];
		buf.get(i_hash);
		setInfoHash(i_hash);
		byte[] peerid = mypeer_id.getBytes(Charset.forName("ASCII"));
		System.arraycopy(protocol, 0, handshake, 1, 19);
		System.arraycopy(reserved, 0, handshake, 20, 8);
		System.arraycopy(i_hash, 0, handshake, 28, 20);
		System.arraycopy(peerid, 0, handshake, 48, 20);
		return handshake;
	}
	
	private void connectToPeer(Socket socket){
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
				    	System.out.println("\nReceived message");
				    	DownLoadMessageHandler ms = new DownLoadMessageHandler(bOut.toByteArray(), this);
				    	byte[] message = ms.getMessageAction();
				    	if (getDownloadCompleted()){
					    	closeConnection = true;
					    	saveToFile();
					    }
				    	else if(message == null){
				    		System.out.println("Waiting for keep-alive. DON'T QUIT PROGRAM!");
				    	}
				    	else{
				    		System.out.println("Sent message\n");
				    		dOut.write(message);
						    dOut.flush();
				    	}
				    	break;
				    }
				}
			}catch (Exception e){
				e.printStackTrace();
					
			}
		}
	}
	
	private void notifyTrackerOfCompletion(){
		try {
			String  info_hash, port;
			//properly encode info_hash
			info_hash = new String(torrentInfo.info_hash.array(), "ISO-8859-1");
			info_hash = URLEncoder.encode(info_hash, "ISO-8859-1");
			port = Integer.toString(6881);
			String url = torrentInfo.announce_url.toString();
			url = url + "?" + "info_hash=" + info_hash + "&peer_id=" + mypeer_id + "&port=" + port + "&uploaded=" + 0 + "&downloaded=" + torrentInfo.file_length + "&left=0&event=completed";
			URL trackerURL = new URL(url);
			
			//Open connection with tracker and send completed message
			URLConnection trackerConnection = trackerURL.openConnection();
			BufferedInputStream responseStream = new BufferedInputStream(trackerConnection.getInputStream());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			baos.close();
			responseStream.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	private void notifyTrackerOfStopped(){
		try {
			String  info_hash, port;
			//properly encode info_hash
			info_hash = new String(torrentInfo.info_hash.array(), "ISO-8859-1");
			info_hash = URLEncoder.encode(info_hash, "ISO-8859-1");
			port = Integer.toString(6881);
			String url = torrentInfo.announce_url.toString();
			url = url + "?" + "info_hash=" + info_hash + "&peer_id=" + mypeer_id + "&port=" + port + "&uploaded=" + 0 + "&downloaded=" + torrentInfo.file_length + "&left=0&event=stopped";
			URL trackerURL = new URL(url);
			
			//Open connection with tracker and send completed message
			URLConnection trackerConnection = trackerURL.openConnection();
			BufferedInputStream responseStream = new BufferedInputStream(trackerConnection.getInputStream());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			baos.close();
			responseStream.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	private void closeStreams(){
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
	
	private void saveToFile(){
		
		byte[] totalBytes = new byte[torrentInfo.file_length];
		int offset = 0;
		int i;
		for (i = 0; i < numPieces - 1; i++){
			System.arraycopy(pieces[i], 0, totalBytes, offset, torrentInfo.piece_length);
			offset = offset + torrentInfo.piece_length;
		}
		System.arraycopy(pieces[i], 0, totalBytes, offset, fPieceLength);
		try{
			FileOutputStream fos = new FileOutputStream(savePath);
			fos.write(totalBytes);
			fos.close();
			System.out.println("save successful");
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Peer extractPeer(Map<ByteBuffer, Object> response_map){
		peerList = new ArrayList<Peer>();
		Peer peer = null;
		String peerString = null;
		String port = null;
		String ip = null;
		ArrayList peers_buff = (ArrayList)response_map.get(KEY_PEERS);
		for(int i = 0; i < peers_buff.size(); i++){
			
			Map<ByteBuffer, Object> peerInfo = (Map<ByteBuffer,Object>)peers_buff.get(i);
			ByteBuffer peerid_bytes = (ByteBuffer)peerInfo.get(KEY_PEERID);
			byte[] peerid = new byte[peerid_bytes.remaining()];
			peerid_bytes.get(peerid);
			//ToolKit.print(peerid);
			try {
				peerString  = new String(peerid_bytes.array(), "ASCII");
			}catch (Exception e){
				e.printStackTrace();
			}
			if (peerString == null || peerString.length() < 3){
				//don't proceed
			}
			else if (peerString.substring(0, 7).equals("-RU1103")){
				
				Integer port_bytes = (Integer)peerInfo.get(KEY_PORT);
				try {
					port = Integer.toString(port_bytes);
				}catch (Exception e){
					e.printStackTrace();
				}
				ByteBuffer ip_bytes = (ByteBuffer)peerInfo.get(KEY_IP);
				try {
					ip = new String(ip_bytes.array(),"ASCII");
				}catch (Exception e){
					e.printStackTrace();
				}
				peer = new Peer(Integer.parseInt(port), ip, peerid);
				peerList.add(peer);
			}
		}
		
		return getFastestPeer();
	}
	
	private Peer getFastestPeer(){
		byte[] ping = {1};
		Peer peer = null;
		int readBytes = -1;
		long starttime = 0;
		long endtime = 0;
		long totaltime = 0;
		long averagetime = 0;
		try{
			for (int i = 0; i < peerList.size(); i++){
				//System.out.println("next peer");
				peer = peerList.get(i);
				socket = new Socket(peer.ipAddress, peer.port);
				socket.setTcpNoDelay(true);
				out = socket.getOutputStream();
			    dOut = new DataOutputStream(out);
			    in = socket.getInputStream();
				bOut = new ByteArrayOutputStream();
				for (int j = 0; j < 10; j++){
						starttime = System.nanoTime();
						dOut.write(ping);
					    dOut.flush();
					    //System.out.println("Sent ping");
					    if ((readBytes = in.read()) > -1){
					    	endtime = System.nanoTime();
					    	totaltime = totaltime + (endtime - starttime);
					    	//System.out.println("ping response");
					    }
					    else {
					    	//System.out.println("no ping response");
					    }
				}
				averagetime = totaltime / 10;
				peer.averagePing = averagetime;
				ToolKit.print(peer.peer_id);
				System.out.println("average ping: " + peer.averagePing + " nanoseconds");
				totaltime = 0;
				averagetime = 0;
			}
		} catch (Exception e){
			e.printStackTrace();
		}
		
		long fastestPing = peerList.get(0).averagePing;
		peer = peerList.get(0);
		//System.out.println("fastest ping: " + fastestPing);
		for (int i = 1; i < peerList.size(); i++){
			if (peerList.get(i).averagePing < fastestPing){
				fastestPing = peerList.get(i).averagePing;
				peer = peerList.get(i);
				//System.out.println("peer: " + i + " fastest");
			}
		}
		closeStreams();
		System.out.println("Fastest peer");
		ToolKit.print(peer.peer_id);
		return peer;
	}
	
	private boolean isValidHandShake(byte[] buffer){
		boolean isValid = true;
		byte[] handshake = new byte[68];
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
	
	
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		if (args.length != 2){
			System.out.println("Incorrect number of arguments. Correct usage is: \n" + 
					 "java -cp RUBTClient \"torrentFileName\" \"saveFileName\"");
		}
		else {
			try{
				
				//Get path of torrentFile and set path for save file
				Path torrentPath = FileSystems.getDefault().getPath( "src/bittorrent" , args[0] ).toAbsolutePath();
				int length = args[0].length();
				String savePath = torrentPath.toString().substring(0, torrentPath.toString().length() - length) + args[1];
				System.out.println(savePath);
				byte[] torrentArray = Files.readAllBytes(torrentPath);
				TorrentInfo torrentInfo = new TorrentInfo(torrentArray);
				RUBTClient client = new RUBTClient(torrentInfo);
				client.setSavePath(savePath);
				
				byte[] responseBytes = client.getPeersList();
				
				
				Map <ByteBuffer, Object>response_map = (Map<ByteBuffer,Object>)Bencoder2.decode(responseBytes);
				Peer peer = client.extractPeer(response_map);
				client.setPeer(peer);
			
				//Commence peer connection
				long startTime = System.nanoTime();
				client.socket = new Socket(peer.ipAddress, peer.port);
				client.connectToPeer(client.socket);
				long endTime = System.nanoTime();
				System.out.println("download time: " + ((endTime - startTime)) / 1000000000 + " seconds");
				client.notifyTrackerOfCompletion();
				client.notifyTrackerOfStopped();
				client.closeStreams();
				} catch (Exception e){
					e.printStackTrace();
				}
		
		}
	}
}

*/
