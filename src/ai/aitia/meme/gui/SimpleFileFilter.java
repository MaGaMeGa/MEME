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
package ai.aitia.meme.gui;

import java.io.File;
import javax.swing.filechooser.FileFilter;

import ai.aitia.meme.utils.Utils;

/** File filter that can filter the files by any extensions. */
public class SimpleFileFilter extends FileFilter {
	
	/** Description of the filter. */
	protected String	description = null;
	/** Supported extensions. */
	protected String[] extensions = null;

	/**
	 * Constructor. 
	 * @param description The format is one of the followings:
	 *                     <code>"description (*.ext)"</code> or
	 *                     <code>"description (*.ext1;*.ext2; *.etc)"</code> or
	 *                     <code>"description (*.*)"</code>
	 */
	public SimpleFileFilter(String description) {
		this.description = description;
		if (description == null) return;
		int a = description.lastIndexOf('(');
		int b = description.lastIndexOf(')');
		if (a < 0 || b <= a + 1) return;
		String[] tmp = description.substring(a+1, b).split("\\s*;\\s*\\*?");
		if (tmp == null || tmp.length == 0) return;
		java.util.TreeSet<String> t = new java.util.TreeSet<String>();
		for (int j = tmp.length - 1; j >= 0; --j) {
			if (tmp[j].length() == 0) continue;
			String ext = tmp[j].substring(tmp[j].indexOf('.')+1).toLowerCase();
			if (ext.equals("*")) return;
			t.add(ext);
		}
		if (t.isEmpty()) return;
		extensions = new String[t.size()];
		int i = 0;
		for (String s : t) extensions[i++] = s;
	}

	@Override
	public boolean accept(File f) {
		if (extensions == null || f.isDirectory()) return true;
		String ext = Utils.getExt(f).toLowerCase();
		for (int i = 0; i < extensions.length; ++i) {
			if (extensions[i].equals(ext)) return true;
		}
		return false;
	}

	@Override
	public String getDescription() {
		return (description == null) ? "" : description;
	}
}
