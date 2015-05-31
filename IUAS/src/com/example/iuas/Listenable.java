/**
 * This class a abstract Class for the Listener.
 *
 * @author Martin Agreiter, Sabrina Schmitzer, Philipp Wirtenberger (alphabetical order)
 * @date 2015
 */

package com.example.iuas;

import java.util.ArrayList;

public abstract class Listenable {
	
	protected ArrayList<ThreadListener> listeners;
	
	/**
	 * every Activity that extends Listenable is able to add listener
	 * @param listener
	 */
	public synchronized void addListener(ThreadListener listener) {
	      listeners.add(listener);
	   }
	
	/**
	 * every Activity that extends Listenable is able to inform Listeners if there are listeners available
	 */

	protected void informListeners() {
		if (listeners != null) {
			for(ThreadListener listener : listeners)
				listener.onEvent();
		}
	}
}
