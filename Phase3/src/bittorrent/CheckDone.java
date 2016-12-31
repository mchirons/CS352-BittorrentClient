package bittorrent;

import java.util.TimerTask;

public class CheckDone extends TimerTask{
	private ClientManager manager;
	public CheckDone(ClientManager manager){
		this.manager = manager;
	}
	
	public void run(){
		if (manager.haveAllPieces()){
			manager.setEndTime();
			manager.getTotalTime();
		}
		
	}
}
