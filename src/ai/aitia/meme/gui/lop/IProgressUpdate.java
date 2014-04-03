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

import java.util.EventListener;

/** Interface for listening progress events. */
public interface IProgressUpdate extends EventListener {

	/**
	 * This method is a callback used by Progress objects; it is 
	 * responsible for updating on the display the progress percentage 
	 * and elapsed/remaining time.
	 * Example: <code>onProgressUpdate(46.5, 1257939497421, "02:10:34")</code>
	 * <br>Note: [EDT or Model thread]!
	 * @param percent negative when the total length is unknown or zero.
	 */
	void onProgressUpdate(double percent, long elapsed, String left);

}
