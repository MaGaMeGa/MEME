package ai.aitia.meme.paramsweep.platform.netlogo.impl;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.nlogo.agent.World;
import org.nlogo.api.Dump;
import org.nlogo.api.WorldDimensions;
import org.nlogo.lab.Exporter;
import org.nlogo.lab.Protocol;
import org.nlogo.nvm.Workspace;

import scala.List;
import scala.Tuple2;

public class AdvancedExperimentExporter extends Exporter {
	
	//====================================================================================================
	// members
	
	public static boolean defaultIncludeExportHeaders = true;
	
	private char delimiter = '|';
	boolean includeExportHeaders;
	
	private Protocol protocol;
	private PrintWriter writer;
	private List<Tuple2<?,?>> runSettings;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public AdvancedExperimentExporter(String fileName, World world, Protocol protocol, PrintWriter writer) {
    	super(fileName,new WorldDimensions(world.minPxcor(),world.maxPxcor(),world.minPycor(),world.maxPycor()),protocol,writer);
        includeExportHeaders = defaultIncludeExportHeaders;
        this.protocol = protocol;
        this.writer = writer;
    }
	
	//----------------------------------------------------------------------------------------------------
	public char getDelimiter() { return delimiter; }
	public void setDelimiter(char delimiter) { this.delimiter = delimiter; }
	public boolean includeExportHeaders() {	return includeExportHeaders; }
	public void setIncludeExportHeaders(boolean val) { includeExportHeaders = val; }

	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public void writeExperimentHeader() {
        writer.print(Dump.csv.header("[run number]"));
        for (int i = 0;i < protocol.valueSets().length();i++) {
        	writer.print(delimiter);
	        writer.print(Dump.csv.header(protocol.valueSets().apply(i).variableName()));
	    }
        writer.print(delimiter);
        writer.print(Dump.csv.header("[step]"));
        for (int i = 0;i < protocol.metrics().length();i++) {
        	writer.print(delimiter);
	        writer.print(Dump.csv.header(protocol.metrics().apply(i)));
	    }
        writer.println();
	}

	//----------------------------------------------------------------------------------------------------
	@Override
	public void writeExportHeader() {
		Date currentDate = new Date();
		SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		writer.println(Dump.csv.header("Timestamp: " + dateFormatter.format(currentDate)));
		writer.println();
		writer.println();
	}
	
	//----------------------------------------------------------------------------------------------------
	public void writeEnd() {
		Date currentDate = new Date();
		SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		writer.println();
		writer.println(Dump.csv.header("End time: " + dateFormatter.format(currentDate)));
	}
	
    //----------------------------------------------------------------------------------------------------
	@Override public void experimentCompleted() {
		writeEnd();
		writer.flush();
		writer.close();
	}
	
    //----------------------------------------------------------------------------------------------------
	@Override public void experimentAborted() { writer.close(); }
	@SuppressWarnings("unchecked") @Override public void runStarted(Workspace w, int runNumber, List runSettings) { this.runSettings = runSettings; }
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public void runCompleted(Workspace w, int runNumber, int step) { 
		writer.flush();
		runSettings = null;
	}

    //----------------------------------------------------------------------------------------------------
	@Override
	public void measurementsTaken(Workspace w, int runNumber, int step, List values) {
        if(!values.isEmpty())
            writeTableRow(runNumber,step,values);
    }

    //----------------------------------------------------------------------------------------------------
	@Override
	public void experimentStarted() {
		if (includeExportHeaders)
			writeExportHeader();
        writeExperimentHeader();
        writer.flush();
    }
	
	//====================================================================================================
	// assistant methods

	//----------------------------------------------------------------------------------------------------
	protected void writeTableRow(int runNumber, int step, List values) {
		writer.print(Dump.csv.data(runNumber));
		for (int j = 0;j < runSettings.length();j++) {
			writer.print(delimiter);
			writer.print(Dump.csv.data(runSettings.apply(j)._2())); 
		}
		writer.print(delimiter);
		writer.print(Dump.csv.data(step));
		for (int j = 0;j < values.length();j++) {
			writer.print(delimiter);
			writer.print(Dump.csv.data(values.apply(j)));
		}
		writer.println();
	}
}