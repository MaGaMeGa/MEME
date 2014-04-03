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

import org.nlogo.agent.World;
import org.nlogo.api.AggregateManagerInterface;
import org.nlogo.api.CompilerException;
import org.nlogo.api.JobOwner;
import org.nlogo.api.LogoException;
import org.nlogo.api.RendererInterface;
import org.nlogo.headless.HeadlessWorkspace;
import org.nlogo.nvm.CompilerInterface;
import org.nlogo.nvm.Context;
import org.nlogo.nvm.Instruction;
import org.nlogo.nvm.LabInterface.ProgressListener;
import org.nlogo.util.Exceptions;
import org.nlogo.workspace.AbstractWorkspace;
import org.xml.sax.SAXException;

public class MEMEHeadlessWorkspace extends HeadlessWorkspace {
	
	//====================================================================================================
	// members
	
	private Context lastContext = null;
	private Instruction lastInstruction = null;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
//	public MEMEHeadlessWorkspace() {
//		this(Version.is3D() ? new World3D() : new World());
//	}
		
	//----------------------------------------------------------------------------------------------------
//	public MEMEHeadlessWorkspace(final World world) {
//		super(world,Compiler$.MODULE$,new Renderer(world),new AggregateManagerLite(),null);
//	}
	
	//----------------------------------------------------------------------------------------------------
    public MEMEHeadlessWorkspace(final World world, final CompilerInterface compiler, final RendererInterface renderer, 
    							 final AggregateManagerInterface aggregateManager, final AbstractWorkspace.HubNetManagerFactory hubNetManagerFactory) {
    	super(world,compiler,renderer,aggregateManager,hubNetManagerFactory);
    	
    }
    
    //----------------------------------------------------------------------------------------------------
	public static MEMEHeadlessWorkspace newWorkspace() {
		return (MEMEHeadlessWorkspace) HeadlessWorkspace.newInstance(MEMEHeadlessWorkspace.class);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void runExperiment(final String xml, final int format, final String fileName, final PrintWriter writer, final char delimiter, final ProgressListener monitor)
																																 throws CompilerException, SAXException {
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
	public void runtimeError(final JobOwner owner, final Context context, final Instruction instruction, final Exception ex) {
        if (ex instanceof LogoException) {
        	final LogoException lastLogoException = (LogoException) ex;
        	lastLogoException_$eq(lastLogoException);
        	lastContext = context;
        	lastInstruction = instruction;
        } else
            Exceptions.handle(ex);
    }
    
    //----------------------------------------------------------------------------------------------------
	public Context getLastContext() { return lastContext; }
	public Instruction getLastInstruction() { return lastInstruction; }
	public void setLastContext(final Context lastContext) { this.lastContext = lastContext; }
	public void setLastInstruction(final Instruction lastInstruction) { this.lastInstruction = lastInstruction; }
}
