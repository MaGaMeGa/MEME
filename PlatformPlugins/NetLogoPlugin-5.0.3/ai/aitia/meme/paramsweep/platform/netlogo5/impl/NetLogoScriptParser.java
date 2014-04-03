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

import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.BREED_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.CLOSE_BRACKET;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.D_LINK_BREED_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.EXTENSIONS_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.GLOBALS_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.INCLUDES_KEYWORD;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.MAGIC_STRING;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.OPEN_BRACKET;
import static ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.U_LINK_BREED_KEYWORD;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import ai.aitia.meme.paramsweep.platform.IPSWInformationProvider;
import ai.aitia.meme.paramsweep.platform.netlogo.info.NLStatisticGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.netlogo5.impl.MEMENetLogoParser.BreedInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.GeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.OperatorGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.platform.repast.info.ScriptGeneratedRecordableInfo;
import ai.aitia.meme.paramsweep.utils.UserDefinedVariable;
import ai.aitia.meme.paramsweep.utils.Util;

public class NetLogoScriptParser {

	//====================================================================================================
	// members
	
	private static final String SUFFIX = "_aitiaGeneratedDummy";
	
	private final ScriptGeneratedRecordableInfo info;
	private final List<GeneratedRecordableInfo> others;
	private final IPSWInformationProvider provider;
	private final List<String> generatedScripts = new ArrayList<String>();
	
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
		final List<String> errors = new ArrayList<String>();
		String model = provider.getModelFile().getAbsolutePath();
		final List<UserDefinedVariable> userVariables = collectVariables(info);
		if (!others.isEmpty() || !userVariables.isEmpty())
			model = createDummyModel(errors,userVariables); // returns null if any error occurs
		if (errors.isEmpty()) {
			final MEMEHeadlessWorkspace workspace = MEMEHeadlessWorkspace.newWorkspace();
			try {
				workspace.open(model);
				final String initializationCode = createInitializationCode(userVariables);
				
				NetLogoSyntaxChecker.SYNTAX_CHECKER.checkSyntax("to-report __bogus_report\n" + initializationCode + "\n" + info.getSource() + "\nend",
																true,workspace.world.program(),workspace.getProcedures(),
																workspace.getExtensionManager(),true);
			} catch (final Exception e) {
				errors.add(Util.getLocalizedMessage(e));
			} finally {
				try {
					workspace.dispose();
				} catch (final InterruptedException _) {}
			}
		}
		if (model != null && (!others.isEmpty() || !userVariables.isEmpty()))
			deleteDummyModel(model);
		return errors;
	}
	
	//----------------------------------------------------------------------------------------------------
	public List<String> checkVariable(final UserDefinedVariable variable) {
		final List<String> errors = new ArrayList<String>();
		final List<UserDefinedVariable> userVariables = new ArrayList<UserDefinedVariable>();
		userVariables.add(variable);
		final String model = createDummyModel(errors,userVariables); // returns null if any error occurs
		if (errors.isEmpty()) {
			final MEMEHeadlessWorkspace workspace = MEMEHeadlessWorkspace.newWorkspace();
			try {
				workspace.open(model);
				final String initializationCode = createInitializationCode(userVariables);
				NetLogoSyntaxChecker.SYNTAX_CHECKER.checkSyntax("to-report __bogus_report\n" + initializationCode + "\nend",true,
																workspace.world.program(),workspace.getProcedures(),workspace.getExtensionManager(),
																true);
			} catch (final Exception e) {
				errors.add(Util.getLocalizedMessage(e));
			} finally {
				try {
					workspace.dispose();
				} catch (final InterruptedException _) {}
			}
		}
		if (model != null)
			deleteDummyModel(model);
		return errors;
	}

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private void deleteDummyModel(final String model) {
		new File(provider.getModelFile().getParentFile(),model).delete();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String createDummyModel(final List<String> errors, final List<UserDefinedVariable> userVariables) {
		String modelName = null;
		try {
			final File file = provider.getModelFile();
			final String modelFileName = file.getName();
			final int idx = modelFileName.lastIndexOf('.');
			final String name = idx == -1 ? modelFileName : modelFileName.substring(0,idx);
			modelName = name + SUFFIX + "__" + Util.getTimeStamp() + ".nlogo";

			final FileReader reader = new FileReader(file);
			final StringBuffer source = new StringBuffer();
			final char[] buffer = new char[8096];
			int chars = 0;
			while ((chars = reader.read(buffer)) > 0) 
				source.append(String.valueOf(buffer,0,chars));
			try {
				reader.close();
			} catch (final IOException _) {}

			final MEMENetLogoParser parser = new MEMENetLogoParser(source.toString(),file.getParent(),SETUP_COMMAND);
			parser.parse();

			final File descendant = new File(file.getParentFile(),modelName);
			final PrintWriter writer = new PrintWriter(new FileWriter(descendant));
			
			// header section
			StringBuilder line = new StringBuilder(EXTENSIONS_KEYWORD);
			line.append(" ").append(OPEN_BRACKET);
			for (final String extension : parser.getExtensions())
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
					final String _line = BREED_KEYWORD + " " + OPEN_BRACKET + " " + bi.getAgentSetName() + 
								   (bi.getAgentName() == null ? "" : " " + bi.getAgentName() + " ") + CLOSE_BRACKET;
					writer.println(_line);
				}
			}
			
			for (final BreedInfo bi : parser.getDirectedLinkBreeds()) {
				if (!bi.isFromIncludeFile()) {
					final String _line = D_LINK_BREED_KEYWORD + " " + OPEN_BRACKET + " " + bi.getAgentSetName() + " " + bi.getAgentName() + " " + CLOSE_BRACKET;
					writer.println(_line);
				}
			}

			for (final BreedInfo bi : parser.getUndirectedLinkBreeds()) {
				if (!bi.isFromIncludeFile()) {
					final String _line = U_LINK_BREED_KEYWORD + " " + OPEN_BRACKET + " " + bi.getAgentSetName() + " " + bi.getAgentName() + " " + CLOSE_BRACKET;
					writer.println(_line);
				}
			}
			
			for (final String otherHeader : parser.getOtherHeaders())
				writer.println(otherHeader);
			writer.println();
			
			// procedures section
			writer.println(parser.getSetupMethod());
			writer.println();
			for (final String method : parser.getMethods()) {
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
		} catch (final IOException e) {
			errors.add(Util.getLocalizedMessage(e));
			modelName = null;
		}
		return modelName == null ? null : new File(provider.getModelFile().getParentFile(),modelName).getAbsolutePath();
	}
	
	//----------------------------------------------------------------------------------------------------
	private void insertReporter(final GeneratedRecordableInfo info, final PrintWriter writer) {
		if (info instanceof NLStatisticGeneratedRecordableInfo) {
			final NLStatisticGeneratedRecordableInfo _info = (NLStatisticGeneratedRecordableInfo) info;
			insertDummyStatistic(_info,writer);
		} else if ((info instanceof ScriptGeneratedRecordableInfo) ||  (info instanceof OperatorGeneratedRecordableInfo)) 
			insertScriptOrOperator(info,writer);
	}
	
	//----------------------------------------------------------------------------------------------------
	private void insertDummyStatistic(final NLStatisticGeneratedRecordableInfo info, final PrintWriter writer) {
		if (generatedScripts.contains(info.getAccessibleName())) return;
		final List<GeneratedRecordableInfo> references = info.getReferences();
		for (final GeneratedRecordableInfo gri : references) {
			if (gri instanceof NLStatisticGeneratedRecordableInfo) {
				final NLStatisticGeneratedRecordableInfo _gri = (NLStatisticGeneratedRecordableInfo) gri;
				insertDummyStatistic(_gri,writer);
			} else if ((gri instanceof ScriptGeneratedRecordableInfo) || (gri instanceof OperatorGeneratedRecordableInfo)) 
				insertScriptOrOperator(gri,writer);
		}
		writer.println(createDummyReporter(info));
		generatedScripts.add(info.getAccessibleName());
	}

	//----------------------------------------------------------------------------------------------------
	private String createDummyReporter(final NLStatisticGeneratedRecordableInfo info) {
		final StringBuilder builder = new StringBuilder("to-report ");
		builder.append(info.getAccessibleName()).append("\n");
		builder.append("report ").append(defaultValue(info.getType())).append("\nend\n");
		return builder.toString();
	}
	
	//----------------------------------------------------------------------------------------------------
	private String defaultValue(final Class<?> type) {
		if (type.isPrimitive() && !type.equals(Boolean.TYPE) && !type.equals(Character.TYPE))
			return "0";
		else if (type.equals(Boolean.TYPE) || type.equals(Boolean.class))
			return "true";
		return "dummy";
	}
	
	//----------------------------------------------------------------------------------------------------
	private void insertScriptOrOperator(final GeneratedRecordableInfo info, final PrintWriter writer) {
		if (generatedScripts.contains(info.getAccessibleName())) return;
		final List<GeneratedRecordableInfo> references = info.getReferences();
		for (final GeneratedRecordableInfo gri : references) {
			if (gri instanceof NLStatisticGeneratedRecordableInfo) {
				final NLStatisticGeneratedRecordableInfo _gri = (NLStatisticGeneratedRecordableInfo) gri;
				insertDummyStatistic(_gri,writer);
			} if ((gri instanceof ScriptGeneratedRecordableInfo) || (gri instanceof OperatorGeneratedRecordableInfo)) 
				insertScriptOrOperator(gri,writer);
		}
		final String source = "to-report " + info.getAccessibleName() + "\n" + info.getSource() + "\nend\n";
		writer.println(source);
		generatedScripts.add(info.getAccessibleName());
	}
	
	//----------------------------------------------------------------------------------------------------
	private void appendOthers(final String originalFilePath, final PrintWriter writer) throws IOException {
		final BufferedReader reader = new BufferedReader(new FileReader(originalFilePath));
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
			final ScriptGeneratedRecordableInfo sgri = (ScriptGeneratedRecordableInfo) gri;
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
