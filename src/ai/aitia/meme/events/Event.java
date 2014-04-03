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
package ai.aitia.meme.events;

import java.lang.ref.WeakReference;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

import ai.aitia.meme.MEMEApp;

/**
 * Generic class for the event-observer pattern. It is 
 *   thread-safe,  
 *   supports both strong and weak references to listeners,
 *   supports delayed dispatching.
 * <br>
 * Listeners must implement a common interface, denoted by 'I'.
 * Only one method of 'I' can be used for event processing.
 * This method must be public. It should be specified by name
 * to the constructor, or may be auto-detected.   
 *
 * @param I  Type of listener objects. It cannot be 'Object'. 
 * @param E  Type of the argument of the listener method. 
 *            java.lang.Void means no argument.  
 */
public class Event<I, E> {
	//=========================================================================
	// Variables

	/** List of listener objects. */
	protected CopyOnWriteArrayList<I>					strong	= null;
	/** List of weak listener objects. */
	protected CopyOnWriteArrayList<WeakReference<I>>	weak	= null;
	/** Method for event processing. */
	final java.lang.reflect.Method					method;
	/** Pending event message. */
	private final AtomicReference<MsgWrapper<E>>	pending	= new AtomicReference<MsgWrapper<E>>();
	/** Flag that determines whether the method has argument or not. */
	private final boolean							noargs;
	/** Flag that determines how fireLater() works when called repeatedly before dispatching begins.
	 *  true means listeners will receive the first message only. false means listeners will receive the last
	 *  message only.
	 */ 
	private volatile boolean						dispatchFirstMessageOnly = true; 


	//=========================================================================
	// Constructor

	/** Constructor.
	 * Verifies that the listener interface 'I' really contains a method
	 * that accepts one argument of type 'E' (or no arguments if 'E' is Void).
	 * The first public method which accepts one argument of type 'E' is selected.
	 * (If 'E' is 'Void', then the method must be argument-less).  
	 * @param listenerClass		Must be I.class
	 * @param eventClass		Must be E.class
	 * @throws IllegalArgumentException if no suitable method can be found.
	 */
	public Event(Class<I> listenerClass, Class<E> eventClass)	{ this(null, listenerClass, eventClass); }
	
	/** Constructor.
	 * Verifies that the listener interface 'I' really contains a method
	 * that accepts one argument of type 'E' (or no arguments if 'E' is Void).
	 * @param methodName		Name of the event processing method within 'I'.
	 *                          If null or empty or ambiguous, the first public method
	 *                          which accepts one argument of type 'E' is selected.
	 *                          (If 'E' is 'Void', then the method must be argument-less)  
	 * @param listenerClass		Must be I.class
	 * @param eventClass		Must be E.class
	 * @throws IllegalArgumentException if no suitable method can be found.
	 */
	public Event(String methodName, Class<I> listenerClass, Class<E> eventClass) {
		if (methodName != null && methodName.length() == 0) methodName = null;
		int numArgs = (eventClass == null || eventClass == Void.TYPE || eventClass == Void.class) ? 0 : 1;  
		noargs = (numArgs == 0);
		java.lang.reflect.Method methodTmp = null;
		for (java.lang.reflect.Method m : listenerClass.getMethods()) {
			if (methodName != null && !m.getName().equals(methodName)) continue;
			Class<?>[] args = m.getParameterTypes();
			if (numArgs == 0) {
				if (args != null && args.length != 0) continue;
			}
			else if (args == null || args.length != numArgs || !args[0].isAssignableFrom(eventClass)) { 
				continue;
			}
			methodTmp = m;
			break;
		}
		method = methodTmp;
		if (methodTmp == null) {
			String s = methodName + '(' + (numArgs>0 ? eventClass.getName() : "") + ')'; 
 			throw new IllegalArgumentException("Method '" + s + "' does not exist (or isn't public) in " +
													listenerClass.getName());
		} else if (!java.lang.reflect.Modifier.isPublic(listenerClass.getModifiers()))
			suppressJavaAccessChecks(true);
	}


	//=========================================================================
	// Public methods

	//-------------------------------------------------------------------------
	/** Adds a new listener object to 'this'. */
	public synchronized <T extends I> T addListener(T observer) {
		if (observer != null && !contains(observer, STRONG)) {
			if (strong == null) strong = new CopyOnWriteArrayList<I>();
			strong.add(observer);
		}
		return observer;
	}

	//-------------------------------------------------------------------------
	/** Adds a new listener object to 'this' as a weak listener. */
	public synchronized <T extends I> T addWeakListener(T observer) {
		if (observer != null && !contains(observer, WEAK)) {
			if (weak == null) weak = new CopyOnWriteArrayList<WeakReference<I>>();
			weak.add(new WeakReference<I>(observer));
		}
		return observer;
	}

	//-------------------------------------------------------------------------
	/** Removes the listener object 'observer'. */
	public synchronized void removeListener(I observer) {
		contains(observer, STRONG + WEAK + REMOVE);
	}

	//=========================================================================
	// Protected methods

	//-------------------------------------------------------------------------
	/**
	 * Notifies all listeners immediately.
	 * If there's a pending fireLater() event, this method dispatches that
	 * or 'msg', with respect to getFireLaterMode().
	 * (I.e. if getFireLaterMode()==true, 'msg' is discarded and the
	 *  pending message is dispatched. Otherwise the pending message 
	 *  is discarded and only 'msg' is dispatched.) 
	 */
	protected void fire(E msg) {
		if (method == null) throw new NullPointerException();

		CopyOnWriteArrayList<I>					c_strong;
		CopyOnWriteArrayList<WeakReference<I>>	c_weak;
		synchronized (this) {
			c_strong	= strong;
			c_weak		= weak;
		}
		boolean doClean = false;

		MsgWrapper<E> tmp = pending.getAndSet(null);
		if (tmp != null && !dispatchFirstMessageOnly)	// TODO: nem inkabb '!' nelkul?
			msg = tmp.msg;

		Object args[] = { msg };
		if (noargs) args = null;

		if (c_strong != null) {
			for (I o : c_strong) { invoke(o, args); }
			c_strong = null;
		}		

		if (c_weak != null) {
			for (WeakReference<I> w : c_weak) {
				I o = (w != null) ? w.get() : null;
				if (o != null)
					invoke(o, args);
				else
					doClean = true;
			}
			c_weak = null;
		}

		if (doClean) doClean();	
	}

	//-------------------------------------------------------------------------
	/** Collects the listeners and weak listeners to the collections 'coll' and 'wcoll'.
	 */
	protected void collectObservers(java.util.Collection<I> coll, java.util.Collection<I> wcoll)
	{
		CopyOnWriteArrayList<I>					c_strong;
		CopyOnWriteArrayList<WeakReference<I>>	c_weak;
		synchronized (this) {
			c_strong	= strong;
			c_weak		= weak;
		}
		boolean doClean = false;

		if (c_strong != null) {
			coll.addAll(c_strong);
			c_strong = null;
		}		

		if (c_weak != null) {
			for (WeakReference<I> w : c_weak) {
				I o = (w != null) ? w.get() : null;
				if (o != null) 
					wcoll.add(o);
				else
					doClean = true;
			}
			c_weak = null;
		}

		if (doClean) doClean();	
	}

	//-------------------------------------------------------------------------
	/** It clears the weak listeners with null value from the list of the weak listeners. */
	private synchronized void doClean() {
		if (weak != null) {
			java.util.ArrayList<WeakReference<I>> tmp = new java.util.ArrayList<WeakReference<I>>(weak.size());
			for (WeakReference<I> w : weak) {
				if (w.get() != null)
					tmp.add(w);
			}
			if (tmp.size() != weak.size()) {
				weak.clear();
				weak.addAll(tmp);
			}
		}
	}

	//-------------------------------------------------------------------------
	/** Notifies all listeners later (it uses the SwingUtilities.invokeLater() method
	 *  to do this.
	 * In case of repeated events before dispatching begins, listeners receive
	 * the first or last message only, depending on getFireLaterMode(). 
	 * (The default is the first message.) 
	 */
	protected void fireLater(E msg) {
		if (dispatchFirstMessageOnly) {
			if (!pending.compareAndSet(null, new MsgWrapper<E>(msg)))
				return;
		} else {
			if (null != pending.getAndSet(new MsgWrapper<E>(msg)))
				return;
		}
		javax.swing.SwingUtilities.invokeLater(new Runnable() { public void run() {
			MsgWrapper<E> tmp = pending.getAndSet(null);
			if (tmp != null)
				fire(tmp.msg); 
		} });
	}

	//-------------------------------------------------------------------------
	protected void setFireLaterMode(boolean dispatchFirstMessageOnly) {
		this.dispatchFirstMessageOnly = dispatchFirstMessageOnly;
	}

	//-------------------------------------------------------------------------
	/**
	 * Returns how fireLater() works when called repeatedly before dispatching begins.
	 * @return true means listeners will receive the first message only.
	 *          false means listeners will receive the last message only. 
	 */
	public boolean getFireLaterMode() {
		return dispatchFirstMessageOnly;
	}

	//-------------------------------------------------------------------------
	/**
	 * Use this method when the event processing method is normally
	 * not accessible. Note that if 'I' is not public, this method
	 * is automatically called by the constructor.    
	 */
	protected synchronized void suppressJavaAccessChecks(boolean b) {
		method.setAccessible(b);
	}



	
	//=========================================================================
	// Internals

	//-------------------------------------------------------------------------
	/** Invokes the event processing method with 'o' as target and 'args' as argument. */ 
	private void invoke(I o, Object[] args) {
		try { method.invoke(o, args); }
		catch (Exception e) {
			removeListener(o);
			MEMEApp.logInvocationError(this.getClass().getCanonicalName() + ".invoke()", o, method, e);
		}
	}

	//-------------------------------------------------------------------------
	/** Returns whether 'this' object contains the 'observer' or not. Use 'action' as
	 *  parameter bits.<br>
	 *  STRONG bit indicates that the method searches the 'observer' as a (strong) listener.
	 *  WEAK bit indicates that the method searches the 'observer' as a weak listener.
	 *  REMOVE bit indicates that if the method has found the object then removes it from
	 *  the appropriate list.
	 */
	private boolean contains(I observer, int action) {
		if ((action & STRONG)!=0 && strong != null) {
			int idx = strong.indexOf(observer);
			if (idx >= 0) {
				if ((action & REMOVE)!=0)
					strong.remove(idx);
				return true;
			}
		}
		if ((action & WEAK)!=0 && weak != null) {
			for (ListIterator<WeakReference<I>> i = weak.listIterator(); i.hasNext(); )
				if (i.next().get() == observer) {
					if ((action & REMOVE)!=0)
						weak.remove(i.previousIndex());
					return true;
				}
		}
		return false;
	}

	//-------------------------------------------------------------------------
	/** Wrapper class for event messages.
	 * 	Allows storing null in 'pending' without being null.
	 */  
	private static class MsgWrapper<E> {
		final E		msg;
		MsgWrapper(E msg) { this.msg = msg; }
	}

	private static final int STRONG	= 1; 
	private static final int WEAK		= 2; 
	private static final int REMOVE	= 4;

}
