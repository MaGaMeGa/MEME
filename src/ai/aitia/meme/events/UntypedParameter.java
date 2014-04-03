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


//-----------------------------------------------------------------------------
/**
 * Represents the potential change of a conceptual "data" (which is something 
 * external from the point of view of this object). 
 * The purpose of this object is to facilitate the management and notification
 * of observers who are interested in the change of that associated "data".  
 * 
 * Therefore, the routines that modify the associated "data" are responsible
 * for generating the change-notification event, either by calling fireLater()
 * or passing this object to ProgramState.fireProgramStateChange(). 
 * 
 * Registration of this object at the global ProgramState service is optional.
 * It allows observers and event-generators to access this object by ID, even 
 * anticipatively (i.e. observers can be added before this object is actually
 * created/registered, using its ID - see ProgramState for details).
 *
 * Objects of this class can be linked to each other, generating a graph.
 * When one of them is "changed" (i.e. change-notification is scheduled), 
 * the linked ones are changed automatically, too. Cycles are detected. 
 * 
 * The getUntypedValue() method should return the corresponding "data". 
 */
public abstract class UntypedParameter extends Event<IProgramStateChangeListener, ProgramStateChangeEvent> {
	java.util.WeakHashMap<Object, UntypedParameter> links = null;	// Invariant: all the values are null

	//-------------------------------------------------------------------------
	protected UntypedParameter()	{ super(IProgramStateChangeListener.class, ProgramStateChangeEvent.class);	}

	//-------------------------------------------------------------------------
	public abstract Object getID();
	public abstract Object getUntypedValue();

	//-------------------------------------------------------------------------
	public void fireLater()		{ ProgramState.SINGLETON.fireProgramStateChange(this); }

	//-------------------------------------------------------------------------
	/**
	 * Arranges for notifying the observers of the other parameter specified
	 * by 'id' when 'this' parameter is changed.  
	 */
	public synchronized void propagateTo(Object id) {
		if (links == null)
			links = new java.util.WeakHashMap<Object, UntypedParameter>();
		links.put(id, null);
	}

	//-------------------------------------------------------------------------
	@Override
	protected void collectObservers(java.util.Collection<IProgramStateChangeListener> coll, 
									 java.util.Collection<IProgramStateChangeListener> wcoll) {
		super.collectObservers(coll, wcoll);
	}
}
