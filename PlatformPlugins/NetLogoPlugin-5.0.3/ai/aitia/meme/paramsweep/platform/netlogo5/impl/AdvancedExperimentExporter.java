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

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.nlogo.agent.World;
import org.nlogo.api.Dump;
import org.nlogo.api.WorldDimensions;
import org.nlogo.lab.Exporter;
import org.nlogo.lab.Protocol;
import org.nlogo.nvm.Workspace;

import scala.Tuple2;
import scala.collection.immutable.List;

public class AdvancedExperimentExporter extends Exporter {
	
	//====================================================================================================
	// members
	
	public static boolean defaultIncludeExportHeaders = true;
	
	private char delimiter = '|';
	private boolean includeExportHeaders;
	
	private final Protocol protocol;
	private final PrintWriter writer;
	private List<Tuple2<?,?>> runSettings;
	
	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public AdvancedExperimentExporter(final String fileName, final World world, final Protocol protocol, final PrintWriter writer) {
    	super(fileName,new WorldDimensions(world.minPxcor(),world.maxPxcor(),world.minPycor(),world.maxPycor()),protocol,writer);
        includeExportHeaders = defaultIncludeExportHeaders;
        this.protocol = protocol;
        this.writer = writer;
    }
	
	//----------------------------------------------------------------------------------------------------
	public char getDelimiter() { return delimiter; }
	public void setDelimiter(final char delimiter) { this.delimiter = delimiter; }
	public boolean includeExportHeaders() {	return includeExportHeaders; }
	public void setIncludeExportHeaders(final boolean val) { includeExportHeaders = val; }

	//----------------------------------------------------------------------------------------------------
	public void writeExperimentHeader() {
        writer.print(Dump.csv().header("[run number]"));
        for (int i = 0;i < protocol.valueSets().length();i++) {
        	writer.print(delimiter);
	        writer.print(Dump.csv().header(protocol.valueSets().apply(i).variableName()));
	    }
        writer.print(delimiter);
        writer.print(Dump.csv().header("[step]"));
        for (int i = 0;i < protocol.metrics().length();i++) {
        	writer.print(delimiter);
	        writer.print(Dump.csv().header(protocol.metrics().apply(i)));
	    }
        writer.println();
	}

	//----------------------------------------------------------------------------------------------------
	@Override
	public void writeExportHeader() {
		final Date currentDate = new Date();
		final SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		writer.println(Dump.csv().header("Timestamp: " + dateFormatter.format(currentDate)));
		writer.println();
		writer.println();
	}
	
	//----------------------------------------------------------------------------------------------------
	public void writeEnd() {
		final Date currentDate = new Date();
		final SimpleDateFormat dateFormatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
		writer.println();
		writer.println(Dump.csv().header("End time: " + dateFormatter.format(currentDate)));
	}
	
    //----------------------------------------------------------------------------------------------------
	@Override public void experimentCompleted() {
		writeEnd();
		writer.flush();
		writer.close();
	}
	
    //----------------------------------------------------------------------------------------------------
	@Override public void experimentAborted() { writer.close(); }
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings({ "unchecked", "rawtypes" }) 
	@Override
	public void runStarted(final Workspace workspace, final int runNumber, final List runSettings) { 
		this.runSettings = runSettings;
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public void runCompleted(final Workspace workspace, final int runNumber, final int step) { 
		writer.flush();
		runSettings = null;
	}

    //----------------------------------------------------------------------------------------------------
	@Override
	public void measurementsTaken(final Workspace workspace, final int runNumber, final int step, @SuppressWarnings("rawtypes") final List values) {
        if (!values.isEmpty())
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
	protected void writeTableRow(final int runNumber, final int step, @SuppressWarnings("rawtypes") final List values) {
		writer.print(Dump.csv().data(runNumber));
		for (int j = 0;j < runSettings.length();j++) {
			writer.print(delimiter);
			writer.print(Dump.csv().data(runSettings.apply(j)._2())); 
		}
		writer.print(delimiter);
		writer.print(Dump.csv().data(step));
		for (int j = 0;j < values.length();j++) {
			writer.print(delimiter);
			writer.print(Dump.csv().data(values.apply(j)));
		}
		writer.println();
	}
}
