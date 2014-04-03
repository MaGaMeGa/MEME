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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;

/** Global event-observer center. It enables to registrate observers for events
 *  that hasn't exists yet. This is a singleton class.
 */
public class ProgramState implements Runnable {

	//=========================================================================
	// Public types and members
	
	/** The only instance of this class. */
	public static final ProgramState SINGLETON = new ProgramState();

	//-------------------------------------------------------------------------
	/** Generic interface for representing parameters. T is the type of the parameter. */
	public interface IParameter<T> {
		/** Returns the value of the parameter. */
		public abstract T getValue();
	}

	//-------------------------------------------------------------------------
	/** Generic abstract class to representing parameters. */
	public static abstract class Parameter<T> extends UntypedParameter implements IParameter<T> {
		@Override public Object getID()					{ return this.getClass().getName(); }
		@Override public final Object getUntypedValue()	{ return getValue(); } 
	}

	//-------------------------------------------------------------------------
	/** Controller of java.awt.Component objects */
	public static abstract class CompCtrl<T> extends Controller implements IParameter<T> {
		@Override public final Object getUntypedValue()	{ return getValue(); } 
	}

	//=========================================================================
	// Public methods

	//-------------------------------------------------------------------------
	/**
	 * Registers a new parameter 'p', or replaces an existing parameter identified
	 * by p.getID().equals(). Note: after registration, p.getID() should not be changed 
	 * in a way that affects p.getID().equals(), as detailed in the documentation
	 * of java.util.Set<>.
	 * @return p   
	 */
	public synchronized <T extends UntypedParameter> T registerParameter(T p) {
		if (p != null) {
			Object key = p.getID();
			UntypedParameter exp = parameters.get(key);
			if (exp != null && exp instanceof PlaceHolder) {
				((PlaceHolder)exp).mergeObservers(p);
			}
			parameters.put(p.getID(), p);
		}
		return p;
	}

	//-------------------------------------------------------------------------
	/** Returns parameter identified by 'id'. If such parameter doesn't exists, it returns
	 *  null.
	 */
	public synchronized UntypedParameter find(Object id) {
		return (id == null) ? null : parameters.get(id);
	}

	//-------------------------------------------------------------------------
	/** Convenience method to find parameter identified by its type. */
	 /* Ennek a muveletnek az volna a celja, hogy ha vki ismeri a pontos tipusat
	 * egy parameternek, akkor tipusosan tudja lekerdezni ot, hogy rovidebben
	 * ferhessen hozza az ertekehez (a kasztolast itt vegezzuk el automatikusan).
	 */ 
	@SuppressWarnings("unchecked")
	public synchronized <T extends IParameter<?>> T find(Class<T> x) {
		//if (x == null || !Parameter.class.isAssignableFrom(x))
		//	throw new IllegalArgumentException("Argument must be ancestor of Parameter<>");
		UntypedParameter ans = parameters.get(x.getName()); 
		return (ans == null) ? null : (T)ans;
	}

	//-------------------------------------------------------------------------
	/** Returns the value of the parameter identified by its type. */
	public <T> T get(Class<? extends IParameter<? extends T>> x) {
		IParameter<? extends T> p = find(x);
		return (p == null) ? null : p.getValue();
	}

	//-------------------------------------------------------------------------
	/** Registers and returns a parameter identified by 'id'.  
	 * 	Throws ClassCastException if 'id' is already registered but not with a Controller */ 
	public synchronized Controller book(Object id)		{ return (Controller)bookPar(id); }

	//-------------------------------------------------------------------------
	/** Registers and returns a parameter identified by 'id'. */  
	public synchronized UntypedParameter bookPar(Object id) {
		UntypedParameter ans = find(id);
		if (ans == null)
			ans = registerParameter(new PlaceHolder(id));
		return ans;
	}

	//-------------------------------------------------------------------------
	/** While a possible event has not been created and registered here, a holder object
	 *  stores the its registered observers. This class represents these holders.
	 */
	private static class PlaceHolder extends Controller {
		final Object id;
		public PlaceHolder(Object id)				{ this.id = id; }
		@Override public Object getID()				{ return id; }
		@Override public Object getUntypedValue()	{ return null; }

		// Merge links and observers from this to dest (and vice versa) 
		// 
		synchronized void mergeObservers(UntypedParameter dest) {			// ???
			synchronized (dest) {

				// Merge links

				if (links != null) {
					if (dest.links == null)
						dest.links = links;
					else 
						dest.links.putAll(links);
					links = dest.links;
				}

				// Merge observers
				// Pay attention to possible duplicates, and preserve order
	
				// HashSet-ekbe ramolom a sajatjaimat. A weak-eket ki kell bontani a WeakReference-bol, 
				// mert a HashSet-ben nem mukodnek maskepp 
				int w = (this.weak == null) ? 0 : this.weak.size();
				LinkedHashSet<IProgramStateChangeListener> weak = new LinkedHashSet<IProgramStateChangeListener>(w);
				while (--w >= 0)
					weak.add(this.weak.get(w).get());
	
				LinkedHashSet<IProgramStateChangeListener> normal = new LinkedHashSet<IProgramStateChangeListener>(
						(this.strong != null) ? this.strong : java.util.Collections.<IProgramStateChangeListener>emptyList()  
				);
				normal.remove(this);	// do not copy Contollers' self-listener (used for controllers' targets)
	
				if (!normal.isEmpty() || !weak.isEmpty()) {	// ezeket kell hozzaadni dest-hez
					dest.collectObservers(normal, weak);	// hozzagyujtom a dest-belieket. Duplikalasok kiszurve.
	
					// Most visszacsomagolom a weak-eket WeakReference-ekbe. Nem egybol dest.weak-be
					// pakolom, mert az CopyOnWriteArrayList, s igy minden add() teljes masolast okozna.
					// Az atmeneti ArrayList 1 muvelette roviditi majd a dolgot.
					ArrayList<java.lang.ref.WeakReference<IProgramStateChangeListener>> tmp = 
						new ArrayList<java.lang.ref.WeakReference<IProgramStateChangeListener>>(weak.size());
					for (IProgramStateChangeListener o : weak)
						tmp.add(new java.lang.ref.WeakReference<IProgramStateChangeListener>(o));
	
					// Johet a frissites
					if (dest.strong == null) {
						if (!normal.isEmpty())
							dest.strong = new java.util.concurrent.CopyOnWriteArrayList<IProgramStateChangeListener>(normal);
					} else {
						dest.strong.clear();
						dest.strong.addAll(0, normal);
					}
					if (dest.strong != null)
						this.strong.addAllAbsent(dest.strong);	// visszafuzzuk this-be is
					// megj: ha dest Controller, akkor ezzel strong-ba bekerult maga dest is.
					// Ez talan nem baj. (Ha this-t illo modon eldobjak, akkor biztosan nem).
	
					
					if (dest.weak == null) {
						if (!weak.isEmpty())
							dest.weak = new java.util.concurrent.CopyOnWriteArrayList<java.lang.ref.WeakReference<IProgramStateChangeListener>>(tmp);
					} else {
						dest.weak.clear();
						dest.weak.addAll(0, tmp);
					}
					this.weak = dest.weak;
				} else {
					// Nem kell dest-hez hozzatenni semmit sem. Csak viszont:
					if (dest.strong != null)
						this.strong.addAllAbsent(dest.strong);
					this.weak = dest.weak;
				}
	
				// Merge Controller rules. Don't care with targets order.
	
				if (!this.components.isEmpty() && (dest instanceof Controller)) {
					Controller cdest = (Controller)dest;
					cdest.components.addAll(this.components);
					this.components.addAll(cdest.components);
				}
			}// synchronized (dest)
		}
	}

	//-------------------------------------------------------------------------
	/** Collects the parameters that has changed and notifies their observers later
	 * (it uses SwingUtilities.invokeLater() to do this).  
	 */
	public synchronized void fireProgramStateChange(Object ... pars) {
		if (pars == null || pars.length == 0)
			return;

		boolean scheduleNow = (toBeNotified == null); 
		if (toBeNotified == null)
			toBeNotified = new WeakHashMap<Object, UntypedParameter>(pars.length);

		for (int i = 0; i < pars.length; ++i) {
			Object x = pars[i];
			if (x instanceof UntypedParameter) {
				UntypedParameter p = (UntypedParameter)x;
				toBeNotified.put(p.getID(), p);
			} else if (x != null) { 
				toBeNotified.put(x, toBeNotified.get(x));
			}
		}

		if (scheduleNow)
			SwingUtilities.invokeLater(this);
	}
	

	//=========================================================================
	// Internals

	//-------------------------------------------------------------------------
	public void run() {
		WeakHashMap<Object, UntypedParameter> tmp;
		synchronized (this) {
			tmp = toBeNotified;
			toBeNotified = null;
		}
		if (tmp != null) {
			WeakHashMap<Object, UntypedParameter> map = new WeakHashMap<Object, UntypedParameter>(tmp.size());
			LinkedHashSet<IProgramStateChangeListener> listeners = new LinkedHashSet<IProgramStateChangeListener>();

			while (!tmp.isEmpty()) {
				Iterator<Entry<Object, UntypedParameter>> it = tmp.entrySet().iterator();
				Entry<Object, UntypedParameter> first = it.next();
				it.remove();
				
				UntypedParameter p = first.getValue();
				if (p == null || (p instanceof PlaceHolder)) {
					p = find(first.getKey());
					if (p == null || (p instanceof PlaceHolder))
						continue;
				}
				map.put(first.getKey(), p);
				p.collectObservers(listeners, listeners);
				if (p.links != null) {
					for (Entry<Object, UntypedParameter> e : p.links.entrySet()) {
						if (!map.containsKey(e.getKey()) && tmp.get(e.getKey()) == null)
							tmp.put(e.getKey(), e.getValue());
					}
				}
			}
			ProgramStateChangeEvent evt = new ProgramStateChangeEvent(this, map); 
			for (IProgramStateChangeListener observer : listeners)
				observer.onProgramStateChange(evt);
		}
	}

	protected HashMap<Object, UntypedParameter>		parameters = new HashMap<Object, UntypedParameter>();
	protected WeakHashMap<Object, UntypedParameter>	toBeNotified = null;
}
