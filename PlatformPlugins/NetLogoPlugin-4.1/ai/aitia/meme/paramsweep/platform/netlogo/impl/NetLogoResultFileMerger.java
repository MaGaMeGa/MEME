package ai.aitia.meme.paramsweep.platform.netlogo.impl;

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
	
	private static final Logger logger = Logger.getLogger(NetLogoResultFileMerger.class.getName());
	private String delimiter = null;
	private long previousRun = 0;
	private boolean inner = false;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public NetLogoResultFileMerger(boolean inner) {
		this.inner = inner;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<File> merge(List<RecorderInfo> recorders, File workingDir) {
		List<File> success = new ArrayList<File>();
		File f;
		String namePattern;
		File[] files;
		for (RecorderInfo ri : recorders) {
			delimiter = String.valueOf(ri.getDelimiter().charAt(0));
			f = ri.getOutputFile();
			namePattern = ".*" + f.getName() + (inner ? ".prt\\d+" : ".part\\d+");
			files = workingDir.listFiles(new MyFileNameFilter(namePattern));
			try {
				previousRun = 0;
				final File merged = new File(workingDir,f.getName());
				merge(files,merged,true);
				success.add(merged);
			} catch (PsSystemException e) {
				logger.log(Level.SEVERE,"Result file merging failed",e);
			}
		}
		return success;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<File> merge(final List<RecorderInfo> recorders, final List<String> suffixes, final File workingDir) {
		List<File> result = new ArrayList<File>();
		File f;
		for (final RecorderInfo ri : recorders) {
			delimiter = String.valueOf(ri.getDelimiter().charAt(0));
			f = ri.getOutputFile();
			final List<File> files = new ArrayList<File>();
			for (final String suffix : suffixes)
				files.add(new File(workingDir,f.getName() + suffix));
			try {
				previousRun = 0;
				final File merged = new File(workingDir,f.getName());
				merge(files.toArray(new File[files.size()]),merged,false);
				result.add(merged);
			} catch (final PsSystemException e) {
				logger.log(Level.SEVERE,"Result file merging failed.",e);
			}
		}
		return result;
	}

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void merge(File[] parts, File merged, boolean sort) throws PsSystemException {
		if (parts == null || parts.length == 0)
			throw new PsSystemException("No parts for recorder " + merged.getAbsolutePath() + " found");
		
		try {
			merged.createNewFile();
		} catch (IOException e) {
			throw new PsSystemException(e);
		}
		
		if (sort)
			Arrays.sort(parts,inner ? new PrtsComparator() : new PartsComparator());
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(merged));
			
			// handle header separately
			BufferedReader br = new BufferedReader(new FileReader(parts[0]));
			writeHeader(br,bw);
			br.close();
			
			for (int i = 0;i < parts.length;++i) {
				br = new BufferedReader(new FileReader(parts[i]));
				writeBody(br,bw);
				if (i == parts.length - 1)
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
		while (!(line = src.readLine()).startsWith("\"[run number]\"")) {
			dst.write(line);
			dst.newLine();
		}
		dst.write(line);
		dst.newLine();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void writeBody(BufferedReader src, BufferedWriter dst) throws IOException, PsSystemException {
		String line;
		while(!(line = src.readLine()).startsWith("\"[run number]\"")); // skip to body
		
		long lastRun = 0;
		while (!(line = src.readLine()).equals("")) {
			int pos = line.indexOf(delimiter);
			String runStr = line.substring(0,pos).trim();
			runStr = runStr.replaceFirst("^\"+","");
			runStr = runStr.replaceFirst("\"+$","");
			try {
				long run = Long.parseLong(runStr);
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
	private void writeFooter(BufferedReader src, BufferedWriter dst) throws IOException {
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
		public int compare(File f1, File f2) {
			int index1 = f1.getName().lastIndexOf("prt");
			String nostr1 = f1.getName().substring(index1 + 3);
			int no1 = Integer.parseInt(nostr1);
			
			int index2 = f2.getName().lastIndexOf("prt");
			String nostr2 = f2.getName().substring(index2 + 3);
			int no2 = Integer.parseInt(nostr2);
			return no1 - no2;
		}
	}
}