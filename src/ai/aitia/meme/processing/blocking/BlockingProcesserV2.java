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
package ai.aitia.meme.processing.blocking;

import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import ai.aitia.meme.database.ColumnType;
import ai.aitia.meme.database.Columns;
import ai.aitia.meme.database.GeneralRow;
import ai.aitia.meme.database.IResultsDbMinimal;
import ai.aitia.meme.database.Model;
import ai.aitia.meme.database.ParameterComb;
import ai.aitia.meme.database.Result;
import ai.aitia.meme.database.ResultInMem;
import ai.aitia.meme.paramsweep.generator.RngSeedManipulatorModel;
import ai.aitia.meme.paramsweep.gui.info.ParameterRandomInfo;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.processing.variation.NaturalVariationV2;

public class BlockingProcesserV2 {
	
	//====================================================================================================
	// members
		
	protected Vector<String> blockingVariables = null;
	protected Vector<Vector<Double[]>> blockingVariances = null;
	protected Vector<Vector<Object[]>> blockingUniqueSamples = null;
	protected Result first = null;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public void processBlockingData(final List<Result> runs, final IResultsDbMinimal db, final Element pluginElement, 
									final String isPluginXMLFileName) throws Exception {
		blockingVariables = new Vector<String>();
		blockingVariances = new Vector<Vector<Double[]>>();
		blockingUniqueSamples = new Vector<Vector<Object[]>>();
		first = runs.get(0);
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		final DocumentBuilder docBuilder = factory.newDocumentBuilder();
		NodeList nl = null;
		Element rsmElem = null;
		nl = pluginElement.getElementsByTagName(RngSeedManipulatorModel.RSM_ELEMENT_NAME);
		if (nl != null && nl.getLength() > 0)
			rsmElem = (Element) nl.item(0);
		
		if (rsmElem != null) {
			nl = null;
			nl = rsmElem.getElementsByTagName(RngSeedManipulatorModel.BLOCKING_ELEMENT);
			if (nl != null && nl.getLength() > 0) {
				for (int i = 0;i < nl.getLength();i++) {
					final Element blItem = (Element) nl.item(i);
					blockingVariables.add(blItem.getAttribute(RngSeedManipulatorModel.BLOCKING_NAME));
					final Document blDoc = docBuilder.newDocument();
				
					final Element fakePluginElement = blDoc.createElement("fake_plugin");
					blDoc.appendChild(fakePluginElement);
				
					final Element fakeRsmElement = blDoc.createElement(RngSeedManipulatorModel.RSM_ELEMENT_NAME);
					fakePluginElement.appendChild(fakeRsmElement);
				
					final NodeList nlRandomInfos = rsmElem.getElementsByTagName(ParameterRandomInfo.PRI_ELEM_NAME);
					if (nlRandomInfos != null && nlRandomInfos.getLength() > 0) {
						for (int j = 0;j < nlRandomInfos.getLength();j++) {
							final Element randomInfoElement = (Element) nlRandomInfos.item(j);
							if (randomInfoElement.getAttribute(ParameterRandomInfo.RND_TYPE_ATTR).equals(String.valueOf(ParameterRandomInfo.SWEEP_RND))) {
								final ParameterRandomInfo fakeRandomInfo = new ParameterRandomInfo("","",null);
								fakeRandomInfo.load(randomInfoElement);
								final Element fakeRandomInfoElement = blDoc.createElement(ParameterRandomInfo.PRI_ELEM_NAME);
								fakeRandomInfo.save(fakeRandomInfoElement);
								fakeRsmElement.appendChild(fakeRandomInfoElement);
							}
						}
						final Element fakeNaturalVariationElement = blDoc.createElement(RngSeedManipulatorModel.NATURAL_VARIATION_ELEMENT);
						fakeNaturalVariationElement.setAttribute("name", blItem.getAttribute(RngSeedManipulatorModel.BLOCKING_NAME));
						fakeNaturalVariationElement.setAttribute("selected", "true");
						fakeRsmElement.appendChild(fakeNaturalVariationElement);
					
						final NaturalVariationV2 natVar = new NaturalVariationV2();
						natVar.loadAndProcessData(runs,fakePluginElement);
						blockingUniqueSamples.add(natVar.getUniqueSamples());
						blockingVariances.add(natVar.getUniqueSampleVariances());
					}
				}
				writeResultsToResultInMems(db,isPluginXMLFileName);
			}
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	private void writeResultsToResultInMems(final IResultsDbMinimal db, final String isPluginXMLFileName) throws Exception {
		//generating columns:
		final Columns blockingColumns = new Columns();
		blockingColumns.append("Blocking Variable",ColumnType.STRING);
		blockingColumns.append("Value",ColumnType.STRING);
		for (int i = 0;i < blockingVariances.get(0).get(0).length;i++) 
			blockingColumns.append("Standard deviation of " + first.getOutputColumns().get(i).getName(),ColumnType.DOUBLE);
		
		//generating new model:
		final String newModelName = first.getModel().getName();
		final String version = first.getModel().getVersion() + "_Blocking_" + Util.getTimeStampFromXMLFileName(isPluginXMLFileName);
		Model model = db.findModel(newModelName, version);
		if (model == null)
			model = new Model(Model.NONEXISTENT_MODELID,newModelName,version);
		
		// Generate batch number
		final int b = db.getNewBatch(model);
		
		final Columns cols = new Columns();
		int runcount = 0;
		for (int h = 0;h < blockingUniqueSamples.size();h++) {
			for (int i = 0;i < blockingUniqueSamples.get(h).size();i++) {
				ResultInMem toAdd = new ResultInMem();
				toAdd.setModel(model);
				toAdd.setBatch(b);
				toAdd.setStartTime(first.getStartTime());
				toAdd.setEndTime(first.getEndTime());
				toAdd.setRun(runcount++);

				ParameterComb pc = new ParameterComb(new GeneralRow(cols));
				Result.Row row = new Result.Row(blockingColumns,0);
				row.set(0,blockingVariables.get(h));
				row.set(1,blockingUniqueSamples.get(h).get(i)[0].toString());
				for (int j = 0;j < blockingVariances.get(h).get(i).length;j++) 
					row.set(2 + j,blockingVariances.get(h).get(i)[j]);
				toAdd.setParameterComb(pc);
				toAdd.add(row);
				db.addResult(toAdd);
			}
		}
	}
}
