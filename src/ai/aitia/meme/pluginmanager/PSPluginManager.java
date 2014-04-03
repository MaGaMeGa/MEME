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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;

import javassist.ClassPool;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.plugin.IIntelliDynamicMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IIntelliStaticMethodPlugin;
import ai.aitia.meme.paramsweep.plugin.IOperatorPlugin;
import ai.aitia.meme.paramsweep.plugin.IStatisticsPlugin;
import ai.aitia.meme.paramsweep.utils.Utilities;

import static ai.aitia.meme.utils.Utils.getLocalizedMessage;

/**
 * This class is responsible for scanning the Plugins directory, 
 * which must be set with the setPluginsDir() method before.
 * 
 */
public class PSPluginManager 
{
	static final String PLUGIN_CLASS_LOAD_METHOD		= "onPluginLoad";
	static final String PLUGIN_CLASS_UNLOAD_METHOD		= "onPluginUnload";

	//=========================================================================
	// Member variables

	/** The location of the directory where the plugins are. */
	private String pluginsDir; 
	/** List of statistic plugins. */
	private PluginList<IStatisticsPlugin> statisticsPlugins = new PluginList<IStatisticsPlugin>();
	private PluginList<IOperatorPlugin> operatorPlugins = new PluginList<IOperatorPlugin>();
	/** List of IntelliSweep static method plugins. */
	private PluginList<IIntelliStaticMethodPlugin> intelliStaticPlugins = new PluginList<IIntelliStaticMethodPlugin>();
	/** List of IntelliSweep dynamic method plugins. */
	private PluginList<IIntelliDynamicMethodPlugin> intelliDynamicPlugins = new PluginList<IIntelliDynamicMethodPlugin>();

	private List<String> pluginClassPath = new ArrayList<String>();
	
	//=========================================================================
	// Getter/setter methods

	public void						setPluginsDir(String pluginsDir)	{ this.pluginsDir = pluginsDir; }
	public String					getPluginsDir()						{ return pluginsDir; }
	public List<String>				getPluginClassPath()				{ return pluginClassPath; }

	public PluginList<IStatisticsPlugin> getStatisticsPluginInfos() { return statisticsPlugins; }
	public PluginList<IIntelliStaticMethodPlugin> getIntelliStaticPluginInfos() { return intelliStaticPlugins; }
	public PluginList<IIntelliDynamicMethodPlugin> getIntelliDynamicPluginInfos() { return intelliDynamicPlugins; }
	public PluginList<IOperatorPlugin> getOperatorPluginInfos() { return operatorPlugins; }
	
	//=========================================================================
	// Public methods

	//-------------------------------------------------------------------------
	/** Storage class to store plugin info objects. */
	@SuppressWarnings("serial")
	public static class PluginList<P extends IPlugin> extends ArrayList<PluginInfo<P>> {
		/** Returns an iterator. */
		public Iterable<P> getPlugins() {
			return new PluginIterator<P>(this);
		}
		/** Returns the plugin identified by 'name'.
		 * @param name the localized name of the plugin
		 */  
		public P findByLocalizedName(Object name) {
			PluginInfo<P> context = findPInfoContextByLocalizedName(name);
			return (context == null) ? null : context.getInstance();
		}
		/** Returns the plugin identified by 'name'.
		 * @param name the internal name of the plugin
		 */  
		public P findByName(Object name) {
			PluginInfo<P> context = findPInfoByName(name);
			return (context == null) ? null : context.getInstance();
		}
		/** Returns the plugin identified by 'name'.
		 * @param name the localized name of the plugin
		 */  
		public PluginInfo<P> findPInfoContextByLocalizedName(Object name) {
			if (name != null) { 
				for (PluginInfo<P> info : this)
					if (name.equals(info.getInstance().getLocalizedName())) return info;
			}
			return null;
		}
		/** Returns the plugin identified by 'name'.
		 * @param name the internal name of the plugin
		 */  
		public PluginInfo<P> findPInfoByName(Object name) {
			if (name != null) { 
				for (PluginInfo<P> info : this)
					if (name.equals(info.getInternalName())) return info;
			}
			return null;
		}
		/** Returns the information object belongs to 'plugin'. */
		public PluginInfo<P> getPInfo(P plugin) {
			if (plugin != null) { 
				for (PluginInfo<P> info : this)
					if (plugin == info.getInstance()) return info;
			}
			return null;
		}
		/** Returns the internal name of 'plugin'. */
		public String getInternalName(P plugin) {
			PluginInfo<P> context = getPInfo(plugin);
			return (context == null) ? null : context.getInternalName();
		}
	}

	//-------------------------------------------------------------------------
	/**
	 * Scans the plugins directory, instantiates the plugin classes and registers
	 * the plugin objects in the appropriate vectors.
	 */
	@SuppressWarnings("deprecation")
	public void scanPlugins(IPlugin.IOnPluginLoad loadCtx) throws Exception {
		File dir = new File(getPluginsDir());
		String files[], clsname;
		ZipFile zf = null;
		InputStream is;

		if (!dir.exists()) {
			ParameterSweepWizard.logError("Cannot locate the Plugins directory '%s'", dir.toString());
			throw new Exception("Cannot locate the Plugins directory");
		}
		files = dir.list();
		for (int i = 0; i < files.length; ++i) {
			String err = null;

			// Open the zip file.

			File f = new File(dir, files[i]);
			try {
				if (!ParameterSweepWizard.isDebugVersion()) {
					zf = new ZipFile(f);	// the release version allows zipfile plugins only
					ClassPool.getDefault().insertClassPath(f.getAbsolutePath());
					pluginClassPath.add(f.getAbsolutePath());
				} else if (f.isDirectory() && f.exists()) {
					zf = null;				// during development, allow subdirs only (skip .jar files)
					ClassPool.getDefault().insertClassPath(f.getAbsolutePath());
					pluginClassPath.add(f.getAbsolutePath());
				} else
					continue;
			}
			catch (IOException ex) {
				continue;  // Not a zip file.
			}
			try {
				URLClassLoader pluginLoader = new URLClassLoader(new URL [] {f.toURL()}); 

				// Find the plugin directory file, and record any plugin classes.

				is = getistream(zf, f, "plugins");
				if (is != null) {

					BufferedReader in = new BufferedReader(new InputStreamReader(is));
					while (true) {
						clsname = in.readLine();
						if (clsname == null)
							break;
						try {
							Class cls = pluginLoader.loadClass(clsname);
							PluginInfo<?> info = null;

							if (IStatisticsPlugin.class.isAssignableFrom(cls)) 
								info = loadPlugin(cls, loadCtx, statisticsPlugins);
							else if (IOperatorPlugin.class.isAssignableFrom(cls))
								info = loadPlugin(cls, loadCtx, operatorPlugins);								
							else if (IIntelliStaticMethodPlugin.class.isAssignableFrom(cls)) 
								info = loadPlugin(cls, loadCtx, intelliStaticPlugins);
							else if (IIntelliDynamicMethodPlugin.class.isAssignableFrom(cls)) 
								info = loadPlugin(cls, loadCtx, intelliDynamicPlugins);
							if (info != null) {
								info.pluginLoader = pluginLoader;
								info.internalName = clsname;
							}
						}
						catch (NoClassDefFoundError ex) { err = String.format("%s: is not found", ex.getLocalizedMessage()); }
						catch (ClassNotFoundException ex) { err = String.format("class %s is not found", clsname);  }
						catch (IllegalAccessException ex) { err = String.format("cannot access class %s", clsname);  }
						catch (InstantiationException ex) { err = String.format("cannot instantiate class %s", clsname); }
						catch (Throwable t) { err = Utilities.getStackTraceLines(t); }
					} // while
					in.close();
					is.close();
				} // if (is != null)  -- the "plugins" file was found
				else {
					err = "missing 'plugins' file";
				}
			}
			catch (IOException ex) {
				err = getLocalizedMessage(ex);
			}
			if (err != null) {
				ParameterSweepWizard.logError("Error reading plugin file %s: %s", files[i], err);
			}
		} // for i in files[]
	}

	//-------------------------------------------------------------------------
	/** Unloads the plugins.
	 * Examples:<pre>
	 *    unloadPlugins(arg)    // unload all plugins, passing 'arg' to onPluginUnload()
	 *    unloadPlugins(arg, getImportPluginInfos())	// unload only specified set of plugins
	 * </pre>
	 */
	public void unloadPlugins(Object arg, Object...whichPlugins) {
		if (whichPlugins == null || whichPlugins.length == 0) {
			unloadPlugins(arg, statisticsPlugins);
		}
		else if (whichPlugins[0] instanceof IPlugin) {
			// Unload 1 plugin
			// Caller is responsible for removing the corresponding
			// plugininfo object from the corresponding array
			unloadPlugin(whichPlugins[0].getClass(), arg);
		}
		else if (whichPlugins[0] instanceof PluginList) {
			PluginList<?> array = (PluginList<?>)whichPlugins[0];
			for (PluginInfo<?> info : array) {
				unloadPlugins(arg, info.getInstance());
			}
			array.clear();
		}
	}

	//=========================================================================
	// Internals

	//-------------------------------------------------------------------------
	/** Iterator class to iterate plugin information lists. */
	static class PluginIterator<P extends IPlugin> implements Iterable<P>, java.util.Iterator<P> {
		/** List of plugin informations object. */
		List<? extends PluginInfo<P>>					list= null;
		java.util.Iterator<? extends PluginInfo<P>>		it	= null;

		PluginIterator(List<? extends PluginInfo<P>> list)	{ this.list = list; }
		public java.util.Iterator<P> iterator()				{ it = list.iterator(); return this; }
		public boolean hasNext()							{ return it.hasNext(); }
		public P next()										{ return it.next().getInstance(); }
		public void remove()								{ it.remove(); }
	}

	//------------------------------------------------------------------------
	/** Loads plugin class and returns a plugin information object. 
	 * Precondition: cls is descendant of P
	 * @throws Exception if error occurs during the method call / newInstance(),
	 *          or the method call returned an invalid object
	 */ 
	@SuppressWarnings("unchecked")
	private static <P extends IPlugin> PluginInfo<P> loadPlugin(Class								cls,
																IPlugin.IOnPluginLoad				loadCtx,
																ArrayList<? super PluginInfo<P>>	array)
														throws	Exception
	{
		Object instance[] = callClassMethod(cls, PLUGIN_CLASS_LOAD_METHOD, loadCtx);
		if (instance == null) {
			instance = new Object[] { cls.newInstance() };
		}
		else if (instance[0] == null) {
			// The plugin refused loading
			return null;
		}
		else if (!cls.isInstance(instance[0])) {
			throw new InstantiationException(String.format(
					"%s.%s() returned an invalid object (of type %s)",cls.getName(), 
					PLUGIN_CLASS_LOAD_METHOD, instance[0].getClass()));
		}
		// Now 'instance[0]' is instance of P
		PluginInfo<P> ans = new PluginInfo<P>();
		array.add(ans);
		ans.instance = (P)instance[0];
		return ans;
	}

	//------------------------------------------------------------------------
	/** Unloads the plugin identified by 'cls'. */
	private static void unloadPlugin(Class cls, Object arg) {
		try {
			callClassMethod(cls, PLUGIN_CLASS_UNLOAD_METHOD, arg);
		} catch (Throwable t) {
			ParameterSweepWizard.logError("Method  %s invocation error at class %s:",PLUGIN_CLASS_UNLOAD_METHOD,cls.getName());
		}
	}

	//------------------------------------------------------------------------
	/** 
	 * Returns null if there's no such method (with either 0 or 1 arguments),
	 * otherwise a single-item array containing the result of the method call.
	 */
	private static Object[] callClassMethod(Class cls, String methodName, Object arg) throws Exception {
		for (java.lang.reflect.Method m : cls.getMethods()) {
			if (!m.getName().equals(methodName)) continue;
			Class<?>[] types = m.getParameterTypes();
			if (types.length == 0 || types.length == 1 && (arg == null || types[0].isInstance(arg))) {
				Object result[] = { (types.length == 0) ? m.invoke(cls) : m.invoke(cls, arg) };
				return result;
			}
		}
		return null;
	}

	//------------------------------------------------------------------------
	static InputStream getistream(ZipFile zf, File f, String name) throws IOException, FileNotFoundException {
		if (zf != null) {
			java.util.zip.ZipEntry ze = zf.getEntry(name);
			return (ze != null) ? zf.getInputStream(ze) : null;
		}
		File g = new File(f, name);
		return (g.exists() && g.isFile()) ? new java.io.FileInputStream(g) : null;
	}
}
