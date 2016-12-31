package bittorrent;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;


public class UploadMessageHandler {

	
	private int length;
	private int incomingMessageType;
	private byte[] payload;
	private UploadThread uThread;
	private SharedData data = SharedData.getInstance();
	private ClientManager manager;
	
	
	
	public UploadMessageHandler(byte[] message, UploadThread dThread){
		this.uThread = dThread;
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
		this.manager = uThread.getManager();
		
	}
	
	
	private byte[] piece(){
		
		
		byte[] index = new byte[4];
		System.arraycopy(payload, 0, index, 0, 4);
		
		byte[] begin = new byte[4];
		System.arraycopy(payload, 4, begin, 0, 4);
		
		byte[] length = new byte[4];
		System.arraycopy(payload, 8, length, 0, 4);
		
		if (!SharedData.getInstance().havePiece(manager.getSaveFile(), Utilities.byteArrayToInt(index))){
			System.out.println("Don't have requested piece");
			return null;
		}
		
		System.out.println("UT " + uThread.getUploadThread() + " sending piece: " + Utilities.byteArrayToInt(index) + " offset: " + Utilities.byteArrayToInt(begin) + " length: " + Utilities.byteArrayToInt(length));

		
		byte[] piece = SharedData.getInstance().getRequestedPiece(manager.getDownloadFile(), Utilities.byteArrayToInt(index), Utilities.byteArrayToInt(begin), Utilities.byteArrayToInt(length), uThread.getTorrentInfo().piece_length);
		
		int l = 9 + piece.length;
		ByteBuffer buff = ByteBuffer.allocate(4);
		buff.putInt(l);
		byte[] len = buff.array();
		
		
		
		byte[] message = new byte[4 + 9 + piece.length];
		
		System.arraycopy(len, 0, message, 0, 4);
		message[4] = 7;
		System.arraycopy(index, 0, message, 5, 4);
		System.arraycopy(begin, 0, message, 9, 4);
		System.arraycopy(piece, 0, message, 13, piece.length);
		
		
		return message;
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
		byte[] message = {0, 0, 0, 1, 2};
		return message;
	}
	
	private byte[] not_interested(){
		byte[] message = {0, 0, 0, 1, 3};
		return null;
	}

	private byte[] keep_alive(){
		return null;
	}
	
	//I may change this to if else statements for future iterations
	public byte[] getMessageAction(){
		
		int s = this.incomingMessageType;
		System.out.println("UT " + uThread.getUploadThread() + " Incoming message type: " + s);
		byte[] returnMessage = null;
		
		switch (s) {
        case 0: 
        	returnMessage = choke();
            break;
        case 1: 
        	break;
        case 2:
        	uThread.switchIsChoked();
        	returnMessage = unchoke();
        	break;
        case 3: 
            break;
        case 4: 
            break;
        case 5: 
        	
            break;
        case 6:
        	returnMessage = piece();
        	break;
        case 8: 
            break;
        case 9: 
            break;
        case 10:
        	break;
        default: 
        	//System.out.println(uThread.getThreadName() + " Message id incorrect");
            break;
		}
		return returnMessage;
	}
	
	
}
