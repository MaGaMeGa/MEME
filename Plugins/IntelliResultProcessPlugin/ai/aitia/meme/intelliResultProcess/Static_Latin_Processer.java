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
package ai.aitia.meme.intelliResultProcess;

import java.util.List;

import javax.swing.JPanel;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ai.aitia.meme.database.IResultsDbMinimal;
import ai.aitia.meme.database.ResultInMem;
import ai.aitia.meme.pluginmanager.IIntelliResultProcesserPlugin;

/**
 * @author Ferschl
 *
 */
public class Static_Latin_Processer implements IIntelliResultProcesserPlugin {

	public Document createCharts(String viewName, String model, String version)
	        throws ParserConfigurationException {
		return null;
	}

	public List<ResultInMem> processResultFiles(IResultsDbMinimal db,
	        List<ResultInMem> runs, Element pluginElement) {
		return null;
	}

	public String getLocalizedName() {
		return "Latin Square_Processer";
	}

	public void doGenericProcessing() {
	}

	public JPanel getGenericProcessPanel(Element pluginElement) {
		return null;
	}

	public boolean isGenericProcessingSupported() {
		return false;
	}


}
