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
package ai.aitia.meme.pluginmanager;

import java.util.BitSet;
import java.util.List;

import org.apache.bsf.BSFException;

import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ViewRec;
import ai.aitia.meme.viewmanager.ParameterSet.Category;

/**
 * Interface for plugins providing a scripting language BSF engine.
 * 
 * {@link #getLocalizedName()} must return the BSF-standard name 
 * of the language (see org.apache.bsf.Languages.properties)
 */ 
 /* Jelenleg a BSF egeszen el van rejtve a pluginba, tehat akar nem is
 * kellene hasznalni a BSF-et. A kesobbiekben azonban felmerulhet az 
 * igeny arra, hogy mas pluginekbol, nyelvfuggetlen modon is lehessen 
 * boviteni a scriptezesi lehetosegeket. Ezt Bean-ek hasznalataval es
 * BSF-el jol meg lehet oldani, tehat ekkor majd bevezetunk egy olyan 
 * muveletet is, aminek az interpretert BSFEngine-kent kell visszaadnia.
 * Szamitani kell tehat erre.
 * Ezert van az, hogy eval() mar most is csak BSFException-t dobhat:
 * igy kenyelmes ViewCreation-ban a legfelso szintu exception-handlerben
 * kulonvalogatni a usernek szolo, scripthibaval kapcsolatos hibauzenetet
 * a tobbitol.
 */
public interface IScriptLanguage extends IPlugin
{
	/** Interface for representing rows for the scripting language plugins. */
	interface IRowInfo {
		static final int GROUP_FIRST	= 1;
		static final int GROUP_LAST	= 2;
		static final int BLOCK_FIRST	= 4;
		static final int BLOCK_LAST	= 8;

		/** Returns the flags. */
		int				getFlags();
	}

	/** Interface for representing information about results rows for the scripting language plugins. */
	interface IResultInfo extends IRowInfo {
		/** Returns the model. */
		Model			getModel();
		/** Returns the batch id. */
		int				getBatch();
		/** Returns the run id. */
		int				getRun();
		/** Returns the tick number. */
		int				getTick();
		/** Returns the start time. */
		long			getStartTime();
		/** Returns the end time. */
		long			getEndTime();
	}

	/** Interface for representing information about views rows for the scripting language plugins. */
	interface IViewInfo extends IRowInfo {
		/** Returns the view. */
		ViewRec			getView();
		/** Returns the id of the row. */
		int				getRowID();
	}

	/** 
	 * Type for arguments of IScriptingLanguage methods.
	 */
	interface IContext extends IPlugin.IContext 
	{
		/** Returns the list of the parameters. */
		List<? extends Parameter>	getAllPars();
		/** Returns the list of the arrays of the values of the parameters. */
		List<Object[]>	getAllValues();			//!< fixed-size list

		/**
		 * Returns true if the value of the parameter denoted by 'idx'
		 * should be <code>getAllValues().get(idx)[0]</code> instead of the
		 * whole Object[] array returned by <code>getAllValues().get(idx)</code>.
		 * May change when 'idx' is on in {@link #getChangedParIndices()}.
		 */
		boolean			isSingleValue(int idx);

		/** Returns the indices of the visible parameters.
		 *  The returned BitSet hasn't changed during one eval() call, but
		 *  between repeated eval() calls may change. The filtering of the 
		 *  unsupportable names is also the task of the plugin and it must
		 *  provide string-based access for all parameters.
		 */
		 /* A visszaadott BitSet egyetlen eval() hivason belul nem valtozik, 
		 * de ismetelt eval() hivasok kozott valtozhat. A plugin feladata 
		 * az is, hogy az adott nyelvben nem megengedett neveket kiszurje,
		 * es biztositson alternativ string-alapu elerest az interpreterben 
		 * az osszes parameterhez (ami csak getAllPars()-ban van). A nevek
		 * valtozasara nem kell szamitani.
		 */
		BitSet			getVisibleParIndices();

		/** Returns the category of the 'p'.
		 *  In getAllPars() there are parameters with the same name, but
		 *  these are always in different category. It must maintain the
		 *  following order in the visibility: output, thisview, input.
		 *  The former hides the latter.
		 */  
		 /* getAllPars()-ban ugyanolyan nevu parameterbol tobb is lehet,   
		 * ilyenkor ezek mindig eltero kategoriakba tartoznak. Lathatosag
		 * szempontjabol ezt a sorrendet kell fenntartani: output, thisview,
		 * input (a korabbi eltakarja a kesobbit).
		 */
		Category		getCategory(Parameter p);

		/** Returns the indices of the changed parameters.
		 * If you calls this method twice in a row, the second returns with
		 * an empty Bitset.*/
		 /* Ha egymas utan ketszer lekerdezed, masodjara mar ures BitSet-et
		 * ad vissza (mindig uj BitSet objektumot hoz letre). A jelzett 
		 * parameterek nem biztos hogy lathatoak is (nem biztos hogy benne 
		 * vannak {@link #getVisibleParIndices()}-ben is).
		 * Ket {@link IScriptLanguage#eval()} kozott altalaban kerulhetnek 
		 * bele elemek, de akar egyetlen eval() kozben is ha a script belehiv 
		 * {@link #callAggrFn()}-ba. 
		 */
		BitSet			getChangedParIndices();

		/**
		 * Calls IVCPlugin.compute() on the plugin object specified by 'pluginType'.
		 * @param pluginType fully qualified class name of an IVCPlugin type, or
		 *              localized name of the plugin. Trailing "()" in the localized
		 *              name may be omitted.
		 * @param args Should be fixed-size but not unmodifiable.
		 *              <code>args.set()</code> should modify the corresponding 
		 *              caller variable in the interpreter. If the new value is
		 *              null or has different length, <code>args.set()</code> 
		 *              may throw an exception as described at 
		 *              {@link IVCPlugin.IContext#getAllValues()}.
		 * @throws NoSuchMethodException if the specified plugin is not loaded.
		 */
		Object			callAggrFn(String pluginType, List<Object[]> args) throws NoSuchMethodException;

		/** Returns an IResultInfo or IViewInfo object, or null */
		IRowInfo		getCurrentRowInfo();
	}

	/**
	 * The created interpreter should be stored into the 'context' map.    
	 * The caller will use separate 'context' object for every interpreter.
	 */
	void	createInterp(IContext context) throws Exception;

	/**
	 * Release the resources allocated by the specified interpreter instance.
	 * @param context The same context object which was passed to createInterp().
	 */
	void	disposeInterp(IContext context);

	/**
	 * Evaluates 'script' in the interpreter corresponding to 'context'.
	 * Precondition: {@link #createInterp()} has been called for 'context'.
	 * @return The return value of the last command in the script.
	 */ 
	 /* Tennivalok:
	 * - elso alkalommal:
	 *   - elore kiszurni a nem megengedett neveket 
	 *   - betenni az interpreterbe lehetoseget a getCurrentRowInfo() 
	 *     altal szolgaltatott adatok lekeresere
	 *   - valamint a parameterek ertekenek nev szerinti lekerdezesere
	 *     kategoriankent (get(name) - visible, geti(name) - input,
	 *     geto(name) - output, getv(name) - view)
	 *   - valamint callAggrFn() hasznalatara
	 * - minden alkalommal: a context.getVisibleParIndices()-ben szereplo
	 *   megengedett nevu es megvaltozott erteku parameterek erteket
	 *   frissiteni az interpreterben.
	 */
	Object	eval(IContext context, String script) throws BSFException;

}
