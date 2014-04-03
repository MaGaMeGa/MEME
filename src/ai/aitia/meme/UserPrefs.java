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
package ai.aitia.meme;

/**
 * Collection of user registry locations used by the program
 * (it is good to see them together, it helps to avoid accidental overwriting).
 * 
 * The strings listed here are field names in the same registry node
 * (specified by MEMEApp.userPrefs).
 */
public class UserPrefs
{
	public static final String SESSION		= "";
	public static final String LOGFILELOC		= "LogFile";

	public static final String LF_SKIN		= "Skin";
	public static final String LF_THEME		= "Theme";
	public static final String LF_WATERMARK	= "Watermark";

	public static final String VWCR_DELIMITER	= "ViewCreationSplitterDelimiter";
	
	public static final String CSVE_DELIMITER = "CsvExportDelimiter";
	public static final String CSVE_DECIMALSIGN = "CsvExportDecimalSign";
	public static final String CSVE_INCLUDEHEADER = "CsvIncludeHeader";
	public static final String CSVE_NULLSTRING	  = "CsvNullString";
	
	public static final String USER_TOOL_GROUP_INDEX = "UserToolGroupIndex";
	public static final String USER_TOOL_VERBOSE_MODE = "UserToolVerboseMode";
	
	public static final String SINGLE_INSTANCE_PORT = "singleInstancePort";
}
