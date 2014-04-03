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
package ai.aitia.meme.paramsweep.platform.simphony.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import ai.aitia.meme.paramsweep.PsSystemException;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.platform.repast.impl.ResultFileMerger.MyFileNameFilter;
import ai.aitia.meme.paramsweep.platform.repast.impl.ResultFileMerger.PartsComparator;

public class SimphonyResultFileMerger {
	
	//====================================================================================================
	// members

	private static final Logger logger = Logger.getLogger(SimphonyResultFileMerger.class.getName());
	
	private String delimiter = null;
	private long previousRun = 0;
	
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
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
				merge(files,new File(workingDir,f.getName()));
				success.add(f);
			} catch (PsSystemException e) {
				// report error, but proceed
				logger.log(Level.SEVERE, "Result file merging failed", e);
			}
		}
		return success;
	}
	
	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void merge(File[] parts, File merged) throws PsSystemException {
		if (parts == null || parts.length == 0) 
			throw new PsSystemException("No parts for recorder " + merged.getAbsolutePath() + " found");
		
		try {
			merged.createNewFile();
		} catch (IOException e) {
			throw new PsSystemException(e);
		}
		
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
	
	//----------------------------------------------------------------------------------------------------
	private void writeHeader(BufferedReader src, BufferedWriter dst) throws IOException {
		String line;
		while (!(line = src.readLine()).startsWith("\"Run Number")) {
			dst.write(line);
			dst.newLine();
		}
		dst.write(line);
		dst.newLine();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void writeBody(BufferedReader src, BufferedWriter dst)  throws IOException, PsSystemException {
		String line;
		while (!(line = src.readLine()).startsWith("\"Run Number")); // skip to body
		
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
	
	//----------------------------------------------------------------------------------------------------
	private void writeFooter(BufferedReader src, BufferedWriter dst) throws IOException {
		String line;
		String prev = "";
		while ((line = src.readLine()) != null) 
			prev = line;
		dst.newLine();
		dst.newLine();
		dst.write(prev);
	}
}
