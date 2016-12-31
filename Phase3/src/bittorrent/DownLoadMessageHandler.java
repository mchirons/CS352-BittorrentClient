package bittorrent;

/*
 * Mark Hirons mch165 167008833
 */

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import GivenTools.ToolKit;
import GivenTools.TorrentInfo;

public class DownLoadMessageHandler {

	private int length;
	private int incomingMessageType;
	private byte[] payload;
	private DownloadThread dThread;
	private SharedData data = SharedData.getInstance();
	private ClientManager manager;
	
	
	
	public DownLoadMessageHandler(byte[] message, DownloadThread dThread){
		this.dThread = dThread;
		byte[] len = {message[0],  message[1], message[2], message[3]};
		this.length = Utilities.byteArrayToInt(len);
		if (length == 0){
			this.incomingMessageType = 10;
			this.payload = null;
		}
		else if (length == 1){
			this.payload = null;
			this.incomingMessageType = message[4];
		}
		else{
			this.incomingMessageType = message[4];
			this.payload = new byte[length - 1];
			System.arraycopy(message, 5, payload, 0, length - 1);
		}
		this.manager = dThread.getManager();
		
	}
	
	private byte[] choke(){
		byte[] message = {0, 0, 0, 1, 0};
		return message;
	}
	
	private byte[] unchoke(){
		byte[] message = {0, 0, 0, 1, 1};
		return message;
	}
	
	private byte[] interested(){
		dThread.switchIsInterested();
		byte[] message = {0, 0, 0, 1, 2};
		return message;
	}
	
	private byte[] not_interested(){
		byte[] message = {0, 0, 0, 1, 3};
		return null;
	}
	
	private byte[] have(){
		return null;
	}
	
	
	
	private byte[] request(){
		
		
		//save piece
    	//verify hash
    	//send another request if any left or hash is wrong
		
		int piece = data.getNextPiece(manager.getSaveFile(), dThread.peerPieces);
		if (piece == -1){
			return null;
		}
		dThread.setCurrentPiece(piece);
		
		System.out.println("DT " + dThread.getThreadName() + " requesting piece: " + piece);
		
		
		ByteBuffer buff = ByteBuffer.allocate(4);
		buff.putInt(piece);
		byte[] pieceIndex = buff.array();
		
		byte[] blockOffset = new byte[4];
		
		buff = ByteBuffer.allocate(4);
		buff.putInt(dThread.getTorrentInfo().piece_length);
		byte[] blockLength = buff.array();
		
		if (piece == manager.getTotalPieces() - 1){
			buff = ByteBuffer.allocate(4);
			//buff.putInt(16384);
			buff.putInt(manager.getFPieceLength());
			blockLength = buff.array();
		}
		
		byte[] message = new byte[17];
		message[0] = 0; message[1] = 0; message[2] = 0; message[3] = 0x0D; message[4] = 6;
		System.arraycopy(pieceIndex, 0, message, 5, 4);
		System.arraycopy(blockOffset, 0, message, 9, 4);
		System.arraycopy(blockLength, 0, message, 13, 4);
		return message;
	}
	
	
	public byte[] handleHave(){
		byte[] returnMessage = null;
		int index = Utilities.byteArrayToInt(payload);
		ArrayList<Integer> difference = new ArrayList<Integer>();
		if (!SharedData.getInstance().isPieceRequested(index)){
			byte[] savedPieces = SharedData.getInstance().getAvailablePiece(manager.getSaveFile());
			
			for (int i = 0; i < dThread.peerPieces.length; i++){
				if (savedPieces[i] == 0 && dThread.peerPieces[i] == 1){
					difference.add(i);
				}
			}
			if (difference.size() == 1 && difference.get(0) == index){
				returnMessage = request();
			}
		}
		return returnMessage;
	}
	
	private void createPeerPieces(){
		byte currentByte = 0;
		int j = 0;
		int k = 0;
		int l = 0;
		for (int i = 0; i < payload.length; i++){
			currentByte = payload[i];
			
			for( j = i * 8, k = 0, l = 7; j + k < dThread.peerPieces.length; k++, l--){
			
					dThread.peerPieces[j + k] = (byte)((currentByte >> l) & 1);
			}
			
		}
		/*
		for (int i = 0; i < dThread.peerPieces.length; i++){
			System.out.println("peerPieces[" + i +"]: " + dThread.peerPieces[i]);
		}
		*/
	}
	
	private void updatePeerPieces(){
		int index = Utilities.byteArrayToInt(payload);
		if (dThread.peerPieces[index] == 0){
			dThread.peerPieces[index] = 1;
		}
	}
	
	private byte[] keep_alive(){
		return null;
	}
	
	//I may change this to if else statements for future iterations
	public byte[] getMessageAction(){
		
		int s = this.incomingMessageType;
		System.out.println("DT " + dThread.getThreadName() + " Incoming message type: " + s);
		byte[] returnMessage = null;
		
		switch (s) {
        case 0: 
        	returnMessage = choke();
            break;
        case 1:
        	dThread.switchIsChoked();
        	returnMessage = request();
            break;
        case 2:
        	break;
        case 3: 
            break;
        case 4: 
        	//update record of available pieces
        	updatePeerPieces();
        	returnMessage = handleHave();
        	if (!manager.haveAllPieces())
        	/*
        	if (!manager.getEvent().equals("completed")){
        		returnMessage = request();
        	}
        	*/
            break;
        case 5: 
        	
        	if (manager.getEvent().equals("completed")){
        		return null;
        	}
        	createPeerPieces();
        	returnMessage = interested();
        	
            break;
        case 6:
        	break;
        case 7: 
        	verifyHash();
        	if (!manager.haveAllPieces()){
        		returnMessage = request();
        	}
        	else {
        		dThread.setCurrentPiece(-1);
        	}
            break;
        case 8: 
            break;
        case 9: 
            break;
        case 10:
        	/*
        	dThread.switchIsInterested();
        	if (dThread.getDownloadCompleted()){
        		return null;
        	}
        	returnMessage = interested();
        	*/
        	break;
        default: 
        	//System.out.println(dThread.getThreadName() + " Message id incorrect");
            break;
		}
		return returnMessage;
	}
	
	private void verifyHash(){
		
		
		
		byte[] piece = new byte[payload.length - 8];
		System.arraycopy(payload, 8, piece, 0, payload.length - 8);
		byte[] buf = {payload[0], payload[1], payload[2], payload[3]};
		int index = Utilities.byteArrayToInt(buf);
		System.out.println("DT " + dThread.getThreadName() + " Index of incoming piece:"+ index);
		System.out.println("DT " + dThread.getThreadName() + " currentPiece: " + dThread.getCurrentPiece());
		//System.out.println("correctHash: " );
		byte[] correctHash = dThread.getTorrentInfo().piece_hashes[dThread.getCurrentPiece()].array();
		//ToolKit.print(correctHash);
		//get hash of payload
		try{
			MessageDigest digest = MessageDigest.getInstance("SHA-1");
			digest.update(piece);
			byte[] info_hash = digest.digest();
			//System.out.println("info_hash: ");
			//ToolKit.print(info_hash);
			
			if (Arrays.equals(correctHash, info_hash)){
				System.out.println("DT " + dThread.getThreadName() + " Valid hash");
				data.saveToFile(manager.getSaveFile(), manager.getDownloadFile(), dThread.getCurrentPiece(), piece, dThread.getTorrentInfo().piece_length);
			}
			else{
				System.out.println("DT " + dThread.getThreadName() + " Invalid hash");
				//dThread.setPurgatory(null);
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		
	}
	
	
}
