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
package org.nlogo.lab;

import java.util.List;

import org.nlogo.lab.SpreadsheetExporter.Run;
import org.nlogo.nvm.Procedure;
import org.nlogo.nvm.Workspace;

import scala.collection.JavaConversions;

public class WorkerAssistant extends Worker {

	//====================================================================================================
	// members
	
	protected Procedure[] metricProcedures;

	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public WorkerAssistant(final Protocol protocol) {
		super(protocol);
	}
	
	//----------------------------------------------------------------------------------------------------
	@Override
	public void compile(final Workspace workspace)  {
		super.compile(workspace);
		metricProcedures = new Procedure[protocol().metrics().length()];
		for (int i = 0; i < metricProcedures.length;i++)
			try {
				metricProcedures[i] = workspace.compileReporter(protocol().metrics().apply(i));
			} catch (final org.nlogo.api.CompilerException e) {
				throw new org.nlogo.lab.WorkerAssistant.CompilerException(e);
			}
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<Object[]> getMeasurements(final Run run) { return JavaConversions.bufferAsJavaList(run.measurements()); } 

	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("serial")
	public static class CompilerException extends RuntimeException {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public CompilerException(final Throwable exception) { super(exception); }
	}
}
