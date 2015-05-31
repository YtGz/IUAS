package com.example.iuas;

import java.util.ArrayList;

public abstract class Listenable {
	
	protected ArrayList<ThreadListener> listeners;
	
	
	public synchronized void addListener(ThreadListener listener) {
	      listeners.add(listener);
	   }

	protected void informListeners() {
		if (listeners != null) {
			for(ThreadListener listener : listeners)
				listener.onEvent();
		}
	}
}
