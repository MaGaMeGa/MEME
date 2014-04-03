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

/** This class represents a request queue.<br>
 *	Invariants: head          == &lt;first&gt; 
 *	 			head.prev     == &lt;last&gt;
 *	  			&lt;last&gt;.next   == null
 */ 
public class RQ {

	/** The head of the request list. */
	private volatile Request	head = null;
	/** The request producer object. */
	private final LOPProducer	gui;
	/** The current request. */
	private volatile Request	current = null;

	//=========================================================================
	//	Public

	//-------------------------------------------------------------------------
	public RQ(LOPProducer gui) {
		this.gui = gui;
	}

	//-------------------------------------------------------------------------
	/** Adds task 'r' to the queue.
	 * @param r task
	 * @param taskName name of the task
	 * @return the request created from the task
	 */
	public synchronized Request add(Callable<Object> r, String taskName) {
		if (r == null)
			return null;
		Request ans = new Request(r, taskName);
		add(ans);
		if (r instanceof IKnowsReqAndGUI)
			((IKnowsReqAndGUI)r).setReq(ans);
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Returns whether the queue is empty or not. If the parameter is true then
	 *  it considers the current request too, otherwise not.
	 */
	public synchronized boolean isEmpty(boolean considerCurrent) {
		return considerCurrent ? (current == null && head == null) : (head == null);
	}

	//-------------------------------------------------------------------------
	public Request getCurrent() {
		return current;
	}

	//-------------------------------------------------------------------------
	/** Get out and returns the current request. The head of the request list will
	 *  be the next current request.
	 */ 
	public synchronized Request consume() {
		Request ans = current;
		current = null;
		if (ans != null)
			notifyAll();				// wake up pop()
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Replaces current request with head. 
	 * It does this only if the current != null and the list is not empty, otherwise
	 * wait while these conditions will be valid. It also get out the head request
	 * from the list.
	 * If 'stop' is true then returns null.   
	 */
	public synchronized Request pop(AtomicBoolean stop) throws InterruptedException {

		do {
			if (stop.get()) return null;
			if (current != null || head == null)
				wait();
			else break;
		} while (true);

		current = head;
		current.remove();
		current.getGUI().setTaskName(current.getTaskName());
		return current;
	}

	//-------------------------------------------------------------------------
	/** This class represents a request for the worker thread. */ 
	public class Request {

		/** The task object. */
		public final Callable<Object>	task;

		/** The result of the task (if any). */
		private volatile Object		result = null;
		/** The exception thrown by the task (if any). */
		volatile Throwable				error = null;
		/** The previous request in the list. */
		private volatile Request		prev;
		/** The next request in the list. */
		private volatile Request 	    next;			// these are synchronized using 'RQ.this'
		private volatile String		taskName;			// synchronized using 'RQ.this'

		/** Flag that determines whether the request displays the errors or not. */
		private volatile boolean		errorDisplay = true;

		//-------------------------------------------------------------------------
		Request(Callable<Object> r, String name) {
			task  = r;
			prev  = this;
			next  = null;
			taskName = name;
		}

		//-------------------------------------------------------------------------
		/**
		 * Removes 'this' Request from the queue.
		 * Note: if 'this' request is already running, the removal does not stop it.
		 * Note2: this method is not public because currently only pop() may use it.
		 *        This is because the current implementation of LOPProducer (timer-management)
		 *        cannot handle early-removals properly. 
		 */
		void remove() {
			synchronized (RQ.this) {
				if (prev != null) {		// prev==null means that it has already been removed
					if (next != null)
						next.prev = prev;
		
					if (prev.next != null) {
						assert head != this;
						prev.next = next;
					} else {
						assert head == this;
						head = next;
						assert (head == null || head.prev != null);
					}
					prev = next = null;
				}
			}
		}

		//-------------------------------------------------------------------------
		/** Returns the request producer object. */
		public LOPProducer	getGUI()		{ return gui; }
		public Object		getResult()		{ return result; }
		public Throwable	getError()		{ return error; }

		public boolean		isErrorDisplay()			{ return errorDisplay; }
		public void		setErrorDisplay(boolean b)	{ errorDisplay = b; }

		public String		getTaskName()				{ return taskName; }
		public void		setTaskName(String s) {
			synchronized (RQ.this) {
				taskName = s;
				if (getCurrent() == this)
					gui.setTaskName(taskName);
			}
		}

		/** Returns whether the task of 'this' request is done or not. */
		public boolean isDone()	{ synchronized (RQ.this) { return (prev == null && current != this); } }

		//-------------------------------------------------------------------------
		// It may return a Callable<Object>, or a Runnable, or an ITRunnable.
//		public Object getTask() {
//			return (task instanceof LOPProducer.RunnableWrapper) ? ((LOPProducer.RunnableWrapper)task).r : task;
//		}

		//-------------------------------------------------------------------------
		//	For the Model thread only	
		/** Executes the task. */
		void run() {
			try {
				gui.checkUserBreak();
				result = task.call();
			} catch (Throwable t) {
				error = t;
			}			
		}
	}

	
	//=========================================================================
	//	Internals

	
	//-------------------------------------------------------------------------
	/** Adds a request to the queue. */
	private synchronized void add(Request req) {
		if (head == null) {
			head = req;
			notifyAll();			// wake up pop()
		} else {
			req.prev = head.prev;
			assert req.prev != null;
			req.prev.next = req;
			head.prev = req; 
		}
	}
}
