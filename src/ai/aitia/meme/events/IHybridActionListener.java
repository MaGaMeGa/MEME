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
 * This interface allows handling actionPerformed() messages at a common place,
 * either originated from ordinary Swing components or Action objects.
 * This is why it is called "hybrid".
 */
public interface IHybridActionListener {

	/**
	 * This method is intended to be a common place for handling "related" actionPerformed()
	 * messages (here "related" usually means that belong to the same window).
	 * @param a Non-null only if this actionPerformed() message is originated from a
	 *           HybridAction object.     
	 */
	public void hybridAction(java.awt.event.ActionEvent e, HybridAction a);

}
