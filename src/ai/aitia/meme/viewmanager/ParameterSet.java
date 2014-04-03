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
package ai.aitia.meme.viewmanager;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.database.AbstractResultsDb;
import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ViewRec;
import ai.aitia.meme.gui.lop.ILopListener;
import ai.aitia.meme.pluginmanager.Parameter;
import ai.aitia.meme.utils.Utils;

//-----------------------------------------------------------------------------
/** This class is manages the parameter sets. Parameters may come from the input tables
 *  (results, views) and the current view table (the parameters before the current 
 *   parameter).
 *   There are 3 namespaces for these parameters
 *  - input parameters of the input results tables (INPUT)
 *  - output parameters of the input results tables + all columns of the input view tables (OUTPUT)
 *  - columns of the current view (THISVIEW). 
 */
@SuppressWarnings("unchecked")
public class ParameterSet implements Runnable, ILopListener
{
	// Ilyen sorrendben jelennek meg a JTabbedPane lapjai:
	/** Categories of the parameters. The order defines the visibility. */
	public static enum Category { 
		OUTPUT		("Output"), 
		THISVIEW	("This view"),
		INPUT		("Input"); 

		Category(String title) { tabTitle = title; }
		public String tabTitle;
	};

	/** The parameters of the INPUT category. */
	TreeMap<Par, Par>		input 	= new TreeMap<Par, Par>();		// [EDT]
	/** The parameters of the OUTPUT category. */
	TreeMap<Par, Par>		output	= new TreeMap<Par, Par>();		// [EDT]
	/** The parameters of the THISVIEW category. */
	ArrayList<ThisViewPar>	thisview= new ArrayList<ThisViewPar>();	// [EDT] A bontott oszlopokat nem tartalmazza!

	EnumMap<Category, ParListModel> listModels = new EnumMap<Category, ParListModel>(Category.class);	// [EDT]
	int thisviewListModelLimit = -1;		// [EDT]

	ParListModel lm_all = null;				// [EDT]

	private HashMap<Long, Model>	modelsCache	= new HashMap<Long, Model>();	// [EDT + Model thread]

	// The following fields are used to pass data between loadParamsFrom() <-> run()
	// (i.e. between threads). These fields are synchronized with 'this'.
	// [EDT + Model thread]
	volatile ArrayList<Object>		tmpTables;
	volatile ArrayList<Parameter>	tmpIO[];
	volatile ArrayList<Object>		tmpFoundTables;
	Utils.IUnary<ArrayList<Object>>	tmpUpdateCb;


	//-------------------------------------------------------------------------
	/** This class represents columns during the view creation and contains category infomations, too. */
	public static class Par extends Parameter {
		private Category cat;
								Par(String name, ColumnType dt, Category c) { super(name, dt); cat = c; }
		public Category			getCategory()								{ return cat; }
	}

	//-------------------------------------------------------------------------
	// A THISVIEW parameterek tipusa surun valtozhat, ezert keruljuk a tipus
	// lemasolasat, redundans tarolasat, inkabb mindenkor behivatkozzuk 
	// Column.initialDataType-bol. Erre szolgal az alabbi osztaly.
	//
	// Ugyelni kell azonban arra is, hogy Column lecserelesekor frissuljon
	// a ra valo hivatkozas. Ez ugy tortenik, hogy Column lecserelese csak
	// Page_Columns.addOrModify()-ban fordul elo, ahonnan -> clearForm()
	// -> owner.beginUpdateParameters() -> ParameterSet.collectParams()
	// -> updateCategory() -> ArrayList.equals() utvonalon ide (equals())
	// kerul a vezerles, majd utana a [getListModelForALL() ->] 
	// ParListModel.updateFrom() -> Page_Columns.intervalAdded() -> 
	// updateDataTypes() utvonalon minden olyan helyre eler a frissites, 
	// ahol THISVIEW kategoriaju Par-okat tarolunk.
	//
	/** This class represents columns contained by the THISVIEW category during the view creation.
	 *  We need this class because the type of the THISVIEW parameters may change frequently. */
	static class ThisViewPar extends Par {
		ViewCreationRule.Column col;
		ThisViewPar(ViewCreationRule.Column c)		{ super(c.getName(), null, Category.THISVIEW); col = c; }
		
		@Override public ColumnType getDatatype()	{ return col.getInitialDataType(); }
		@Override public void setDatatype(ColumnType dt) { col.setInitialDataType(dt); }

		@Override public boolean equals(Object obj) {
			if (!super.equals(obj)) return false;
			// A ThisViewPar-okat 'col' szerint is hasonlitjuk. Erre azert van szukseg,
			// hogy updateCategory()-ban a p_newpars.equals() vizsgalat lecsereltesse
			// a Par-okat olyankor is, amikor sem a nev, sem a tipus nem valtozott, hanem 
			// "csak" kicsereltek alattuk a Column objektumot.
			if (obj instanceof ThisViewPar) return col == ((ThisViewPar)obj).col;
			return true;
		}
	}


	//-------------------------------------------------------------------------
	@SuppressWarnings("serial")
	static class ParListCellRenderer extends javax.swing.DefaultListCellRenderer {
		@Override
		public java.awt.Component getListCellRendererComponent(javax.swing.JList list, 
				Object value, int index, boolean isSelected, boolean cellHasFocus) {
			super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			Parameter p = (Parameter)value;
			setText(p.getName());
			setIcon(JTableScrollPane.getColumnTypeIcon(p.getDatatype()));
			return this;
		}
	}

	//-------------------------------------------------------------------------
	public synchronized HashMap<Long, Model> getModelsCache() {
		return modelsCache;
	}

	//-------------------------------------------------------------------------
	/**
	 * Looks for name in the specified category (null means all categories).
	 * @param c null means all categories.
	 * @param limit considered only when c == THISVIEW: only parameters _before_ 
	 *         this index are used (e.g. 0 means none). Negative value
	 *         means infinite.
	 */
	// [EDT]
	public Par findParam(String name, Category c, int limit) {
		if (name == null) 
			return null;
		Par namep = new Par(name, null, null);
		if (c == null || c == Category.OUTPUT) {
			Par ans = output.get(namep);
			if (ans != null) return ans;
		}
		if (c == null || c == Category.THISVIEW) {
			limit = (limit < 0) ? thisview.size() : Math.min(limit, thisview.size());
			for (int i = 0; i < limit; ++i)
				if (thisview.get(i).equals(name)) 
					return thisview.get(i);
		}
		if (c == null || c == Category.INPUT) {
			Par ans = input.get(namep);
			if (ans != null) return ans;
		}
		return null;
	}

	//-------------------------------------------------------------------------
	/** This method collects the parameters in an asynchronous way. It returns 
	 *  before the list of the parameters is created. An other thread, the Model
	 *  thread collect the parameters from the database. When it has finished, 
	 *  then a ListDataEvent will be generated and sent to the listeners of the 
	 *  list models returned by getListModel().
	 * @param viewcols the list of the columns of the current view 
	 * @param tables its elements can be Long[] arrays {model_id,batch#,run#} or
	 *                ViewRec objects. In the case of ViewRec we use the viewid,
	 *                then we haven't found the view, search further by name.
	 * @param updateCb A callback function object. The method calls this with the 
	 *  			   updated tables. The callback will be called in the EDT thread,
	 *  			   and its parameter is null if the updated table == original table 
	 */
	/* 
	 * Aszinkron modon mukodik: hamarabb visszater, mint ahogy a parameterek
	 * listaja tenylegesen eloallna. Egy masik szal (a Model szal) huzza be 
	 * az adatbazisbol parametereket. Amikor keszen van, akkor a getListModel()
	 * altal eleddig visszaadott ListModel-ek listenerei fognak ertesitest kapni
	 * (ListDataListener-ek, vszleg tobbszorosen).
	 * Megj: ListSelectionEvent nem biztos hogy generalodik a ListModel-beli
	 * valtozas hatasara!
	 */
	// [EDT]
	public void collectParams(	ArrayList<ViewCreationRule.Column>	viewcols,
								ArrayList<Object>					tables,
								Utils.IUnary<ArrayList<Object>>		updateCb)
	{
		if (tables != null) {
			synchronized (this) { tmpTables = tables; tmpUpdateCb = updateCb; }
			MEMEApp.LONG_OPERATION.begin("Collecting parameters...", this);
		}
		if (viewcols != null) {
			ArrayList<ThisViewPar> tmp = new ArrayList<ThisViewPar>(viewcols.size());
			for (ViewCreationRule.Column c : viewcols) {
				if (!c.isSplitted())
					tmp.add(new ThisViewPar(c));
			}
			updateCategory(Category.THISVIEW, tmp);
			if (tables == null && updateCb != null)
				updateCb.run(null);
		}
	}

	//-------------------------------------------------------------------------
	/* Feltolti input[]-ot es output[]-ot a tablesTmp[]-ben felsorolt tablakbol */ 
	// [Model thread]
	public void run() {
		Object[] allTables;
		synchronized (this) {
			allTables	= tmpTables.toArray();
			tmpTables	= null;
		}
		LinkedHashMap<Object, Columns> inputPars = new LinkedHashMap<Object, Columns>();
		LinkedHashMap<Object, Columns> outputPars = new LinkedHashMap<Object, Columns>();
		ArrayList<Object> found = new ArrayList<Object>();
		HashMap<Long, Model> tmpModelsCache = new HashMap<Long, Model>(allTables.length);

		for (Object o : allTables) {
			if (o instanceof Long[]) {
				Long model_id = ((Long[])o)[0];
				if (outputPars.containsKey(model_id)) {
					found.add(o);
					continue;
				}

				java.util.List<Result> l = MEMEApp.getResultsDb().getResults(model_id, 
													AbstractResultsDb.JUST_ANY_ONE);
				if (l != null && !l.isEmpty()) {
					Result r = l.get(0);
					inputPars.put(model_id, r.getParameterComb().getNames());
					outputPars.put(model_id, r.getOutputColumns());
					tmpModelsCache.put(model_id, r.getModel());
					found.add(o);
				}
			} else {
				ViewRec rec = (ViewRec)o;
				if (outputPars.containsKey(rec)) {
					found.add(o);
					continue;
				}

				String name = MEMEApp.getViewsDb().getName(rec.getViewID());
				if (name != null) {
					if (!name.equals(rec.getName())) rec = new ViewRec(name, rec.getViewID());
				} else {
					Long view_id = MEMEApp.getViewsDb().findView(rec.getName());
					if (view_id == null) continue;
					if (!view_id.equals(rec.getViewID())) rec = new ViewRec(rec.getName(), view_id);
				}

				Columns c = MEMEApp.getViewsDb().getColumns(rec.getViewID());
				if (c != null) {
					outputPars.put(rec, c);
					found.add(rec);
				}
			}
		}

		Columns inputs = new Columns();
		for (Columns c : inputPars.values())
			inputs.merge(c, null);

		Columns outputs = new Columns();
		for (Columns c : outputPars.values())
			outputs.merge(c, null);

		tmpIO = new Columns[] { inputs, outputs };
		tmpFoundTables = found.equals(java.util.Arrays.asList(allTables)) ? null : found;
		
		synchronized (this) {
			// Mindig uj peldanyra csereljuk, igy ha valaki - tetszoleges szalban - hasznalja
			// a regit akkor ez a csere nem zavar be neki
			modelsCache = tmpModelsCache;
		}
	}

	//-------------------------------------------------------------------------
	// [EDT]
	public void finished() throws Exception {
		if (tmpUpdateCb != null) {
			tmpUpdateCb.run(tmpFoundTables);
			tmpFoundTables = null;
			tmpUpdateCb = null;
		}

		updateCategory(Category.INPUT, tmpIO[0]);
		updateCategory(Category.OUTPUT, tmpIO[1]);
	}

	//-------------------------------------------------------------------------
	// [EDT]
	/** This method is used by collectParams() and finished() methods. */
	private void updateCategory(Category c, ArrayList<? extends Parameter> p_newpars)
	{
		assert(EventQueue.isDispatchThread());
		java.util.Collection<? extends Par> newpars = null;
		if (c == Category.THISVIEW) {
			if (!p_newpars.equals(thisview)) {
				newpars = thisview = (ArrayList<ThisViewPar>)p_newpars;
			}
		}
		else {
			TreeMap<Par, Par> candidate = new TreeMap<Par, Par>();
			//
			// Megj: azert nem TreeSet-et hasznalunk TreeMap helyett, mert visszakeresni
			// reszlegesen kitoltott Par szerint is kell tudni (ld. findParam())
			// es Set-bol ez nehezen menne
			//
			for (Parameter par : p_newpars) {
				Par p = new Par(par.getName(), par.getDatatype(), c);
				candidate.put(p, p);	
			}
			
			// Bug fix #1446
			if (c == Category.INPUT) {
				Par p = new Par("Run ",ColumnType.INT,c);
				candidate.put(p,p);
				p = new Par("Tick ",ColumnType.INT,c);
				candidate.put(p,p);
			}
			// End of bug fix #1446
			
			switch (c) {
				case INPUT	:	if (!candidate.equals(input)) { 
									input = candidate;
									newpars = candidate.keySet();
								}
								break;
				case OUTPUT	:	if (!candidate.equals(output)) { 
									output = candidate;
									newpars = candidate.keySet();
								}
								break;
				default	 :		throw new IllegalArgumentException(c.toString());
			}
		}

		if (newpars != null) {
			ParListModel lm;
			lm = listModels.get(c);
			if (lm != null) {
				lm.updateFrom(newpars);
			}
			if (lm_all != null) {
				getListModelForALL();
			}
		}
	}

	//-------------------------------------------------------------------------
	/** Returns a ListModel where every element is a 'Par' object.
	 * Egy olyan ListModel-t ad vissza, amiben minden elem 1-1 'Par' objektum.
	 * @param limit it uses this only when c == THISVIEW. The limit-th parameter
	 *        won't be in the list. Negative limit means no limit. 
	 */
	 /*        Param�ter le�r�s kieg�sz�t�se
	  * 		Nem keszit uj ListModel-t, hanem a korabbi, masik limit-hez
	  *         keszultet modositja (ujratolti). Az 'all' tipusu ListModel-t
	  *         is frissiti automatikusan (ha getListModelForALL()-t hasznaltak mar). 
	  */
	// [EDT]
	public javax.swing.AbstractListModel getListModel(Category c, int limit) {
		if (c == Category.THISVIEW) {
			if (limit < 0) limit = thisview.size();
			else limit = Math.min(limit, thisview.size());
		}
		ParListModel ans = listModels.get(c);
		boolean isLimitChange = (c == Category.THISVIEW && limit != thisviewListModelLimit);
		if (ans == null || isLimitChange) {
			if (ans == null) {
				ans = new ParListModel();
				listModels.put(c, ans);
			}
			java.util.Collection<? extends Par> coll;
			switch (c) {
				case INPUT 		: coll = input.keySet();	break;
				case OUTPUT		: coll = output.keySet();	break;
				case THISVIEW	: coll = thisview.subList(0, limit);
								  thisviewListModelLimit = limit; break;
				default : throw new IllegalArgumentException(c.toString());
			}
			ans.updateFrom(coll);
			if (isLimitChange && lm_all != null)
				getListModelForALL();
		}
		return ans;
	}

	//-------------------------------------------------------------------------
	// [EDT]
	public javax.swing.AbstractListModel getListModelForALL() {
		if (lm_all == null) {
			lm_all = new ParListModel();
		}
		lm_all.updateFrom(getVisiblePars(null));
		return lm_all;
	}

	//-------------------------------------------------------------------------
	/** Returns a list the contains all parameters in undefined order. */ 
	 /* A visszaadott listaban minden parameter szerepel, nem definialt sorrendben,
	 * kiveve a THISVIEW parameterek kozul a bontott oszlopokat.
	 * limit-et figyelmen kivul hagyja.
	 */
	public ArrayList<Par> getAllPars()	{
		// Az alabbi sorrend megletere csak getVisibleParIndices() implementaciojaban 
		// szabad epiteni!
		int n = output.size() + thisview.size() + input.size();
		ArrayList<Par> ans = new ArrayList<Par>(n);
		ans.addAll(output.keySet());
		ans.addAll(thisview);
		ans.addAll(input.keySet());
		
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Returns an index list for the result of the getAllPars().
	 * @param all if null, if calls the getAllPars=(). If not null, then it 
	 *            must be generated by getAllPars().
	 */ 
	 /* Egy index-listat ad vissza, ami a 'getAllPars()' altal generalt sorrendre ertendo.
	 * @param all Ha null, akkor getAllPars()-t hivja a lista eloallitasara.
	 *             Ha nem null, akkor getAllPars() altal visszaadott listanak kell
	 *             lennie. (Ez fontos, mert epit a sorrendjere.)
	 */
	public ArrayList<Integer> getVisibleParIndices(java.util.List<? extends Parameter> all, Integer limit) {
		if (limit == null) limit = thisviewListModelLimit;
		limit = (limit < 0) ? thisview.size() : Math.min(limit, thisview.size());
		if (all == null) all = getAllPars();

		// Az alabbiak feltetelezik a getAllPars() fenti implementacioja szerinti sorrendet
		int firstThisviewIdx = output.size();
		int lastThisviewIdx = firstThisviewIdx + thisview.size() - 1;

		int n = output.size() + limit + input.size();
		HashSet<String> h = new HashSet<String>(n);
		ArrayList<Integer> ans = new ArrayList<Integer>(n);
		n = all.size();
		int nvBegin = firstThisviewIdx + limit, nvEnd = lastThisviewIdx;	// non-visible interval
		for (int i = 0; i < n; ++i) {
			if (nvBegin <= i && i <= nvEnd) continue;	// non-visible THISVIEW parameters
			String name = all.get(i).getName();
			if (!h.contains(name)) {
				h.add(name);
				ans.add(i);
			}
		}
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Returns the list of the visible parameters. */ 
	public ArrayList<Par> getVisiblePars(Integer limit) {
		ArrayList<Par> all = getAllPars();
		ArrayList<Integer> indices = getVisibleParIndices(all, limit);
		if (indices.size() == all.size()) 
			return all;
		ArrayList<Par> ans = new ArrayList<Par>(indices.size());
		for (int i : indices) ans.add(all.get(i));
		return ans;
	}

	//-------------------------------------------------------------------------
	// Erre az osztalyra azert van szukseg mert a platformban levo ListModel  
	// implementaciok csak egyesevel engedik az elemek hozzaadasat. En pedig 
	// szeretnem egyben lecserelni az egeszet ugy hogy minel kevesebb uzenet 
	// menjen rola a listenereknek.
	@SuppressWarnings("serial")
	/** List model for parameter lists. */
	private class ParListModel extends javax.swing.AbstractListModel
	{
		ArrayList<Par>	pars = new ArrayList<Par>();

		public int				getSize() 					{ return pars.size(); }
		public Object			getElementAt(int index)		{ return pars.get(index); }
		
		// [EDT]
		void updateFrom(java.util.Collection<? extends Par> elements) {
			int nbefore = pars.size();
			if (nbefore > 0) {
				pars.clear();
				fireIntervalRemoved(this, 0, nbefore);
			}
			// now 'pars' is empty 
			if (!elements.isEmpty()) {
				pars.addAll(elements);
				fireIntervalAdded(this, 0, pars.size());
			}
		}
	}

}
