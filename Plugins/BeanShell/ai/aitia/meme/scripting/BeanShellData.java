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
package ai.aitia.meme.scripting;

import java.util.BitSet;
import java.util.List;

import org.apache.bsf.BSFException;

import bsh.Interpreter;
import bsh.Primitive;

import ai.aitia.meme.pluginmanager.Parameter;
import ai.aitia.meme.pluginmanager.IScriptLanguage;
import ai.aitia.meme.pluginmanager.IScriptLanguage.IContext;
import ai.aitia.meme.utils.Utils;

//-------------------------------------------------------------------------
public class BeanShellData 
{
	Interpreter	interp;
	BitSet		isNameOK;

	//=========================================================================
	//	Package methods

	//-------------------------------------------------------------------------
	BeanShellData init(IContext context) throws BSFException {
		// Ha majd valamikor vissza kell terni BSFManager hasznalatara, akkor 
		// igy kell letrehozni az interpretert:
		//BSFManager mgr = new BSFManager();
		//mgr.setClassLoader(this.getClass().getClassLoader());
		//BSFEngine engine = mgr.loadScriptingEngine("beanshell");
		//interp = (bsh.Interpreter)engine.eval("", -1, -1, "this.interpreter.parent");

		interp = new Interpreter();
		try {
			// Az alabbi csomagban levo .class fajlok BeanShell parancsokat implementalnak.
			// Ezek adnak lehetoseget 
			// - nev szerinti parameterkeresesre (get(), geti(), geto(), getv())
			// - getCurrentRowInfo() adatok lekerdezesere (csoport/tagolas elso/utolso infokra is): gett()
			// - context.callAggrFn() hasznalatara (call())
			interp.eval("importCommands(\"ai.aitia.meme.scripting.bshcmds\")");
		} catch (Exception e) {
			throw asBSFException(e);
		}

		detectProperNames(context);
		return this;
	}

	//-------------------------------------------------------------------------
	public static void setContext(Interpreter interp, IContext context) throws bsh.UtilEvalError {
		// Technikai valtozo az interpreterben, a bshcmds csomagban levo parancsoknak
		// van szukseguk ra. Ld. getContext() hasznaloit.
		setVariable(interp, "$MEME$", context);
	}
	public static IContext getContext(Interpreter interp) throws bsh.EvalError {
		return (IContext)interp.get("$MEME$");
	}

	//-------------------------------------------------------------------------
	void dispose() {
		isNameOK = null;
		if (interp != null) {
			// bsh.Interpreter.sharedObject-ben marad egy statikus referencia az 
			// interpreterre, es igy mindenre ami benne letrejott. Ezert szukseges
			// kitakaritani az interpretert.
			interp.getNameSpace().clear();
			interp = null;
		}
	}

	//-------------------------------------------------------------------------
	Object eval(IContext context, String script) throws BSFException {
		updateChangedValues(context);
		try {
			setContext(interp, context);
			return interp.eval(script);
		} catch (Exception e) {
			throw asBSFException(e);
		}
	}

	//=========================================================================
	//	Internals

	//-------------------------------------------------------------------------
	private void updateChangedValues(IContext context) throws BSFException {
		IScriptLanguage.IRowInfo info = context.getCurrentRowInfo();
		if (info != null) {
			Integer run = null, tick = null;
			if (info instanceof IScriptLanguage.IResultInfo) {
				IScriptLanguage.IResultInfo ri = (IScriptLanguage.IResultInfo)info;
				//batch	= ri.getBatch();
				run		= ri.getRun();
				tick	= ri.getTick();
			} 
			else if (info instanceof IScriptLanguage.IViewInfo) {
				tick	= ((IScriptLanguage.IViewInfo)info).getRowID();
			}
			//setVariable("$Batch$", batch);
			setVariable("$Run$",   run);
			setVariable("$Tick$",  tick);
		}
		
		BitSet changed = context.getChangedParIndices();
		changed.and(isNameOK);
		if (changed.isEmpty()) return;
		changed.and(context.getVisibleParIndices());

		List<? extends Parameter> allPars = context.getAllPars();
		List<Object[]> allValues = context.getAllValues();
		for (int i = changed.nextSetBit(0); i >= 0; i = changed.nextSetBit(i+1)) {
			Object val = (context.isSingleValue(i)) ? allValues.get(i)[0] : allValues.get(i);
			setVariable(allPars.get(i).getName(), val);
		}
	}

	//-------------------------------------------------------------------------
	private void detectProperNames(IContext context) {
		List<? extends Parameter> allPars = context.getAllPars();
		isNameOK = new BitSet(allPars.size());
		int i = 0;
		for (Parameter p : allPars) {
			isNameOK.set(i++, isNameOK(p.getName()));
		}
	}

	//-------------------------------------------------------------------------
	private boolean isNameOK(String name) {
		if (name == null || name.length() == 0)
			return false;
		if (!Character.isJavaIdentifierStart(name.charAt(0)))
			return false;
		for (int i = 1; i < name.length(); ++i) {
			if (!Character.isJavaIdentifierPart(name.charAt(i)))
				return false;
		}
		return true;
	}

	//-------------------------------------------------------------------------
	private void setVariable(String name, Object value) throws BSFException {
		try {
			setVariable(interp, name, value);
		} catch (Exception e) {
			throw asBSFException(e);
		}
	}

	//-------------------------------------------------------------------------
	private static void setVariable(Interpreter interp, String name, Object value) throws bsh.UtilEvalError {
		if (value == null) {
			value = Primitive.NULL;
		} 
		else if (Primitive.isWrapperType(value.getClass())) {
			value = new Primitive(value);
		}
		// ez a hivas interp.set()-hez kepest megsporolja a BeanShell-callstack generalast
		interp.getNameSpace().setVariable(name, value, false);
		// false: a nemletezo valtozok automatikus letrehozasa
	}

	//-------------------------------------------------------------------------
	private static BSFException asBSFException(Exception e) {
		if (e instanceof bsh.InterpreterError)
			return new BSFException("BeanShell interpreter internal error: " + Utils.getLocalizedMessage(e));
		if (e instanceof bsh.TargetError)
			return new BSFException("Exception occured in script: " + 
					Utils.getLocalizedMessage(((bsh.TargetError)e).getTarget()));
		if (e instanceof bsh.EvalError)
			return new BSFException("BeanShell script error: " + Utils.getLocalizedMessage(e));

		return new BSFException("Exception occured in script: " + Utils.getLocalizedMessage(e));
	}

}
