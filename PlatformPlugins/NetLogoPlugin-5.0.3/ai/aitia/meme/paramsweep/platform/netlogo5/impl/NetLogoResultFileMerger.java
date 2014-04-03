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
package ai.aitia.meme.paramsweep.platform.netlogo5.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ai.aitia.meme.paramsweep.PsSystemException;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.platform.repast.impl.ResultFileMerger.MyFileNameFilter;
import ai.aitia.meme.paramsweep.platform.repast.impl.ResultFileMerger.PartsComparator;

public class NetLogoResultFileMerger {

	//====================================================================================================
	// members
	
	private static final Logger LOGGER = Logger.getLogger(NetLogoResultFileMerger.class.getName());
	private String delimiter = null;
	private long previousRun = 0;
	private final boolean inner;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public NetLogoResultFileMerger(final boolean inner) {
		this.inner = inner;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<File> merge(final List<RecorderInfo> recorders, final File workingDir) {
		final List<File> success = new ArrayList<File>();
		File file;
		String namePattern;
		File[] files;
		for (final RecorderInfo ri : recorders) {
			delimiter = String.valueOf(ri.getDelimiter().charAt(0));
			file = ri.getOutputFile();
			namePattern = ".*" + file.getName() + (inner ? ".prt\\d+" : ".part\\d+");
			files = workingDir.listFiles(new MyFileNameFilter(namePattern));
			try {
				previousRun = 0;
				final File merged = new File(workingDir,file.getName());
				merge(files,merged,true);
				success.add(merged);
			} catch (PsSystemException e) {
				LOGGER.log(Level.SEVERE,"Result file merging failed",e);
			}
		}
		return success;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<File> merge(final List<RecorderInfo> recorders, final List<String> suffixes, final File workingDir) {
		final List<File> result = new ArrayList<File>();
		File file;
		for (final RecorderInfo ri : recorders) {
			delimiter = String.valueOf(ri.getDelimiter().charAt(0));
			file = ri.getOutputFile();
			final List<File> files = new ArrayList<File>();
			for (final String suffix : suffixes)
				files.add(new File(workingDir,file.getName() + suffix));
			try {
				previousRun = 0;
				final File merged = new File(workingDir,file.getName());
				merge(files.toArray(new File[files.size()]),merged,false);
				result.add(merged);
			} catch (final PsSystemException e) {
				LOGGER.log(Level.SEVERE,"Result file merging failed.",e);
			}
		}
		return result;
	}

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void merge(final File[] parts, final File merged, final boolean sort) throws PsSystemException {
		if (parts == null || parts.length == 0)
			throw new PsSystemException("No parts for recorder " + merged.getAbsolutePath() + " found");
		
		try {
			merged.createNewFile();
		} catch (final IOException e) {
			throw new PsSystemException(e);
		}
		
		if (sort)
			Arrays.sort(parts,inner ? new PrtsComparator() : new PartsComparator());
		try {
			final BufferedWriter bwriter = new BufferedWriter(new FileWriter(merged));
			
			// handle header separately
			BufferedReader breader = new BufferedReader(new FileReader(parts[0]));
			writeHeader(breader,bwriter);
			breader.close();
			
			for (int i = 0;i < parts.length;++i) {
				breader = new BufferedReader(new FileReader(parts[i]));
				writeBody(breader,bwriter);
				if (i == parts.length - 1)
					writeFooter(breader,bwriter);
				breader.close();
			}
			bwriter.close();
		} catch (final IOException e) {
			throw new PsSystemException(e);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void writeHeader(final BufferedReader src, final BufferedWriter dst) throws IOException {
		String line;
		while ((line = src.readLine()) != null && !line.startsWith("\"[run number]\"")) {
//		while (!(line = src.readLine()).startsWith("\"[run number]\"")) {
			dst.write(line);
			dst.newLine();
		}
		if (line != null) {
			dst.write(line);
			dst.newLine();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void writeBody(final BufferedReader src, final BufferedWriter dst) throws IOException, PsSystemException {
		String line;
//		while(!(line = src.readLine()).startsWith("\"[run number]\"")); // skip to body
		while((line = src.readLine()) != null && !line.startsWith("\"[run number]\"")); // skip to body
		
		long lastRun = 0;
//		while (!(line = src.readLine()).equals("")) {
		while ((line = src.readLine()) != null && !line.equals("")) {
			final int pos = line.indexOf(delimiter);
			String runStr = line.substring(0,pos).trim();
			runStr = runStr.replaceFirst("^\"+","");
			runStr = runStr.replaceFirst("\"+$","");
			try {
				final long run = Long.parseLong(runStr);
				dst.write((previousRun + run) + line.substring(pos));
				dst.newLine();
				lastRun = previousRun + run;
			} catch (NumberFormatException e) {
				throw new PsSystemException(e);
			}
		}
		previousRun = lastRun;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void writeFooter(final BufferedReader src, final BufferedWriter dst) throws IOException {
		String line, prev = "";
		while ((line = src.readLine()) != null)
			prev = line;
		dst.newLine();
		dst.write(prev);
	}
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	public static class PrtsComparator implements Comparator<File> { 

		//----------------------------------------------------------------------------------------------------
		public int compare(final File file1, final File file2) {
			final int index1 = file1.getName().lastIndexOf("prt");
			final String nostr1 = file1.getName().substring(index1 + 3);
			final int no1 = Integer.parseInt(nostr1);
			
			final int index2 = file2.getName().lastIndexOf("prt");
			final String nostr2 = file2.getName().substring(index2 + 3);
			final int no2 = Integer.parseInt(nostr2);
			return no1 - no2;
		}
	}
}
