package ai.aitia.meme.paramsweep.platform.netlogo.impl;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import org.nlogo.api.CompilerException;
import org.nlogo.api.ExtensionManager;
import org.nlogo.api.Program;
import org.nlogo.api.Token;
import org.nlogo.api.TokenizerInterface;
import org.nlogo.compiler.ExpressionParser$;
import org.nlogo.compiler.IdentifierParser;
import org.nlogo.compiler.StructureParser;
import org.nlogo.nvm.Procedure;
import org.nlogo.util.Femto;
import org.nlogo.util.JCL$;

import scala.Function1;
import scala.Iterable;
import scala.runtime.BoxedUnit;

public class NetLogoSyntaxChecker {

	//====================================================================================================
	// members
	
	public static final NetLogoSyntaxChecker SYNTAX_CHECKER = new NetLogoSyntaxChecker();
    private final TokenizerInterface tokenizer3D = Femto.scalaSingleton(TokenizerInterface.class, "org.nlogo.lex.Tokenizer3D");
    private final TokenizerInterface tokenizer2D = Femto.scalaSingleton(TokenizerInterface.class, "org.nlogo.lex.Tokenizer2D");
    private final Map<String,Procedure> noProcedures =  new LinkedHashMap<String,Procedure>(); 

	//====================================================================================================
	// methods
	
    //----------------------------------------------------------------------------------------------------
	public void checkSyntax(final String source, final boolean subprogram, final Program program, final Map<String,Procedure> oldProcedures,
							final ExtensionManager extensionManager, final boolean parse) throws CompilerException {
		final TokenizerInterface t = tokenizer(program.is3D);
		final StructureParser.Results results = (new StructureParser(t.tokenizeRobustly(source),program,oldProcedures,extensionManager,t)).parse(subprogram);
//		final IdentifierParser identifierParser = new IdentifierParser(program,noProcedures,results.procedures(),!parse);
//		final IdentifierParser identifierParser = new IdentifierParser(program,results.procedures(),noProcedures,!parse);
		final IdentifierParser identifierParser = new IdentifierParser(program,oldProcedures,noProcedures,!parse);
		JCL$.MODULE$.iterableFromJava(results.procedures().values()).foreach(new CheckSyntaxFunction(parse,results,identifierParser));
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
	private static class CheckSyntaxFunction implements Function1<Procedure,Object>, Serializable {
		
		//====================================================================================================
		// members
		
		private static final long serialVersionUID = 4022967438939089082L;
	    private final IdentifierParser identifierParser;
	    private final StructureParser.Results results;
	    private final boolean parse;
	    
	    //====================================================================================================
		// methods

	    public CheckSyntaxFunction(final boolean parse, final StructureParser.Results results, final IdentifierParser identifierparser) {
	        this.parse = parse;
	        this.results = results;
	        this.identifierParser = identifierparser;
	    }

	    
	    //====================================================================================================
		// implemented interfaces
	    
	    //----------------------------------------------------------------------------------------------------
		public final Object apply(Procedure procedure) {
	        apply1(procedure);
	        return BoxedUnit.UNIT;
	    }
		
		//====================================================================================================
		// assistant methods

	    //----------------------------------------------------------------------------------------------------
		private final void apply1(final Procedure procedure) {
	        final Iterable<Token> tokens = identifierParser.process(results.tokens().apply(procedure).elements(),procedure);
	        if (parse)
	            ExpressionParser$.MODULE$.parse(tokens);
	    }
	}
}