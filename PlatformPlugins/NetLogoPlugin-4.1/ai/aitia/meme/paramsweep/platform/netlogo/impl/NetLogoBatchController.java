package ai.aitia.meme.paramsweep.platform.netlogo.impl;

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
	
	private static final String experimentNamePrefix	= "experiment_";
	private static final String experimentNameSuffix 	= "_aitiaGenerated";
	private static final int MAX_NUMBER_OF_FILES		= 1000; 
	
	private List<IBatchListener> listeners = new ArrayList<IBatchListener>();
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
	private String setupCommand = null;
	private String goCommand = null;
	private String generatedModelName = null;
	
	private MEMEHeadlessWorkspace workspace = null;

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public NetLogoBatchController(String setupCommand, String goCommand, String generatedModelName) {
		this.setupCommand = setupCommand;
		this.goCommand = goCommand;
		this.generatedModelName = generatedModelName;
	}

	//====================================================================================================
	// implemented interfaces

	//----------------------------------------------------------------------------------------------------
	public void addBatchListener(IBatchListener listener) { listeners.add(listener); }
	public void removeBatchListener(IBatchListener listener) { listeners.remove(listener); }
	public void setModelPath(String path) {}
	
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
	public void setEntryPoint(String entryPoint) throws InvalidEntryPointException {
		if (entryPoint == null)
			throw new IllegalArgumentException("'entryPoint' cannot be null.");
		File f = new File(entryPoint);
		if (!f.exists() || f.isDirectory()) {
			String tmp = entryPoint.replace('/',File.separatorChar).replace('\\',File.separatorChar);
			int idx = tmp.lastIndexOf(File.separatorChar);
			f = new File(System.getProperty("user.dir") + File.separator + tmp.substring(idx + 1));

		}
		if (!f.exists() || f.isDirectory())
			throw new InvalidEntryPointException();
		if (generatedModelName != null) 
			f = new File(f.getParentFile(),generatedModelName + ".nlogo");
		this.model = f.getAbsolutePath();
		this.modelName = f.getName();
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setParameters(ParameterTree paramTree) {
		if (paramTree == null)
			throw new IllegalArgumentException("'paramTree' cannot be null.");
		this.paramTree = paramTree;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setRecorders(List<RecorderInfo> recorders) {
		if (recorders == null)
			throw new IllegalArgumentException("'recorders' cannot be null.");
		this.recorders = recorders;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void setStopCondition(String conditionCode) {
		if (conditionCode == null)
			throw new IllegalArgumentException("'conditionCode' cannot be null.");
		this.condition = conditionCode;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void startBatch() throws BatchException {
		try {
			Iterator<List<AbstractParameterInfo>> it = new NetLogoParameterPartitioner().partition(paramTree);
			Document xml = null;
			int fileCnt = 0;
			while (!stopped && it.hasNext()) {
				batchCount++;
				List<AbstractParameterInfo> combination = it.next();
				if (first) {
					xml = createXml(combination);
					first = false;
				} else
					setParameters(xml,combination);
				String xmlStr = createStringFromXML(xml);
				startSim(xmlStr);
				if (fileCnt++ == MAX_NUMBER_OF_FILES) {
					fileCnt = 0;
					prepareResult();
					deleteFileParts();
					final File f = recorders.get(0).getOutputFile();
					f.renameTo(new File(f.getParentFile(),f.getName() + ".prt000"));
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
	public void stepCompleted(Workspace workspace, int i) { fireTickChanged(i); }
	
	//----------------------------------------------------------------------------------------------------
	public void runtimeError(Workspace workspace, int i, Throwable throwable) {
		if (throwable instanceof CompilerException)
			throw new org.nlogo.lab.WorkerAssistant.CompilerException(throwable);
		else
			throw new RuntimeException(throwable);
	}
	
	//----------------------------------------------------------------------------------------------------
	public void experimentAborted() {}
	public void experimentCompleted() {}
	public void experimentStarted() {}
	public void runCompleted(Workspace workspace, int i, int j) {}
	public void runStarted(Workspace workspace, int i, scala.List<Tuple2<String,Object>> list) {}
	public void measurementsTaken(Workspace workspace, int i, int j, scala.List<Object> list) {}

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void fireTickChanged(int tick) {
		BatchEvent event = new BatchEvent(this,EventType.STEP_ENDED,tick);
		for (IBatchListener l : listeners)
			l.timeProgressed(event);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void fireRunChanged() {
		BatchEvent event = new BatchEvent(this,EventType.RUN_ENDED,batchCount);
		for (IBatchListener l : listeners) 
			l.runChanged(event);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void fireBatchEnded() {
		BatchEvent event = new BatchEvent(this,EventType.BATCH_ENDED);
		List<IBatchListener> _tempList = new ArrayList<IBatchListener>(listeners);
		for (IBatchListener l : _tempList)
			l.batchEnded(event);
	}
	
	//----------------------------------------------------------------------------------------------------
	private Document createXml(List<AbstractParameterInfo> combination) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document document = builder.newDocument();
		
		Element experiments = document.createElement(EXPERIMENTS);
		
		Element experiment = document.createElement(EXPERIMENT);
		experiment.setAttribute(NAME,experimentNamePrefix + modelName + experimentNameSuffix);
		experiment.setAttribute(REPETITIONS,"1");
		experiment.setAttribute(RUN_METRICS_EVERY_STEP,String.valueOf(runMetricsEveryStep()));
		
		Element setup = document.createElement(SETUP);
		setup.appendChild(document.createTextNode(isSystemDynamicsModel() ? NetLogoModelGenerator.SD_SETUP_METHOD : setupCommand));
		experiment.appendChild(setup);
		
		Element go = document.createElement(GO);
		go.appendChild(document.createTextNode(goCommand));
		experiment.appendChild(go);

		condition = condition.trim();
		if (condition.startsWith("{") && condition.endsWith("}")) {
			Element exitCondition = document.createElement(EXIT_CONDITION);
			exitCondition.appendChild(document.createTextNode(exitCondition()));
			experiment.appendChild(exitCondition);
		} else {
			Element timeLimit = document.createElement(TIME_LIMIT);
			timeLimit.setAttribute(STEPS,condition);
			experiment.appendChild(timeLimit);
		}
		
		
		for (RecordableInfo info : recorders.get(0).getRecordables()) {
			Element metric = document.createElement(METRIC);
			metric.appendChild(document.createTextNode(info.getAccessibleName()));
			experiment.appendChild(metric);
		}
		
		for (AbstractParameterInfo info : combination) {
			Element eVS = document.createElement(ENUMERATED_VALUE_SET);
			eVS.setAttribute(VARIABLE,info.getName());
			Element value = document.createElement(VALUE);
			value.setAttribute(VALUE,value(info));
			eVS.appendChild(value);
			experiment.appendChild(eVS);
		}
		
		experiments.appendChild(experiment);
		document.appendChild(experiments);
		return document;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void setParameters(Document document, List<AbstractParameterInfo> combination) {
		Element experiments = document.getDocumentElement();
		NodeList nl = experiments.getElementsByTagName(EXPERIMENT);
		Element experiment = (Element) nl.item(0);
		nl = experiment.getElementsByTagName(ENUMERATED_VALUE_SET);
		for (int i = 0;i < nl.getLength();++i) {
			Element eVS = (Element) nl.item(i);
			String variable = eVS.getAttribute(VARIABLE);
			NodeList values = eVS.getChildNodes();
			Element value = (Element) values.item(0);
			AbstractParameterInfo info = findParameterInfo(combination,variable);
			value.setAttribute(VALUE,value(info));
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean runMetricsEveryStep() {
		String recordType = recorders.get(0).getRecordType();
		return ModelGenerator.ITERATION.equals(recordType);
	}
	
	//----------------------------------------------------------------------------------------------------
	private String exitCondition() {
//		condition = condition.trim();
//		if (condition.startsWith("{") && condition.endsWith("}")) {
			String result = condition.substring(1,condition.length() - 1);
			return result.trim();
//		} else {
//			return "ticks >= " + condition; 
//		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private String value(AbstractParameterInfo info) {
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
	private AbstractParameterInfo findParameterInfo(List<AbstractParameterInfo> combination, String name) {
		for (AbstractParameterInfo info : combination) {
			if (info.getName().equals(name))
				return info;
		}
		throw new IllegalStateException("missing parameter");
	}
	
	//----------------------------------------------------------------------------------------------------
	private String createStringFromXML(Document document) throws Exception {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		transformerFactory.setAttribute("indent-number",4);
		Transformer transformer = transformerFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.INDENT,"yes");
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
		
		Source source = new DOMSource(document);
		StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        transformer.transform(source,result);
        
        return sw.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void startSim(String experiment) throws Exception {
		PrintWriter writer = new PrintWriter(recorderFile());
		char delimiter = recorders.get(0).getDelimiter().charAt(0);
		workspace = new MEMEHeadlessWorkspace();
		workspace.open(model);
		workspace.runExperiment(experiment,0,recorderFile().getPath(),writer,delimiter,this);
		workspace.dispose();
		fireRunChanged();
	}
	
	//----------------------------------------------------------------------------------------------------
	private File recorderFile() {
		RecorderInfo info = recorders.get(0);
		String prefix = "";
		if (batchCount < 10)
			prefix = "00";
		else if (batchCount < 100)
			prefix = "0";
		File file = new File(info.getOutputFile().getParentFile(),info.getOutputFile().getName() + ".prt" + prefix + batchCount);
		return file;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void prepareResult() {
		if (firstMerge) {
			firstMerge = false;
			File original = recorders.get(0).getOutputFile();
			final File file = new File(original.getParentFile(),original.getName() + ".prt000");
			if (file.exists()) // junk 
				file.delete();
		}
		
		File outputFile = recorders.get(0).getOutputFile();
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
			File original = recorders.get(0).getOutputFile();
			File file = new File(original.getParentFile(),original.getName() + ".prt001");
			file.renameTo(original);
		} else
			new NetLogoResultFileMerger(true).merge(recorders,workingDir);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void deleteFileParts() {
		File workingDir = recorders.get(0).getOutputFile().getParentFile();
		if (workingDir == null)
			workingDir = new File(".");
		File[] files = workingDir.listFiles();
		String pattern = ".*\\.prt\\d+";	
		for (File f : files) {
			if (!f.isDirectory() && f.getName().matches(pattern))
				f.delete();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isSystemDynamicsModel() {
		FileReader reader = null;
		try {
			reader = new FileReader(model);
			String source = new String();
			char[] buffer = new char[8096];
			int chars = 0;
			while ((chars = reader.read(buffer)) > 0) 
				source += String.valueOf(buffer,0,chars);
			return source.contains("org.nlogo.sdm.gui.AggregateDrawing");
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (reader != null)
				try { reader.close(); } catch (final IOException _) {}
		}
	}
}