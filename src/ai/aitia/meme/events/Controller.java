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
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.WeakHashMap;

import ai.aitia.meme.Logger;
import ai.aitia.meme.utils.Utils;

//-----------------------------------------------------------------------------
/**
 * This untyped ProgramState-parameter performs <em>actions</em> on a set of 
 * targets when change of 'this' ProgramState-parameter is announced. Targets
 * can be registered with the add() or control() methods, and will be stored
 * with weak references.<br>
 *
 * The possible <em>actions</em> (IPSAction) are binary operations, whose first    
 * argument is the target object, and the second argument is the value of 'this'
 * ProgramState-parameter (or something calculated from it).
 * This second argument can be calculated individually for every {target, action}
 * pair, if a calculation object (ICalculation) is specified. ICalculation represents
 * an arbitrary function that takes the value of 'this' ProgramState-parameter as input 
 * and returns an Object that is suitable for the action in question.<br>
 *
 * For example, setEnabled() and setVisible() are actions whose second argument is a
 * boolean value. If this.getUntypedValue() always returns a Boolean value, no calculation 
 * is needed. If 'this' ProgramState-parameter returns Integer values, an ICalculation 
 * object - representing a condition - have to be specified to return a boolean value 
 * for any integer value of 'this' ProgramState-parameter. The isEqual(), isLessOrEqual(), 
 * isGreaterOrEqual() methods can help to create an appropriate ICalculation object.
 * Different calculations (conditions) can be used for different JComponent targets.<br>
 * 
 * Note: this class is NOT synchronized. It is intended to be used 
 * from the event-dispatcher thread only.
 */
public abstract class Controller extends UntypedParameter implements IProgramStateChangeListener
{
	/** Recorder class for observers of this event. */
	static class Rec {
		/** The target of the 'action'. */
		WeakReference<Object>	wcomponent = null;
		/** The action object. */
		IPSAction				action = null;
		/** The calculation object. */
		ICalculation			cond = null;
		@Override
		public boolean equals(Object obj) {
			if (obj != this && obj instanceof Rec) {
				Rec rec = (Rec) obj;
				return (wcomponent.equals(rec.wcomponent) && action.equals(rec.action));
			}
			return super.equals(obj);
		}
		@Override public int hashCode() {
			return wcomponent.hashCode() ^ action.hashCode();
		}
	}
	
	/** Observers of this event. */
	protected final LinkedHashSet<Rec>	components = new LinkedHashSet<Rec>();

	//=========================================================================
	//	Public interface

	
	//-------------------------------------------------------------------------
	//	Program State Actions
	
	/** Interface for program state actions. */
	public interface IPSAction {
		/** This method stores the arguments of the actions. 'o' serves as the
		 *  target of the action, 'parameterValueOrCalculationResult' serves as 
		 *  ProgramState-parameter (or something calculated from it).
		 *  @see PSAction PSAction */
		void op(Object o, Object parameterValueOrCalculationResult);
	}

	/** 
	 * This enum is designed to facilitate easy extension of built-in operations 
	 * (beyond setEnabled() and setVisible()) 
	 */ 
	public static enum PSAction implements IPSAction {
		SET_ENABLED { public void op(Object c, Object val) { call(c, "setEnabled", val); } },
		SET_VISIBLE { public void op(Object c, Object val) { call(c, "setVisible", val); } };

		/** Calls the method 'methodName' with 'target' as target and 'args' as
		 *  arguments.
		 */
		static void call(Object target, String methodName, Object... args) {
			Method m = map.get(target);
			if (m == null) {
				m = Utils.findMethodV(target, methodName, args, false);
				map.put(target, m);
			}
			try {
				m.invoke(target, args);
			} catch (Exception e) {
				Logger.logExceptionCallStack("Controller.PSAction.call()", e);
			}
		}
		/** It maps target objects with methods. */
		static WeakHashMap<Object, Method> map = new WeakHashMap<Object, Method>();
	}

	
	//-------------------------------------------------------------------------
	//	Conditions
	
	/** Interface for representing a calculation operation. */
	public interface ICalculation {
		/** Executes the calculation and returns the result.
		 * @param parameterValue the parameter(s) of the calculation
		 * @return the result of the calculation
		 */
		Object evaluate(Object parameterValue);
	}

	/** Returns the Equals To calculation object. */ 
	public CtrlCalc isEqual(final int base) {
		return new CtrlCalc() {
			public Object evaluate(Object parameterValue)		{ return (Integer)parameterValue == base; }
		};
	}
	/** Returns the Lesser Than or Equals To calculation object. */ 
	public CtrlCalc isLessOrEqual(final int base) {
		return new CtrlCalc() {
			public Object evaluate(Object parameterValue)		{ return (Integer)parameterValue <= base; }
		};
	}
	/** Returns the Greater Than or Equals To calculation object. */ 
	public CtrlCalc isGreaterOrEqual(final int base) {
		return new CtrlCalc() {
			public Object evaluate(Object parameterValue)		{ return (Integer)parameterValue >= base; }
		};
	}
	/** This is a convenience class, for easier use of Controller.control() */ 
	public abstract class CtrlCalc implements ICalculation {
		public Controller getCtrl()	{ return Controller.this; }
	}


	//-------------------------------------------------------------------------
	//	Constructors
	
 	/** 'parameter' should return boolean value, or something suitable for all used conditions or actions */
	public Controller() {
		addListener(this);	// listens to itself - arranges for notifying 'components' when this parameter is fired
	}

	//-------------------------------------------------------------------------
	//	Convenience methods

	/**
	 * Use this method when the type of ctrl.parameter is suitable for action 'a'.  
	 */ 
	public static void control(Object o, IPSAction a, Controller ctrl)		{ ctrl.add(o, a, null); }

	/**
	 * Use this method when the type of ctrl.parameter differs from the argument type
	 * of action 'a'. cond.evaluate() must perform the conversion (calculation) from 
	 * ctrl.parameter.getUntypedValue() to the argument of the action.
	 */ 
	public static void control(Object o, IPSAction a, Controller.CtrlCalc cond)	{ cond.getCtrl().add(o, a, cond); }

	/** Same as control(o, PSAction.SET_ENABLED, this) */
	public void controlEnabled(Object o)									{ add(o, PSAction.SET_ENABLED, null); }

	/** Same as control(o, PSAction.SET_VISIBLE, this) */
	public void controlVisible(Object o)									{ add(o, PSAction.SET_VISIBLE, null); }

	
	//-------------------------------------------------------------------------
	//	Public methods

	//-------------------------------------------------------------------------
	@Override public Object getID() {
		return this.getClass().getName();
	} 

	//-------------------------------------------------------------------------
	/** Add new observer to this event. */
	public void add(Object o, IPSAction a, ICalculation cond) {
		if (a == null) throw new NullPointerException();
		Rec rec = new Rec();
		rec.wcomponent= new WeakReference<Object>(o);
		rec.action	  = a;
		rec.cond      = cond;
		components.add(rec);
	}

	//-------------------------------------------------------------------------
	/** Removes the component from all actions */ 
	public void remove(Object o) {
		remove(o, null, null);
	}

	//-------------------------------------------------------------------------
	/** Removes the observer from this event. */
	public void remove(Object o, IPSAction a, ICalculation cond) {
		if (a == null) {	// null means all
			for (java.util.Iterator<Rec> it = components.iterator(); it.hasNext(); ) {
				if (it.next().wcomponent.get() == o)
					it.remove();
			}
		} else {
			Rec tmp = new Rec();
			tmp.wcomponent= new WeakReference<Object>(o);
			tmp.action	  = a;
			tmp.cond      = cond;
			components.remove(tmp);
		}
	}

	//-------------------------------------------------------------------------
	/** 
	 * This method is public as an implementation side effect.
	 * Do not call or override.
	 * (If you call it, the observers of linked parameters will not be notified.)  
	 */
	public void onProgramStateChange(ProgramStateChangeEvent parameters) {
		if (!components.isEmpty()) {
			Object val = getUntypedValue(); 
			for (java.util.Iterator<Rec> it = components.iterator(); it.hasNext(); ) {
				Rec r = it.next();
				Object target = r.wcomponent.get();
				if (target == null)
					it.remove();
				else
					r.action.op(target, (r.cond == null) ? val : r.cond.evaluate(val));
			}
		}
	}

}
