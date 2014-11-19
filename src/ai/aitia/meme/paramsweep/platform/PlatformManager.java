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
package ai.aitia.meme.paramsweep.platform;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javassist.ClassPool;
import ai.aitia.meme.Logger;
import ai.aitia.meme.paramsweep.platform.repast.RepastPlatform;
import ai.aitia.meme.paramsweep.utils.Util;
import ai.aitia.meme.pluginmanager.IPlugin;
import ai.aitia.meme.pluginmanager.PluginInfo;

/**
 * The entry point for platform handling.
 */
public class PlatformManager {
	
	//====================================================================================================
	// nested classes
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * The types of platforms supported by this system. They provide the
	 * appropriate controller implementation.
	 */
	public enum PlatformType implements Serializable {
		REPAST,
		SIMPHONY, 
		SIMPHONY2,
		EMIL, // EMIL-S REPAST platform
		TRASS, // EMIL-S TRASS platform
		NETLOGO,
		NETLOGO5,
		MASON,
		CUSTOM;
		
		@Override public String toString() { return getPlatform(this) == null ? super.toString() :  getPlatform(this).getDisplayableName(); }
	}
	
	//-------------------------------------------------------------------------
	/** Storage class to store plugin info objects. */
	@SuppressWarnings("serial")
	public static class PlatformPluginList<P extends IPluginPlatform> extends ArrayList<PluginInfo<P>> {
		
		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		public Iterable<P> getPlugins() { return new PluginIterator<P>(this); }
		
		//----------------------------------------------------------------------------------------------------
		public P findByLocalizedName(Object name) {
			PluginInfo<P> context = findPInfoContextByLocalizedName(name);
			return (context == null) ? null : context.getInstance();
		}
		
		//----------------------------------------------------------------------------------------------------
		public P findByName(Object name) {
			PluginInfo<P> context = findPInfoByName(name);
			return (context == null) ? null : context.getInstance();
		}
		
		//----------------------------------------------------------------------------------------------------
		public P findByType(PlatformType type) {
			PluginInfo<P> context = findPInfoByType(type);
			return (context == null) ? null : context.getInstance();
		}
		
		//----------------------------------------------------------------------------------------------------
		public PluginInfo<P> findPInfoContextByLocalizedName(Object name) {
			if (name != null) { 
				for (PluginInfo<P> info : this) {
					if (name.equals(info.getInstance().getLocalizedName())) 
						return info;
				}
			}
			return null;
		}

		//----------------------------------------------------------------------------------------------------
		public PluginInfo<P> findPInfoByName(Object name) {
			if (name != null) { 
				for (PluginInfo<P> info : this) {
					if (name.equals(info.getInternalName()))
						return info;
				}
			}
			return null;
		}
		
		//----------------------------------------------------------------------------------------------------
		public PluginInfo<P> getPInfo(P plugin) {
			if (plugin != null) { 
				for (PluginInfo<P> info : this) {
					if (plugin == info.getInstance())
						return info;
				}
			}
			return null;
		}
		
		//----------------------------------------------------------------------------------------------------
		public String getInternalName(P plugin) {
			PluginInfo<P> context = getPInfo(plugin);
			return (context == null) ? null : context.getInternalName();
		}
		
		//----------------------------------------------------------------------------------------------------
		public PluginInfo<P> findPInfoByType(PlatformType type) {
			if (type != null) {
				for (PluginInfo<P> info : this) {
					if (type == info.getInstance().getPlatfomType())
						return info;
				}
			}
			return null;
		}
	}
	
	//-------------------------------------------------------------------------
	static class PluginIterator<P extends IPluginPlatform> implements Iterable<P>, Iterator<P> {
		
		//====================================================================================================
		// members
		
		List<? extends PluginInfo<P>> list = null;
		java.util.Iterator<? extends PluginInfo<P>> it = null;

		//====================================================================================================
		// methods
		
		//----------------------------------------------------------------------------------------------------
		PluginIterator(List<? extends PluginInfo<P>> list) { this.list = list; }
		public Iterator<P> iterator() { it = list.iterator(); return this; }
		public boolean hasNext() { return it.hasNext(); }
		public P next()	{ return it.next().getInstance(); }
		public void remove() { it.remove(); }
	}
	
	//====================================================================================================
	// members

	public static final Map<PlatformType,String> registeredPlatforms = new HashMap<PlatformType,String>();
	static {
		registeredPlatforms.put(PlatformType.REPAST,".");
	}
	
	static final String PLUGIN_CLASS_LOAD_METHOD 	= "onPluginLoad";
	static final String PLUGIN_CLASS_UNLOAD_METHOD 	= "onPluginUnload";
	
	private static String platformPluginsDir;
	private static PlatformPluginList<IPluginPlatform> platformPlugins = new PlatformPluginList<IPluginPlatform>();
	
	private static Boolean DEBUG = null;
	
	//====================================================================================================
	// methods
	
	//----------------------------------------------------------------------------------------------------
	public static PlatformType[] getSupportedPlatforms() {
		List<PlatformType> supported = new ArrayList<PlatformType>();
		supported.add(PlatformType.REPAST);
//		supported.add(PlatformType.TRASS); // TODO: ehelyett az TRASS-t is pluginosĂ„â€šĂ˘â‚¬ĹľÄ‚Ë�Ă˘â€šÂ¬ÄąË‡Ă„â€šĂ‹ďż˝Ä‚Ë�Ă˘â‚¬ĹˇĂ‚Â¬Ă„Ä…Ă„ÄľĂ„â€šĂ˘â‚¬ĹľÄ‚â€žĂ˘â‚¬Â¦Ă„â€šĂ˘â‚¬ĹľÄ‚Ë�Ă˘â€šÂ¬Ă‚Â¦Ä‚â€žĂ˘â‚¬ĹˇÄ‚Ë�Ă˘â€šÂ¬ÄąÄľĂ„â€šĂ˘â‚¬ĹľÄ‚Ë�Ă˘â€šÂ¬Ă‚Â¦Ä‚â€žĂ˘â‚¬ĹˇÄ‚Ë�Ă˘â€šÂ¬ÄąÄľĂ„â€šĂ˘â‚¬Ä…Ă„Ä…Ă„â€žĂ„â€šĂ˘â‚¬ĹľÄ‚Ë�Ă˘â€šÂ¬ÄąË‡Ă„â€šĂ‹ďż˝Ä‚Ë�Ă˘â‚¬ĹˇĂ‚Â¬Ä‚â€žĂ˘â‚¬Â¦Ă„â€šĂ˘â‚¬ĹľÄ‚â€žĂ˘â‚¬Â¦Ă„â€šĂ˘â‚¬ĹľÄ‚Ë�Ă˘â€šÂ¬ÄąÄľtani kell
		for (PluginInfo<IPluginPlatform> info : getPlatformPlugins())
			supported.add(info.getInstance().getPlatfomType());
		return supported.toArray(new PlatformType[0]);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static boolean isSupportedPlatform(PlatformType type) {
		PlatformType[] supported = getSupportedPlatforms();
		for (PlatformType t : supported) {
			if (t == type) return true;
		}
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	public static boolean registerPlatform(PlatformType type, String installationDir) {
		File dir = new File(installationDir);
		if  (dir.exists() && dir.isDirectory()) {
			registeredPlatforms.put(type,installationDir);
			return true;
		}
		return false;
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void removePlatform(PlatformType type) {
		if (PlatformType.REPAST != type)
			registeredPlatforms.remove(type);
	}
	
	//----------------------------------------------------------------------------------------------------
	/**
	 * Platform retrieval.
	 * @param type
	 * @return
	 */
	 public static Platform getPlatform(PlatformType type) {
		 if (type == PlatformType.REPAST)
			 return RepastPlatform.getPlatform();
		 else if (type == PlatformType.TRASS) // TODO: ehelyett az TRASS-t is pluginosĂ„â€šĂ˘â‚¬ĹľÄ‚Ë�Ă˘â€šÂ¬ÄąË‡Ă„â€šĂ‹ďż˝Ä‚Ë�Ă˘â‚¬ĹˇĂ‚Â¬Ă„Ä…Ă„ÄľĂ„â€šĂ˘â‚¬ĹľÄ‚â€žĂ˘â‚¬Â¦Ă„â€šĂ˘â‚¬ĹľÄ‚Ë�Ă˘â€šÂ¬Ă‚Â¦Ä‚â€žĂ˘â‚¬ĹˇÄ‚Ë�Ă˘â€šÂ¬ÄąÄľĂ„â€šĂ˘â‚¬ĹľÄ‚Ë�Ă˘â€šÂ¬Ă‚Â¦Ä‚â€žĂ˘â‚¬ĹˇÄ‚Ë�Ă˘â€šÂ¬ÄąÄľĂ„â€šĂ˘â‚¬Ä…Ă„Ä…Ă„â€žĂ„â€šĂ˘â‚¬ĹľÄ‚Ë�Ă˘â€šÂ¬ÄąË‡Ă„â€šĂ‹ďż˝Ä‚Ë�Ă˘â‚¬ĹˇĂ‚Â¬Ä‚â€žĂ˘â‚¬Â¦Ă„â€šĂ˘â‚¬ĹľÄ‚â€žĂ˘â‚¬Â¦Ă„â€šĂ˘â‚¬ĹľÄ‚Ë�Ă˘â€šÂ¬ÄąÄľtani kell
			 throw new IllegalStateException();
//			 return TRASSPlatform.getPlatform(); 
		 else if (isSupportedPlatform(type)) 
			 return getPlatformPlugins().findByType(type);
		 return null;
	}
	 
	//----------------------------------------------------------------------------------------------------
	public static Set<PlatformType> getRegisteredPlatforms() { return registeredPlatforms.keySet(); }
	public static String getInstallationDirectory(PlatformType type) { return registeredPlatforms.get(type); }
	
	//----------------------------------------------------------------------------------------------------
	public static PlatformType platformTypeFromString(String id) { //TODO: finish
		if ("REPAST".equals(id))
			return PlatformType.REPAST;
		else if ("SIMPHONY".equals(id))
			return PlatformType.SIMPHONY;
		else if ("SIMPHONY2".equals(id))
			return PlatformType.SIMPHONY2;
		else if ("EMIL".equals(id))
			return PlatformType.EMIL;
		else if ("TRASS".equals(id))
			return PlatformType.TRASS;
		else if ("NETLOGO4".equals(id))
			return PlatformType.NETLOGO;
		else if ("NETLOGO5".equals(id))
			return PlatformType.NETLOGO5;
		else if ("MASON".equals(id))
			return PlatformType.MASON;
		else if ("CUSTOM".equals(id))
			return PlatformType.CUSTOM;
		return null;
	}
	 
	//----------------------------------------------------------------------------------------------------
	public static String idStringForPlatform(PlatformType type) { //TODO: finish
		switch (type) {
		case REPAST 	: return "REPAST"; 
		case SIMPHONY 	: return "SIMPHONY";
		case SIMPHONY2	: return "SIMPHONY2";
		case EMIL 		: return "EMIL";
		case NETLOGO 	: return "NETLOGO4";
		case NETLOGO5	: return "NETLOGO5";
		case MASON 		: return "MASON";
		case CUSTOM		: return "CUSTOM";
		case TRASS		: return "TRASS";
		default 		: return null;
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	public static Map<PlatformType,String> checkRegisteredPlatform() {
		Map<PlatformType,String> invalidPlatforms = new HashMap<PlatformType,String>();
		for (Entry<PlatformType,String> e : registeredPlatforms.entrySet()) {
			File dir = new File(e.getValue());
			if (!dir.exists() || !dir.isDirectory()) {
				invalidPlatforms.put(e.getKey(),e.getValue());
				registeredPlatforms.remove(e.getKey());
			}
		}
		return invalidPlatforms;
	}
	
	// plugin manager methods 
	
	//----------------------------------------------------------------------------------------------------
	public static void setPlatformPluginsDir(String platformPluginsDir) { PlatformManager.platformPluginsDir = platformPluginsDir; }
	public static String getPlatformPluginsDir() { return platformPluginsDir; }
	public static PlatformPluginList<IPluginPlatform> getPlatformPlugins() { return platformPlugins; }
	
	//----------------------------------------------------------------------------------------------------
	public static String[] scanPlatformPlugins(IPlugin.IOnPluginLoad loadCtx) throws Exception {
		File dir = new File(getPlatformPluginsDir());
		
		if (!dir.exists()) 
			throw new Exception("Cannot locate the PlatformPlugins directory");
		String[] files = dir.list();
		ZipFile zf = null;
		InputStream is;
		List<String> errs = new ArrayList<String>();
		for (String file : files) {
			String err = null;
			File f = new File(dir,file);
			try {
				if (!isDebugVersion()) {
				//if (true){
					zf = new ZipFile(f); // the release version allows zip file platform plugins only
//					ClassPool.getDefault().insertClassPath(f.getAbsolutePath());
				} else if (f.isDirectory() && f.exists()) {
					zf = null; // during developement, allow subdirs only (skip .jar files)
//					ClassPool.getDefault().insertClassPath(f.getAbsolutePath());
				} else continue;
			} catch (IOException e) { continue; /* Not a zip file. */ }
			try {
				ArrayList<File> urlList = new ArrayList<File>();
				if (isDebugVersion() && f.isDirectory() && f.exists()){
					//File libFile = Paths.get(f.getAbsolutePath()).resolve("lib").toFile();
					File libFile = new File(f.getAbsolutePath() + File.separator + "lib");
					File[] jarFiles = libFile.listFiles(new FilenameFilter() {
						
						@Override
						public boolean accept(File dir, String name) {
							return name.endsWith(".jar");
						}
					});
					
					if (jarFiles != null && jarFiles.length > 0){
						urlList.addAll(Arrays.asList(jarFiles));
					}
					
//					File binDir = new File(f,"bin");
//					if (binDir.exists())
//						urlList.add(binDir);
				}
				urlList.add(f);
				
				URL[] urls = new URL[urlList.size()] ;
				int index = 0;
				for (File file2 : urlList) {
					urls[index++] = file2.toURI().toURL();
//					ClassPool.getDefault().insertClassPath(file2.getAbsolutePath());
				}
				URLClassLoader pluginLoader = new URLClassLoader(urls);
				
				// Find the plugin directory file, and record any platform plugin classes
				
				is = getistream(zf,f,"plugins");
				if (is != null) {
					BufferedReader in = new BufferedReader(new InputStreamReader(is));
					while (true) {
						String clsName = in.readLine();
						if (clsName == null) break;
						
						try {
							Class<?> cls = pluginLoader.loadClass(clsName);
							PluginInfo<?> info = null;
							if (IPluginPlatform.class.isAssignableFrom(cls))
								info = loadPlugin(cls,loadCtx,platformPlugins);
							if (info != null) {
								info.setPluginLoader(pluginLoader);
								info.setInternalName(clsName);
							}
						} catch (NoClassDefFoundError e) {
							err = String.format("class %s is not found",clsName);
						} catch (ClassNotFoundException e) {
							err = String.format("class %s is not found",clsName);
						} catch (IllegalAccessException e) {
							err = String.format("cannot access class %s",clsName);
						} catch (InstantiationException e) {
							err = String.format("cannot instantiate class %s",clsName);
						}
					}
					in.close();
					is.close();
				} else 
					err = "missing 'plugins' file";
			} catch (IOException e) {
				err = Util.getLocalizedMessage(e);
			}
			if (err != null)
				errs.add(err);
		}
		return errs.toArray(new String[0]);
	}
	
	//----------------------------------------------------------------------------------------------------
	public static void unloadPlugins(Object arg, Object... whichPlugins) {
		if (whichPlugins == null || whichPlugins.length == 0)
			unloadPlugins(arg,platformPlugins);
		else if (whichPlugins[0] instanceof IPlugin) {
			// unload 1 plugin
			// Caller is responsible for removing the corresponding plugininfo object from the corresponding array
			unloadPlugin(whichPlugins[0].getClass(),arg);
		} else if (whichPlugins[0] instanceof PlatformPluginList) {
			PlatformPluginList<?> array = (PlatformPluginList<?>) whichPlugins[0];
			for (PluginInfo<?> info : array)
				unloadPlugins(arg,info.getInstance());
			array.clear();
		}
	}
	
	//----------------------------------------------------------------------------------------------------
	@SuppressWarnings("unchecked")
	public static <P extends IPluginPlatform> PluginInfo<P> loadPlugin(Class<?> cls, IPlugin.IOnPluginLoad loadCtx,
																		ArrayList<? super PluginInfo<P>> array) throws Exception {
		Object[] instance = callClassMethod(cls,PLUGIN_CLASS_LOAD_METHOD,loadCtx);
		if (instance == null)
			instance = new Object[] { cls.newInstance() };
		else if (instance[0] == null)
			return null; // The platform refused loading
		else if (!cls.isInstance(instance[0]))
			throw new InstantiationException(String.format("%s.%s() returned an invalid object (of type %s)",cls.getName(),PLUGIN_CLASS_LOAD_METHOD,
											 instance[0].getClass()));
		
		// Now 'instance[0]' is instance of P
		PluginInfo<P> ans = new PluginInfo<P>();
		array.add(ans);
		ans.setInstance((P)instance[0]);
		return ans;
	}
	
	//====================================================================================================
	// assistant methods
	 
	//----------------------------------------------------------------------------------------------------
	private PlatformManager() {}
	
	//----------------------------------------------------------------------------------------------------
	private static boolean isDebugVersion() {
		if (DEBUG == null) {
			try	{
				URL baseURL = PlatformManager.class.getResource("/ai/aitia/meme/paramsweep/platform/PlatformManager.class");
			    DEBUG = !baseURL.toString().startsWith("jar:"); 
			} catch (Exception ex) {
				Logger.logExceptionCallStack(ex);
			}
		}
		return DEBUG;
	}
	
	//----------------------------------------------------------------------------------------------------
	static InputStream getistream(ZipFile zf, File f, String name) throws IOException, FileNotFoundException {
		if (zf != null) {
			ZipEntry ze = zf.getEntry(name);
			return (ze != null) ? zf.getInputStream(ze) : null;
		}
		File g = new File(f,name);
		return (g.exists() && g.isFile()) ? new FileInputStream(g) : null;
	}
	
	//----------------------------------------------------------------------------------------------------
	private static void unloadPlugin(Class<?> cls, Object arg) {
		try {
			callClassMethod(cls,PLUGIN_CLASS_UNLOAD_METHOD,arg);
		} catch (Throwable _) {}
	}
	
	//----------------------------------------------------------------------------------------------------
	private static  Object[] callClassMethod(Class<?> cls, String methodName, Object arg) throws Exception {
		for (Method m : cls.getMethods()) {
			if (!m.getName().equals(methodName)) continue;
			Class<?>[] types = m.getParameterTypes();
			if (types.length == 0 || types.length == 1 && (arg == null || types[0].isInstance(arg))) {
				Object[] result = { (types.length == 0) ? m.invoke(cls) : m.invoke(cls,arg) };
				return result;
			}
		}
		return null;
	}
}
