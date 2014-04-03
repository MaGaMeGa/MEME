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
package ai.aitia.meme.paramsweep.platform.repast.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ai.aitia.meme.paramsweep.PsSystemException;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;

/**
 * The class knows the structure of Repast result files, uses the same convention
 * that tasks uses to produce them and unify them into a single file, what would
 * otherwise be produced by a local batch running.  
 * A logger with the class fully qualified name is defined.  
 */
public class ResultFileMerger {
	private static final Logger logger =  Logger.getLogger(ResultFileMerger.class.getName());
	private String delimiter = null;
	private long previousRun = 0;
	
	/**
	 * For each output files in recorders searches in the working dir for 
	 * the tasks produced result files (ending in partX, where X is task index)
	 *  and merges them to the base file name in working dir.
	 * @param recorders
	 * @param workingDir
	 * @return
	 */
	public List<File> merge(List<RecorderInfo> recorders, File workingDir) {
		List<File> success = new ArrayList<File>();
		File f;
		String namePattern;
		File[] files;
		for (RecorderInfo ri : recorders) {	// each output file
			delimiter = ri.getDelimiter();
			f = ri.getOutputFile();
			namePattern = ".*" + f.getName() + ".part\\d+";	// filter ones created by tasks
			files = workingDir.listFiles(new MyFileNameFilter(namePattern));
			try {
				previousRun = 0;
				final File merged = new File(workingDir,f.getName());
				merge(files,merged,true);
				success.add(merged);
			} catch (PsSystemException e) {
				// report error, but proceed
				logger.log(Level.SEVERE, "Result file merging failed", e);
			}
		}
		return success;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<File> merge(final List<RecorderInfo> recorders, final List<String> suffixes, final File workingDir) {
		final List<File> result = new ArrayList<File>();
		File f;
		for (final RecorderInfo ri : recorders) { // each output file
			delimiter = ri.getDelimiter();
			f = ri.getOutputFile();
			final List<File> files = new ArrayList<File>();
			for (final String suffix : suffixes)
				files.add(new File(workingDir,f.getName() + suffix));
			try {
				previousRun = 0;
				File merged = new File(workingDir,f.getName());
				merge(files.toArray(new File[files.size()]),merged,false);
				result.add(merged);
			} catch (final PsSystemException e) {
				// report error, but proceed
				logger.log(Level.SEVERE,"Result file merging failed", e);
			}
		}
		return result;
	}
	
	/**
	 * Files produced by tasks are merged into one. The first header and the  
	 * last footer is kept, run numbers are replaced by real values in data bodies.
	 * @param parts
	 * @param merged
	 * @throws PsSystemException
	 */
	private void merge(File[] parts, File merged, boolean sort) throws PsSystemException {
		if (parts == null || parts.length == 0) 
			throw new PsSystemException("No parts for recorder " + merged.getAbsolutePath() + " found");
		
		try {
			merged.createNewFile();
		} catch (IOException e) {
			throw new PsSystemException(e);
		}
		
		if (sort)
			Arrays.sort(parts,new PartsComparator());
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(merged));
			
			// handle header separately
			BufferedReader br = new BufferedReader(new FileReader(parts[0]));
			writeHeader(br,bw);
			br.close();
			
			for (int i = 0;i < parts.length;i++) {
				br = new BufferedReader(new FileReader(parts[i]));
				writeBody(br,bw);
				if (i == parts.length - 1) 	// last file
					writeFooter(br,bw);
				br.close();
			}
			bw.close();
		} catch (IOException e) {
			throw new PsSystemException(e);
		}
	}
	
	private void writeHeader(BufferedReader src, BufferedWriter dst) throws IOException {
		String line;
		while (!(line = src.readLine()).startsWith("\"run")) {
			dst.write(line);
			dst.newLine();
		}
		dst.write(line);
		dst.newLine();
	}
	
	/**
	 * Skips header, then writes out body.
	 * @param src
	 * @param dst
	 * @param index
	 * @throws IOException
	 */
	private void writeBody(BufferedReader src, BufferedWriter dst)  throws IOException, PsSystemException {
		String line;
		while (!(line = src.readLine()).startsWith("\"run")); // skip to body
		
		long lastRun = 0;
		while (!(line = src.readLine().trim()).equals("")) {
			int pos = line.indexOf(delimiter);
			String runStr = line.substring(0,pos);
			try {
				long run = Long.parseLong(runStr);
				dst.write((previousRun + run) + line.substring(pos,line.length()));
				dst.newLine();
				lastRun = previousRun + run;
			} catch (NumberFormatException e) {
				throw new PsSystemException(e);
			}
		}
		previousRun = lastRun;
	}
	
	private void writeFooter(BufferedReader src, BufferedWriter dst) throws IOException {
		String line;
		String prev = "";
		while ((line = src.readLine()) != null) 
			prev = line;
		dst.newLine();
		dst.newLine();
		dst.write(prev);
	}
	
	//////////////////////////////////////////////
	//////// INNER ///////////////////////////////
	//////////////////////////////////////////////
	
	//----------------------------------------------------------------------------------------------------
	public static class MyFileNameFilter implements FilenameFilter {
		private String namePattern;
		
		public MyFileNameFilter(String namePattern) {
			this.namePattern = namePattern; 
		}
		
		public boolean accept(File dir, String name) {
			File f = new File(dir, name);
			if ( !f.isFile() ) {
				return false;
			}
			return f.getAbsolutePath().matches(namePattern);
		}
		
	}
	
	//----------------------------------------------------------------------------------------------------
	public static class PartsComparator implements Comparator<File> { // also known as MyWonderfulComparator :)

		//----------------------------------------------------------------------------------------------------
		public int compare(File f1, File f2) {
			int index1 = f1.getName().lastIndexOf("part");
			String nostr1 = f1.getName().substring(index1 + 4);
			int no1 = Integer.parseInt(nostr1);
			
			int index2 = f2.getName().lastIndexOf("part");
			String nostr2 = f2.getName().substring(index2 + 4);
			int no2 = Integer.parseInt(nostr2);
			return no1 - no2;
		}
	}
}
