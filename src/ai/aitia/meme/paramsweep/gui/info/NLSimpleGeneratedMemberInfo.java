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
package ai.aitia.meme.paramsweep.gui.info;

public class NLSimpleGeneratedMemberInfo extends SimpleGeneratedMemberInfo {

	//====================================================================================================
	// members
	
	private static final long serialVersionUID = 2123938581495821584L;
	
	protected String syntaxBody = null;
	protected String reportBody = null;

	//====================================================================================================
	// methods

	//----------------------------------------------------------------------------------------------------
	public NLSimpleGeneratedMemberInfo(String name, String type, Class<?> javaType) {
		super(name,type,javaType);
	}
	
	//----------------------------------------------------------------------------------------------------
	public String getSyntaxBody() { return syntaxBody; }
	public String getReportBody() { return reportBody; }
	
	//----------------------------------------------------------------------------------------------------
	public void setSyntaxBody(String syntaxBody) { this.syntaxBody = syntaxBody; }
	public void setReportBody(String reportBody) { this.reportBody = reportBody; }
}
