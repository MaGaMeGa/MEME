package ai.aitia.meme.paramsweep.platform.netlogo.impl;

import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.BREED_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.CLOSE_BRACKET;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.D_LINK_BREED_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.EXTENSIONS_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.GLOBALS_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.INCLUDES_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.MAGIC_STRING;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.OPEN_BRACKET;
import static ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.U_LINK_BREED_KEYWORD;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.platform.netlogo.impl.MEMENetLogoParser.BreedInfo;
import ai.aitia.meme.paramsweep.platform.netlogo.info.NLStatisticGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.GeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.OperatorGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.ScriptGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;
import ai.aitia.meme.paramsweep.utils.Util;

public class NetLogoScriptParser {

	//====================================================================================================
	// members
	
	private static final String SUFFIX = "_aitiaGeneratedDummy";
	
	private ScriptGeneratedRecordableInfo info = null;
	private List<GeneratedRecordableInfo> others = null;
	private IPSWInformationProvider provider = null;
	private List<String> generatedScripts = new ArrayList<String>();
	
	private final String SETUP_COMMAND;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public NetLogoScriptParser(final ScriptGeneratedRecordableInfo info, final  List<GeneratedRecordableInfo> others,
							   final IPSWInformationProvider provider, final String setupCommand) {
		this.info = info;
		this.others = others;
		this.provider = provider;
		this.SETUP_COMMAND = setupCommand;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> check() {
		List<String> errors = new ArrayList<String>();
		String model = provider.getModelFile().getAbsolutePath();
		final List<UserDefinedVariable> userVariables = collectVariables(info);
		if (others.size() > 0 || userVariables.size() > 0)
			model = createDummyModel(errors,userVariables); // returns null if any error occurs
		if (errors.size() == 0) {
			MEMEHeadlessWorkspace workspace = new MEMEHeadlessWorkspace();
			try {
				workspace.open(model);
				final String initializationCode = createInitializationCode(userVariables);
				
				NetLogoSyntaxChecker.SYNTAX_CHECKER.checkSyntax("to-report __bogus_report\n" + initializationCode + "\n" + info.getSource() + "\nend",
																true,workspace.world.program(),workspace.getProcedures(),
																workspace.getExtensionManager(),true);
			} catch (Exception e) {
				errors.add(Util.getLocalizedMessage(e));
			} finally {
				try {
					workspace.dispose();
				} catch (InterruptedException e) {}
			}
		}
		if (model != null && (others.size() > 0 || userVariables.size() >0))
			deleteDummyModel(model);
		return errors;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> checkVariable(final UserDefinedVariable variable) {
		final List<String> errors = new ArrayList<String>();
		final List<UserDefinedVariable> userVariables = new ArrayList<UserDefinedVariable>();
		userVariables.add(variable);
		final String model = createDummyModel(errors,userVariables); // returns null if any error occurs
		if (errors.size() == 0) {
			MEMEHeadlessWorkspace workspace = new MEMEHeadlessWorkspace();
			try {
				workspace.open(model);
				final String initializationCode = createInitializationCode(userVariables);
				NetLogoSyntaxChecker.SYNTAX_CHECKER.checkSyntax("to-report __bogus_report\n" + initializationCode + "\nend",true,
																workspace.world.program(),workspace.getProcedures(),workspace.getExtensionManager(),
																true);
			} catch (Exception e) {
				errors.add(Util.getLocalizedMessage(e));
			} finally {
				try {
					workspace.dispose();
				} catch (InterruptedException e) {}
			}
		}
		if (model != null)
			deleteDummyModel(model);
		return errors;
	}

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void deleteDummyModel(String model) {
		new File(provider.getModelFile().getParentFile(),model).delete();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String createDummyModel(final List<String> errors, final List<UserDefinedVariable> userVariables) {
		String modelName = null;
		try {
			File file = provider.getModelFile();
			String modelFileName = file.getName();
			int idx = modelFileName.lastIndexOf('.');
			String name = idx == -1 ? modelFileName : modelFileName.substring(0,idx);
			modelName = name + SUFFIX + "__" + Util.getTimeStamp() + ".nlogo";

			FileReader reader = new FileReader(file);
			String source = new String();
			char[] buffer = new char[8096];
			int chars = 0;
			while ((chars = reader.read(buffer)) > 0) 
				source += String.valueOf(buffer,0,chars);
			try {
				reader.close();
			} catch (IOException ee) {}

			MEMENetLogoParser parser = new MEMENetLogoParser(source,file.getParent(),SETUP_COMMAND);
			parser.parse();

			File descendant = new File(file.getParentFile(),modelName);
			PrintWriter writer = new PrintWriter(new FileWriter(descendant));
			
			// header section
			StringBuilder line = new StringBuilder(EXTENSIONS_KEYWORD);
			line.append(" ").append(OPEN_BRACKET);
			for (String extension : parser.getExtensions())
				line.append(extension).append(" ");
			line.append(CLOSE_BRACKET);
			writer.println(line.toString());
			
			line = new StringBuilder(GLOBALS_KEYWORD);
			line.append(" ").append(OPEN_BRACKET).append(" ");
			for (String global : parser.getGlobals())
				line.append(global).append(" ");
			for (final UserDefinedVariable variable : userVariables)
				line.append(variable.getName()).append(" ");
			line.append(CLOSE_BRACKET);
			writer.println(line.toString());
			
			line = new StringBuilder(INCLUDES_KEYWORD);
			line.append(" ").append(OPEN_BRACKET).append(" ");
			for (String include : parser.getIncludes())
				line.append("\"").append(include).append("\" ");
			line.append(CLOSE_BRACKET);
			writer.println(line.toString());
			
			for (final BreedInfo bi : parser.getBreeds()) {
				if (!bi.isFromIncludeFile()) {
					String _line = BREED_KEYWORD + " " + OPEN_BRACKET + " " + bi.getAgentSetName() + 
								   (bi.getAgentName() == null ? "" : " " + bi.getAgentName() + " ") + CLOSE_BRACKET;
					writer.println(_line);
				}
			}
			
			for (final BreedInfo bi : parser.getDirectedLinkBreeds()) {
				if (!bi.isFromIncludeFile()) {
					String _line = D_LINK_BREED_KEYWORD + " " + OPEN_BRACKET + " " + bi.getAgentSetName() + " " + bi.getAgentName() + " " + CLOSE_BRACKET;
					writer.println(_line);
				}
			}

			for (final BreedInfo bi : parser.getUndirectedLinkBreeds()) {
				if (!bi.isFromIncludeFile()) {
					String _line = U_LINK_BREED_KEYWORD + " " + OPEN_BRACKET + " " + bi.getAgentSetName() + " " + bi.getAgentName() + " " + CLOSE_BRACKET;
					writer.println(_line);
				}
			}
			
			for (String otherHeader : parser.getOtherHeaders())
				writer.println(otherHeader);
			writer.println();
			
			// procedures section
			writer.println(parser.getSetupMethod());
			writer.println();
			for (String method : parser.getMethods()) {
				writer.println(method);
				writer.println();
			}
			
			if (others != null) {
				for (GeneratedRecordableInfo info : others) 
					insertReporter(info,writer);
			}

			writer.println(MAGIC_STRING);
			writer.println();
			
			// other sections
			appendOthers(file.getAbsolutePath(),writer);
			
			writer.flush();
			writer.close();
		} catch (IOException e) {
			errors.add(Util.getLocalizedMessage(e));
			modelName = null;
		}
		return new File(provider.getModelFile().getParentFile(),modelName).getAbsolutePath();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void insertReporter(GeneratedRecordableInfo info, PrintWriter writer) {
		if (info instanceof NLStatisticGeneratedRecordableInfo) {
			NLStatisticGeneratedRecordableInfo _info = (NLStatisticGeneratedRecordableInfo) info;
			insertDummyStatistic(_info,writer);
		} else if ((info instanceof ScriptGeneratedRecordableInfo) ||  (info instanceof OperatorGeneratedRecordableInfo)) 
			insertScriptOrOperator(info,writer);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void insertDummyStatistic(NLStatisticGeneratedRecordableInfo info, PrintWriter writer) {
		if (generatedScripts.contains(info.getAccessibleName())) return;
		List<GeneratedRecordableInfo> references = info.getReferences();
		for (GeneratedRecordableInfo gri : references) {
			if (gri instanceof NLStatisticGeneratedRecordableInfo) {
				NLStatisticGeneratedRecordableInfo _gri = (NLStatisticGeneratedRecordableInfo) gri;
				insertDummyStatistic(_gri,writer);
			} else if ((gri instanceof ScriptGeneratedRecordableInfo) || (gri instanceof OperatorGeneratedRecordableInfo)) 
				insertScriptOrOperator(gri,writer);
		}
		writer.println(createDummyReporter(info));
		generatedScripts.add(info.getAccessibleName());
	}

	//----------------------------------------------------------------------------------------------------
	private String createDummyReporter(NLStatisticGeneratedRecordableInfo info) {
		StringBuilder sb = new StringBuilder("to-report ");
		sb.append(info.getAccessibleName()).append("\n");
		sb.append("report ").append(defaultValue(info.getType())).append("\nend\n");
		return sb.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String defaultValue(Class<?> type) {
		if (type.isPrimitive() && !type.equals(Boolean.TYPE) && !type.equals(Character.TYPE))
			return "0";
		else if (type.equals(Boolean.TYPE) || type.equals(Boolean.class))
			return "true";
		return "dummy";
	}
	
	//----------------------------------------------------------------------------------------------------
	private void insertScriptOrOperator(GeneratedRecordableInfo info, PrintWriter writer) {
		if (generatedScripts.contains(info.getAccessibleName())) return;
		List<GeneratedRecordableInfo> references = info.getReferences();
		for (GeneratedRecordableInfo gri : references) {
			if (gri instanceof NLStatisticGeneratedRecordableInfo) {
				NLStatisticGeneratedRecordableInfo _gri = (NLStatisticGeneratedRecordableInfo) gri;
				insertDummyStatistic(_gri,writer);
			} if ((gri instanceof ScriptGeneratedRecordableInfo) || (gri instanceof OperatorGeneratedRecordableInfo)) 
				insertScriptOrOperator(gri,writer);
		}
		String source = "to-report " + info.getAccessibleName() + "\n" + info.getSource() + "\nend\n";
		writer.println(source);
		generatedScripts.add(info.getAccessibleName());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void appendOthers(String originalFilePath, PrintWriter writer) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(originalFilePath));
		String line = null;
		boolean afterMagicLine = false;
		while ((line = reader.readLine()) != null) {
			line = line.trim();
			if (!afterMagicLine) {
				if (MAGIC_STRING.equals(line))
					afterMagicLine = true;
			} else 
				writer.println(line);
			
		}
		reader.close();
	}
	
	//----------------------------------------------------------------------------------------------------
	private List<UserDefinedVariable> collectVariables(final GeneratedRecordableInfo gri) {
		final List<UserDefinedVariable> result = new ArrayList<UserDefinedVariable>();
		final List<GeneratedRecordableInfo> references = gri.getReferences();
		for (final GeneratedRecordableInfo info : references) 
			Util.addAllDistinct(result,collectVariables(info));
		if (gri instanceof ScriptGeneratedRecordableInfo) {
			ScriptGeneratedRecordableInfo sgri = (ScriptGeneratedRecordableInfo) gri;
			Util.addAllDistinct(result,sgri.getUserVariables());
		}
		return result;
	}
	
	//----------------------------------------------------------------------------------------------------
	private String createInitializationCode(final List<UserDefinedVariable> userVariables) {
		final StringBuilder code = new StringBuilder(); 
		for (final UserDefinedVariable variable : userVariables)
			code.append(variable.getInitializationCode()).append("\n");
		return code.toString();
	}
}