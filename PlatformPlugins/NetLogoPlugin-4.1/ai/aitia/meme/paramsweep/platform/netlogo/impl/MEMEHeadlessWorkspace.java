package ai.aitia.meme.paramsweep.platform.netlogo.impl;

import java.io.PrintWriter;

import org.nlogo.agent.World;
import org.nlogo.agent.World3D;
import org.nlogo.api.CompilerException;
import org.nlogo.api.JobOwner;
import org.nlogo.api.LogoException;
import org.nlogo.api.Version;
import org.nlogo.compiler.Compiler$;
import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.nvm.Context;
import org.nlogo.nvm.Instruction;
import org.nlogo.nvm.LabInterface.ProgressListener;
import org.nlogo.render.Renderer;
import org.nlogo.sdm.AggregateManagerLite;
import org.nlogo.util.Exceptions;
import org.xml.sax.SAXException;

public class MEMEHeadlessWorkspace extends HeadlessWorkspace {
	
	//====================================================================================================
	// members
	
	Context lastContext = null;
	Instruction lastInstruction = null;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public MEMEHeadlessWorkspace() {
		this(Version.is3D() ? new World3D() : new World());
	}
		
	//----------------------------------------------------------------------------------------------------
	public MEMEHeadlessWorkspace(final World world) {
		super(world,Compiler$.MODULE$,new Renderer(world),new AggregateManagerLite());
	}
	
	//----------------------------------------------------------------------------------------------------
	public void runExperiment(String xml, int format, String fileName, PrintWriter writer, char delimiter, ProgressListener monitor) throws CompilerException,
																														   SAXException {
		this.lastContext = null;
		this.lastInstruction = null;
		MEMENetLogoWorker.runExperiment(this,xml,format,fileName,writer,delimiter,monitor);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void abortExperiment() {
		MEMENetLogoWorker.abortExperiment();
	}
	
	//----------------------------------------------------------------------------------------------------
    @Override
	public void runtimeError(JobOwner owner, Context context, Instruction instruction, Exception ex) {
        if (ex instanceof LogoException) {
        	lastLogoException = (LogoException) ex;
        	lastContext = context;
        	lastInstruction = instruction;
        } else
            Exceptions.handle(ex);
    }
    
    //----------------------------------------------------------------------------------------------------
	public Context getLastContext() { return lastContext; }
	public Instruction getLastInstruction() { return lastInstruction; }
}