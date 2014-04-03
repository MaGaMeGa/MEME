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
package ai.aitia.meme.paramsweep.platform.netlogo.info;

import ai.aitia.meme.paramsweep.platform.repast.info.StatisticGeneratedRecordableInfo;

public class NLStatisticGeneratedRecordableInfo extends StatisticGeneratedRecordableInfo {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 7988140716665425454L;
	
	protected String syntaxBody = null;
	protected String reportBody = null;

	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public NLStatisticGeneratedRecordableInfo(String name, Class<?> type, String accessibleName, String source, String syntaxBody, String reportBody) {
		super(name,type,accessibleName,source);
		this.syntaxBody = syntaxBody;
		this.reportBody = reportBody;
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getSyntaxBody() { return syntaxBody; }
	public String getReportBody() { return reportBody; }
}
