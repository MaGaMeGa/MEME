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
package ai.aitia.meme.paramsweep.generator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

public class ZipGenerator {

	//=================================================================================
	// members
	
	/** The list of the path of the source directories. */ 
	private List<String> sources = null;
	private List<String> resources = null;
	/** The path and name of the generated zip file. */
	private String targetFile = null;
	/** The output stream of the zip file. */
	private ZipOutputStream zos = null;
	
	private String modelDir = null;
	
	//=================================================================================
	// methods
	
	//---------------------------------------------------------------------------------
	public ZipGenerator(List<String> sources, List<String> resources, String targetFile, String modelDir) {
		if (sources == null)
			throw new IllegalArgumentException("'sources' is null.");
		if (resources == null)
			throw new IllegalArgumentException("'resources' is null.");
		if (sources.size() == 0 && resources.size() == 0)
			throw new IllegalArgumentException("Empty source and resource lists.");
		if (targetFile == null || "".equals(targetFile.trim()))
			throw new IllegalArgumentException("'targetFile' is null or empty string.");
		if (!targetFile.trim().endsWith(".zip"))
			throw new IllegalArgumentException("The extension of 'targetFile' is not zip.");
		if (modelDir == null)
			throw new IllegalArgumentException("'modelDir' is null.");
		this.sources = sources;
		this.resources = resources;
		this.targetFile = targetFile;
		this.modelDir = modelDir;
	}
	
	//---------------------------------------------------------------------------------
	public void zip() throws IOException {
		try {
			zos = new ZipOutputStream(new FileOutputStream(targetFile));
			zos.setLevel(Deflater.BEST_COMPRESSION);
			
			for (String source : sources) {
				File file = new File(source);
				if (!file.exists()) continue;
				zipFiles(file,false);
			}
			for (String resource : resources) {
				File file = new File(resource);
				if (!file.exists()) continue;
				zipFiles(file,true);
			}
			zos.finish();
			zos.close();
		} catch (FileNotFoundException e) {
			// never happens
			throw new IllegalStateException();
		}
	}
	
	//---------------------------------------------------------------------------------
	/** Recursive method to filling the generated zip file.
	 * @param file the file (or directory) that must adds to the zip file
	 * @param folder the original source directory of the <code>file</code>
	 * @throws IOException if any problems occure during the generation (except "duplicate
	 *         entry" excepions)
	 */
	private void zipFiles(File file, boolean resource) throws IOException {
		if (file.isDirectory()) {
			String strZipEntryName = file.getPath().replace('\\','/');
			try {
				ZipEntry dir = new ZipEntry(strZipEntryName + "/");
				zos.putNextEntry(dir);
				zos.closeEntry();
			} catch (ZipException e) {
				if (!e.getLocalizedMessage().startsWith("duplicate entry")) 
					throw e;
			}								
			File[] files = file.listFiles();
			for (int i = 0;i < files.length;++i)
				zipFiles(files[i],false);
		} else {
			String strZipEntryName = null;
			if (resource)
				strZipEntryName = file.getPath().substring(modelDir.length() + 1).replace('\\','/');
			else 
				strZipEntryName = file.getName();
			try {
				ZipEntry ze = new ZipEntry(strZipEntryName);
				ze.setTime(file.lastModified());
				InputStream is = new FileInputStream(file);
				zos.putNextEntry(ze);
				copyStream(is,zos);
				zos.closeEntry();
			} catch (ZipException e) {
				if (!e.getLocalizedMessage().startsWith("duplicate entry")) 
					throw e;
			}								
		}
	}
	
	//-------------------------------------------------------------------------
	private OutputStream copyStream(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[8192];
		int length;
		try {
			while ((length = in.read(buffer)) != -1)
				out.write(buffer,0,length);
		} finally {
			in.close();
		}
		return out;
	} 
}
