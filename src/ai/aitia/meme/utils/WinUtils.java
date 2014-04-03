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
package ai.aitia.meme.utils;

import java.io.IOException;

/**
 * Windows-specific operations. This class stands for all versions of Windows (excluding 3.x).
 * Things specific to certain Windows versions should go to descendant classes. 
 */
public class WinUtils extends OSUtils 
{
	//-------------------------------------------------------------------------
	// [Model thread]
	@Override
	public void openDocument(java.net.URI uri, java.io.File dir) throws IOException
	{
		Runtime.getRuntime().exec(new String[] { "rundll32", "url.dll,FileProtocolHandler", uri.toString() }, null, dir);
	}

	//-------------------------------------------------------------------------
	// [Model thread]
	@Override
	protected int getRelevance() {
		return (getOSType() == OSType.WINDOWS) ? 1 : 0;
		// Higher values are reserved for Windows-version-specific descendant classes
	}
}
