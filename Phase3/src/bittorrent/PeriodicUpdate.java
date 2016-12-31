package bittorrent;

import java.util.TimerTask;

public class PeriodicUpdate extends TimerTask{
	
	private ClientManager manager;

	public PeriodicUpdate(ClientManager manager){
		this.manager = manager;
	}
	
	public void run(){
		manager.periodicUpdate();
	}
}
