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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import org.jdesktop.jdic.desktop.Desktop;
import org.jdesktop.jdic.desktop.DesktopException;

public class UnixUtils extends OSUtils {

	//----------------------------------------------------------------------------------------------------
	@Override protected int getRelevance() { return (getOSType() == OSType.UNIX) ? 1 : 0; }

	//----------------------------------------------------------------------------------------------------
	@Override
	public void openDocument(URI uri, File dir) throws IOException {
		URL url = uri.toURL();
		try {
			if ("file".equals(url.getProtocol())) 
				Desktop.open(new File(url.getPath()));
			else
				Desktop.browse(url);
		} catch (DesktopException e) {
			throw new IOException(e.getLocalizedMessage());
		}
	}
}
