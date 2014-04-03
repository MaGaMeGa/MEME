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
package ai.aitia.meme.paramsweep.utils;

/** Exception class used during loading model settings. */
public class WizardLoadingException extends Exception {

	//=================================================================================
	// members

	private static final long serialVersionUID = -7494069933297468153L;
	/** Flag that determines whether the exception is fatal. */
	private boolean fatal = false;

	//=================================================================================
	// methods
	
	//---------------------------------------------------------------------------------
	/** Constructor.
	 * @param fatal is the exception fatal?
	 * @param message the message of the exception
	 */
	public WizardLoadingException(boolean fatal, String message) {
		super(message);
		this.fatal = fatal;
	}
	
	//----------------------------------------------------------------------------------------------------
	public WizardLoadingException(boolean fatal, Throwable cause) {
		super(cause);
		this.fatal = fatal;
	}
	
	//---------------------------------------------------------------------------------
	public boolean getFatal() { return fatal; }
}
