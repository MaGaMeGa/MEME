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

/**
 * Listeners of program state changes must implement this interface.
 */
public interface IProgramStateChangeListener extends java.util.EventListener {
	/**
	 * This method is triggered after ProgramState.fireProgramStateChange() was 
	 * called (not immediately, because SwingUtilities.invokeLater() is involved).
	 * Thus it is always executed in the EDT thread. Multiple calls of 
	 * fireProgramStateChange() may be coalesced into a single call. 
	 * @param parameters Collection of parameters that have been passed to
	 *   fireProgramStateChange() (cumulative). It should include the parameters 
	 *   whose values have been changed. However, it cannot be guaranteed that
	 *   all such parameters are included. In particular, parameters==null is 
	 *   possible, or the collection may be empty even if the value of several
	 *   parameters have been changed.
	 */
	public void onProgramStateChange(ProgramStateChangeEvent parameters);
}
