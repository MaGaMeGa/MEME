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
package ai.aitia.meme.paramsweep.generator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;

public class QCGJobDescriptorGenerator {

	//====================================================================================================
	// members
	
	private static final long A_WEEK = 60 * 24 * 7;
	private static final String JOB_DESCRIPTOR_NAME = "qcgJobDescriptor.xml";
	
	public static final String GRMS_JOB									= "grmsJob";
	public static final String APP_ID									= "appId";
	public static final String XMLNS_XSI								= "xmlns:xsi";
	public static final String XSI_NO_NAMESPACE_SCHEMA_LOCATION			= "xsi:noNamespaceSchemaLocation";
	public static final String TASK										= "task";
	public static final String TASK_ID									= "taskId";
	public static final String REQUIREMENTS								= "requirements";
	public static final String TOPOLOGY									= "topology";
	public static final String PROCESSES								= "processes";
	public static final String PROCESSES_ID								= "processesId";
	public static final String CLUSTER									= "cluster";
	public static final String PROCESSES_COUNT							= "processesCount";
	public static final String VALUE									= "value";
	public static final String MASTER_GROUP								= "masterGroup";
	public static final String EXECUTION								= "execution";
	public static final String TYPE										= "type";
	public static final String EXECUTABLE								= "executable";
	public static final String APPLICATION								= "application";
	public static final String NAME										= "name";
	public static final String APP_PROPERTY								= "appProperty";
	public static final String ARGUMENTS								= "arguments";
	public static final String STAGE_IN_OUT								= "stageInOut";
	public static final String FILE										= "file";
	public static final String LOCATION									= "location";
	public static final String EXECUTION_TIME							= "executionTime";
	public static final String EXECUTION_DURATION						= "executionDuration";
	
	private static final String APP_ID_VALUE							= "usecase9";
	private static final String XSI_NO_NAMESPACE_SCHEMA_LOCATION_VALUE	= "..\\schema\\GrmsJobDescriptionSchema-0.4.xsd";
	private static final String PROCESSES_ID_VALUE						= "Net1";
	private static final String PROCESSES_ID_MASTER_VALUE				= "Master";
	private static final String CLUSTER_VALUE							= "dort2";
	
	private static final String XMLNS_XSI_VALUE							= "http://www.w3.org/2001/XMLSchema-instance";
	private static final String EXECUTION_TYPE_VALUE					= "proactive";
	private static final String APPLICATION_NAME_VALUE					= "qcg_proactive";
	private static final String APP_PROPERTY_VERSION					= "version";
	private static final String APP_PROPERTY_VERSION_VALUE				= "3.9";
	private static final String APP_PROPERTY_JAVA_VERSION				= "javaVersion";
	private static final String APP_PROPERTY_JAVA_VERISON_VALUE 		= "1.6";
	private static final String ARGUMENT_HEAP_SPACE						= "-Xmx1024M";
	private static final String ARGUMENT_CP								= "-cp";
	private static final String ARGUMENT_JAVA_PRG						= "ai.aitia.meme.paramsweep.qcg.QCGSimulationMaster";
	private static final String FILE_TYPE_IN							= "in";
	private static final String FILE_TYPE_OUT							= "out";
	private static final String LOCATION_TYPE_URL						= "URL";
	private static final String EXECUTION_DURATION_VALUE				= "PT" + A_WEEK + "MOS";
	
	private String jobName = null;
	private List<RecorderInfo> recorders = null;
	private File jobDir = null;
	private JobDescriptorType descriptorType = null; 
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public QCGJobDescriptorGenerator(JobDescriptorType descriptorType, File jobDir, String jobName, List<RecorderInfo> recorders) {
		if (descriptorType == null || jobDir == null || jobName == null || recorders == null || recorders.size() == 0)
			throw new IllegalArgumentException();
		this.descriptorType = descriptorType;
		this.jobDir = jobDir;
		this.jobName = jobName;
		this.recorders = recorders;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void generateJobDescriptor() throws Exception {
		switch (descriptorType) {
		case BASIC_DORTMUND	: generateBasicDortmundDescriptor(); 
							  break;
		default 			: throw new UnknownJobDescriptorTypeException("Unknown job descriptor type: " + descriptorType.toString());
		}
	}

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void generateBasicDortmundDescriptor() throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();
		
		Element root = document.createElement(GRMS_JOB);
		root.setAttribute(APP_ID,APP_ID_VALUE);
		root.setAttribute(XMLNS_XSI,XMLNS_XSI_VALUE);
		root.setAttribute(XSI_NO_NAMESPACE_SCHEMA_LOCATION,XSI_NO_NAMESPACE_SCHEMA_LOCATION_VALUE);
		
		Element task = document.createElement(TASK);
		task.setAttribute(TASK_ID,jobName);
		
		Element requirements = document.createElement(REQUIREMENTS);
		Element topology = document.createElement(TOPOLOGY);
		
		Element processes = document.createElement(PROCESSES);
		processes.setAttribute(PROCESSES_ID,PROCESSES_ID_VALUE);
		processes.setAttribute(CLUSTER,CLUSTER_VALUE);
		Element processesCount = document.createElement(PROCESSES_COUNT);
		Element value = document.createElement(VALUE);
		value.appendChild(document.createTextNode(String.valueOf(ParameterSweepWizard.getPreferences().getNumberOfRequestedWorkers())));
		processesCount.appendChild(value);
		processes.appendChild(processesCount);
		topology.appendChild(processes);
		
		processes = document.createElement(PROCESSES);
		processes.setAttribute(PROCESSES_ID,PROCESSES_ID_MASTER_VALUE);
		processes.setAttribute(MASTER_GROUP,String.valueOf(true));
		processes.setAttribute(CLUSTER,CLUSTER_VALUE);
		processesCount = document.createElement(PROCESSES_COUNT);
		value = document.createElement(VALUE);
		value.appendChild(document.createTextNode("1"));
		processesCount.appendChild(value);
		processes.appendChild(processesCount);
		topology.appendChild(processes);
		
		requirements.appendChild(topology);
		task.appendChild(requirements);
		
		Element execution = document.createElement(EXECUTION);
		execution.setAttribute(TYPE,EXECUTION_TYPE_VALUE);
		
		Element executable = document.createElement(EXECUTABLE);
		Element application = document.createElement(APPLICATION);
		application.setAttribute(NAME,APPLICATION_NAME_VALUE);
		Element appProperty = document.createElement(APP_PROPERTY);
		appProperty.setAttribute(NAME,APP_PROPERTY_VERSION);
		appProperty.appendChild(document.createTextNode(APP_PROPERTY_VERSION_VALUE));
		application.appendChild(appProperty);
		appProperty = document.createElement(APP_PROPERTY);
		appProperty.setAttribute(NAME,APP_PROPERTY_JAVA_VERSION);
		appProperty.appendChild(document.createTextNode(APP_PROPERTY_JAVA_VERISON_VALUE));
		application.appendChild(appProperty);
		executable.appendChild(application);
		execution.appendChild(executable);
		
		Element arguments = document.createElement(ARGUMENTS);
		
		value = document.createElement(VALUE);
		value.appendChild(document.createTextNode(ARGUMENT_HEAP_SPACE));
		arguments.appendChild(value);
		
		value = document.createElement(VALUE);
		value.appendChild(document.createTextNode(ARGUMENT_CP));
		arguments.appendChild(value);
		
		value = document.createElement(VALUE);
		value.appendChild(document.createTextNode(jobName + ".jar"));
		arguments.appendChild(value);
		
		value = document.createElement(VALUE);
		value.appendChild(document.createTextNode(ARGUMENT_JAVA_PRG));
		arguments.appendChild(value);
		
		value = document.createElement(VALUE);
		value.appendChild(document.createTextNode(jobName + ".settings.xml"));
		arguments.appendChild(value);
		
		value = document.createElement(VALUE);
		value.appendChild(document.createTextNode(String.valueOf(ParameterSweepWizard.getPreferences().rngSeedAsParameter())));
		arguments.appendChild(value);
		
		execution.appendChild(arguments);
		
		Element stageInOut = document.createElement(STAGE_IN_OUT);
		
		generateInputSection(stageInOut);
		generateOutputSection(stageInOut);
		
		execution.appendChild(stageInOut);
		task.appendChild(execution);
		
		Element executionTime = document.createElement(EXECUTION_TIME);
		Element executionDuration = document.createElement(EXECUTION_DURATION);
		executionDuration.appendChild(document.createTextNode(EXECUTION_DURATION_VALUE));
		executionTime.appendChild(executionDuration);
		task.appendChild(executionTime);
		
		root.appendChild(task);
		document.appendChild(root);
		
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformerFactory.setAttribute("indent-number",4);
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT,"yes");
		Source source = new DOMSource(document);
		FileOutputStream os = new FileOutputStream(new File(jobDir,JOB_DESCRIPTOR_NAME));
		Result result = new StreamResult(new OutputStreamWriter(os,"utf-8"));
		transformer.transform(source,result);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateInputSection(Element stageInOut) {
		Document document = stageInOut.getOwnerDocument();
		File[] files = jobDir.listFiles();
		for (File f : files) {
			Element file = document.createElement(FILE);
			file.setAttribute(NAME,f.getName());
			file.setAttribute(TYPE,FILE_TYPE_IN);
			
			Element location = document.createElement(LOCATION);
			location.setAttribute(TYPE,LOCATION_TYPE_URL);
			location.appendChild(document.createTextNode(f.getName()));
			file.appendChild(location);
			
			stageInOut.appendChild(file);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void generateOutputSection(Element stageInOut) {
		Document document = stageInOut.getOwnerDocument();
		for (RecorderInfo ri : recorders) {
			Element file = document.createElement(FILE);
			file.setAttribute(NAME,ri.getOutputFile().getName());
			file.setAttribute(TYPE,FILE_TYPE_OUT);
			
			Element location = document.createElement(LOCATION);
			location.setAttribute(TYPE,LOCATION_TYPE_URL);
			location.appendChild(document.createTextNode(ri.getOutputFile().getName()));
			file.appendChild(location);
			
			stageInOut.appendChild(file);
		}
	}

	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	public static enum JobDescriptorType { BASIC_DORTMUND };
	
	//----------------------------------------------------------------------------------------------------
	public static class UnknownJobDescriptorTypeException extends Exception {
		
		//====================================================================================================
		// members
		
		private static final long serialVersionUID = 1L;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public UnknownJobDescriptorTypeException() { super(); }
		public UnknownJobDescriptorTypeException(String msg) { super(msg); }
	}
}
