package bittorrent;

import java.util.TimerTask;

public class EndConnection extends TimerTask {
	
	private UploadThread thread;
	
	public EndConnection(UploadThread thread){
		this.thread = thread;
	}
	
	public void run(){
		thread.checkConnection();
	}
}
