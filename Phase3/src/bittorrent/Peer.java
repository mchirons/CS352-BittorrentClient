package bittorrent;

/*
 * Mark Hirons mch165 167008833
 */

public class Peer {
	
	int port;
	String ipAddress;
	byte[] peer_id;
	long averagePing;

	public Peer(int port, String ipAddress, byte[] peer_id){
		this.port = port;
		this.ipAddress = ipAddress;
		this.peer_id = peer_id;
		this.averagePing = 0;
	}
	
}
