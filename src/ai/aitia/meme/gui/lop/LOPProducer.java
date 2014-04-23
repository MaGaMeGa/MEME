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

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import ai.aitia.meme.Logger;
import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.utils.Utils;
import ai.aitia.meme.utils.GUIUtils;

/** This class represents a  producer that put the tasks to the request queue.
 * 	To be used from the GUI thread only (EDT)
 */
public class LOPProducer implements ActionListener, IProgressUpdate {
	
	static final int POPUP_DELAY	= 2000;
	static final int UPDATE_DELAY	= 500;

	/** The request queue object. */
	final RQ					queue;		// accessed by LOPCustomers, too

	/** Flag that determines whether the user aborted the current task or not. */
	private volatile boolean	userBreak	= false;	// any thread
	/** The progress object of the current task. */
	private volatile Progress	pr			= null;		// any thread
	private JFrame				parent		= null;		// any thread
	private volatile boolean	isVisible	= false;	// any thread 
	private final AtomicLong	displayTime	= new AtomicLong(0); // any thread 

	private Timer				timer		= null;		// EDT only
	private LOPDialog			window		= null;		// EDT only

	//=========================================================================
	//	Public interface - EDT only

	//-------------------------------------------------------------------------
	public LOPProducer() {
		queue = new RQ(this);
	}

	//=========================================================================
	//	Public interface - any threads
	
	public void setAbortable(boolean abortable) {
		window.getJAbortButton().setVisible(abortable);
	}

	//-------------------------------------------------------------------------
	/** Puts a new request to the request queue. The request is defined by 'taskName'
	 *  and 'r'. The method returns immediately.
	 * @return the request  
	 */
	public RQ.Request begin(String taskName, Callable<Object> r) {
		if (r == null)
			return null;
		RQ.Request ans;
		// The following synchronization block ensures that EDT will execute 
		// this.startTimer() _before_ this.finished(). In other words, the new
		// request cannot be retrieved from the queue (and therefore cannot be
		// executed and finished) until the timer start message is sent to the 
		// EDT thread.
		//
		synchronized (queue) {
			ans = queue.add(r, taskName);
			startTimer();
		}
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Puts a new request to the request queue. The request is defined by 'taskName'
	 *  and 'r'. The method returns immediately.
	 * @return the request  
	 */
	public RQ.Request begin(String taskName, Runnable r) {
		return (r == null) ? null : begin(taskName, new RunnableWrapper(r));
	}
	//-------------------------------------------------------------------------
	/** Puts a new request to the request queue. The request is defined by 'taskName'
	 *  and 'r'. The method returns immediately.
	 * @return the request  
	 */
	public RQ.Request begin2(String taskName, ITRunnable r) {
		return (r == null) ? null : begin(taskName, new RunnableWrapper(r));
	}

	public static final java.lang.reflect.Method BEGIN_CALLABLE;
	public static final java.lang.reflect.Method BEGIN_RUNNABLE;
	static {
		java.lang.reflect.Method tmp[] = { null, null };
		try {
			tmp[0] = LOPProducer.class.getMethod("begin", new Class[] {String.class, Callable.class});
			tmp[1] = LOPProducer.class.getMethod("begin", new Class[] {String.class, Runnable.class});
		} catch (NoSuchMethodException e) {
		}
		BEGIN_CALLABLE   = tmp[0];
		BEGIN_RUNNABLE   = tmp[1];
	}


	//-------------------------------------------------------------------------
	/* 
	 * Egymasba agyazott execute()-ok eseten a taszkok a beadas sorrendjeben 
	 * futnak le, maguk az execute()-ok viszont forditott sorrendben ternek 
	 * vissza (eloszor a legtuolso ter vissza, s majd utana a korabbiak; tehat 
	 * a legelso bevarja a legutolsot). Ha ezt EDT-ben csinaljuk, akkor nagyon
	 * megterheli a stack-et, ezert "rapid", "gyorstuzelesu" taszkokat EDT-ben 
	 * execute-al rekurzive futtatni nem szabad.
	 */
	/** Puts a new request to the request queue. The request is defined by 'taskName'
	 *  and 'r'. The method is blocked until the task is finished.
	 * @param errorDisplay does the task display the errors?  
	 * @return the request  
	 */
	@SuppressWarnings("cast")
	protected RQ.Request execute(String taskName, boolean errorDisplay, final Callable<Object> r) throws Exception 
	{
		if (r == null) return null;
		final RQ.Request req;
		if (EventQueue.isDispatchThread()) {
			// [EDT]
			if (useModalLoop == null) {
				try {
					GUIUtils.modalEventLoop(null, null, new Callable<Boolean>() {
						public Boolean call() throws Exception { return false; }
					});
					useModalLoop = Boolean.TRUE;
				} catch (UnsupportedOperationException e) {
					useModalLoop = Boolean.FALSE;
				}
			}

			// addig ne kezdodjon el a feldolgozasa amig errorDisplay-t be nem allitottuk
			// (nehogy o is azzal kezdje hogy atallitja s aztan mi meg itt felulvagjuk)
			synchronized (queue) {
				req = begin(taskName, (Callable<Object>)r);
				req.setErrorDisplay(errorDisplay);
			}
			if (useModalLoop) {
				// getJAbortButton() nem Container, ezert gyakorlatilag letilt mindenkit
				GUIUtils.modalEventLoop(getWindow().getJAbortButton(), null, new Callable<Boolean>() {
					public Boolean call() throws Exception { return !req.isDone(); }
				});
			} else if (isWindowVisible()) {
				// Ha mar most is kint van az ablak akkor 'req' bizonyara varakozo
				// pozicioba kerult. Megvarjuk amig a kintlevo ablak eltunik, es ujra
				// megjelenitjuk. Ha kell ismeteljuk mindaddig, amig 'req' sorra nem kerul.
				while (!req.isDone()) {
					getWindow().setVisible(false);
					getWindow().setVisible(true);	// this blocks!
				}
			} else {
				showProgressNow();
			}
		} else {
			// [Model thread]
			req = queue.new Request(r, taskName);
			req.setErrorDisplay(errorDisplay);
			if (r instanceof IKnowsReqAndGUI)
				((IKnowsReqAndGUI)r).setReq(req);
			req.run();
			if (r instanceof ILopListener) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() { 
							try { ((ILopListener)r).finished(); }
							catch (Exception e) { req.error = e; }
						}
					});
				} catch (InvocationTargetException ite) {
					req.error = ite.getCause();
				}
			}
			if (req.getError() != null && req.isErrorDisplay()) {
				try {
					SwingUtilities.invokeAndWait(new Runnable() {
						public void run() {
							Logger.logExceptionCallStack(req.getError());
							Utils.invokeLater(this, "setTaskName", req.getTaskName());
							appendErrorInfo(req.getError());
							if (queue.isEmpty(true)) {
								getWindow().begin();	// this may block!
							} else {
								getWindow().done();		// this may block!
							}
						}
					});
				} catch (InvocationTargetException ite) {
					Utils.rethrow(ite.getCause());
				}
			}
		}
		if (req.getError() != null && !req.isErrorDisplay()) {
			Utils.rethrow(req.getError());
		}
		return req;
	}
	static Boolean useModalLoop = null;

	//-------------------------------------------------------------------------
	/** Puts a new request to the request queue. The request is defined by 'taskName'
	 *  and 'r'. The method is blocked until the task is finished.
	 * @return the result of the task  
	 */
	/* Ha hiba van, nem jelzi ki a progress bar ablakban, hanem tovabbdobja */ 
	public Object execute(String taskName, Callable<Object> r) throws Exception {
		RQ.Request req = execute(taskName, false, r);
		return (req == null) ? null : req.getResult();
	}
	/** Puts a new request to the request queue. The request is defined by 'taskName'
	 *  and 'r'. The method is blocked until the task is finished.
	 */
	public void execute(String taskName, Runnable r) throws Exception {
		if (r != null) execute(taskName, new RunnableWrapper(r));
	}
	/** Puts a new request to the request queue. The request is defined by 'taskName'
	 *  and 'r'. The method is blocked until the task is finished.
	 */
	public void execute2(String taskName, ITRunnable r) throws Exception {
		if (r != null) execute(taskName, new RunnableWrapper(r));
	}
	/** Puts a new request to the request queue. The request is defined by 'taskName'
	 *  and 'e'. The method is blocked until the task is finished.
	 * @return the result of the task  
	 */
	public Object execute(String taskName, final java.beans.Expression e) throws Exception {
		return (e == null) ? null : execute(taskName, new Callable<Object>() { 
			public Object call() throws Exception { return e.getValue(); }
		});
	}
	/** Puts a new request to the request queue. The request is defined by 'taskName',
	 *  'target', 'methodName' and 'args'. The method is blocked until the task is
	 *   finished.
	 * @return the result of the task  
	 */
	public Object execute(String taskName, Object target, String methodName, Object...args) throws Exception {
		return (target == null) ? null : 
					execute(taskName, new java.beans.Expression(target, methodName, args));
	}

	//-------------------------------------------------------------------------
	/* 
	 * Ha hiba van, kijelzi a progressbar ablakban es Throwable-kent visszaadja.
	 * (Kivetel, ha a hibakijelzo invokeAndWait()-ben keletkezik a hiba, 
	 *  mert akkor ezt nem tudja kijelezni, csak visszaadja.)
	 */
	/** Puts a new request to the request queue. The request is defined by 'taskName'
	 *  and 'r'. The method is blocked until the task is finished. If any exception 
	 *  occures during the processing, it displays on the progress dialog and gives back
	 *  in 'th'
	 * @return the result of the task  
	 */
	public Object executeNE(String taskName, Throwable[] th, Callable<Object> r) {
		RQ.Request req = null;
		Throwable e = null;
		try {
			req = execute(taskName, true, r);	// true: hiba megjelenitese
		} catch (Throwable t) {
			Logger.logExceptionCallStack(t);
			e = t;
		}
		if (th != null && th.length > 0) th[0] = e;
		return (req == null) ? null : req.getResult();
	}
	/** Puts a new request to the request queue. The request is defined by 'taskName'
	 *  and 'r'. The method is blocked until the task is finished. 
	 * @return the error that occures during the processing or null
	 */
	public Throwable executeNE(String taskName, Runnable r) {
		Throwable ans[] = { null };
		executeNE(taskName, ans, (r == null) ? null : new RunnableWrapper(r));
		return ans[0];
	}
	/** Puts a new request to the request queue. The request is defined by 'taskName'
	 *  and 'r'. The method is blocked until the task is finished. 
	 * @return the error that occures during the processing or null
	 */
	public Throwable execute2NE(String taskName, ITRunnable r) {
		Throwable ans[] = { null };
		executeNE(taskName, ans, (r == null) ? null : new RunnableWrapper(r));
		return ans[0];
	}
	/** Puts a new request to the request queue. The request is defined by 'taskName'
	 *  and 'e'. The method is blocked until the task is finished. If any exception 
	 *  occures during the processing, it displays on the progress dialog and gives back
	 *  in 't'
	 * @return the result of the task  
	 */
	public Object executeNE(String taskName, Throwable[] t, final java.beans.Expression e) {
		return (e == null) ? null : executeNE(taskName, t, new Callable<Object>() { 
			public Object call() throws Exception { return e.getValue(); }
		});
	}
	/** Puts a new request to the request queue. The request is defined by 'taskName',
	 *  'target', 'methodName' and 'args'. The method is blocked until the task is finished.
	 *  If any exception occures during the processing, it displays on the progress
	 *  dialog and gives back in 't'
	 * @return the result of the task  
	 */
	public Object executeNE(String taskName, Throwable[] t, Object target, String methodName, Object...args) {
		return (target == null) ? null : 
					executeNE(taskName, t, new java.beans.Expression(target, methodName, args));
	}


	//-------------------------------------------------------------------------
	public JFrame getParent() {
		return parent;
	}
	public void setParent(JFrame f) {
		parent = f;			// comes to effect next time when the window is displayed
	}

	
	//-------------------------------------------------------------------------
	/** Checks whether the current task is aborted by the user. It throws UserBreakException
	 *  in this case.
	 */
	public void checkUserBreak() throws UserBreakException {
		if (userBreak)
			throw new UserBreakException();
	}
	public void setUserBreak(boolean b) {
		userBreak = b;
	}
	public boolean isUserBreak() {
		return userBreak; 
	}

	//-------------------------------------------------------------------------
	/** 
	 * If previousTitle!=null and not called from the EDT thread, blocks until
	 * the EDT thread gets the title into 'previousTitle[0]'. Otherwise returns
	 * immediately.
	 * @param title null means the default title.
	 */
	public void setTitle(final String title, final String[] previousTitle) {
		if (EventQueue.isDispatchThread()) {
			if (previousTitle != null)
				previousTitle[0] = getWindow().getTitle();
			if (title == null) getWindow().resetTitle(); else getWindow().setTitle(title);
		}
		else {
			Runnable r = new Runnable() { public void run() { setTitle(title, previousTitle); } };
			if (previousTitle == null) 
				SwingUtilities.invokeLater(r);
			else try {
				SwingUtilities.invokeAndWait(r);
			} catch (Exception e) {		// InterruptedException, InvocationTargetException
				Logger.logExceptionCallStack("LOPProducer.getTaskName()", e);
			}
		}
	}

	//-------------------------------------------------------------------------
	public void setTaskName(final String name) {
		if (EventQueue.isDispatchThread())
			getWindow().setTaskName(name);
		else SwingUtilities.invokeLater(new Runnable() {
			public void run() { setTaskName(name); }
		});
	}
	//-------------------------------------------------------------------------
	public String getTaskName() {
		if (EventQueue.isDispatchThread())
			return getWindow().getTaskName();

		final String ans[] = { "" };
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() { ans[0] = getTaskName(); }
			});
		} catch (Exception e) {		// InterruptedException, InvocationTargetException
			Logger.logExceptionCallStack("LOPProducer.getTaskName()", e);
		}
		return ans[0];
	}

	//-------------------------------------------------------------------------
	/**
	 * Updates the progressbar.
	 * <dl compact><dt>
	 * (-1,-1)</dt><dd>        reset (time restart, current=0, unknown length)
	 * </dd><dt>
	 * (-1, end)</dt><dd>      set length only (in units). 0 means unknown.
	 * </dd><dt>
	 * (current, -1)</dt><dd>  set position only
	 * </dd><dt>
	 * (current, end)</dt><dd> set both position and length. end=0 means unknown.
	 *                         current=0 causes time restart.
	 * </dd></dl>
	 */
	public synchronized void progress(double current, double end) {
		if (pr == null) {
			pr = new Progress();
			pr.setListener(this);
		}
		if (current < 0 && end < 0) {
			pr.restart(end);
		}
		pr.update(current, end);
	}

	//-------------------------------------------------------------------------
	/** Identical to <pre> checkUserBreak(); progress(current, -1); </pre> */
	public void progress(double current) throws UserBreakException {
		checkUserBreak();
		progress(current, -1);
	}


	/** Returns the current position of progress. */
	public synchronized double getProgressCurrent()	{ return (pr == null) ? -1 : pr.getCurrent(); }	
	/** Returns the length of progress. */
	public synchronized double getProgressEnd()		{ return (pr == null) ? -1 : pr.getEnd(); }


	//-------------------------------------------------------------------------
	/**
	 * If called from the Model thread, <i>blocks until</i> the progress window
	 * is displayed (can be used to ensure that the user don't make simultaneous
	 * modifications to the Model).<br>
	 * If called from the GUI thread (EDT), <i>blocks <b>to</b></i> display the 
	 * progress window, i.e. it will not return until the task is finished  
	 * in the Model thread (do not call it when no task is running because then  
	 * it will never return...). If the window is already displayed, it does not 
	 * block. To avoid this, call it immediately after launching the task.<br>
	 * Note that this method is somewhat incongruos in LOPProducer since this 
	 * class was designed to delay the popup of the progress window. I admit that 
	 * in some cases it makes the life too difficult and its more straightforward 
	 * to pop up the window immediately and use it to block the EDT thread - this
	 * method is a fallback for these cases.
	 */
	public void showProgressNow() {
		if (!isWindowVisible()) {
			displayTime.set(System.currentTimeMillis());
			if (EventQueue.isDispatchThread()) {
				setTimerForUpdate();
				getWindow().begin();	// this blocks! (This displays the window.)
			} else {
				Utils.invokeLater(this, "showProgressNow");
				try { SwingUtilities.invokeAndWait(new LongRunnable()); }
				catch (InterruptedException e) {}
				catch (InvocationTargetException e) {}
			}
		}
	}

	//-------------------------------------------------------------------------
	/**
	 * Returns the amount of time left until the progress window is displayed.
	 * This is an estimate value which may change at any time.
	 * @return Zero means that the window is already visible. A negative value 
	 *          means that the time is unknown (no one asked to display the window).
	 *          A positive value means that displaying is in progress: a timer has
	 *          been set to display the window. However, either it may be cancelled,
	 *          or the window may be forced to be visible sooner.
	 */
	public long getTimeLeftUntilWindowAppears() {
		if (isWindowVisible())
			return 0;
		return displayTime.get() - System.currentTimeMillis(); 
	}

	//=========================================================================
	//	Internals	

	//-------------------------------------------------------------------------
	// EDT only - intended to be called from LOPDialog only
	void setVisible(boolean show) {
		isVisible = show;
		if (!show) {
			queue.consume();
			if (!queue.isEmpty(true))
				setTimerForUpdate();
		}
	}

	//-------------------------------------------------------------------------
	// EDT only
	private LOPDialog getWindow() {
		if (window != null && window.getParent() != getParent()) {
			window.dispose();
			window = null;
		}
		if (window == null) {
			window = new LOPDialog(this);
		}
		return window;
	}

	//-------------------------------------------------------------------------
	// Any thread
	private boolean isWindowVisible() {
		if (EventQueue.isDispatchThread())
			isVisible = (window == null) ? false : window.isVisible();
		return isVisible;
	}

	//-------------------------------------------------------------------------
	// EDT only
	private Timer getTimer() {
		if (timer == null) {
			timer = new Timer(POPUP_DELAY, this);
			timer.setRepeats(true);
		}
		return timer;
	}
	
	//-------------------------------------------------------------------------
	// EDT only
	private void setTimerForUpdate() {
		getTimer().setInitialDelay(UPDATE_DELAY);
		timer.setDelay(UPDATE_DELAY);
		timer.restart();
	}

	//-------------------------------------------------------------------------
	// Any thread
	/** Starts the timer. If the time achieves the POPUP_DELAY and the current task
	 *  hasn't finished yet, a progress dialog appears.
	 */
	private void startTimer() {
		if (EventQueue.isDispatchThread()) {
			if (isWindowVisible()) {
				setTimerForUpdate();
				getWindow().begin();		// will not block
			} else {
				boolean restart = true;
				if (timer == null) {
					getTimer().start();
				} else if (!timer.isRunning()) {
					timer.setInitialDelay(POPUP_DELAY);
					timer.setDelay(POPUP_DELAY);
					timer.restart();
				} else {
					restart = false;
				}
				if (restart) {
					displayTime.set(System.currentTimeMillis() + POPUP_DELAY);
					progress(-1, -1);
				}
				GUIUtils.setBusy(getParent(), true);
			}
		}
		else {
			displayTime.set(System.currentTimeMillis() + POPUP_DELAY); 
			SwingUtilities.invokeLater(new Runnable() {
				public void run() { startTimer(); }
			});
		}
	}

	//-------------------------------------------------------------------------
	// Triggered on timer events
	// EDT only
    public void actionPerformed(ActionEvent evt) {
    	if (isWindowVisible()) {
    		// The window is already visible - update it regularly (elapsed time, progress bar etc.)
			// It is necessary because the worker thread may not send progress info regularly.
    		// 
    		if (pr != null)
    			pr.update();
			setTimerForUpdate();
    	} else {
    		long dt = displayTime.get();	// used to detect if a new Request is submitted before displayTime is changed below  
    		if (queue.isEmpty(true)) {
    			// All tasks have been finished meantime. Do not display the window (yet). 
    			//
    			if (timer != null) timer.stop();
    			displayTime.compareAndSet(dt, 0);
    		} else {
    			// Display the window and continue the timer to update the window regularly.
    			//
    			setTimerForUpdate();
	    		getWindow().begin();		// this blocks! (This displays the window.)
    		}
    	}
    }

    //-------------------------------------------------------------------------
	/** 
	 * Called when 'req' is finished, either successfully or with error.<br>
	 * Note: the current element of the queue is consumed in the {@link #setVisible()}
	 * method, not here. setVisible(false) is called when the window is closing.
	 * Until that the Model thread is blocking and cannot access any new Request.
	 * This is because the Abort button should affect only that task which is 
	 * displayed in the progress window.
	 */
    // [EDT only]
	void finished(RQ.Request req)
	{
		assert(queue.getCurrent() == req);

		if (req.getError() != null) {
			Logger.logExceptionCallStack(req.getError());
			if (req.isErrorDisplay())
				appendErrorInfo(req.getError());
		}

		// Execute user-specified finishing code
		// Note that the user-break flag is STILL set if the Abort button has been pressed
		//
		if (req.task instanceof ILopListener) {
			try { ((ILopListener)req.task).finished(); }
			catch (Throwable t) {
				Logger.logExceptionCallStack(t);
				if (req.isErrorDisplay())
					appendErrorInfo(t);
				else
					req.error = t;
			}
		}

		long dt = displayTime.get();
		if (queue.isEmpty(false) || getWindow().isInfo()) {		// stop the timer if displaying error,
			displayTime.compareAndSet(dt, 0);					// this.setVisible(false) will restart it.
			if (timer != null)
				timer.stop();
		}

		getWindow().done();		// this may block! (if it needs to display the window due to error info)
								// this will call this.setVisible(false)
	}

    //-------------------------------------------------------------------------
	/** Appends error info to the information text of the progress dialog. */
	void appendErrorInfo(Throwable t) {
		if (t != null) {
			if (t instanceof OutOfMemoryError && !MEMEApp.getDbSettings().isExternal()
					&& MEMEApp.getDatabase().getSQLDialect().isHSQLDB()) {
				MEMEApp.userErrors("Fatal error",
						"Out of memory. To avoid corruption of data in the database, " +
						"the program will exit now."
				);
				System.exit(MEMEApp.EXIT_CODE_OUTOFMEMORY);
			}
			String s = Utils.getLocalizedMessage(t);
			if (s == null)
				s = "<<internal error>>";
			if (!Utils.isHTML(s))
				s = Utils.htmlPage("<font color=red>" + Utils.htmlQuote(s));
			getWindow().appendInfo(s);
		}
	}

    //-------------------------------------------------------------------------
	public void onProgressUpdate(final double percent, final long elapsed, final String left) {
		if (EventQueue.isDispatchThread())
			getWindow().onProgressUpdate(percent, elapsed, left);
		else SwingUtilities.invokeLater(new Runnable() {
			public void run() { onProgressUpdate(percent, elapsed, left); }
		});
	}

	//-------------------------------------------------------------------------
	/** Wrapper class that enables to manage all type of task as a LongCallable 
	 *  task.
	 */
	static class RunnableWrapper extends LongCallable<Object> {
		/** The task. */
		final Object r;
		RunnableWrapper(Runnable r)		{ this.r = r; }
		RunnableWrapper(ITRunnable r)	{ this.r = r; }

		@Override public Object call() throws Exception {
			if (r instanceof ITRunnable)
				((ITRunnable)r).trun();
			else if (r instanceof Runnable)
				((Runnable)r).run();
			return null;
		}

		@Override public void finished() throws Exception {
			assert(getReq() != null && getReq().task == this);
			if (r instanceof ILopListener)
				((ILopListener)r).finished();
		}

		@Override public void setReq(RQ.Request req) {
			super.setReq(req);
			if (r instanceof IKnowsReqAndGUI)
				((IKnowsReqAndGUI)r).setReq(req);
		}

		@Override public RQ.Request getReq() {
			if (r instanceof IKnowsReqAndGUI)
				return ((IKnowsReqAndGUI)r).getReq();
			return super.getReq();
		}
	}
}
