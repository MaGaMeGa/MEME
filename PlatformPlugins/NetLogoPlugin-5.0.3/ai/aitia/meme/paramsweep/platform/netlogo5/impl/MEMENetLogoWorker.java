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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.nlogo.api.LogoException;
import org.nlogo.api.WorldDimensions;
import org.nlogo.lab.Exporter;
import org.nlogo.lab.Protocol;
import org.nlogo.lab.ProtocolLoader;
import org.nlogo.lab.SpreadsheetExporter;
import org.nlogo.lab.Worker;
import org.nlogo.lab.WorkerAssistant;
import org.nlogo.nvm.EngineException;
import org.nlogo.nvm.LabInterface.ProgressListener;
import org.nlogo.nvm.Workspace;
import org.nlogo.util.Exceptions;
import org.xml.sax.SAXException;

import com.sun.xml.internal.txw2.IllegalSignatureException;

import scala.Tuple2;
import scala.collection.immutable.List;
import scala.runtime.AbstractFunction0;

public class MEMENetLogoWorker extends WorkerAssistant implements ProgressListener {
	
	//====================================================================================================
	// members
	
	private static Worker actualWorker = null;

	//====================================================================================================
	// methods
		
	//----------------------------------------------------------------------------------------------------
	public MEMENetLogoWorker(final Protocol protocol) { 
		super(protocol);
		this.addListener(this);
	}
    
	//----------------------------------------------------------------------------------------------------
	public static void runExperiment(final Workspace workspace, final String xml, final int format, final String fileName, final PrintWriter writer, 
									 final char delimiter, final ProgressListener monitor) throws CompilerException, SAXException {
        final List<Protocol> protocols = new ProtocolLoader(workspace).loadAll(xml);
        if (protocols.size() == 1)
        	runExperiment(workspace,protocols.apply(0),format,fileName,writer,delimiter,monitor);
        else 
        	throw new IllegalStateException("the XML contains more than one experiment setup");
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void runExperiment(final Workspace workspace, final Protocol protocol, final int format, final String fileName, final PrintWriter writer, 
									 final char delimiter, final ProgressListener monitor) throws CompilerException {
        Exporter exporter;
        switch	(format) {
        case 0 	: exporter = new AdvancedExperimentExporter(fileName,workspace.world(),protocol,writer);
        		  ((AdvancedExperimentExporter)exporter).setDelimiter(delimiter);
            	  break;
        case 1 	: exporter = new SpreadsheetExporter(fileName,new WorldDimensions(workspace.world().minPxcor(),workspace.world().maxPxcor(),
        											 workspace.world().minPycor(),workspace.world().maxPycor()),protocol,writer);
            	  break;
        case 2 	: exporter = null; // no export
        		  break;
        default : throw new IllegalArgumentException("unknown format: " + format);
        }
        
        actualWorker = new MEMENetLogoWorker(protocol);
        if (exporter != null)
        	actualWorker.addListener(exporter);
        runExperiment(workspace,protocol,monitor);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void runExperiment(final Workspace workspace, final Protocol protocol, final ProgressListener monitor) {
		try {
	       if (monitor != null)
	    	   actualWorker.addListener(monitor);
	       actualWorker.run(workspace,new AbstractFunction0<Workspace>() {
	    	   public Workspace apply() { return workspace; }
	       },1);
		} finally {
			actualWorker = null;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void abortExperiment() {
		if (actualWorker != null)
			actualWorker.abort();
	}
	
	//====================================================================================================
	// implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public void experimentAborted() {}
	public void experimentCompleted() {}
	public void experimentStarted() {}
	public void measurementsTaken(final Workspace workspace, final int runNumber, final int step, @SuppressWarnings("rawtypes") final List values) {}
	public void runCompleted(final Workspace workspace, final int runNumber, final int step) {}
	public void runStarted(final Workspace workspace, final int runNumber, final List<Tuple2<String,Object>> runSettings) {}
	public void stepCompleted(final Workspace workspace, final int step) {}

	//----------------------------------------------------------------------------------------------------
	public void runtimeError(final Workspace workspace, final int _, final Throwable exception) {
		if (workspace instanceof MEMEHeadlessWorkspace) {
			final MEMEHeadlessWorkspace _workspace = (MEMEHeadlessWorkspace) workspace;
			if (exception instanceof LogoException) {
				final LogoException ex = (LogoException) exception;
	        	_workspace.lastLogoException_$eq(ex);
	        	if (ex instanceof EngineException) {
					final EngineException eex = (EngineException) ex;
					_workspace.setLastContext(eex.context);
					_workspace.setLastInstruction(eex.instruction);
				}
			} else
				Exceptions.handle(exception);
		}
	}
}
