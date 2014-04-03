package org.nlogo.lab;

import java.util.List;

import org.nlogo.lab.SpreadsheetExporter.Run;
import org.nlogo.nvm.Procedure;
import org.nlogo.nvm.Workspace;
import org.nlogo.util.JCL;

public class WorkerAssistant extends Worker {

        //====================================================================================================
        // members
        
        protected Procedure[] _metricProcedures;

        //====================================================================================================
        // methods

        //----------------------------------------------------------------------------------------------------
        public WorkerAssistant(Protocol protocol) {
                super(protocol);
        }
        
        //----------------------------------------------------------------------------------------------------
        @Override
        public void compile(Workspace workspace)  {
                super.compile(workspace);
                _metricProcedures = new Procedure[protocol().metrics().length()];
                for (int i = 0; i < _metricProcedures.length;i++)
                        try {
                                _metricProcedures[i] = workspace.compileReporter(protocol().metrics().apply(i));
                        } catch (final org.nlogo.api.CompilerException e) {
                                throw new org.nlogo.lab.WorkerAssistant.CompilerException(e);
                        }
        }
        
        //----------------------------------------------------------------------------------------------------
        @SuppressWarnings("unchecked")
        public List<Object[]> getMeasurements(Run run) { return JCL.seqToJavaList(run.measurements().toSeq()); } 

        //====================================================================================================
        // nested classes
        
        //----------------------------------------------------------------------------------------------------
        @SuppressWarnings("serial")
        public static class CompilerException extends RuntimeException {
                
                //====================================================================================================
                // methods
                
                //----------------------------------------------------------------------------------------------------
                public CompilerException(Throwable t) { super(t); }
        }
}