package bittorrent;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import java.util.Timer;

import GivenTools.Bencoder2;
import GivenTools.TorrentInfo;


public class ClientManager {
	
	public final static ByteBuffer KEY_PEERS = ByteBuffer.wrap(new byte[]
		    { 'p', 'e', 'e', 'r' , 's'});
	public final static ByteBuffer KEY_PEERID = ByteBuffer.wrap(new byte[]
		    { 'p', 'e', 'e', 'r' , ' ', 'i', 'd'});
	public final static ByteBuffer KEY_PORT = ByteBuffer.wrap(new byte[]
		    { 'p', 'o', 'r', 't'});
	public final static ByteBuffer KEY_IP = ByteBuffer.wrap(new byte[]
		    { 'i', 'p'});
	public final static ByteBuffer KEY_MININTERVAL = ByteBuffer.wrap(new byte[]
			{'m', 'i', 'n', ' ', 'i', 'n', 't', 'e', 'r' , 'v', 'a', 'l'});
	public final static ByteBuffer KEY_INTERVAL = ByteBuffer.wrap(new byte[]
			{'i', 'n', 't', 'e', 'r' , 'v', 'a', 'l'});
	public final static ByteBuffer KEY_TRACKERID = ByteBuffer.wrap(new byte[]
			{'t', 'r', 'a', 'c', 'k', 'e', 'r', ' ','i', 'd'});
	
	
	

	private TorrentInfo torrentInfo;
	private ArrayList<Peer> peers;
	private String id;
	private ArrayList<Thread> downloadThreads;
	public ArrayList<Thread> uploadThreads;
	private int totalPieces;
	private int fPieceLength;
	private int threadCount;
	private String downloadFile;
	private String saveFile = "saved.txt";
	private int interval;
	private int minInterval;
	private String trackerID;
	private String event;
	private long startTime;
	private long endTime;
	private Timer endTimer;
	
	public ClientManager(String[] args){
		Path torrentPath = FileSystems.getDefault().getPath( "src/bittorrent" , args[0] ).toAbsolutePath();
		int length = args[0].length();
		downloadFile = args[1];
		System.out.println("Saving file to : " + downloadFile);
		
		try {
			byte[] torrentArray = Files.readAllBytes(torrentPath);
			this.torrentInfo = new TorrentInfo(torrentArray);
		} catch (Exception e){
			e.printStackTrace();
		}
		this.id = Utilities.generatePeerID();
		if (torrentInfo.file_length % torrentInfo.piece_length > 0){
			this.fPieceLength = torrentInfo.file_length % torrentInfo.piece_length;
			this.totalPieces = torrentInfo.file_length / torrentInfo.piece_length + 1;
			
		}
		else{
			this.fPieceLength = torrentInfo.piece_length;
			this.totalPieces = torrentInfo.file_length / torrentInfo.piece_length;
			
		}
		checkPieces();
		this.downloadThreads = new ArrayList<Thread>();
		
	}
	
	private void findPeers(){
			
			byte[] responseBytes = null;
			try {
				String  info_hash, port, left, dL;
				//properly encode info_hash
				info_hash = new String(torrentInfo.info_hash.array(), "ISO-8859-1");
				info_hash = URLEncoder.encode(info_hash, "ISO-8859-1");
				
				port = Integer.toString(6881);
				//left = Integer.toString(torrentInfo.file_length);
				
				byte[]  pieces= SharedData.getInstance().getAvailablePiece(saveFile);
				int downloaded = SharedData.getInstance().getDownloaded();
				if (pieces[pieces.length - 1] == 1){
					downloaded = torrentInfo.piece_length * (downloaded - 1);
					downloaded = downloaded + fPieceLength;
				}
				else{
					downloaded = torrentInfo.piece_length * downloaded;
				}
				
				
				dL = Integer.toString(downloaded);
				
				left = Integer.toString(torrentInfo.file_length - downloaded);
				event = "started";
	
				//Build URL
				String url = torrentInfo.announce_url.toString();
				url = url + "?" + "info_hash=" + info_hash + "&peer_id=" + id + "&port=" + port + "&uploaded=0&downloaded=" + dL + "&left=" + left + "&event=" + event;
				System.out.println("initial contact url:" + url);
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
			try {
				Map <ByteBuffer, Object>response_map = (Map<ByteBuffer,Object>)Bencoder2.decode(responseBytes);
				extractPeers(response_map);
				extractIntervals(response_map);
			} catch (Exception e){
				e.printStackTrace();
			}
			
			
	}
	
	private void extractIntervals(Map<ByteBuffer, Object> response_map){
		try {
			minInterval = (Integer)response_map.get(KEY_MININTERVAL);
			interval = (Integer)response_map.get(KEY_INTERVAL);
			ByteBuffer t = (ByteBuffer)response_map.get(KEY_TRACKERID);
			if (t != null){
				trackerID = new String(t.array(), "ISO-8859-1");
				
			}
			else{
				//System.out.println("no tracker id");
			}
			//this.interval = Utilities.byteArrayToInt(interval.array());
			//this.minInterval = Utilities.byteArrayToInt(minInterval.array());
			//System.out.println("interval: " + this.interval);
			//System.out.println("minInterval: " + this.minInterval);
			//System.out.println("trackerID: " + trackerID);
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void extractPeers(Map<ByteBuffer, Object> response_map){
		peers = new ArrayList<Peer>();
		Peer peer = null;
		String port = null;
		String ip = null;
		ArrayList peers_buff = (ArrayList)response_map.get(KEY_PEERS);
		for(int i = 0; i < peers_buff.size(); i++){
			
			Map<ByteBuffer, Object> peerInfo = (Map<ByteBuffer,Object>)peers_buff.get(i);
			ByteBuffer peerid_bytes = (ByteBuffer)peerInfo.get(KEY_PEERID);
			byte[] peerid = new byte[peerid_bytes.remaining()];
			peerid_bytes.get(peerid);
			//ToolKit.print(peerid);
			
			ByteBuffer ip_bytes = (ByteBuffer)peerInfo.get(KEY_IP);
			try {
				ip = new String(ip_bytes.array(),"ASCII");
			}catch (Exception e){
				e.printStackTrace();
			}
			if (ip.equals("172.16.97.11") || ip.equals("172.16.97.13") || ip.equals("172.16.97.12")){ //change later
				Integer port_bytes = (Integer)peerInfo.get(KEY_PORT);
				try {
					port = Integer.toString(port_bytes);
				}catch (Exception e){
					e.printStackTrace();
				}
				peer = new Peer(Integer.parseInt(port), ip, peerid);
				peers.add(peer);
			}
			
		}
		System.out.println("Peers found: " + peers.size());
		
	}
	
	private void download(){
		//spawn download threads for each peer
		Thread dThread;
		for (int i = 0; i < peers.size(); i++){
			threadCount++;
			DownloadThread t = new DownloadThread(Integer.toString(i), this, peers.get(i));
			dThread = new Thread(t);
			t.setThread(dThread);
			downloadThreads.add(dThread);
			dThread.start();
		}
		
	}
	
	private void listen(){
		//open thread to listen for handshakes
		Thread thread = new Thread(new PeerListener(this));
		thread.start();
		
		//spawn a thread to handle requests for each unique handshake
		
	}
	
	private void stop(){
		Thread sThread;
		sThread = new Thread(new Terminator(this));
		sThread.start();
	}
	
	private void checkPieces(){
		try {
			int size = torrentInfo.file_length;
			File save = new File(saveFile);
			FileOutputStream out = null;
			if (!save.exists()){
				save.createNewFile();
				
				out = new FileOutputStream(save);
				out.write(new byte[totalPieces]);
				out.flush();
				out.close();
			}
			
			
			File download = new File(downloadFile);
			if (!download.exists()){
				download.createNewFile();
				out = new FileOutputStream(download); 
				out.write(new byte[size]);
				out.flush();
				out.close();
			}
		;
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private void notifyTrackerOfCompletion(){
		try {
			String  info_hash, port, left, dL;
			//properly encode info_hash
			info_hash = new String(torrentInfo.info_hash.array(), "ISO-8859-1");
			info_hash = URLEncoder.encode(info_hash, "ISO-8859-1");
			
			port = Integer.toString(6881);
			//left = Integer.toString(torrentInfo.file_length);
			
			byte[]  pieces= SharedData.getInstance().getAvailablePiece(saveFile);
			int downloaded = SharedData.getInstance().getDownloaded();
			if (pieces[pieces.length - 1] == 1){
				downloaded = torrentInfo.piece_length * (downloaded - 1);
				downloaded = downloaded + fPieceLength;
			}
			else{
				downloaded = torrentInfo.piece_length * downloaded;
			}
			
			dL = Integer.toString(downloaded);
			
			left = Integer.toString(torrentInfo.file_length - downloaded);
			
			event = "completed";

			//Build URL
			String url = torrentInfo.announce_url.toString();
			url = url + "?" + "info_hash=" + info_hash + "&peer_id=" + id + "&port=" + port + "&uploaded=0&downloaded=" + dL + "&left=" + left + "&event=" + event;
			System.out.println("notify completed url:" + url);
			URL trackerURL = new URL(url);
			//Open connection with tracker and intercept response
			URLConnection trackerConnection = trackerURL.openConnection();
			BufferedInputStream responseStream = new BufferedInputStream(trackerConnection.getInputStream());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int read;
			while((read = responseStream.read()) != -1) {
			    baos.write(read);
			}
			baos.close();
			responseStream.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	public void notifyTrackerOfStopped(){
		try {
			String  info_hash, port, left, dL;
			//properly encode info_hash
			info_hash = new String(torrentInfo.info_hash.array(), "ISO-8859-1");
			info_hash = URLEncoder.encode(info_hash, "ISO-8859-1");
			
			port = Integer.toString(6881);
			//left = Integer.toString(torrentInfo.file_length);
			
			byte[]  pieces= SharedData.getInstance().getAvailablePiece(saveFile);
			int downloaded = SharedData.getInstance().getDownloaded();
			if (pieces[pieces.length - 1] == 1){
				downloaded = torrentInfo.piece_length * (downloaded - 1);
				downloaded = downloaded + fPieceLength;
			}
			else{
				downloaded = torrentInfo.piece_length * downloaded;
			}
			
			dL = Integer.toString(downloaded);
			
			left = Integer.toString(torrentInfo.file_length - downloaded);
			
			event = "stopped";

			//Build URL
			String url = torrentInfo.announce_url.toString();
			url = url + "?" + "info_hash=" + info_hash + "&peer_id=" + id + "&port=" + port + "&uploaded=0&downloaded=" + dL + "&left=" + left + "&event=" + event;
			System.out.println("notify stopped url:" + url);
			URL trackerURL = new URL(url);
			//Open connection with tracker and intercept response
			URLConnection trackerConnection = trackerURL.openConnection();
			BufferedInputStream responseStream = new BufferedInputStream(trackerConnection.getInputStream());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int read;
			while((read = responseStream.read()) != -1) {
			    baos.write(read);
			}
			baos.close();
			responseStream.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public void periodicUpdate(){
		
		try {
			String  info_hash, port, left, dL;
			//properly encode info_hash
			info_hash = new String(torrentInfo.info_hash.array(), "ISO-8859-1");
			info_hash = URLEncoder.encode(info_hash, "ISO-8859-1");
			
			port = Integer.toString(6881);
			//left = Integer.toString(torrentInfo.file_length);
			
			byte[]  pieces= SharedData.getInstance().getAvailablePiece(saveFile);
			int downloaded = SharedData.getInstance().getDownloaded();
			if (pieces[pieces.length - 1] == 1){
				downloaded = torrentInfo.piece_length * (downloaded - 1);
				downloaded = downloaded + fPieceLength;
			}
			else{
				downloaded = torrentInfo.piece_length * downloaded;
			}
			
			dL = Integer.toString(downloaded);
			
			left = Integer.toString(torrentInfo.file_length - downloaded);

			//Build URL
			String url = torrentInfo.announce_url.toString();
			url = url + "?" + "info_hash=" + info_hash + "&peer_id=" + id + "&port=" + port + "&uploaded=0&downloaded=" + dL + "&left=" + left + "&event=" + event;
			System.out.println("update url:" + url);
			URL trackerURL = new URL(url);
			//Open connection with tracker and intercept response
			URLConnection trackerConnection = trackerURL.openConnection();
			BufferedInputStream responseStream = new BufferedInputStream(trackerConnection.getInputStream());
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int read;
			while((read = responseStream.read()) != -1) {
			    baos.write(read);
			}
			baos.close();
			responseStream.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		
		//responseBytes = baos.toByteArray();	
	}
	
	public TorrentInfo getTorrentInfo(){
		return torrentInfo;
	}
	
	public String getId(){
		return id;
	}
	
	public int getThreadCount(){
		return threadCount;
	}
	
	public String getSaveFile(){
		return saveFile;
	}
	
	public String getDownloadFile(){
		return downloadFile;
	}
	
	public int getTotalPieces(){
		return totalPieces;
	}
	
	public int getFPieceLength(){
		return fPieceLength;
	}
	
	public String getEvent(){
		return event;
	}
	
	public boolean haveAllPieces(){
		byte[] temp = SharedData.getInstance().getAvailablePiece(saveFile);
		int downloaded = SharedData.getInstance().getDownloaded();
		if (downloaded == totalPieces){
			return true;
		}
		else {
			return false;
		}
	}
	
	public void setEndTime(){
		endTime = System.nanoTime();
	}
	
	public void getTotalTime(){
		long endTime = System.nanoTime();
		System.out.println("Download time: " + ((endTime - startTime)) / 1000000000 + " seconds");
		notifyTrackerOfCompletion();
		endTimer.cancel();
		endTimer.purge();
	}
	
	public void timeDL(){
		endTimer = new Timer();
		endTimer.schedule(new CheckDone(this), 0, 1000);
	}

	public static void main(String[] args) {
		//Get path of torrentFile and set path for save file
		ClientManager manager = new ClientManager(args);
		manager.findPeers();
		//update tracker thread
		manager.startTime = System.nanoTime();
		Timer update = new Timer();
		//System.out.println("interval: " + manager.interval * 1000 );
		update.schedule(new PeriodicUpdate(manager), 0, manager.interval * 1000);
		manager.timeDL();
		manager.stop();
		manager.download();
		manager.periodicUpdate();
		manager.listen();
		
		
		try {
			for (Thread thread : manager.downloadThreads){
				thread.join();
			}
			
		} catch (Exception e){
			e.printStackTrace();
		}
		
		try {
			for (Thread thread : manager.uploadThreads){
				thread.join();
			}
			
		} catch (Exception e){
			e.printStackTrace();
		}
		
		
		//close connections when downloads complete
	}

}
