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
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.Deflater;
import java.util.zip.ZipException;

/** This class allows for the user to create a jar file from the class files of one or more
 *  directories.
 */ 
public class JarGenerator {

	//=================================================================================
	// members
	
	/** The list of the path of the source directories. */ 
	private List<String> sources = null;
	/** The path and name of the generated jar file. */
	private String targetFile = null;
	/** The output stream of the jar file. */
	private JarOutputStream jos = null;
	
	//=================================================================================
	// methods
	
	//---------------------------------------------------------------------------------
	/** Constructor.
	 * @param sources the list of the path of the source directories
	 * @param targetFile the path and name of the generated jar file
	 */
	public JarGenerator(List<String> sources, String targetFile) {
		if (sources == null || sources.size() == 0)
			throw new IllegalArgumentException("'sources' is null or empty");
		if (targetFile == null || "".equals(targetFile.trim()))
			throw new IllegalArgumentException("'targetFile' is null or empty string");
		if (!targetFile.trim().endsWith(".jar"))
			throw new IllegalArgumentException("The extension of 'targetFile' is not jar");
		this.sources = sources;
		this.targetFile = targetFile;
	}
	
	//---------------------------------------------------------------------------------
	/** Generates a jar file from the class files contained by the source directories.
	 * @throws IOException if any problem occures during the generation
	 */
	public void generateJar() throws IOException {
		try {
			InputStream is = new FileInputStream("resources/MANIFEST.MF");
			jos = new JarOutputStream(new FileOutputStream(targetFile),new Manifest(is));
			is.close();
			jos.setLevel(Deflater.NO_COMPRESSION);
			
			for (String source : sources) {
				File file = new File(source);
				if (!file.exists()) continue;
				jarFiles(file,file);
			}
			jos.finish();
			jos.close();
		} catch (FileNotFoundException e) {
			// never happens
			throw new IllegalStateException();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void jarDirectory(File dir2zip, File destFile) throws IOException {
		JarOutputStream jos = new JarOutputStream(new FileOutputStream(destFile));
		jos.setLevel(Deflater.BEST_COMPRESSION);
		JarGenerator.jarDir(dir2zip,dir2zip,jos);
		jos.finish();
		jos.close();
	}
	
	//=================================================================================
	// private methods
	
	//---------------------------------------------------------------------------------
	/** Recursive method to filling the generated jar file.
	 * @param file the file (or directory) that must adds to the jar file
	 * @param folder the original source directory of the <code>file</code>
	 * @throws IOException if any problems occure during the generation (except "duplicate
	 *         entry" excepions)
	 */
	private void jarFiles(File file, File folder) throws IOException {
		if (file.isDirectory()) {
			if (!file.equals(folder)) {
				try {
					String strAbsPath = file.getAbsolutePath();
					String strJarEntryName = strAbsPath.substring(folder.getAbsolutePath().length() + 1,strAbsPath.length()).replace('\\','/');
					JarEntry dir = new JarEntry(strJarEntryName + "/");
					jos.putNextEntry(dir);
					jos.closeEntry();
				} catch (ZipException e) {
					if (!e.getLocalizedMessage().startsWith("duplicate entry")) 
						throw e;
				}								
			}
			File[] files = file.listFiles();
			for (int i = 0;i < files.length;++i)
				jarFiles(files[i],folder);
		} else {
			if (!file.getName().endsWith(".class"))
				return;
			String strAbsPath = file.getAbsolutePath();
			String strJarEntryName = strAbsPath.substring(folder.getAbsolutePath().length() + 1,strAbsPath.length()).replace('\\','/');
			byte [] b = new byte[(int)file.length()];
			
			try {
				JarEntry je = new JarEntry(strJarEntryName);
				je.setTime(file.lastModified());
				InputStream is = new FileInputStream(file);
				is.read(b);
				is.close();
				jos.putNextEntry(je);
				jos.write(b,0,(int)file.length());
				jos.closeEntry();
			} catch (ZipException e) {
				if (!e.getLocalizedMessage().startsWith("duplicate entry")) 
					throw e;
			}								
		}
	}

	//----------------------------------------------------------------------------------------------------
	private static void jarDir(File original, File dir2zip, JarOutputStream jos) throws IOException {
		
	    //get a listing of the directory content 
	    File[] dirList = dir2zip.listFiles(); 
	    byte[] readBuffer = new byte[2156]; 
	    int bytesIn = 0; 
	    //loop through dirList, and zip the files
	    int idx = original.getPath().length();
	    for (int i = 0;i < dirList.length;i++) { 
	        String substring = dirList[i].getPath().substring(idx + 1).replace('\\','/');
			if (dirList[i].isDirectory()) {
	        	JarEntry anEntry = new JarEntry(substring + "/");
	        	jos.putNextEntry(anEntry);
	        	jos.closeEntry();
	        	jarDir(original,dirList[i],jos); 
	        } else {
	        	FileInputStream fis = new FileInputStream(dirList[i]); 
	        	JarEntry anEntry = new JarEntry(substring); 
	        	jos.putNextEntry(anEntry);
	        	while ((bytesIn = fis.read(readBuffer)) != -1) 
	        		jos.write(readBuffer,0,bytesIn); 
	        	jos.closeEntry();
	        	fis.close();
	        }
	    } 
	}
}
