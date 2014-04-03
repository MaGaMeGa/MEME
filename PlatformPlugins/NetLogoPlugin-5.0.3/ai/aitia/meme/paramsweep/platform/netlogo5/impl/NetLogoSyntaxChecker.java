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

import java.util.LinkedHashMap;
import java.util.Map;

import org.nlogo.api.CompilerException;
import org.nlogo.api.ExtensionManager;
import org.nlogo.api.Program;
import org.nlogo.api.Token;
import org.nlogo.api.TokenizerInterface;
import org.nlogo.compiler.ExpressionParser;
import org.nlogo.compiler.IdentifierParser;
import org.nlogo.compiler.StructureParser;
import org.nlogo.nvm.Procedure;
import org.nlogo.util.Femto;

import scala.Function1;
import scala.Option;
import scala.Some;
import scala.collection.Iterable;
import scala.collection.Iterator$;
import scala.collection.JavaConversions;
import scala.runtime.AbstractFunction1;
import scala.runtime.BoxedUnit;

public class NetLogoSyntaxChecker {

	//====================================================================================================
	// members
	
	public static final NetLogoSyntaxChecker SYNTAX_CHECKER = new NetLogoSyntaxChecker();
    private final TokenizerInterface tokenizer3D = Femto.scalaSingleton(TokenizerInterface.class,"org.nlogo.lex.Tokenizer3D");
    private final TokenizerInterface tokenizer2D = Femto.scalaSingleton(TokenizerInterface.class,"org.nlogo.lex.Tokenizer2D");
    private final Map<String,Procedure> noProcedures =  new LinkedHashMap<String,Procedure>(); 

	//====================================================================================================
	// methods
    
    //----------------------------------------------------------------------------------------------------
	public void checkSyntax(final String source, final boolean subprogram, final Program program, final Map<String,Procedure> oldProcedures,
							final ExtensionManager extensionManager, final boolean parse) throws CompilerException {
		final TokenizerInterface tokenizer = tokenizer(program.is3D());
	    final Option<String> NONE = Some.<String>apply(null);
		final StructureParser.Results results = new StructureParser(tokenizer.tokenizeRobustly(source),NONE,program,oldProcedures,extensionManager,tokenizer).
																																					  parse(subprogram);
//		final IdentifierParser identifierParser = new IdentifierParser(program,noProcedures,results.procedures(),!parse);
//		final IdentifierParser identifierParser = new IdentifierParser(program,results.procedures(),noProcedures,!parse);
		final IdentifierParser identifierParser = new IdentifierParser(program,oldProcedures,noProcedures,!parse);
		JavaConversions.collectionAsScalaIterable(results.procedures().values()).foreach(new CheckSyntaxFunction(parse,results,identifierParser));
	}

	//====================================================================================================
	// assistant methods
	
	//----------------------------------------------------------------------------------------------------
	private NetLogoSyntaxChecker() {}
	
	//----------------------------------------------------------------------------------------------------
	private TokenizerInterface tokenizer(final boolean is3D) { return is3D ? tokenizer3D : tokenizer2D; }
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	private static class CheckSyntaxFunction extends AbstractFunction1<Procedure,Object> implements Function1<Procedure,Object> /*, Serializable */ {
		
		//====================================================================================================
		// members
		
//		private static final long serialVersionUID = 4022967438939089082L;
	    private final IdentifierParser identifierParser;
	    private final StructureParser.Results results;
	    private final boolean parse;
	    
	    //====================================================================================================
		// methods

	    public CheckSyntaxFunction(final boolean parse, final StructureParser.Results results, final IdentifierParser identifierparser) {
	    	super();
	        this.parse = parse;
	        this.results = results;
	        this.identifierParser = identifierparser;
	    }
	    
	    //====================================================================================================
		// implemented interfaces
	    
	    //----------------------------------------------------------------------------------------------------
		public final Object apply(final Procedure procedure) {
	        apply1(procedure);
	        return BoxedUnit.UNIT;
	    }
		
		//====================================================================================================
		// assistant methods

	    //----------------------------------------------------------------------------------------------------
		private final void apply1(final Procedure procedure) {
	        final Iterable<Token> tokens = identifierParser.process(results.tokens().apply(procedure).iterator(),procedure);
	        if (parse) {
	        	final ExpressionParser parser = new ExpressionParser(procedure,Iterator$.MODULE$.from(1));
	            parser.parse(tokens);
	        }
	    }
	}
}
