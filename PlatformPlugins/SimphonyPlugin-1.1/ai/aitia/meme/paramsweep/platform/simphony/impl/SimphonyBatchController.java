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
package ai.aitia.meme.paramsweep.platform.simphony.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.TreeSet;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import repast.simphony.batch.BatchScenario;
import repast.simphony.batch.BatchScenarioCreator;
import repast.simphony.engine.controller.Controller;
import repast.simphony.engine.controller.DefaultController;
import repast.simphony.engine.environment.AbstractRunner;
import repast.simphony.engine.environment.ControllerRegistry;
import repast.simphony.engine.environment.DefaultRunEnvironmentBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.environment.RunEnvironmentBuilder;
import repast.simphony.engine.environment.RunListener;
import repast.simphony.engine.environment.RunState;
import repast.simphony.engine.schedule.ISchedule;
import repast.simphony.engine.schedule.Schedule;
import repast.simphony.parameter.ParameterConstants;
import repast.simphony.parameter.ParameterSetter;
import repast.simphony.parameter.Parameters;
import repast.simphony.parameter.ParametersCreator;
import repast.simphony.parameter.SweeperProducer;
import ai.aitia.meme.paramsweep.batch.BatchEvent;
import ai.aitia.meme.paramsweep.batch.BatchException;
import ai.aitia.meme.paramsweep.batch.IBatchController;
import ai.aitia.meme.paramsweep.batch.IBatchListener;
import ai.aitia.meme.paramsweep.batch.InvalidEntryPointException;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.batch.param.AbstractParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.IncrementalParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;
import ai.aitia.meme.paramsweep.batch.param.ParameterNode;
import ai.aitia.meme.paramsweep.batch.param.ParameterTree;
import ai.aitia.meme.paramsweep.utils.Util;

public class SimphonyBatchController extends AbstractRunner implements IBatchController, 
																	   RunListener {
	
	//=============================================================================================
	//members
	
	private static final String DEFAULT_DELIMITER = "|";
	
	protected String scenarioDir = null;
	protected String entryPoint = null;
	protected double stopCondition;
	protected ArrayList<IBatchListener> listeners;
	protected List<RecorderInfo> recorders;
	protected ParameterTree paramTree;

	private RunEnvironmentBuilder runEnvironmentBuilder;
	protected Controller controller;
	protected boolean pause = false;
	protected Object monitor = new Object();
	protected SweeperProducer producer;
	private ISchedule schedule;
	protected BatchScenarioCreator generatedCreator = null;
	private int noRuns = -1;
	private String generatedCreatorName = null;
	
	private String parameterHeader = null;
	private List<String> parameterValues = new ArrayList<String>();
	private Date start = null;
	private Date end = null;

	//=============================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	/** Sets the path to the model directory. This information is used
	 *  to locate the model to be run.
	 */
	public SimphonyBatchController(String generatedCreatorName) {
		listeners = new ArrayList<IBatchListener>();
		
		runEnvironmentBuilder = new DefaultRunEnvironmentBuilder(this,true);
		controller = new DefaultController(runEnvironmentBuilder);
		controller.setScheduleRunner(this);
		
		this.generatedCreatorName = generatedCreatorName;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void execute(RunState toExecuteOn) { //TODO: ez miez?
		// required AbstractRunner stub.  We will control the
		//  schedule directly.
	}

	//----------------------------------------------------------------------------------------------------
	// Step the schedule
	public void step() { schedule.execute(); }

	//----------------------------------------------------------------------------------------------------
	// stop the schedule
	public void stop() {
		if (schedule != null)
			schedule.executeEndActions();
	}
	
	//=============================================================================================
	//implemented interfaces
	
	//----------------------------------------------------------------------------------------------------
	public void setModelPath(String path) { scenarioDir = path; }
	public void setParameters(ParameterTree paramTree) { this.paramTree = paramTree; }
	public void setRecorders(List<RecorderInfo> recorders) { this.recorders = recorders; }
	public void setStopCondition( double time ) { stopCondition = time; }
	public void stopBatch() throws BatchException{ controller.getScheduleRunner().stop(); }
	public void addBatchListener(IBatchListener listener) { listeners.add(listener); }
	public void removeBatchListener(IBatchListener listener) { listeners.remove(listener); }
	
	//----------------------------------------------------------------------------------------------------
	public void setEntryPoint(String entryPoint) throws InvalidEntryPointException {
		this.entryPoint = entryPoint;
		if (generatedCreatorName == null)
			restoreGeneratedCreatorName();
		try {
			generatedCreator = (BatchScenarioCreator) Class.forName(generatedCreatorName).newInstance();
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}

	}
	
	//----------------------------------------------------------------------------------------------------
	public void setStopCondition(String conditionCode) { 
		try {
			double d = Double.valueOf(conditionCode);
			setStopCondition(d);
		} catch (NumberFormatException e) {
			throw new UnsupportedOperationException(e);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public void startBatch() throws BatchException {
		try {
			load();
		} catch (Exception e) {
			throw new BatchException(e);
		}
		run();
	}
	
	//----------------------------------------------------------------------------------------------------
	public void stopCurrentRun() throws BatchException{
		//TODO: revision this
		controller.getScheduleRunner().stop();
	}
	
	//----------------------------------------------------------------------------------------------------
	public int getNumberOfRuns(){
		if (noRuns < 0)  
			noRuns = (int) Util.calculateNumberOfRuns(paramTree,true);
		return noRuns;
	}
	
	//----------------------------------------------------------------------------------------------------
	public void stopped() {}
	public void paused() {}
	public void started() {}
	public void restarted() {}

	//=============================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	protected void load() throws Exception {
		//if (creator != null) 
			//BatchScenarioLoader loader = new BatchScenarioLoader(scenarioDir);
			//ControllerRegistry registry = loader.load(runEnvironmentBuilder);
		
		BatchScenario scenario = generatedCreator.createScenario();
		controller.setControllerRegistry(scenario.createRegistry(runEnvironmentBuilder));
		controller.batchInitialize();
			
			//controller.setControllerRegistry(registry);
		//} else {
		//		msgCenter.error("Scenario not found",new IllegalArgumentException("Invalid scenario " + scenarioDir.getAbsolutePath()));
		//	return;
		//}
	}

	//----------------------------------------------------------------------------------------------------
	protected void runInitialize() { runInitialize(null); }
	protected void cleanUpRun() { controller.runCleanup(); }
	protected void cleanUpBatch() { controller.batchCleanup(); }

	//----------------------------------------------------------------------------------------------------
	protected void runInitialize(Parameters params) {
		controller.runInitialize(params);
		schedule = RunState.getInstance().getScheduleRegistry().getModelSchedule();
	}

	//----------------------------------------------------------------------------------------------------
	// returns the tick count of the next scheduled item
	protected double getNextScheduledTime() { return ((Schedule)RunEnvironment.getInstance().getCurrentSchedule()).peekNextAction().getNextTime(); }

	//----------------------------------------------------------------------------------------------------
	// returns the number of model actions on the schedule
	protected int getModelActionCount() { return schedule.getModelActionCount(); }

	//----------------------------------------------------------------------------------------------------
	// returns the number of non-model actions on the schedule
	protected int getActionCount() { return schedule.getActionCount(); }

	//----------------------------------------------------------------------------------------------------
	protected void setFinishing(boolean fin) { schedule.setFinishing(fin); }
	
	//----------------------------------------------------------------------------------------------------
	protected Parameters setupParams() throws BatchException {
		TreeSet<Long> runs = runsOfBranches();
		if (runs.size() > 1) {
			long size = runs.first();
			producer = new XMLStringSweeperProducer(generateXML(trimTree(paramTree,size).getRoot()));
		} else
			producer = new XMLStringSweeperProducer(generateXML(paramTree.getRoot()));
		
		Parameters params = null;
		ControllerRegistry registry = controller.getControllerRegistry();
		if (producer != null) {
			producer.init(registry,registry.getMasterContextId());
			try {
				params = producer.getParameters();
				registry.addParameterSetter(producer.getParameterSweeper());
			} catch (IOException e) {
				throw new BatchException("Unable to initialize the parameter sweeper.\n" + e.getMessage());
			}
		}

		if (params == null) {
			ParametersCreator pCreator = new ParametersCreator();
			params = pCreator.createParameters();
		}

		if (!params.getSchema().contains(ParameterConstants.DEFAULT_RANDOM_SEED_USAGE_NAME)) {
			ParametersCreator pCreator = new ParametersCreator();
			pCreator.addParameters(params);
			pCreator.addParameter(ParameterConstants.DEFAULT_RANDOM_SEED_USAGE_NAME,Integer.class,(int)System.currentTimeMillis(),false);
			params = pCreator.createParameters();
		}
		
		return params;
	}
	
	//----------------------------------------------------------------------------------------------------
	protected boolean keepRunning() {
		for (ParameterSetter setter : controller.getControllerRegistry().getParameterSetters()) {
			if (!setter.atEnd()) return true;
		}
		return false;
	}

	//----------------------------------------------------------------------------------------------------
	protected void waitForRun() {
		synchronized (monitor) {
			while (pause) {
				try {
					monitor.wait();
				} catch (InterruptedException e) {
					//TODO: revision this
					e.printStackTrace();
					break;
				}
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	protected void run() throws BatchException {
//		try {
//			if (producer != null && producer.getParameterSweeper() != null && producer.getParameterSweeper().getTerracottaRunFile()!=null) 
//				runDistributed();
//		} catch (IOException e) {
//			e.printStackTrace();
//			//msgCenter.error("Error getting terracotta distributed run file", e);
//		}
		
		start = new Date();
		
		// create .bak files if any previous result file has the same name
		for (RecorderInfo ri : recorders) {
			File oldFile = ri.getOutputFile();
			if (oldFile.exists()) {
				// find the first free .bak file number
				int firstFreeNum = 400;	// if more than 200 .bak files, then data will be lost...
				String resultFileName = oldFile.getAbsolutePath();
				String resultNameBody = resultFileName.substring(0,resultFileName.lastIndexOf('.'));
				for (int i = 1;i < 400;++i) {
					File bakFile = new File(resultNameBody + ".bak" + i + ".txt");
					if (!bakFile.exists()) {
						firstFreeNum = i;
						break;
					}
				}
				oldFile.renameTo(new File(resultNameBody + ".bak" + firstFreeNum + ".txt"));
			}
		}
		
		Parameters params = setupParams();
		int runCnt = 1;
		
		parameterHeader = "";
		for (String name : params.getSchema().parameterNames()) 
			parameterHeader += "\"" + name + "\"" + DEFAULT_DELIMITER;
		parameterValues.clear();

		while (keepRunning()) {			
			controller.runParameterSetters(params);
			//controller.runInitialize(params);
			//controller.execute();
			//pause = true;
			//waitForRun();
			//controller.runCleanup();
			
			runInitialize(params);  // initialize the run
			storeParameterValues(params);
			
			while (go() && getActionCount() > 0) {  // loop until last action is left
				
				if (schedule.getTickCount() >= stopCondition) break; // >= because of -1.0 tick
				
				if (getModelActionCount() == 0) 
					setFinishing(true);
				
				step();  // execute all scheduled actions at next tick

				//notify listeners
				for (int i = 0;i < listeners.size();++i)
					listeners.get(i).timeProgressed(new BatchEvent(this,BatchEvent.EventType.STEP_ENDED,schedule.getTickCount()));
			}

			stop(); // execute any actions scheduled at run end
			cleanUpRun();
			
			// notify listeners
			for (int i = 0;i < listeners.size();++i) 
				listeners.get(i).runChanged(new BatchEvent(this, BatchEvent.EventType.RUN_ENDED,runCnt));
			runCnt++;
		}
		controller.batchCleanup();
		controller = null;
		
		// extends output files
		end = new Date();
		extendResults(); 
		
		// notify listeners
		for (int i = 0;i < listeners.size();++i)
			listeners.get(i).batchEnded(new BatchEvent(this,BatchEvent.EventType.BATCH_ENDED));
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Generates the content of the parameter file from the parameter tree.
	 * @param root the root node of the parameter tree
	 */
	private String generateXML(ParameterNode root) {
		StringBuilder output = new StringBuilder();
		Enumeration<DefaultMutableTreeNode> e = new ParameterEnumeration(root);
		DefaultMutableTreeNode prev = null;
		int tagNo = 0;
		
		DefaultMutableTreeNode aNode = root.getFirstLeaf();
		AbstractParameterInfo pInfo = (AbstractParameterInfo) aNode.getUserObject(); 
		
		output.append( "<sweep runs=\"").append(pInfo.getRunNumber()).append("\">");
		while (e.hasMoreElements()) {
			DefaultMutableTreeNode node = e.nextElement();
			if (node.equals(root)) continue;
			AbstractParameterInfo info = (AbstractParameterInfo) node.getUserObject();
			if (node.getParent().equals(root)) {
				for (int i = 0;i < tagNo;++i) {
					if (i == 0)
						output.append("/>");
					else {
						output.append("\n");
						for (int j = 0;j < tagNo - i - j + 1;++j)
							output.append("\t");
						output.append("</parameter>");
					}
				}
				tagNo = 0;
				output.append("\n");
				for (int i = 0;i < tagNo + 1;++i)
					output.append("\t");
				output.append(generateXMLTag(info));
				tagNo++;
			} else { // child of other parameter
				if (prev.equals(node.getParent())) 
					output.append(">");
				else {
					// currently we don't use this branch
					System.out.println("Wrong branch");
					output.append("/>");
					tagNo--;
					DefaultMutableTreeNode dmt = (DefaultMutableTreeNode) prev.getParent();
					while (!dmt.equals(node.getParent())) {
						output.append("\n");
						for (int i = 0;i < tagNo + 1;++i)
							output.append("\t");
						output.append("</parameter>");
						tagNo--;
						dmt = (DefaultMutableTreeNode) dmt.getParent();
					}
				}
				output.append("\n");
				for (int i = 0;i < tagNo + 1;++i)
					output.append("\t");
				output.append(generateXMLTag(info));
				tagNo++;
			}
			prev = node;
		}
		
		for (int i = 0;i < tagNo;++i) {
			if (i == 0)
				output.append("/>");
			else {
				output.append("\n");
				for (int j = 0;j < tagNo + 1;++j)
					output.append("\t");
				output.append("</parameter>");
			}
		}

		output.append("\n</sweep>");
		return output.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	/** Generates and returns the xml tag according to this parameter. */
	@SuppressWarnings("unchecked")
	private String generateXMLTag(AbstractParameterInfo info) {
		StringBuilder sb = new StringBuilder();
		sb.append("<parameter name=\"");
		sb.append((info.getName().equalsIgnoreCase("randomSeed") ? "randomSeed" : info.getName()));
		sb.append("\" type=\"");
		String actType =  info.getDefaultValue().getClass().getSimpleName();
		List<Object> values;
		switch (info.getValueType()) {
		case CONSTANT	: sb.append("constant\" constant_type=\"");
						  if (isNumeric(actType)) 
							  sb.append("number\"");
						  else if (isBoolean(actType))
							  sb.append("boolean\"");
						  else
							  sb.append("string\"");
						  sb.append(" value=\"");
						  values = ((ParameterInfo)info).getValues();
						  sb.append(toStringWithoutScientificNotation(values.get(0),actType));
						  sb.append("\"");
						  break;
		case LIST		: sb.append("list\" value_type=\"");
						  sb.append(getSimphonyTypeString(actType));
						  sb.append("\" values=\"");
						  values = ((ParameterInfo)info).getValues();
						  for (int i = 0;i < values.size();++i) {
							  if (i > 0)
								  sb.append(" ");
							  sb.append(toStringWithoutScientificNotation(values.get(i),actType));
							  sb.append("");
						  }
						  sb.append("\"");
						  break;
		case INCREMENT	: Number startValue = ((IncrementalParameterInfo)info).getStart(),
						  		 endValue = ((IncrementalParameterInfo)info).getEnd(),
						  		 step = ((IncrementalParameterInfo)info).getIncrement();
						  // Transforms an iterator parameter with negative step value to an equivalent 
						  // parameter that uses positive step value
						  boolean need = false;
						  if ("double".equals(actType) || "Double".equals(actType) || "float".equals(actType) || "Float".equals(actType)) {
							  double start = startValue.doubleValue(); 
							  double end = endValue.doubleValue();
							  double incr = step.doubleValue();
							  need = incr < 0 && end <= start;
						  } else if ("byte".equals(actType) || "Byte".equals(actType) || "short".equals(actType) || "Short".equals(actType) ||
									 "int".equals(actType) || "Integer".equals(actType) || "long".equals(actType) || "Long".equals(actType)) {
							  long start = startValue.longValue();
							  long end = endValue.longValue();
							  long incr = step.longValue();
							  need = incr < 0 && end <= start;
						  }
						  if (need) {
							  Number temp = endValue;
							  endValue = startValue;
							  startValue = temp;
						  }
						  //Sets <code>step</code> to its absolute value.
						  if ("byte".equals(actType) || "Byte".equals(actType)) {
							  byte b = step.byteValue();
							  if (b < 0)
								  b *= Byte.parseByte("-1");
							  step = new Byte(b);
						  } else if ("short".equals(actType) || "Short".equals(actType)) {
							  short s = step.shortValue();
							  if (s < 0)
								  s *= Short.parseShort("-1");
							  step = new Short(s);
						  } else if ("int".equals(actType) || "Integer".equals(actType)) {
							  int i = step.intValue();
							  i = Math.abs(i);
							  step = new Integer(i);
						  } else if ("long".equals(actType) || "Long".equals(actType)) {
							  long l = step.longValue();
							  l = Math.abs(l);
							  step = new Long(l);
						  } else if ("float".equals(actType) || "Float".equals(actType)) {
							  float f = step.floatValue();
							  f = Math.abs(f);
							  step = new Float(f);
						  } else if ("double".equals(actType) || "Double".equals(actType)) {
							  double d = step.doubleValue();
							  d = Math.abs(d);
							  step = new Double(d);
						  }
						  sb.append("number\" start=\"");
						  Number val = ((IncrementalParameterInfo)info).getStart();
						  sb.append(toStringWithoutScientificNotation(val,actType));
						  sb.append("\" end=\"");
						  val = ((IncrementalParameterInfo)info).getEnd();
						  sb.append(toStringWithoutScientificNotation(val,actType));
						  sb.append("\" step=\"");
						  val = ((IncrementalParameterInfo)info).getIncrement();
						  sb.append(toStringWithoutScientificNotation(val,actType));
						  sb.append("\"");
		}
		return sb.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String getSimphonyTypeString( String type ){		
		if ("Integer".equalsIgnoreCase(type) || "int".equalsIgnoreCase(type))
			return "int";
		if ("Double".equalsIgnoreCase(type))
			return "double";
		if ("Boolean".equalsIgnoreCase(type))
			return "boolean";
		if ("Long".equalsIgnoreCase(type))
			return "long";
		if ("Float".equalsIgnoreCase(type))
			return "float";
		if ("String".equalsIgnoreCase(type))
			return "string";
		
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isNumeric(String type) {
		return Arrays.asList(new String[] {"byte", "Byte", "short", "Short", "int", "Integer", "long", "Long", "float", "Float",
				                           "double", "Double"}).contains(type);
	}
	
	//----------------------------------------------------------------------------------------------------
	private boolean isBoolean(String type) { return ("boolean".equals(type) || "Boolean".equals(type)); }
	
	//----------------------------------------------------------------------------------------------------
	private String toStringWithoutScientificNotation(Object num, String type) {
		if (null == num)
			return "null";
		StringBuilder result = new StringBuilder();
		String string = num.toString();
		if ("float".equals(type) || "Float".equals(type) ||	"double".equals(type) || "Double".equals(type)) {
			String[] split = string.trim().split("E");
			if (split.length == 1) 
				return string;
			else {
				int exp = Integer.parseInt(split[1]);
				if (exp < 0) {
					int _exp = -1 * exp;
					for (int i = 0;i < _exp;++i) {
						result.append("0");
						if (i == 0)
							result.append(".");
					}
					result.append(split[0].replaceAll("\\.",""));
				} else {
					int dotIndex = split[0].indexOf('.');
					int fragment = split[0].substring(dotIndex,split[0].length()).length();
					result.append(split[0].replaceAll("\\.",""));
					for (int i = 0; i <= exp - fragment;++i) 
						result.append("0");
				}
				return result.toString();
			}
		} else
			return string;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void storeParameterValues(Parameters params) {
		String line = "";
		for (String name : params.getSchema().parameterNames())
			line += params.getValueAsString(name) + DEFAULT_DELIMITER;
		parameterValues.add(line);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void extendResults() throws BatchException {
		for (RecorderInfo recorder : recorders) {
			BufferedReader reader = null;
			PrintWriter writer = null;
			try {
				File newFile = new File(recorder.getOutputFile().getName() + ".new");
				reader = new BufferedReader(new FileReader(recorder.getOutputFile()));
				writer = new PrintWriter(new FileWriter(newFile));
				
				StringBuilder b = new StringBuilder();
				b.append("Timestamp: ").append(DateFormat.getDateTimeInstance().format(start));
				writer.println(b.toString());
				writer.println();
				
				String header = reader.readLine();
				String paramHeader = parameterHeader.replaceAll("\\" + DEFAULT_DELIMITER,recorder.getDelimiter());
				int idx = header.indexOf(recorder.getDelimiter()); // first delimiter
				idx = header.indexOf(recorder.getDelimiter(),idx + 1); // second delimiter
				String newHeader = header.substring(0,idx + 1) + paramHeader + header.substring(idx + 1);
				writer.println(newHeader);
				
				String line = null;
				while ((line = reader.readLine()) != null) {
				    idx = line.indexOf(recorder.getDelimiter()); // first delimiter
				    if (idx < 0) break;
				    String runStr = line.substring(0,idx);
				    String paramValues = null;
				    try {
				    	int run = Integer.parseInt(runStr);
				    	paramValues = parameterValues.get(run - 1);
				    } catch (NumberFormatException e) {
				    	throw new BatchException("invalid result file format at file: " + recorder.getOutputFile());
				    }
				    
				    paramValues = paramValues.replaceAll("\\" + DEFAULT_DELIMITER,recorder.getDelimiter());
				    idx = line.indexOf(recorder.getDelimiter(),idx + 1); // second delimiter
				    String newLine = line.substring(0,idx + 1) + paramValues + line.substring(idx + 1);
				    writer.println(newLine);
				}
				
				writer.println();
				String endTime = DateFormat.getDateTimeInstance().format(end);
				writer.println("End Time: " + endTime);
				
				writer.flush();
				writer.close();
				writer = null;
				
				reader.close();
				reader = null;
				
//				for (int tries = 0;tries < 50;++tries) {
//					System.gc();
//					if (recorder.getOutputFile().delete()) break;
//				}
//				boolean bo = newFile.renameTo(recorder.getOutputFile());
				copyContent(newFile,recorder.getOutputFile());
				newFile.delete();
			} catch (IOException e) {
				throw new BatchException(e);
			} finally {
				if (reader != null)
					try { reader.close(); } catch (IOException e) {}
				if (writer != null) {
					writer.flush();
					writer.close();
				}
			}
		    
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void copyContent(File source, File dest) throws IOException {
		RandomAccessFile rand = new RandomAccessFile(dest,"rw");
		rand.setLength(0);
		rand.seek(0);
		
		byte[] buffer = new byte[2048];
		FileInputStream fis = new FileInputStream(source);
		int size = -1;
		while ((size = fis.read(buffer)) != -1) 
			rand.write(buffer,0,size);
		fis.close();
		rand.close();
	}
 	
	//----------------------------------------------------------------------------------------------------
	private void restoreGeneratedCreatorName() throws InvalidEntryPointException {
		// get the implementation package of the model
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder parser = null;
		Document document = null;
		try {
			parser = factory.newDocumentBuilder();
			String tmp = entryPoint.replace('\\','/');
			int idx = tmp.lastIndexOf('/');
			if (idx != -1)
				tmp = entryPoint.substring(idx + 1);
			document = parser.parse(new File(tmp).toURI().toString());
		} catch (ParserConfigurationException e) {
			throw new InvalidEntryPointException("Cannot parse model score file due parser configuration problems!");
		} catch (SAXException e) {
			throw new InvalidEntryPointException("Cannot parse model score file!");
		} catch (IOException e) {
			throw new InvalidEntryPointException("Cannot read model score file!");
		}
		
		Element rootElement = document.getDocumentElement();
		NodeList nl = rootElement.getElementsByTagName("implementation");
		Element impl = (Element) nl.item(0);
		String implementationPackage = impl.getAttribute("package");
		String modelName = Util.capitalize(implementationPackage);
		
		// an inconvenient way to acquire the timestamp
		// it assumes that the settings xml file is in the . directory
		// FIXME need a better solution
		File xml = new File(System.getProperty("user.dir")).listFiles(new FileFilter() {
			public boolean accept(File pathname) { return pathname.getName().endsWith(".settings.xml"); }
		})[0];
		int idx1 = xml.getName().lastIndexOf("__");
		int idx2 = xml.getName().lastIndexOf(".settings.xml");
		String timestamp = xml.getName().substring(idx1,idx2);

		generatedCreatorName = RepastSOutputterGenerator.packagePrefix + "." + modelName +  RepastSOutputterGenerator.scenarioSuffix + timestamp;
	}
	
	//----------------------------------------------------------------------------------------------------
	private TreeSet<Long> runsOfBranches() {
		final TreeSet<Long> result = new TreeSet<Long>();
		final Enumeration<ParameterNode> children = paramTree.children();
		ParameterNode node = null;
		while (children.hasMoreElements()) {
			node = children.nextElement();
			if (node.getParameterInfo().isOriginalConstant()) continue ; // these are constants
			else {
				final long actBranch = calculateNumberOfBranchesRuns(node);
				result.add(actBranch);
			}
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private long calculateNumberOfBranchesRuns(final ParameterNode branch) {
		final long result = branch.getParameterInfo().getMultiplicity();
		return branch.isLeaf() ? result : result * calculateNumberOfBranchesRuns((ParameterNode)branch.getFirstChild());
	}
	
	//----------------------------------------------------------------------------------------------------
	private ParameterTree trimTree(final ParameterTree orig, final long size) {
		final ParameterTree result = orig.clone();
		final Enumeration<ParameterNode> children = result.children();
		final List<ParameterNode> removables = new ArrayList<ParameterNode>();
		final List<ParameterNode> addables = new ArrayList<ParameterNode>();
		
		while (children.hasMoreElements()) {
			final ParameterNode node = children.nextElement();
			if (node.getParameterInfo().isOriginalConstant()) continue; // these are constants
			else {
				final List<ParameterNode> evens = evenBranch(node,size);
				removables.add(node);
				for (ParameterNode e : evens)
					addables.add(e);
			}
		}
		
		for (ParameterNode n : removables)
			result.getRoot().remove(n);
		
		for (ParameterNode n : addables) 
			result.addNode(n);
		
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private List<ParameterNode> evenBranch(final ParameterNode node, final long size) {
		final long run = node.getParameterInfo().getRunNumber();
		final SubTreeIterator it = new SubTreeIterator(buildParamList(node));
		final Map<AbstractParameterInfo,List> values = new HashMap<AbstractParameterInfo,List>();
		long idx = 0;
		
		while (idx++ < size && it.hasNext()) {
			final List<AbstractParameterInfo> next = it.next();
			for (AbstractParameterInfo p : next) {
				List list = values.get(p);
				if (list == null) {
					list = new ArrayList();
					values.put(p,list);
				}
				list.add(p.iterator().next());
			}
		}
		
		final List<ParameterNode> result = new ArrayList<ParameterNode>();
		for (AbstractParameterInfo p : values.keySet()) {
			List list = values.get(p);
			ParameterInfo info = new ParameterInfo(p.getName(),"",p.getDefaultValue());
			info.setRunNumber(run);
			info.setValues(list);
			result.add(new ParameterNode(info));
		}
		
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	private List<AbstractParameterInfo> buildParamList(final ParameterNode node) {
		final List<AbstractParameterInfo> subTreeParams = new ArrayList<AbstractParameterInfo>();
		final Enumeration<ParameterNode> e = node.breadthFirstEnumeration();
		
		while (e.hasMoreElements()) {
			final AbstractParameterInfo parameterInfo = e.nextElement().getParameterInfo();
			parameterInfo.setRunNumber(1); // we don't need global run info here
			subTreeParams.add(parameterInfo);
		}
		return subTreeParams;
	}
	
	//=============================================================================================
	//inner classes
	
	//----------------------------------------------------------------------------------------------------
	final private class SubTreeIterator implements Iterator<List<AbstractParameterInfo>> {
		
		//====================================================================================================
		// members
		
		private boolean finished = false;
		private int index = 0;
		
		/** List of parameters. */
		List<AbstractParameterInfo> params;
		
		/** List of iterators that provide SVd ParameterInfo instances. */
		List<Iterator<AbstractParameterInfo>> valueIterators;
		
		/** Result list. */
		List<AbstractParameterInfo> res;
		
		//====================================================================================================
		// methods

		//----------------------------------------------------------------------------------------------------
		@SuppressWarnings("unchecked")
		public SubTreeIterator(List<AbstractParameterInfo> params) {
			this.params = new ArrayList<AbstractParameterInfo>(params);
			res = new ArrayList<AbstractParameterInfo>(params);	// init with some values
			valueIterators = new ArrayList<Iterator<AbstractParameterInfo>>();
			// get value iterators for each parameter
			for (AbstractParameterInfo p : params) 
				valueIterators.add(p.parameterIterator());
		}

		//====================================================================================================
		// impemented interfaces
		
		//----------------------------------------------------------------------------------------------------
		public boolean hasNext() { return !finished; }

		//----------------------------------------------------------------------------------------------------
		public List<AbstractParameterInfo> next() {
			if (!hasNext()) 
				throw new NoSuchElementException();

			createNext();
			return res;
		}

		//----------------------------------------------------------------------------------------------------
		public void remove() { throw new UnsupportedOperationException(); }

		
		//====================================================================================================
		// assistant methods
		
		//----------------------------------------------------------------------------------------------------
		/** Produces a new list of single valued parameters in <code>res</code> */
		@SuppressWarnings("unchecked")
		private void createNext() {
			if (!hasNext()) return;

			Iterator<AbstractParameterInfo> it = valueIterators.get(index);
			if (it.hasNext()) {
				res.set(index,it.next());
				if (!isLast(index)) 
					index++;
				else {
					if (hasAllFinished()) 
						finished = true;	// top, no more combination
					return;
				}
			} else {
				// consumed
				// new value iterator
				valueIterators.set(index, params.get(index).parameterIterator());
				index--;
			}
			createNext();
		}

		//----------------------------------------------------------------------------------------------------
		private boolean isLast(int index) { return index == valueIterators.size() - 1; }
		
		//----------------------------------------------------------------------------------------------------
		private boolean hasAllFinished() {
			for (Iterator<AbstractParameterInfo> i : valueIterators) {
				if (i.hasNext()) return false;	// there are more
			}
			return true;
		}
	}

	//----------------------------------------------------------------------------------------------------
	public class ParameterEnumeration implements Enumeration<DefaultMutableTreeNode> {
		
		//==============================================================================
		// members
		
		private Stack<DefaultMutableTreeNode> stack = null;
		
		//==============================================================================
		// methods
		
		//------------------------------------------------------------------------------
		/** Constructor. 
		 * @param root the root node of the tree
		 */
		public ParameterEnumeration(DefaultMutableTreeNode root) {
			stack = new Stack<DefaultMutableTreeNode>();
			stack.push(root);
		}
		
		//==============================================================================
		// implemented interfaces

		//------------------------------------------------------------------------------
		public boolean hasMoreElements() {
			return !stack.empty();
		}

		//------------------------------------------------------------------------------
		public DefaultMutableTreeNode nextElement() {
			DefaultMutableTreeNode res = stack.pop();
			for (int i=res.getChildCount()-1;i>=0;--i)
				stack.push((DefaultMutableTreeNode)res.getChildAt(i));
			return res;
		}
	}
}
