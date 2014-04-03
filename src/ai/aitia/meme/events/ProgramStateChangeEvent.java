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

import java.util.Collection;
import java.util.Map;

import ai.aitia.meme.events.ProgramState.Parameter;

//-----------------------------------------------------------------------------
/** Event class for representing program state events. */
@SuppressWarnings("serial")
public class ProgramStateChangeEvent extends java.util.EventObject {

	/** The changed parameters. */
	private final Map<Object, UntypedParameter> parameters;
	
	public ProgramStateChangeEvent(ProgramState source, Map<Object, UntypedParameter> parameters) {
		super(source);
		this.parameters = parameters;
	}

	@Override public ProgramState			getSource()				{ return (ProgramState)super.getSource(); }
	public Collection<UntypedParameter>		getParameters()			{ return parameters.values(); }
	public Map<Object, UntypedParameter>	getMap()				{ return parameters; }

	public boolean							contains(Object x)		{
		if (x instanceof UntypedParameter) x = ((UntypedParameter)x).getID(); 
		return parameters.get(x) != null;
	}
	public UntypedParameter					find(Object id)			{ return parameters.get(id); }
	@SuppressWarnings("unchecked")
	public <T extends Parameter<?>> T		find(Class<T> id)		{ return (T)parameters.get(id); }
}
