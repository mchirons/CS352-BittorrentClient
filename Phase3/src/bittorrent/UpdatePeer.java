package bittorrent;

import java.util.Timer;
import java.util.TimerTask;

public class UpdatePeer extends TimerTask{
	
	private UploadThread thread;
	
	
	public UpdatePeer(UploadThread thread){
		this.thread = thread;
		
	}
	
	public void run(){
		thread.notifyPeer();
	}

}
