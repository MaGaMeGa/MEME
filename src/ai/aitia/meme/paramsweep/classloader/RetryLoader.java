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
package ai.aitia.meme.paramsweep.classloader;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.ClassPath;
import javassist.DamnVisibilityUtils;
import javassist.Loader;
import javassist.NotFoundException;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;
import ai.aitia.meme.paramsweep.internal.platform.PlatformSettings;
import ai.aitia.meme.paramsweep.platform.PlatformManager;
import ai.aitia.meme.paramsweep.utils.ClassPathPair;
import ai.aitia.meme.paramsweep.utils.ModelFileFilter;
import ai.aitia.meme.paramsweep.utils.Util.IStoppableRetryLoader;
import ai.aitia.meme.utils.GUIUtils;

/**
 * A special class loader. Like the javassist.Loader it delegates class loading
 * requests to the parent class loader before trying to load the class itself
 * only in special cases (in the case of java core classes, GUI-related classes,
 * Look&Feel classes, etc.) If it tries to load a class and doesn't find that
 * then asks for the user to give the location of the missing class, extends the
 * classpath and tries again. If the class loading fails again then the class
 * loader deleteges the request to the parent class loader.
 */
public class RetryLoader extends Loader implements IStoppableRetryLoader {

	// ===============================================================================
	// members

	/** The owner of the class loader. */
	private ParameterSweepWizard owner = null;
	private boolean retry = true;

	// ===============================================================================
	// methods

	// -------------------------------------------------------------------------------
	/**
	 * Constructor.
	 * 
	 * @param owner
	 *            the owner of the class loader
	 */
	public RetryLoader(ParameterSweepWizard owner) {
		super(PlatformManager.getPlatform(PlatformSettings.getPlatformType())
				.getClass().getClassLoader(), owner.getClassPool());
		this.owner = owner;
		try {

			for (URL url : ((URLClassLoader) getParent()).getURLs())
				owner.getClassPool()
						.appendClassPath(url.getFile());

		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
	}
	}

	// ----------------------------------------------------------------------------------------------------
	@Override
	protected Enumeration<URL> findResources(final String name)
			throws IOException {
		final List<URL> result = new ArrayList<URL>();
		for (int i = 0; i < owner.getClassPathListModel().getSize(); ++i) {
			final ClassPathPair pair = (ClassPathPair) owner
					.getClassPathListModel().getElementAt(i);
			final ClassPath cp = pair.getClassPath();
			final URL url = getURLFrom(cp, name);
			if (url != null)
				result.add(url);
		}
		return new Enumeration<URL>() {

			// ====================================================================================================
			// members

			private int idx = 0;

			// ====================================================================================================
			// methods

			// ----------------------------------------------------------------------------------------------------
			public boolean hasMoreElements() {
				return idx < result.size();
			}

			public URL nextElement() {
				return result.get(idx++);
			}
		};
	}

	// ----------------------------------------------------------------------------------------------------
	@Override
	protected URL findResource(String name) {
		for (int i = 0; i < owner.getClassPathListModel().getSize(); ++i) {
			final ClassPathPair pair = (ClassPathPair) owner
					.getClassPathListModel().getElementAt(i);
			final ClassPath cp = pair.getClassPath();
			final URL url = getURLFrom(cp, name);
			if (url != null)
				return url;
		}
		
//		if (getParent() != null){
//			getParent().
//		}
		
		return null;
	}

	// ----------------------------------------------------------------------------------------------------
	private URL getURLFrom(final ClassPath cp, final String name) {
		if (cp == null)
			return null;

		// Replacing dots in names would break the loading of the following
		// resource, see [*] tags
		// META-INF/services/dk.ange.octave.io.spi.OctaveDataWriter

		if (DamnVisibilityUtils.isDirClassPath(cp)) {
			final char sep = File.separatorChar;
			final String filename = DamnVisibilityUtils
					.getDirFromDirClassPath(cp) + sep + name; // name.replace('.',sep);
																// // [*]
	        final File f = new File(filename);
		    if (f.exists()) {
		    	try {
		    		return f.getCanonicalFile().toURI().toURL();
		        } catch (MalformedURLException e) {
				} catch (IOException e) {
		    }
			}
	        return null;
		} else if (DamnVisibilityUtils.isJarClassPath(cp)) {
			// final String jarname = name.replace('.','/'); // [*]
			JarFile jarFile = DamnVisibilityUtils
					.getJarFileFromJarClassPath(cp);
			if (jarFile != null) {
				final JarEntry je = jarFile.getJarEntry(name);
				if (je != null) {
					try {
						return new URL(
								"jar:"
										+ DamnVisibilityUtils
												.getJarURLFromJarClassPath(cp)
										+ "!/" + name);
					} catch (MalformedURLException e) {
					}
			}
	        return null;      
		}
			}
		return null;
	}

	// ----------------------------------------------------------------------------------------------------
	public void stopRetry() {
		retry = false;
	}

	public void startRetry() {
		retry = true;
	}

	// -------------------------------------------------------------------------------
	@Override
	protected Class<?> loadClass(String name, boolean resolve)
			throws ClassFormatError, ClassNotFoundException {
		name = name.intern();
		synchronized (name) {
			Class<?> c = findLoadedClass(name);
			if (c == null)
				c = loadClassByDelegation(name);
			if (c == null)
				c = findClass(name);
			// the org.eclipse.persistence.* check is here because the
			// eclipselink (moxy) jaxb implementation looks for such classes
			if (c == null
					&& (name.endsWith("BeanInfo")
							|| name.endsWith("Customizer") || name
								.startsWith("org.eclipse.persistence.internal.localization.i18n.ToStringLocalizationResource_en")))
				c = delegateToParent(name);
			if (c == null && retry) {
				boolean res = extendClassPath(name);
				if (res)
					c = findClass(name);
				else
					c = delegateToParent(name);
			}
			if (resolve) {
				resolveClass(c);
			}

			return c;
		}
	}

	// --------------------------------------------------------------------------------
	@Override
	protected Class<?> loadClassByDelegation(String name)
			throws ClassNotFoundException {

		Class<?> c = super.loadClassByDelegation(name);
		if (c == null && (name.startsWith("org.jvnet.") ||
						  name.startsWith("contrib.ch.randelshofer.") ||
						  name.startsWith("contrib.com.jgoodies.") ||
						  name.startsWith("contrib.net.xoetrope.") ||
						  name.startsWith("ai.aitia.meme.utils.") ||
						  name.startsWith("com.jgoodies.forms.") ||
						  name.startsWith("ai.aitia.meme.gui.IPanelManager") ||
						  name.startsWith("ai.aitia.meme.paramsweep.plugin.IIntelliDynamicMethodPlugin") ||
						  name.startsWith("ai.aitia.meme.paramsweep.plugin.IntelliDynamicParameter") ||
						  name.startsWith("ai.aitia.meme.paramsweep.intellisweepPlugin.RepastResultParser") ||
						  name.startsWith("_") ||
						  name.startsWith("fables.paramsweep.runtime.annotations") || 
						  name.startsWith("ai.aitia.meme.paramsweep.batch") ||
						  name.startsWith("ai.aitia.meme.paramsweep.utils.AssistantMethod") ||
						  name.contains("ToStringLocalizationResource") ||
						  name.startsWith("ai.aitia.meme.paramsweep.platform.IPSWInformationProvider") ||
						  name.startsWith("javassist") || // needed by the platforms to initialize IModelInformation objects
						  name.startsWith("net.sf.cglib.core.MethodWrapper$MethodWrapperKey$") ||
						  name.startsWith("groovy.runtime") ||
						  name.startsWith("com.maplesoft.openmaple") ||
						  //name.startsWith("sim.engine") || name.startsWith("sim.util") || //MASON
						  name.startsWith("ec.util") || //MASON
						  name.startsWith("org.nlogo") || name.startsWith("scala") || 
						  //name.startsWith("org.objectweb.asm") || // NetLogo
						  (name.startsWith("ai.aitia.meme.paramsweep") && name.endsWith("Info") && !name.startsWith("ai.aitia.meme.paramsweep.platform.mason.recording.RecordingHelper"))
						  )
			) 
			c = delegateToParent(name);
		return c;
	}

	// -------------------------------------------------------------------------------
	/**
	 * It shows a file dialog where the user can define the location of a
	 * missing class file (the user must select the class file or the jar file
	 * that contains it). Then it extends the classpath with the first component
	 * of the given path (the part that not contains the fully qualified name of
	 * the missing class). The file dialog allows for the user to select more
	 * files.
	 * 
	 * @param clazz
	 *            the fully qualified name of the missing class
     * @return false if the user chooses the Cancel button, true otherwise
     */
	public boolean extendClassPath(String clazz) {
		RetryLoader.ClassPathDialog dialog = new RetryLoader.ClassPathDialog(
				ParameterSweepWizard.getFrame(), clazz);
		int result = dialog.showDialog();
		File[] f = null;
		if (result == JFileChooser.APPROVE_OPTION)
			f = dialog.getSelectedFiles();
		if (f == null || f.length == 0)
			return false;
		try {
			for (File file : f) {
				if (file.getName().endsWith(".jar")) {
					ClassPathPair pair = new ClassPathPair(
							file.getAbsolutePath(), null);
					if (owner.getClassPathListModel().contains(pair))
						continue;
					ClassPath cp = owner.getClassPool().insertClassPath(
							file.getAbsolutePath());
					owner.getClassPathListModel().add(0,
							new ClassPathPair(file.getAbsolutePath(), cp));
				} else {
					String pathName = findPath(clazz, file.getParent());
					ClassPathPair pair = new ClassPathPair(pathName, null);
					if (owner.getClassPathListModel().contains(pair))
						continue;
					ClassPath cp = owner.getClassPool().insertClassPath(
							pathName);
					owner.getClassPathListModel().add(0,
							new ClassPathPair(pathName, cp));
				}
			}
			return true;
		} catch (NotFoundException e) {
		}
		return false;
	}

	// -------------------------------------------------------------------------------
	/**
	 * This methods finds the first component of the parameter <code>path</code>
	 * that not contains the package name of the class specified by
	 * <code>className</code>.
	 * 
	 * @param className
	 *            the fully qualified name of a class
	 * @param path
	 *            a file path
	 * @return the first (see above) component of <code>path</code>.
	 */
	private String findPath(String className, String path) {
		int index = className.lastIndexOf('.');
		if (index != -1)
			className = className.substring(0, index);
		className = className.replace('.', File.separatorChar);
		index = path.lastIndexOf(className);
		String returnPath = (index == -1) ? path : path.substring(0, index - 1);
		return returnPath;
	}

	// ====================================================================================================
	// nested classes

	/**
	 * This class describes the special file dialog that is used to asks from
	 * the user the location of the missing classes.
	 */
	public static class ClassPathDialog extends JDialog implements
			ActionListener {

		// ====================================================================================================
		// members

		private static final long serialVersionUID = 1L;
		private JFileChooser chooser = null;
		private Frame owner = null;
		/** The selected files. */
		private File[] selectedFiles = null;
		/** The fully qualified name of the missing class. */
		private String className = null;
		/**
		 * The return value of the dialog (same as in the case of JFileChooser).
		 */
		private int returnValue = JFileChooser.ERROR_OPTION;

		// ====================================================================================================
		// methods

		// ----------------------------------------------------------------------------------------------------
		/**
		 * Constructor.
		 * 
		 * @param owner
		 *            the parent of the dialog
		 * @param className
		 *            the fully qualified name of the missing class
		 */
		public ClassPathDialog(Frame owner, String className) {
			super(owner, "Extend class path", true);
			this.owner = owner;
			this.className = className;
			this.setName("dial_classpath");
			initialize();
			this.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					returnValue = JFileChooser.CANCEL_OPTION;
				}
			});
		}

		// ----------------------------------------------------------------------------------------------------
		/**
		 * Shows the dialog and returns an integer constant that describes which
		 * button is pressed before the dialog is closed.
		 */
		public int showDialog() {
			setVisible(true);
			int result = returnValue;
			dispose();
			return result;
		}

		// ----------------------------------------------------------------------------------------------------
		public File[] getSelectedFiles() {
			return selectedFiles;
		}

		// ====================================================================================================
		// assistant methods

		// ----------------------------------------------------------------------------------------------------
		private void initialize() {
			chooser = new JFileChooser(ParameterSweepWizard.getLastDir());
			chooser.setMultiSelectionEnabled(true);
			chooser.setAcceptAllFileFilterUsed(false);
			chooser.addChoosableFileFilter(new ModelFileFilter(true));
			chooser.setDialogType(JFileChooser.OPEN_DIALOG);
			chooser.addActionListener(this);
			chooser.setName("filechooser_classpath");

			Component[] btns = ((JPanel) ((JPanel) (chooser.getComponents()[3]))
					.getComponents()[3]).getComponents();
			btns[0].setName("btn_ok");
			btns[1].setName("btn_cancel");

			JPanel panel = new JPanel(new BorderLayout());
			JTextPane area = new JTextPane();
			area.setBackground(Color.ORANGE);
			GUIUtils.setTextPane(
					area,
					"<html><font face=sansserif size=-1>The Java class loader could not locate some of the classes needed to run the selected model.<br>"
							+ "Please locate and add all the required classes! Especially<br><br> <center><b>"
							+ className + "</b></center></font></html>");
			area.setEditable(false);
			area.setName("fld_classpath");
			area.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			panel.add(area, BorderLayout.NORTH);
			panel.add(chooser, BorderLayout.CENTER);
			final JScrollPane sp = new JScrollPane(panel,
					JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
					JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			sp.setBorder(null);
			this.setContentPane(sp);
			this.pack();
			Dimension oldD = this.getPreferredSize();
			this.setPreferredSize(new Dimension(oldD.width
					+ sp.getVerticalScrollBar().getWidth(), oldD.height
					+ sp.getHorizontalScrollBar().getHeight()));
			sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			oldD = this.getPreferredSize();
			final Dimension newD = GUIUtils.getPreferredSize(this);
			if (!oldD.equals(newD))
				this.setPreferredSize(newD);
			this.pack();
			this.setLocationRelativeTo(owner);
		}

		// ====================================================================================================
		// implemented interfaces

		// ----------------------------------------------------------------------------------------------------
		public void actionPerformed(ActionEvent e) {
			selectedFiles = chooser.getSelectedFiles();
			boolean approve = e.getActionCommand().equals("ApproveSelection");
			returnValue = approve ? JFileChooser.APPROVE_OPTION
					: JFileChooser.CANCEL_OPTION;
			if (approve)
				ParameterSweepWizard.setLastDir(selectedFiles[0]);
			setVisible(false);
		}
	}
}
