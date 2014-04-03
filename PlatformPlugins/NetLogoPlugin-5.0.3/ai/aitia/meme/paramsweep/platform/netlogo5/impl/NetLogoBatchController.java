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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.nlogo.api.CompilerException;
import org.nlogo.nvm.Workspace;
import org.nlogo.nvm.LabInterface.ProgressListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import scala.Tuple2;
import ai.aitia.meme.paramsweep.batch.BatchEvent;
import ai.aitia.meme.paramsweep.batch.BatchException;
import ai.aitia.meme.paramsweep.batch.IBatchController;
import ai.aitia.meme.paramsweep.batch.IBatchListener;
import ai.aitia.meme.paramsweep.batch.InvalidEntryPointException;
import ai.aitia.meme.paramsweep.batch.BatchEvent.EventType;
import ai.aitia.meme.paramsweep.batch.output.RecordableInfo;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;
import ai.aitia.meme.paramsweep.platform.repast.impl.ModelGenerator;
import ai.aitia.meme.paramsweep.utils.Util;

public class NetLogoBatchController implements IBatchController, ProgressListener {

	//====================================================================================================
	// members
	
	//----------------------------------------------------------------------------------------------------
	// xml constants
	
	private static final String EXPERIMENTS				= "experiments";
	private static final String	EXPERIMENT				= "experiment";
	private static final String NAME					= "name";
	private static final String REPETITIONS				= "repetitions";
	private static final String RUN_METRICS_EVERY_STEP	= "runMetricsEveryStep";
	private static final String SETUP					= "setup";
	private static final String GO						= "go";
	private static final String EXIT_CONDITION			= "exitCondition";
	private static final String METRIC					= "metric";
	private static final String ENUMERATED_VALUE_SET	= "enumeratedValueSet";
	private static final String VARIABLE				= "variable";
	private static final String VALUE					= "value";
	private static final String TIME_LIMIT				= "timeLimit";
	private static final String STEPS					= "steps";
	
	//----------------------------------------------------------------------------------------------------
	
	private static final String EXPERIMENT_NAME_PREFIX	= "experiment_";
	private static final String EXPERIMENT_NAME_SUFFIX 	= "_aitiaGenerated";
	private static final int MAX_NUMBER_OF_FILES		= 1000; 
	
	private final List<IBatchListener> listeners = new ArrayList<IBatchListener>();
	private int noRuns = -1;
	private long batchCount = 0;
	private boolean stopped = false;
	private boolean first = true;
	private boolean firstMerge = true;
	
	private ParameterTree paramTree = null;
	private List<RecorderInfo> recorders = null;
	private String condition = null;
	private String model = null;
	private String modelName = null;
	private final String setupCommand;
	private final String goCommand;
	private final String generatedModelName;
	
	private MEMEHeadlessWorkspace workspace = null;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public NetLogoBatchController(final String setupCommand, final String goCommand, final String generatedModelName) {
		this.setupCommand = setupCommand;
		this.goCommand = goCommand;
		this.generatedModelName = generatedModelName;
	}

	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public void addBatchListener(final IBatchListener listener) { listeners.add(listener); }
	public void removeBatchListener(final IBatchListener listener) { listeners.remove(listener); }
	public void setModelPath(final String path) {}
	
	//----------------------------------------------------------------------------------------------------
	public int getNumberOfRuns() {
		if (noRuns < 0) {
//			if (paramTree == null) 
//				return 0;
//			int temp = 0;
//			for (Iterator<List<AbstractParameterInfo>> it = new NetLogoParameterPartitioner().partition(paramTree);it.hasNext();it.next(),temp++);
//			noRuns = temp;
			noRuns = (int) Util.calculateNumberOfRuns(paramTree,true);
		}
		return noRuns;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setEntryPoint(final String entryPoint) throws InvalidEntryPointException {
		if (entryPoint == null)
			throw new IllegalArgumentException("'entryPoint' cannot be null.");
		File file = new File(entryPoint);
		if (!file.exists() || file.isDirectory()) {
			final String tmp = entryPoint.replace('/',File.separatorChar).replace('\\',File.separatorChar);
			final int idx = tmp.lastIndexOf(File.separatorChar);
			file = new File(System.getProperty("user.dir") + File.separator + tmp.substring(idx + 1));

		}
		
		if (!file.exists() || file.isDirectory())
			throw new InvalidEntryPointException();
		
		if (generatedModelName != null) 
			file = new File(file.getParentFile(),generatedModelName + ".nlogo");
		
		this.model = file.getAbsolutePath();
		this.modelName = file.getName();
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setParameters(final ParameterTree paramTree) {
		if (paramTree == null)
			throw new IllegalArgumentException("'paramTree' cannot be null.");
		this.paramTree = paramTree;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setRecorders(final List<RecorderInfo> recorders) {
		if (recorders == null)
			throw new IllegalArgumentException("'recorders' cannot be null.");
		this.recorders = recorders;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setStopCondition(final String conditionCode) {
		if (conditionCode == null)
			throw new IllegalArgumentException("'conditionCode' cannot be null.");
		this.condition = conditionCode;
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	public void startBatch() throws BatchException {
		try {
			final Iterator<List<AbstractParameterInfo>> iterator = new NetLogoParameterPartitioner().partition(paramTree);
			Document xml = null;
			int fileCnt = 0;
			while (!stopped && iterator.hasNext()) {
				batchCount++;
				final List<AbstractParameterInfo> combination = iterator.next();
				if (first) {
					xml = createXml(combination);
					first = false;
				} else
					setParameters(xml,combination);
				final String xmlStr = createStringFromXML(xml);
				startSim(xmlStr);
				if (fileCnt++ == MAX_NUMBER_OF_FILES) {
					fileCnt = 0;
					prepareResult();
					deleteFileParts();
					final File file = recorders.get(0).getOutputFile();
					file.renameTo(new File(file.getParentFile(),file.getName() + ".prt000"));
				}
			}
			prepareResult();
			deleteFileParts();
			fireBatchEnded();
		} catch (Exception e) {
			throw new BatchException(e);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void stopCurrentRun() throws BatchException {
		if (workspace != null) {
			workspace.abortExperiment();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void stopBatch() throws BatchException { 
		stopped = true;
		stopCurrentRun();
	}
	
	//====================================================================================================
	
	//----------------------------------------------------------------------------------------------------
	public void stepCompleted(final Workspace workspace, final int step) { fireTickChanged(step); }
	
	//----------------------------------------------------------------------------------------------------
	public void runtimeError(final Workspace workspace, final int _, final Throwable throwable) {
		if (throwable instanceof CompilerException)
			throw new org.nlogo.lab.WorkerAssistant.CompilerException(throwable);
		else
			throw new RuntimeException(throwable);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void experimentAborted() {}
	public void experimentCompleted() {}
	public void experimentStarted() {}
	public void runCompleted(final Workspace workspace, final int i, final int j) {}
	public void runStarted(final Workspace workspace, final int i, final scala.collection.immutable.List<Tuple2<String,Object>> list) {}
	public void measurementsTaken(final Workspace workspace, final int i, final int j, final scala.collection.immutable.List<Object> list) {}

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void fireTickChanged(final int tick) {
		final BatchEvent event = new BatchEvent(this,EventType.STEP_ENDED,tick);
		for (final IBatchListener l : listeners)
			l.timeProgressed(event);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void fireRunChanged() {
		final BatchEvent event = new BatchEvent(this,EventType.RUN_ENDED,batchCount);
		for (final IBatchListener l : listeners) 
			l.runChanged(event);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void fireBatchEnded() {
		final BatchEvent event = new BatchEvent(this,EventType.BATCH_ENDED);
		final List<IBatchListener> _tempList = new ArrayList<IBatchListener>(listeners);
		for (final IBatchListener l : _tempList)
			l.batchEnded(event);
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	private Document createXml(final List<AbstractParameterInfo> combination) throws Exception {
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder builder = factory.newDocumentBuilder();
		final Document document = builder.newDocument();
		
		final Element experiments = document.createElement(EXPERIMENTS);
		
		final Element experiment = document.createElement(EXPERIMENT);
		experiment.setAttribute(NAME,EXPERIMENT_NAME_PREFIX + modelName + EXPERIMENT_NAME_SUFFIX);
		experiment.setAttribute(REPETITIONS,"1");
		experiment.setAttribute(RUN_METRICS_EVERY_STEP,String.valueOf(runMetricsEveryStep()));
		
		final Element setup = document.createElement(SETUP);
		setup.appendChild(document.createTextNode(isSystemDynamicsModel() ? NetLogoModelGenerator.SD_SETUP_METHOD : setupCommand));
		experiment.appendChild(setup);
		
		final Element go = document.createElement(GO);
		go.appendChild(document.createTextNode(goCommand));
		experiment.appendChild(go);

		condition = condition.trim();
		if (condition.charAt(0) == '{' && condition.endsWith("}")) {
			final Element exitCondition = document.createElement(EXIT_CONDITION);
			exitCondition.appendChild(document.createTextNode(exitCondition()));
			experiment.appendChild(exitCondition);
		} else {
			final Element timeLimit = document.createElement(TIME_LIMIT);
			timeLimit.setAttribute(STEPS,condition);
			experiment.appendChild(timeLimit);
		}
		
		for (final RecordableInfo info : recorders.get(0).getRecordables()) {
			final Element metric = document.createElement(METRIC);
			metric.appendChild(document.createTextNode(info.getAccessibleName()));
			experiment.appendChild(metric);
		}
		
		for (final AbstractParameterInfo info : combination) {
			final Element eVS = document.createElement(ENUMERATED_VALUE_SET);
			eVS.setAttribute(VARIABLE,info.getName());
			final Element value = document.createElement(VALUE);
			value.setAttribute(VALUE,value(info));
			eVS.appendChild(value);
			experiment.appendChild(eVS);
		}
		
		experiments.appendChild(experiment);
		document.appendChild(experiments);
		return document;
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	private void setParameters(final Document document, final List<AbstractParameterInfo> combination) {
		final Element experiments = document.getDocumentElement();
		NodeList nodeList = experiments.getElementsByTagName(EXPERIMENT);
		final Element experiment = (Element) nodeList.item(0);
		nodeList = experiment.getElementsByTagName(ENUMERATED_VALUE_SET);
		for (int i = 0;i < nodeList.getLength();++i) {
			final Element eVS = (Element) nodeList.item(i);
			final String variable = eVS.getAttribute(VARIABLE);
			final NodeList values = eVS.getChildNodes();
			final Element value = (Element) values.item(0);
			final AbstractParameterInfo info = findParameterInfo(combination,variable);
			value.setAttribute(VALUE,value(info));
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean runMetricsEveryStep() {
		final String recordType = recorders.get(0).getRecordType();
		return ModelGenerator.ITERATION.equals(recordType);
	}
	
	//----------------------------------------------------------------------------------------------------
	private String exitCondition() {
//		condition = condition.trim();
//		if (condition.startsWith("{") && condition.endsWith("}")) {
			final String result = condition.substring(1,condition.length() - 1);
			return result.trim();
//		} else {
//			return "ticks >= " + condition; 
//		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private String value(@SuppressWarnings("rawtypes") final AbstractParameterInfo info) {
		if (info.getDefaultValue().getClass().equals(String.class)) {
			String result = info.iterator().next().toString();
			if (!result.startsWith("\""))
				result = "\"" + result;
			if (!result.endsWith("\""))
				result += "\"";
			return result;
		} else 
			return String.valueOf(info.iterator().next());
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("rawtypes")
	private AbstractParameterInfo findParameterInfo(final List<AbstractParameterInfo> combination, final String name) {
		for (final AbstractParameterInfo info : combination) {
			if (info.getName().equals(name))
				return info;
		}
		throw new IllegalStateException("missing parameter");
	}
	
	//----------------------------------------------------------------------------------------------------
	private String createStringFromXML(final Document document) throws Exception {
		final TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformerFactory.setAttribute("indent-number",4);
		final Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT,"yes");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
		
		final Source source = new DOMSource(document);
		final StringWriter writer = new StringWriter();
        final StreamResult result = new StreamResult(writer);
        transformer.transform(source,result);
        
        return writer.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void startSim(final String experiment) throws Exception {
		final PrintWriter writer = new PrintWriter(recorderFile());
		final char delimiter = recorders.get(0).getDelimiter().charAt(0);
		// netlogo uses the contextclassloader to load classes, therefore we
		// should set it to the one that loaded the netlogo plugin
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
		try {
			workspace = MEMEHeadlessWorkspace.newWorkspace();
			workspace.open(model);
			workspace.runExperiment(experiment,0,recorderFile().getPath(),writer,delimiter,this);
			workspace.dispose();
			fireRunChanged();
		} finally {
			Thread.currentThread().setContextClassLoader(contextClassLoader);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private File recorderFile() {
		final RecorderInfo info = recorders.get(0);
		String prefix = "";
		if (batchCount < 10)
			prefix = "00";
		else if (batchCount < 100)
			prefix = "0";
		return new File(info.getOutputFile().getParentFile(),info.getOutputFile().getName() + ".prt" + prefix + batchCount);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void prepareResult() {
		if (firstMerge) {
			firstMerge = false;
			final File original = recorders.get(0).getOutputFile();
			final File file = new File(original.getParentFile(),original.getName() + ".prt000");
			if (file.exists()) // junk 
				file.delete();
		}
		
		final File outputFile = recorders.get(0).getOutputFile();
		if (outputFile.exists()) {
			File newFile = new File(outputFile.getAbsolutePath() + ".bak1");
			int idx = 2;
			while (newFile.exists())
				newFile = new File(outputFile.getAbsolutePath() + ".bak" + idx++);
			outputFile.renameTo(newFile);
		}
		
		File workingDir = outputFile.getParentFile();
		if (workingDir == null)
			workingDir = new File(".");
		if (getNumberOfRuns() == 1) {
			final File original = recorders.get(0).getOutputFile();
			final File file = new File(original.getParentFile(),original.getName() + ".prt001");
			file.renameTo(original);
		} else
			new NetLogoResultFileMerger(true).merge(recorders,workingDir);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void deleteFileParts() {
		File workingDir = recorders.get(0).getOutputFile().getParentFile();
		if (workingDir == null)
			workingDir = new File(".");
		
		final File[] files = workingDir.listFiles();
		final String pattern = ".*\\.prt\\d+";	
		for (final File f : files) {
			if (!f.isDirectory() && f.getName().matches(pattern))
				f.delete();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isSystemDynamicsModel() {
		FileReader reader = null;
		try {
			reader = new FileReader(model);
			final StringBuffer source = new StringBuffer();
			final char[] buffer = new char[8096];
			int chars = 0;
			while ((chars = reader.read(buffer)) > 0) 
				source.append(String.valueOf(buffer,0,chars));
			return source.toString().contains("org.nlogo.sdm.gui.AggregateDrawing");
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (reader != null)
				try { reader.close(); } catch (final IOException _) {}
		}
	}
}
