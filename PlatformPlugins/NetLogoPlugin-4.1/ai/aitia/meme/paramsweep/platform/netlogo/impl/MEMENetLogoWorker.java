package ai.aitia.meme.paramsweep.platform.netlogo.impl;

import java.io.PrintWriter;

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

import scala.Function0;
import scala.List;
import scala.Tuple2;

public class MEMENetLogoWorker extends WorkerAssistant implements ProgressListener {
	
	//====================================================================================================
	// members
	
	private static Worker actualWorker = null;

	//====================================================================================================
	// methods
		
	//----------------------------------------------------------------------------------------------------
	public MEMENetLogoWorker(Protocol protocol) { 
		super(protocol);
		this.addListener(this);
	}
    
	//----------------------------------------------------------------------------------------------------
	public static void runExperiment(Workspace workspace, String xml, int format, String fileName, PrintWriter writer, char delimiter,
									 ProgressListener monitor) throws CompilerException, SAXException {
        List<Protocol> protocols = new ProtocolLoader(workspace).loadAll(xml);
        if (protocols.size() != 1)
            throw new IllegalStateException("the XML contains more than one experiment setup");
        else 
            runExperiment(workspace,protocols.apply(0),format,fileName,writer,delimiter,monitor);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void runExperiment(Workspace workspace, Protocol protocol, int format, String fileName, PrintWriter writer, char delimiter, ProgressListener monitor)
									 throws CompilerException {
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
        runExperiment(workspace,protocol,monitor);;
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void runExperiment(final Workspace workspace, Protocol protocol, ProgressListener monitor) {
		try {
	       if (monitor != null)
	    	   actualWorker.addListener(monitor);
	       actualWorker.run(workspace,new Function0<Workspace>() {
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
	public void measurementsTaken(Workspace w, int runNumber, int step, List values) {}
	public void runCompleted(Workspace w, int runNumber, int step) {}
	public void runStarted(Workspace w, int runNumber, List<Tuple2<String,Object>> runSettings) {}
	public void stepCompleted(Workspace w, int step) {}

	//----------------------------------------------------------------------------------------------------
	public void runtimeError(Workspace w, int i, Throwable t) {
		if (w instanceof MEMEHeadlessWorkspace) {
			final MEMEHeadlessWorkspace workspace = (MEMEHeadlessWorkspace) w;
			if (t instanceof LogoException) {
				final LogoException ex = (LogoException) t;
	        	workspace.lastLogoException = ex;
	        	if (ex instanceof EngineException) {
					final EngineException eex = (EngineException) ex;
					workspace.lastContext = eex.context;
					workspace.lastInstruction = eex.instruction;
				}
			} else
				Exceptions.handle(t);
		}
	}
}