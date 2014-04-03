package ai.aitia.meme.paramsweep.platform.netlogo.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import org.nlogo.sdm.AggregateManagerLite;

import ai.aitia.meme.paramsweep.platform.netlogo.NetLogoPlatform;
import ai.aitia.meme.utils.Utils.Pair;

public class MEMENetLogoParser {

	//====================================================================================================
	// members
	
	public static final String GLOBALS_KEYWORD 		= "globals";
	public static final String EXTENSIONS_KEYWORD 	= "extensions";
	public static final String BREED_KEYWORD		= "breed";
	public static final String D_LINK_BREED_KEYWORD = "directed-link-breed";
	public static final String U_LINK_BREED_KEYWORD = "undirected-link-breed";
	public static final String INCLUDES_KEYWORD		= "__includes";
	public static final String TO_KEYWORD			= "to";
	public static final String TO_REPORT_KEYWORD	= "to-report";
	public static final String END_KEYWORD			= "end";
	public static final String OPEN_BRACKET			= "[";
	public static final String CLOSE_BRACKET		= "]";
	public static final String MAGIC_STRING			= "@#$#@#$#@";
	
	public static final String GRAPHICS_WINDOW		= "GRAPHICS-WINDOW";
	public static final String CC_WINDOW			= "CC-WINDOW";
	public static final String BUTTON 				= "BUTTON";
	public static final String MONITOR				= "MONITOR";
	public static final String SWITCH				= "SWITCH";
	public static final String PLOT					= "PLOT";
	public static final String SLIDER				= "SLIDER";
	public static final String INPUTBOX				= "INPUTBOX";
	public static final String CHOOSER				= "CHOOSER";
	public static final String OUTPUT				= "OUTPUT";
	public static final String TEXTBOX				= "TEXTBOX";
	
	protected static final List<String> interfaceElementTypes; 
	static {
		interfaceElementTypes = Arrays.asList(new String[] { GRAPHICS_WINDOW, CC_WINDOW, BUTTON, MONITOR, SWITCH, PLOT, SLIDER, INPUTBOX, CHOOSER,
														  	 OUTPUT, TEXTBOX });
	}
	
	protected String originalSource = null;
	protected String modelRoot = null;
	protected StringTokenizer tokenizer = null;
	
	protected List<String> extensions = new ArrayList<String>();
	protected List<String> globals = new ArrayList<String>();
	protected List<String> allGlobals = new ArrayList<String>();
	protected List<BreedInfo> breeds = new ArrayList<BreedInfo>();
	protected List<BreedInfo> directedLinkBreeds = new ArrayList<BreedInfo>();
	protected List<BreedInfo> undirectedLinkBreeds = new ArrayList<BreedInfo>();
	protected List<String> includes = new ArrayList<String>();
	protected List<File> allIncludedFiles = new ArrayList<File>();
	
	protected List<String> otherHeaders = new ArrayList<String>();
	protected List<String> methods = new ArrayList<String>();
	protected List<String> allMethods = new ArrayList<String>();
	protected String setupMethod = null;
	protected List<InterfaceParameter> interfaceParameters = new ArrayList<InterfaceParameter>();
	
	private List<String> allIncludes = new ArrayList<String>();
	
	private final String SETUP_COMMMAND;
	
	protected List<String> sdGlobals = new ArrayList<String>();
	protected List<String> sdMethods = new ArrayList<String>();

	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public MEMENetLogoParser(final String originalSource, final String modelRoot, final String setupCommand) {
		this.originalSource = originalSource;
		this.modelRoot = modelRoot;
		this.SETUP_COMMMAND = setupCommand;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> getExtensions() { return extensions; }
	public List<String> getGlobals() { return globals; }
	public List<String> getAllGlobals() { return allGlobals; }
	public List<BreedInfo> getBreeds() { return breeds; }
	public List<BreedInfo> getDirectedLinkBreeds() { return directedLinkBreeds; }
	public List<BreedInfo> getUndirectedLinkBreeds() { return undirectedLinkBreeds; }
	public List<String> getIncludes() { return includes; }
	public List<File> getAllIncludedFiles() { return allIncludedFiles; } 
	public List<String> getOtherHeaders() { return otherHeaders; }
	public List<String> getMethods() { return methods; }
	public String getSetupMethod() { return setupMethod; }
	public List<String> getAllMethods() { return allMethods; }
	public List<InterfaceParameter> getInterfaceParameters() { return interfaceParameters; }
	public List<String> getSDGlobals() { return sdGlobals; }
	public List<String> getSDMethods() { return sdMethods; }
	
	//----------------------------------------------------------------------------------------------------
	public List<String> getReporters() {
		List<String> result = new ArrayList<String>();
		for (String s: getAllMethods()) {
			if (s.trim().toLowerCase().startsWith(TO_REPORT_KEYWORD))
				result.add(s);
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> getReporterNames() {
		List<String> result = new ArrayList<String>();
		for (String s: getAllMethods()) {
			if (s.trim().toLowerCase().startsWith(TO_REPORT_KEYWORD)) {
				String[] parts = s.trim().split("\\s+");
				result.add(parts[1]);
			}
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> getNullArgReporters() {
		List<String> result = new ArrayList<String>();
		for (String s : getReporters()) {
			String[] parts = s.trim().split("\\s+");
			if (!OPEN_BRACKET.equals(parts[2])) // means no arguments
				result.add(s);
		}
		return result;
	}

	//----------------------------------------------------------------------------------------------------
	public List<String> getNullArgReporterNames() {
		List<String> result = new ArrayList<String>();
		for (String s : getReporters()) {
			String[] parts = s.trim().split("\\s+");
			if (!OPEN_BRACKET.equals(parts[2])) // means no arguments
				result.add(parts[1]);
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<Pair<String,Integer>> getOtherReporterNamesAndNoOfParams() {
		List<Pair<String,Integer>> result = new ArrayList<Pair<String,Integer>>();
		for (String s : getReporters()) {
			String[] parts = s.trim().split("\\s+");
			if (OPEN_BRACKET.equals(parts[2])) {
				int i = 3;
				for (;!CLOSE_BRACKET.equals(parts[i]);++i);
				result.add(new Pair<String,Integer>(parts[1],i - 3));
			}
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<Pair<String,Integer>> getCommandNamesAndNoOfParams() {
		List<Pair<String,Integer>> result = new ArrayList<Pair<String,Integer>>();
		for (String s: getAllMethods()) {
			String[] parts = s.trim().split("\\s+");
			if (parts[0].equalsIgnoreCase(TO_KEYWORD)) {
				if (OPEN_BRACKET.equals(parts[2])) {
					int i = 3;
					for (;!CLOSE_BRACKET.equals(parts[i]);++i);
					result.add(new Pair<String,Integer>(parts[1],i - 3));
				} else
					result.add(new Pair<String,Integer>(parts[1],0));
			}
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	// based on the idea of Richard O'Legendi
	public void parse() throws IOException {
		final String preparsedSource = preparsing(originalSource);
		tokenizer = new StringTokenizer(preparsedSource);
		String token = modelHeader();
		modelMethods(token);
		modelInterface();
		if (isSystemDynamicsModel()) 
			parseSystemDynamicsDiagram();
		parseOtherSourceFiles();
	}
	
	//----------------------------------------------------------------------------------------------------
	public boolean isSystemDynamicsModel() {
		return originalSource.contains("org.nlogo.sdm.gui.AggregateDrawing");
	}

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	public String preparsing(String source) {
		String result = removeComments(source);
		result = result.replaceAll("[\\s]+"," ");
		result = result.replaceAll("\\" + OPEN_BRACKET,"  \\" + OPEN_BRACKET + " ");
		result = result.replaceAll("\\" + CLOSE_BRACKET," \\" + CLOSE_BRACKET + " ");
		result = result.replaceAll("[\\s]+"," ");
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String modelHeader() {
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (TO_KEYWORD.equalsIgnoreCase(token) || TO_REPORT_KEYWORD.equalsIgnoreCase(token)) 
				return token;
			if (EXTENSIONS_KEYWORD.equalsIgnoreCase(token))
				extensions();
			else if (GLOBALS_KEYWORD.equalsIgnoreCase(token)) 
				globals();
			else if (INCLUDES_KEYWORD.equalsIgnoreCase(token))
				includes();
			else if (BREED_KEYWORD.equalsIgnoreCase(token))
				breed(BreedType.TURTLE);
			else if (D_LINK_BREED_KEYWORD.equalsIgnoreCase(token))
				breed(BreedType.DIRECTED);
			else if (U_LINK_BREED_KEYWORD.equalsIgnoreCase(token))
				breed(BreedType.UNDIRECTED);
			else
				otherHeaders(token);
		}
		throw new IllegalStateException();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void globals() {
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (OPEN_BRACKET.equals(token)) continue;
			if (CLOSE_BRACKET.equals(token)) break;
			globals.add(token);
			allGlobals.add(token);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void extensions() {
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (OPEN_BRACKET.equals(token)) continue;
			if (CLOSE_BRACKET.equals(token)) break;
			extensions.add(token);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void includes() {
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (OPEN_BRACKET.equals(token)) continue;
			if (CLOSE_BRACKET.equals(token)) break;
			token = token.replaceAll("^\"","").replaceAll("\"$","");
			includes.add(token);
			allIncludes.add(token);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void breed(final BreedType type) {
		try {
			String token = tokenizer.nextToken(); 
			if (!OPEN_BRACKET.equals(token)) 
				throw new IllegalStateException("expected token: " + OPEN_BRACKET);
			final String agentSetName = tokenizer.nextToken(); 
			String agentName = tokenizer.nextToken(); 
			
			if (type == BreedType.TURTLE && CLOSE_BRACKET.equals(agentName)) {
				token = agentName; 
				agentName = null;
			} else
				token = tokenizer.nextToken();
			if (!CLOSE_BRACKET.equals(token))
				throw new IllegalStateException("expected token: " + CLOSE_BRACKET);
			BreedInfo breedInfo = new BreedInfo(agentSetName,agentName,false,type);
			switch (type) {
			case TURTLE : breeds.add(breedInfo); break;
			case DIRECTED : directedLinkBreeds.add(breedInfo); break;
			case UNDIRECTED : undirectedLinkBreeds.add(breedInfo); break;
			}
		} catch (final NoSuchElementException e) {
			throw new IllegalStateException(e); 
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void otherHeaders(String keyword) {
		StringBuilder entry = new StringBuilder(keyword);
		entry.append(" ");
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			entry.append(token).append(" ");
			if (CLOSE_BRACKET.equals(token)) break;
		}
		String entryStr = entry.toString();
		otherHeaders.add(entryStr.substring(0,entryStr.length() - 1));
	}
	
	//----------------------------------------------------------------------------------------------------
	private void modelMethods(String firstToken) {
		String token = firstToken;
		do {
			if (MAGIC_STRING.equals(token)) break;
			if (TO_KEYWORD.equalsIgnoreCase(token) || TO_REPORT_KEYWORD.equalsIgnoreCase(token))
				method(token);
			else
				throw new IllegalStateException();
			if (tokenizer.hasMoreTokens())
				token = tokenizer.nextToken();
		} while (tokenizer.hasMoreTokens());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void method(String keyword) {
		StringBuilder entry = new StringBuilder(keyword);
		entry.append(" ");
		String methodName = null;
		if (tokenizer.hasMoreTokens()) {
			methodName = tokenizer.nextToken();
			entry.append(methodName).append(" ");
		} else assert false;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			entry.append(token).append(" ");
			if (END_KEYWORD.equalsIgnoreCase(token)) break;
		}
		String entryStr = entry.toString();
		entryStr = entryStr.substring(0,entryStr.length() - 1);
		if (SETUP_COMMMAND.equalsIgnoreCase(methodName))
			setupMethod = entryStr;
		else
			methods.add(entryStr);
		allMethods.add(entryStr);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void modelInterface() {
		String token = null;
		if (tokenizer.hasMoreElements())
			token = tokenizer.nextToken();
		else return;
		do {
			if (token == null || MAGIC_STRING.equals(token)) break;
			if (interfaceElementTypes.contains(token))
				token = interfaceElement(token);
		} while ((tokenizer.hasMoreTokens()));
	}
	
	//----------------------------------------------------------------------------------------------------
	private String interfaceElement(String type) {
		if (SLIDER.equals(type)) 
			return slider(type);
		else if (SWITCH.equals(type))
			return switcs(type);
		else if (CHOOSER.equals(type))
			return chooser(type);
		else if (INPUTBOX.equals(type))
			return inputbox(type);
		else
			return otherIntfsType(type);
	}
	
	//----------------------------------------------------------------------------------------------------
	private String slider(String type) {
		// TODO: kinyerhet� info: esetleg min-max �rt�k (b�r ezt a NetLogo sem tartja be)
		StringBuilder entry = new StringBuilder(type); 
		entry.append(" ");
		List<String> parts = new ArrayList<String>();
		String returnToken = null;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (interfaceElementTypes.contains(token) || MAGIC_STRING.equals(token)) {
				returnToken = token;
				break;
			}
			parts.add(token);
		}
		interfaceParameters.add(new InterfaceParameter(parts.get(4),Double.class,Double.valueOf(parts.get(8))));
		return returnToken;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String switcs(String type) {
		StringBuilder entry = new StringBuilder(type); 
		entry.append(" ");
		List<String> parts = new ArrayList<String>();
		String returnToken = null;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (interfaceElementTypes.contains(token) || MAGIC_STRING.equals(token)) {
				returnToken = token;
				break;
			}
			parts.add(token);
		}
		interfaceParameters.add(new InterfaceParameter(parts.get(4),Boolean.class,"0".equals(parts.get(6)) ? true : false));
		return returnToken;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String chooser(String type) {
		StringBuilder entry = new StringBuilder(type); 
		entry.append(" ");
		List<String> parts = new ArrayList<String>();
		String returnToken = null;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (interfaceElementTypes.contains(token) || MAGIC_STRING.equals(token)) {
				returnToken = token;
				break;
			}
			parts.add(token);
		}
		Class<?> clazz = Double.class;
		List<Double> possibleValues = new ArrayList<Double>();
		for (int i = 6;i < parts.size() - 1;++i) {
			try {
				possibleValues.add(Double.parseDouble(parts.get(i)));
			} catch (NumberFormatException e) {
				clazz = String.class;
				break;
			}
		}
		
		int defaultIdx = Integer.parseInt(parts.get(parts.size() - 1));
		
		List<String> possibleStringValues = new ArrayList<String>();
		if (clazz.equals(String.class)) {
			for (int i = 6;i < parts.size() - 1;++i) 
				possibleStringValues.add(parts.get(i));
		}
			
		interfaceParameters.add(new InterfaceParameter(parts.get(4),clazz,
													   (clazz.equals(Double.class) ? possibleValues : possibleStringValues).get(defaultIdx),
													   (clazz.equals(Double.class) ? possibleValues : possibleStringValues)));
		return returnToken;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String inputbox(String type) {
		StringBuilder entry = new StringBuilder(type); 
		entry.append(" ");
		List<String> parts = new ArrayList<String>();
		String returnToken = null;
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (interfaceElementTypes.contains(token) || MAGIC_STRING.equals(token)) {
				returnToken = token;
				break;
			}
			parts.add(token);
		}
		if (!"Color".equalsIgnoreCase(parts.get(parts.size() - 1))) {
			Class<?> clazz = Double.class;
			if (!"Number".equalsIgnoreCase(parts.get(parts.size() - 1)))
				clazz = String.class;
			interfaceParameters.add(new InterfaceParameter(parts.get(4),clazz,
														   (clazz.equals(Double.class) ? Double.valueOf(parts.get(5)): parts.get(5))));		
		}
		return returnToken;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String otherIntfsType(String type) {
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (interfaceElementTypes.contains(token) || MAGIC_STRING.equals(token)) 
				return token;
		}
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String removeComments(String source) {
		BufferedReader reader = new BufferedReader(new StringReader(source));
		StringWriter stringWriter = new StringWriter();
		PrintWriter writer = new PrintWriter(stringWriter);
		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.contains(";"))
					line = modifyLine(line);
				writer.println(line);
			}
			return stringWriter.toString();
		} catch (IOException e) {
			throw new IllegalStateException();
		} finally {
			writer.flush();
			writer.close();
			try {
				reader.close();
			} catch (IOException ee) {}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private String modifyLine(String line) {
		boolean inString = false;
		char prev = '0';
		int idx = -1;
		for (int i = 0;i < line.length();++i) {
			char c = line.charAt(i);
			if (c == '"' && prev != '\\')
				inString ^= true; // negate boolean value
			else if (c == '\\' && prev == '\\')
				c = '0';
			else if (!inString && c == ';') {
				idx = i;
				break;
			}
			prev = c;
		}
		if (idx != -1)
			return line.substring(0,idx);
		return line;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void parseOtherSourceFiles() throws IOException {
		while (allIncludes.size() > 0) {
			String include = allIncludes.get(0);
			File f = findSourceFile(include);
			allIncludedFiles.add(f);
			String source = readSource(f);
			source = preparsing(source);
			tokenizer = new StringTokenizer(source);
			String token = includeModelHeader();
			if (token != null) 
				includeModelMethods(token);
			allIncludes.remove(0);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private File findSourceFile(String source) throws FileNotFoundException {
		File f = new File(source);
		if (!f.exists()) 
			f = new File(modelRoot + File.separator + source);
		if (!f.exists())
			throw new FileNotFoundException(f.getAbsolutePath());
		return f;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String readSource(File file) throws IOException {
		FileReader reader = null;
		try {
			reader = new FileReader(file);
			String source = new String();
			char[] buffer = new char[8096];
			int chars = 0;
			while ((chars = reader.read(buffer)) > 0) 
				source += String.valueOf(buffer,0,chars);
			return source;
		} finally {
			if (reader != null)
				reader.close();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private String includeModelHeader() {
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (TO_KEYWORD.equalsIgnoreCase(token) || TO_REPORT_KEYWORD.equalsIgnoreCase(token)) 
				return token;
			if (GLOBALS_KEYWORD.equalsIgnoreCase(token)) 
				includeGlobals();
			else if (INCLUDES_KEYWORD.equalsIgnoreCase(token))
				includeIncludes();
			else if (BREED_KEYWORD.equalsIgnoreCase(token))
				includeBreed(BreedType.TURTLE);
			else if (D_LINK_BREED_KEYWORD.equalsIgnoreCase(token))
				includeBreed(BreedType.DIRECTED);
			else if (U_LINK_BREED_KEYWORD.equalsIgnoreCase(token))
				includeBreed(BreedType.UNDIRECTED);
		}
		return null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private void includeGlobals() {
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (OPEN_BRACKET.equals(token)) continue;
			if (CLOSE_BRACKET.equals(token)) break;
			allGlobals.add(token);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void includeIncludes() {
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			if (OPEN_BRACKET.equals(token)) continue;
			if (CLOSE_BRACKET.equals(token)) break;
			token = token.replaceAll("^\"","").replaceAll("\"$","");
			allIncludes.add(token);
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void includeBreed(final BreedType type) {
		try {
			String token = tokenizer.nextToken(); 
			if (!OPEN_BRACKET.equals(token)) 
				throw new IllegalStateException("expected token: " + OPEN_BRACKET);
			String agentSetName = tokenizer.nextToken(); 
			String agentName = tokenizer.nextToken(); 

			if (type == BreedType.TURTLE && CLOSE_BRACKET.equals(agentName)) {
				token = agentName; 
				agentName = null;
			} else
				token = tokenizer.nextToken();
			
			if (!CLOSE_BRACKET.equals(token))
				throw new IllegalStateException("expected token: " + CLOSE_BRACKET);
			BreedInfo breedInfo = new BreedInfo(agentSetName,agentName,true,type);
			switch (type) {
			case TURTLE : breeds.add(breedInfo); break;
			case DIRECTED : directedLinkBreeds.add(breedInfo); break;
			case UNDIRECTED : undirectedLinkBreeds.add(breedInfo); break;
			}
		} catch (final NoSuchElementException e) {
			throw new IllegalStateException(e); 
		}
	}

	
	//----------------------------------------------------------------------------------------------------
	private void includeModelMethods(String firstToken) {
		String token = firstToken;
		do {
			if (MAGIC_STRING.equals(token)) break;
			if (TO_KEYWORD.equalsIgnoreCase(token) || TO_REPORT_KEYWORD.equalsIgnoreCase(token))
				includeMethod(token);
			else
				throw new IllegalStateException();
			if (tokenizer.hasMoreTokens())
				token = tokenizer.nextToken();
		} while (tokenizer.hasMoreTokens());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void includeMethod(String keyword) {
		StringBuilder entry = new StringBuilder(keyword);
		entry.append(" ");
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken();
			entry.append(token).append(" ");
			if (END_KEYWORD.equalsIgnoreCase(token)) break;
		}
		String entryStr = entry.toString();
		entryStr = entryStr.substring(0,entryStr.length() - 1);
		allMethods.add(entryStr);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void parseSystemDynamicsDiagram() throws IOException {
		final String[] parts = originalSource.split("\n");
		
		boolean sdPart = false;
		final StringBuilder sb = new StringBuilder();
		for (int i = 0;i < parts.length - 2;++i) {
			if (MAGIC_STRING.equals(parts[i]) && (parts[i + 1].toUpperCase().startsWith(NetLogoPlatform.NETLOGO_41x_VERSION) || parts[i + 1].toUpperCase().startsWith(NetLogoPlatform.NETLOGO_41x_3D_VERSION)) &&
				MAGIC_STRING.equals(parts[i + 2])) {
				sdPart = true;
				i += 4;
			}
			
			if (sdPart) {
				if (MAGIC_STRING.equals(parts[i])) break;
				sb.append(parts[i]).append(" ");
			}
		}
		
//		final int idx = originalSource.indexOf(MAGIC_STRING + " " + NetLogoPlatform.NETLOGO_41x_VERSION);
//		String tempSource = originalSource.substring(idx);
//		
//		StringTokenizer tokenizer = new StringTokenizer(tempSource);
//		tokenizer.nextToken(); // reads MAGIC_STRING
//		String token = tokenizer.nextToken(); // reads NetLogo
//		while (tokenizer.hasMoreTokens() && !MAGIC_STRING.equals(token))
//			token = tokenizer.nextToken();
//		tokenizer.nextToken(); // reads MAGIC_STRING
//		
//		token = tokenizer.nextToken(); // starts System Dynamic code here
//		while (tokenizer.hasMoreTokens() && !MAGIC_STRING.equals(token)) {
//			token = tokenizer.nextToken();
//		}
//		tokenizer = null;
		
		final MEMEHeadlessWorkspace workspace = new MEMEHeadlessWorkspace();
		final AggregateManagerLite aggregateManager = new AggregateManagerLite();
		aggregateManager.load(sb.toString(),workspace);
		final String tempSource = aggregateManager.source();
		try {
			workspace.dispose();
		} catch (final InterruptedException e) {
			// I don't care
		}
		
		final MEMENetLogoParser parser = new MEMENetLogoParser(tempSource,modelRoot,SETUP_COMMMAND);
		parser.parse();
		
		sdGlobals.addAll(parser.getGlobals());
		allGlobals.addAll(sdGlobals);
		sdMethods.addAll(parser.getMethods());
		allMethods.addAll(sdMethods);
	}
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	public static class InterfaceParameter implements Serializable {
		
		//====================================================================================================
		// members
		
		private static final long serialVersionUID = -8146595380028976967L;
		
		protected String name = null;
		protected Class<?> type = null;
		protected Object defaultValue = null;
		protected List<?> possibleValues = null;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public InterfaceParameter(String name, Class<?> type, Object defaultValue) { this(name,type,defaultValue,null); }
		
		//----------------------------------------------------------------------------------------------------
		public InterfaceParameter(String name, Class<?> type, Object defaultValue, List<?> possibleValues) {
			if (name == null)
				throw new IllegalArgumentException("'name' cannot be null");
			if (type == null)
				throw new IllegalArgumentException("'type' cannot be null");
			if (defaultValue == null)
				throw new IllegalArgumentException("'defaultValue' cannot be null");
			this.name = name;
			this.type = type;
			this.defaultValue = defaultValue;
			this.possibleValues = possibleValues;
		}
		
		//----------------------------------------------------------------------------------------------------
		public String getName() { return name; }
		public Class<?> getType() { return type; }
		public Object getDefaultValue() { return defaultValue; }
		public List<?> getPossibleValues() { return possibleValues; }
	}
	
	//----------------------------------------------------------------------------------------------------
	public static class BreedInfo {
		
		//====================================================================================================
		// members
		
		protected final String agentSetName;
		protected final String agentName;
		protected final boolean fromIncludeFile;
		protected final BreedType type;
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public BreedInfo(final String agentSetName, final String agentName, final boolean fromIncludeFile, final BreedType type) {
			this.agentSetName = agentSetName;
			this.agentName = agentName;
			this.fromIncludeFile = fromIncludeFile;
			this.type = type;
		}
		
		//----------------------------------------------------------------------------------------------------
		public String getAgentSetName() { return agentSetName; }
		public String getAgentName() { return agentName; }
		public boolean isFromIncludeFile() { return fromIncludeFile; }
		public BreedType getBreedType() { return type; }
		
		//----------------------------------------------------------------------------------------------------
		@Override 
		public String toString() {
			String result = agentSetName;
			if (agentName != null)
				result += "/" + agentName;
			return result;
		}
		
		//----------------------------------------------------------------------------------------------------
		@Override
		public int hashCode() {
			int result = 17;
			result = 31 * result + agentSetName.hashCode();
			if (agentName != null)
				result = 31 * result + agentName.hashCode();
			result = 31 * result + type.hashCode();
			return result;
		}
		
		//----------------------------------------------------------------------------------------------------
		@Override
		public boolean equals(final Object o) {
			if (o instanceof BreedInfo) {
				BreedInfo that = (BreedInfo) o;
				return this.agentSetName.equals(that.agentSetName) && this.type == that.type &&
				       ((this.agentName == null && that.agentName == null) || this.agentName.equals(that.agentName));
			}
			return false;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	static enum BreedType { TURTLE, DIRECTED, UNDIRECTED }
}