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
package ai.aitia.meme.gui;

import java.util.HashMap;
import java.util.HashSet;

import javax.swing.UIManager;

import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.SubstanceWidgetSupport;
import org.jvnet.substance.skin.SkinInfo;
import org.jvnet.substance.theme.ThemeInfo;
import org.jvnet.substance.utils.SubstanceConstants;
import org.jvnet.substance.utils.SubstanceTitlePane;
import org.jvnet.substance.watermark.SubstanceImageWatermark;
import org.jvnet.substance.watermark.WatermarkInfo;

import ai.aitia.meme.MEMEApp;
import ai.aitia.meme.UserPrefs;

//-----------------------------------------------------------------------------
/**
 * This class allows MEME to work with and without the custom Look & Feel (Substance).
 * 
 * Portability note: this class uses the global <code>MEMEApp.userPrefs</code> object,
 * which is responsible for storing the various Look&Feel settings (and other settings
 * as well). If you wish to use this source code in a different application, you can 
 * replace all occurences of <code>MEMEApp.userPrefs</code> with the corresponding 
 * service in your application.     
 */
public class LookAndFeel
{
	/** The name of the current skin. */
	private String currentSkinName = null;
	/** Flag that determines whether the current L&F is the Substance or not. */
	private boolean isSubstance = false;
	/** Flag that determines whether the heap status display is available or not. */
	private boolean isHeapStatusAvailable = true;		//!< for Substance's heap status display

	//=========================================================================
	//	Public interface

	public static enum ListType {
		SKIN, THEME, WATERMARK
	};
	public static final String SKIN_JAVA				= "Standard Java";
	public static final String SKIN_JAVA_NATIVE		= "Standard Java (system native)";
	public static final String DEFAULT_DISPLAYNAME	= "Default";


	//-------------------------------------------------------------------------
	/** Initializes the Look&Feel.
 	 *  This method is intended to be called only once, at program startup.
	 *  Note: this method should work for test applications, too, i.e. without
	 *  initializing the whole application completely. Please keep dependencies at the minimum!
	 */
	public void init() throws Exception
	{
		// A registry-ben levo adatok alapjan allitunk be egy L&F osztalyt,
		// ill. Substance eseteben meg theme-t es watermark-ot is.
		// Egyszeru lenne a registry-bol kiolvasott osztalynevet egyszeruen peldanyositani,
		// ez azonban nyilvanvalo biztonsagi rest jelentene. Ezert a registry-bol kiolvasott
		// nevet ellenorizzuk, es csak az ismert neveket hasznaljuk.
		// Ehhez van az alabbi lista az ismert nevekrol:

		HashMap<String, String> skin2LF = new HashMap<String, String>();
		String cls = "org.jvnet.substance.skin.";
		String tmp[] = {		// Ezek azok a skin-ek amiknek LookAndFeel megfelelojuk is van
				"Autumn",				cls+"SubstanceAutumnLookAndFeel", 
				"Business",				cls+"SubstanceBusinessLookAndFeel", 
				"Challenger Deep",		cls+"SubstanceChallengerDeepLookAndFeel", 
				"Creme",				cls+"SubstanceCremeLookAndFeel", 
				"Emerald Dusk",			cls+"SubstanceEmeraldDuskLookAndFeel", 
				"Field of Wheat",		cls+"SubstanceFieldOfWheatLookAndFeel", 
				"Green Magic",			cls+"SubstanceGreenMagicLookAndFeel", 
				"Magma",				cls+"SubstanceMagmaLookAndFeel", 
				"Mango",				cls+"SubstanceMangoLookAndFeel", 
				"Moderate",				cls+"SubstanceModerateLookAndFeel", 
				"Office Blue 2007",		cls+"SubstanceOfficeBlue2007LookAndFeel", 
				"Office Silver 2007",	cls+"SubstanceOfficeSilver2007LookAndFeel", 
				"Raven",				cls+"SubstanceRavenLookAndFeel", 
				"Sahara",				cls+"SubstanceSaharaLookAndFeel",

				SKIN_JAVA,				UIManager.getCrossPlatformLookAndFeelClassName(),
				SKIN_JAVA_NATIVE,		UIManager.getSystemLookAndFeelClassName()
		};
		for (int i = 1; i < tmp.length; i += 2) {
			skin2LF.put(tmp[i-1], tmp[i]);
		}
		HashSet<String> knownLFcls = new HashSet<String>(skin2LF.values());
		for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
			knownLFcls.add(info.getClassName());

		String def = "Moderate";
		String skin = MEMEApp.userPrefs.get(UserPrefs.LF_SKIN, def);
		// LF_SKIN may be
		// - one of the above brief skin names
		// - a fully qualified name of a (known) Swing LookAndFeel class
		// - one of Substance's skin names
		//
		if (skin2LF.containsKey(skin)) {
			cls = skin2LF.get(skin);
			currentSkinName = skin;
			skin = null;
		} else if (knownLFcls.contains(skin)) {
			cls = skin;
			skin = null;
		} else {
			cls = skin2LF.get(def);
		}
		isSubstance = cls.matches(".*\\.Substance[^.]*");
		if (isSubstance) {
			System.setProperty("substancelaf.watermark.tobleed", "");

			//javax.swing.JDialog.setDefaultLookAndFeelDecorated(true);

			if (isHeapStatusAvailable) {
				javax.swing.JFrame.setDefaultLookAndFeelDecorated(true);
				System.setProperty("substancelaf.heapStatusPanel", "");

				// This disables the menu search, lock-icon in uneditable text fields, extra system menu items etc.
				System.setProperty("substancelaf.noExtraElements", "");
			}

			UIManager.put(SubstanceLookAndFeel.MENU_GUTTER_FILL_KIND,
					org.jvnet.substance.utils.SubstanceConstants.MenuGutterFillKind.NONE);
		}

		//Object o = this.getClass().getClassLoader().loadClass(cls).newInstance();
		//javax.swing.UIManager.setLookAndFeel((javax.swing.LookAndFeel)o);
		javax.swing.UIManager.setLookAndFeel(cls);

		// Is it one of Substance's Look&Feel classes?  
		//isSubstance = (o instanceof SubstanceLookAndFeel);
		Object o;
		if (isSubstance) {
			// Set the skin, theme & watermark, too
			if (skin != null) {
				o = getMap(ListType.SKIN).get(skin);
				if (o instanceof SkinInfo) {
					currentSkinName = skin;
					SubstanceLookAndFeel.setSkin(((SkinInfo)o).getClassName());
				}
			}

			String name = MEMEApp.userPrefs.get(UserPrefs.LF_THEME, null);
			if (name != null) {
				o = getMap(ListType.THEME).get(name);
				if (o != null && (o instanceof ThemeInfo))
					SubstanceLookAndFeel.setCurrentTheme((ThemeInfo)o);
			}

			name = MEMEApp.userPrefs.get(UserPrefs.LF_WATERMARK, null);
			if (name != null) {
				o = getMap(ListType.WATERMARK).get(name);
				if (o != null && (o instanceof WatermarkInfo))
					SubstanceLookAndFeel.setCurrentWatermark(((WatermarkInfo)o).getClassName());
				// TODO: probald meg fajlnevkent is ertelmezni a nevet es ha megtalalhato,
				// akkor SubstanceImageWatermark-kent hasznalni. A tiling/opacity ertekeknek
				// lehetnenek kulon mezok a registry-ben.
			} else {
				java.io.InputStream is = MainWindow.class.getResourceAsStream("icons/MEME_logo_big.png");
				if (is != null) {
					SubstanceLookAndFeel.setImageWatermarkKind(SubstanceConstants.ImageWatermarkKind.APP_CENTER);
					SubstanceLookAndFeel.setCurrentWatermark(new SubstanceImageWatermark(is));
					SubstanceLookAndFeel.setImageWatermarkOpacity(0.05f);
				}
			}

			// HACK: Disable the lock-icon on noneditable text components
			org.jvnet.lafwidget.LafWidgetRepository.getRepository().setLafSupport(
					new SubstanceWidgetSupport() {
						@Override public boolean hasLockIcon(java.awt.Component comp) { return false; }
					}
			);

		} else {
			isHeapStatusAvailable = false;

			// Ez ahhoz kell, hogy a Window menuben olyankor is latszodjanak a lehetseges 
			// Substance-es skin-ek, amikor nem a Substance-et hasznaljuk.
			new SubstanceLookAndFeel();
		}
	}

	//-------------------------------------------------------------------------
	public boolean isSubstance() {
		return this.isSubstance;
	}

	//-------------------------------------------------------------------------
	/** Returns all available skin/theme/watermark according to the argument. */
	public java.util.Collection<String> getAll(ListType t) {
		java.util.Collection<String> ans = getMap(t).keySet();
		if (t == ListType.SKIN) {
			ans = new java.util.ArrayList<String>(ans);
			ans.add(SKIN_JAVA);
			ans.add(SKIN_JAVA_NATIVE);
		}
		return ans;
	}

	//-------------------------------------------------------------------------
	/** Sets the current skin/theme/watermark. 'name' is the name of the new
	 *  skin/theme/watermark.
	 */
	public void set(ListType t, String name) {
		String key = null;
		switch (t) {
			case SKIN :		key = UserPrefs.LF_SKIN;		break;
			case THEME :	key = UserPrefs.LF_THEME;		break;
			case WATERMARK:	key = UserPrefs.LF_WATERMARK;	break;
		}
		if (DEFAULT_DISPLAYNAME.equals(name)) {
			MEMEApp.userPrefs.remove(key);
		} else {
			MEMEApp.userPrefs.put(key, name);
		}
		MEMEApp.userAlert("Please restart the program for the changes to take effect.");
	}

	//-------------------------------------------------------------------------
	/** Returns the name of the current skin/theme/watermark according to the argument. */
	public String getCurrent(ListType t) {
		if (isSubstance()) {
			switch (t) {
				case SKIN		: return currentSkinName; 
				case THEME		: return SubstanceLookAndFeel.getCurrentThemeName();
				case WATERMARK	: return SubstanceLookAndFeel.getCurrentWatermarkName(); 
			}
		}
		return null;
	}

	//-------------------------------------------------------------------------
	/** Makes component 'comp' to transparent. 
	 * @return comp
	 */
	public javax.swing.JComponent makeTransparent(javax.swing.JComponent comp) {
		return makeTransparent(comp, 0.4f, 0.75f);
	}

	//-------------------------------------------------------------------------
	/** Makes component 'comp' to transparent. 'active' and 'nonActive' are the
	 *  measures of the transaprency when the component is active or not. 
	 * @return comp
	 */
	public javax.swing.JComponent makeTransparent(javax.swing.JComponent comp, Float active, Float nonActive) {
		if (isSubstance()) {
			Object composite = null;
			if (active != null && nonActive != null) {
				composite = new org.jvnet.substance.painter.AlphaControlBackgroundComposite(active, nonActive);
				comp.putClientProperty(SubstanceLookAndFeel.OVERLAY_PROPERTY, Boolean.TRUE);
			} else {
				comp.putClientProperty(SubstanceLookAndFeel.OVERLAY_PROPERTY, null);
			}
			comp.putClientProperty(SubstanceLookAndFeel.BACKGROUND_COMPOSITE, composite); 
		}
		return comp;
	}
	
	//-------------------------------------------------------------------------
	/** b=true: treat 'table' as big table; b=false: do not treat it as big table any more.
	 * We need this method because in Substance L&F the scrolling of big tables causes crash
	 * without it.  
	 */
	public void treatAsBigTable(javax.swing.JTable table, boolean b) {
		if (isSubstance()) {
			table.putClientProperty(org.jvnet.lafwidget.LafWidget.ANIMATION_KIND,
					b ? org.jvnet.lafwidget.utils.LafConstants.AnimationKind.NONE : null);
		}
	}

	//-------------------------------------------------------------------------
	/** Shows the heap status display. */
	public void showHeapStatusMonitorIfPossible(javax.swing.JRootPane pane) {
		if (isHeapStatusAvailable) {
			// Sajnos permanentlyShowHeapStatusPanel() feltetelezi hogy az ablak
			// rendszer-menujeben az extra menuelemeket is hasznaljuk. Mivel mi
			// azt fentebb 'substancelaf.noExtraElements' altal kitiltottuk,
			// ezert permanentlyShowHeapStatusPanel() onmagaban elszall.
			// Egyelore az alabbi putClientProperty megoldotta a gondot, bar
			// nemdokumentalt, "internal" feature-t hasznal:
			pane.putClientProperty(SubstanceTitlePane.HEAP_STATUS_PANEL_PERMANENT, true);
			SubstanceLookAndFeel.permanentlyShowHeapStatusPanel(pane);
		}
	}

	//=========================================================================
	//	Internals


	//-------------------------------------------------------------------------
	private java.util.Map<String,?> getMap(ListType t) {
		switch (t) {
			case SKIN		: return SubstanceLookAndFeel.getAllSkins(); 
			case THEME		: return SubstanceLookAndFeel.getAllThemes(); 
			case WATERMARK	: return SubstanceLookAndFeel.getAllWatermarks(); 
		}
		return java.util.Collections.emptyMap();
	}

}
