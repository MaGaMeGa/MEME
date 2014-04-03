/*******************************************************************************
 * Copyright (C) 2006-2013 AITIA International, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package ai.aitia.meme.gui.lop;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

/** This class represents a consumer that executes the tasks which are
 *  in the request queue.
 * 	To be used from Model thread(s) only. 
 */
public class LOPConsumer {

	/** The producr object. */
	private final LOPProducer		gui;
	/** Atomic flag that indicates for the consumer to stop the processing. */
	private final AtomicBoolean	stop = new AtomicBoolean(false);
	/** Flag that determines whether the comsumer is working or not. */
	private volatile boolean		working = false;

	//=========================================================================
	//	Public interface

	//-------------------------------------------------------------------------
	public LOPConsumer(LOPProducer gui) {
		this.gui = gui;
	}

	//-------------------------------------------------------------------------
	/**
	 * This method processes the Requests which were sent to the corresponding 
	 * LOPProducer ('gui'). It does not return until stop() is called. 
	 * This method is intended to be called at the end of the main() method  
	 * of the application. It is not reentrant!
	 */
	public void start() {
		stop.set(false);
		working = false;
		while (!stop.get()) {
			try {
				// The following call blocks until the current element of the queue is free,
				// and then moves the head of the queue to the current element of the queue.
				//
				final RQ.Request current = gui.queue.pop(stop);
				if (current == null)
					continue;

				// The following synchronization allows the GUI thread to force short delays here.
				synchronized (this) {
					working = true;
				}
				try {
					current.run();		// Monitors the user break and catches it. 
										// Note: the user break flag is not cleared here. 
										// The 'gui' thread is responsible for doing so.
				} finally {
					working = false;
				}

				// The GUI thread is responsible for making the current element of the queue
				// available (free) again (when processing the following message).
				// Note that the forthcoming pop() will block us until this happens.
				//
				SwingUtilities.invokeLater(new Finish(gui, current));

			} catch (InterruptedException e) {
				// The current task has been stopped.
				//
				Thread.interrupted();	// Clear the interrupted status and continue.
										// Use stop() to stop this method.
			}
		}
	}
	/** Internal task class. Its run() method calls the producer finished() method. */
	private static class Finish implements Runnable {
		LOPProducer gui;
		RQ.Request	req;
		Finish(LOPProducer gui, RQ.Request req) { this.gui = gui; this.req = req; } 
		public void run() { gui.finished(req); }
	}

	
	//-------------------------------------------------------------------------
	/** Stops the consumer. */
	public void stop() {
		stop.set(true);
		synchronized (gui.queue) {
			gui.queue.notifyAll();			// wake up pop() 
		}
	}

	//-------------------------------------------------------------------------
	/**
	 * This method is intended to be called from the GUI thread.
	 * Note: This function may report <code>true</code> instead of <code>false</code>, because the
	 *       <code>working=false</code> assignment is not synchronized in <code>start()</code>.
	 *       However, this function may never report <code>false</code> instead of <code>true</code>.      
	 */
	public synchronized boolean isWorking(final boolean wait) {
		final long PERIOD = 100;
		while (wait && working) {
			if (gui.getTimeLeftUntilWindowAppears() < PERIOD)
				break;
			try { Thread.sleep(PERIOD); }
			catch (InterruptedException e) { break; }
		}
		return working;
	}

	//-------------------------------------------------------------------------
	public IKnowsReqAndGUI getCurrent() {
		RQ.Request h = getCurrentReq();
		return (h == null || !(h.task instanceof IKnowsReqAndGUI)) ? null : (IKnowsReqAndGUI)h.task;
	}

	//-------------------------------------------------------------------------
	public Callable<Object> getCurrentCallable() {
		RQ.Request h = getCurrentReq();
		return (h == null) ? null : h.task; 
	}

	//-------------------------------------------------------------------------
	public RQ.Request getCurrentReq() {
		return gui.queue.getCurrent();
	}

}
