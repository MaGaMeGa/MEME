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
package ai.aitia.meme.paramsweep.sftp;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import ai.aitia.meme.paramsweep.ParameterSweepWizard;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;
import com.jcraft.jsch.ChannelSftp.LsEntry;

/** SFTP services for the MASS/MEME Parameter Sweep Wizard and Monitor applications. */
public class SftpClient {
	
	//=================================================================================
	// members
	
	private final long NO_GUI_UPLOAD_LIMIT = 1024 * 1024; // 1MB
	private final long NO_GUI_DOWNLOAD_LIMIT = 100; // 100 byte
	private final int SSH_DEFAULT_PORT = 22;

	/** Host of the SFTP server. */
	private String hostName = null;
	/** Port of the SFTP server. */
	private int port = SSH_DEFAULT_PORT;
	/** Username to the SFTP server. */
	private String userName = null;
	/** Password to the SFTP server. */
	private String password = null;
	/** A path of a file that contains a private key to the SFTP server. */
	private String privateKeyFile = null;
	/** Passhprase to the SFTP server. */
	private String passphrase = null;
	/** The workspace of the MASS/MEME Simulation Server. */ 
	private String workspace = null;
	
	/** Session object.
	 * @see com.jcraft.jsch.Session
	 */
	private Session session = null;
	/** SFTP channel object.
	 * @see com.jcraft.jsch.ChannelSftp
	 */
	private ChannelSftp channel = null;
	
	//=================================================================================
	// methods
	
	//---------------------------------------------------------------------------------
	/** Constructor.
	 * @param hostName host of the SFTP server
	 * @param port port of the SFTP server
	 * @param userName username to the SFTP server
	 * @param password password to the SFTP server
	 * @param workspace workspace of the MASS/MEME Simulation Server
	 */
	public SftpClient(String hostName,
					  int port,
					  String userName,
					  String password,
					  String workspace) {
		if (hostName == null || "".equals(hostName.trim()))
			throw new IllegalArgumentException("'hostName' is invalid");
		if (userName == null || "".equals(userName.trim()))
			throw new IllegalArgumentException("'userName' is invalid");
		if (password == null)
			throw new IllegalArgumentException("'password' is invalid");
		int tempPort = port < 1 || port > 65535 ? SSH_DEFAULT_PORT : port;
		String tempWorkspace = (workspace == null) ? "" : workspace.trim();
		if (workspace.endsWith("/") || workspace.endsWith("\\"))
			tempWorkspace = tempWorkspace.substring(0,tempWorkspace.length()-1);
		this.hostName = hostName.trim();
		this.port = tempPort;
		this.userName = userName.trim();
		this.password = password;
		this.workspace = tempWorkspace;
	}
	
	//---------------------------------------------------------------------------------
	/** Constructor.
	 * @param hostName host of the SFTP server
	 * @param port port of the SFTP server
	 * @param userName username to the SFTP server
	 * @param privateKeyFile path of a file that contains a private key to the SFTP server
	 * @param passphrase passphrase to the SFTP server
	 * @param workspace workspace of the MAA/MEME Simulation Server
	 */
	public SftpClient(String hostName,
					  int port,
					  String userName,
					  String privateKeyFile,
					  String passphrase,
					  String workspace) {
		if (hostName == null || "".equals(hostName.trim()))
			throw new IllegalArgumentException("'hostName' is invalid");
		if (userName == null || "".equals(userName.trim()))
			throw new IllegalArgumentException("'userName' is invalid");
		if (privateKeyFile == null || "".equals(privateKeyFile.trim()))
			throw new IllegalArgumentException("'privateKeyFile' is invalid");
		int tempPort = port < 1 || port > 65535 ? SSH_DEFAULT_PORT : port;
		String tempWorkspace = (workspace == null) ? "" : workspace.trim();
		if (tempWorkspace.endsWith("/") || tempWorkspace.endsWith("\\"))
			tempWorkspace = tempWorkspace.substring(0,tempWorkspace.length()-1);
		this.hostName = hostName.trim();
		this.port = tempPort;
		this.userName = userName.trim();
		this.privateKeyFile = privateKeyFile;
		this.passphrase = "".equals(passphrase) ? null : passphrase;
		this.workspace = tempWorkspace;
	}
	
	//---------------------------------------------------------------------------------
	/** Connects to the SFTP server.
	 * @return error message (or null if there is no error)
	 */
	public String connect() {
		JSch jSch = new JSch();
		Channel ch = null;
		String error = null;
		try {
			if (privateKeyFile != null) {
				jSch.addIdentity(privateKeyFile);
			}
			
			session = jSch.getSession(userName,hostName,port);
			UserInfo ui = new NotInteractiveUserInfo();
			session.setUserInfo(ui);
			session.connect();
			ch = session.openChannel("sftp");
			ch.connect();
			channel = (ChannelSftp)ch;
			return null;
		} catch (JSchException e) {
			if (e.getCause() == null) 
				error = "Invalid username and/or password (or private key, passphrase).";
			else 
				error = "Connection attempt failed. Please check your internet connection and " +
						"be sure that your firewall does not block the program.\nIf everything " +
						"seems to be fine please contact to the system administrator of the cluster.";
			e.printStackTrace(ParameterSweepWizard.getLogStream());
		}
		if (ch != null)
			ch.disconnect();
		if (session != null) {
			session.disconnect();
			session = null;
		}
		jSch = null;
		System.gc(); System.gc(); System.gc(); System.gc(); // This is important, because session.disconnect() doesn't close
		System.gc(); System.gc(); System.gc(); System.gc(); // the connection truly so users stay in the sftp server in login phase.
		return error;
	}
	
	//--------------------------------------------------------------------------------
	/** Closes the connection to the SFTP server. */
	public void disconnect() {
		if (channel != null)
			channel.disconnect();
		if (session != null)
			session.disconnect();
		channel = null;
		session = null;
		System.gc(); System.gc(); System.gc(); System.gc();
		System.gc(); System.gc(); System.gc(); System.gc(); // caution
	}
	
	//--------------------------------------------------------------------------------
	/** Changes the current directory of the SFTP connection to the workspace directory.
	 * @throws SftpException if the workspace directory does not exists or cannot access,
	 *                       or the connection is broken
	 */
	@SuppressWarnings("unchecked")
	public void goToWorkspace() throws SftpException {
		if (channel == null || session == null)
			throw new SftpException(ChannelSftp.SSH_FX_CONNECTION_LOST,"No connection");
		channel.cd(workspace); // can throw SftpException if the workspace directory does not exist
							   // or cannot access
	}
	
	//--------------------------------------------------------------------------------
	/** Creates a directory on the host of the SFTP server.
	 * @param path the path of new directory
	 * @throws SftpException if any problem occurs
	 */
	public void generateDirectory(String path) throws SftpException {
		if (path == null || "".equals(path.trim()))
			throw new IllegalArgumentException("'path' is invalid");
		if (channel == null || session == null)
			throw new SftpException(ChannelSftp.SSH_FX_CONNECTION_LOST,"No connection");
		channel.mkdir(path.trim());
		channel.chmod(0777,path.trim());
	}
	
	//--------------------------------------------------------------------------------
	/** Transfers a file from the local computer to the remote computer.
	 * @param owner a main window of the application that uses this client (used by the progress dialog, if necessary)
	 * @param source the path of the file (on local host)
	 * @param destDir the destination directory (on remote host)
	 * @return true if the uploading is not aborted by the user
	 * @throws SftpException if any problem occurs during the transfer
	 * @throws FileNotFoundException if the file doesn't exists
	 */
	public boolean upload(Frame owner, String source, String destDir) throws SftpException,
																			 FileNotFoundException {
		if (source == null || "".equals(source.trim()))
			throw new IllegalArgumentException("'source' is invalid");
		if (destDir == null || "".equals(destDir.trim()))
			throw new IllegalArgumentException("'destination' is invalid");
		if (channel == null || session == null)
			throw new SftpException(ChannelSftp.SSH_FX_CONNECTION_LOST,"No connection");
		String absolute = absolutePath(source.trim());
		File file = new File(absolute);
		if (!file.exists())
			throw new FileNotFoundException(source.trim());
		if (file.length() <= NO_GUI_UPLOAD_LIMIT) {
			channel.put(absolute,destDir.trim(),ChannelSftp.OVERWRITE);
			return true;
		} else {
			SftpFileLoadingMonitor monitor = new SftpFileLoadingMonitor(owner,absolute,true); 
			try {
				channel.put(absolute,destDir.trim(),monitor,ChannelSftp.OVERWRITE);
			} finally {
				monitor.setVisible(false);
				if (monitor.isDisplayable())
					monitor.dispose();
			}
			if (monitor.isAborted()) {
				String fileName = source.trim();
				int index = fileName.lastIndexOf(File.separatorChar);
				if (index != -1)
					fileName = fileName.substring(index+1);
				channel.rm(destDir.trim() + "/" + fileName);
			}
			return !monitor.isAborted();
		}
	}
	
	//--------------------------------------------------------------------------------
	/** Transfers a file from the remote computer to the local computer.
	 * @param owner a main window of the application that uses this client (used by the progress dialog, if necessary)
	 * @param source the path of the file (on the remote host)
	 * @param destDir the destination directory (on the local host)
	 * @return true if the downloading is not aborted by the user
	 * @throws SftpException if any problem occurs during the transfer
	 * @throws FileNotFoundException if the file doesn't exists
	 */
	@SuppressWarnings("unchecked")
	public boolean download(Window owner, String source, String destDir) throws SftpException,
																			   FileNotFoundException {
		if (source == null || "".equals(source.trim()))
			throw new IllegalArgumentException("'source' is invalid");
		if (destDir == null || "".equals(destDir.trim()))
			throw new IllegalArgumentException("'destDir' is invalid");
		if (channel == null || session == null)
			throw new SftpException(ChannelSftp.SSH_FX_CONNECTION_LOST,"No connection");
		File file = new File(destDir);
		if (!file.exists())
			throw new FileNotFoundException(destDir.trim());
		int index = source.lastIndexOf('/');
		String sourceDir = source.trim().substring(0,index);
		String sourceFile = source.trim().substring(index + 1);
		Vector<LsEntry> results = channel.ls(sourceDir);
		long fileSize = -1;
		for (LsEntry entry : results) { 
			if (entry.getFilename().equals(sourceFile)) {
				fileSize = entry.getAttrs().getSize();
				break;
			}
		}
		if (fileSize == -1) // missing source file
			throw new FileNotFoundException("Cannot find file: " + source);
		if (fileSize <= NO_GUI_DOWNLOAD_LIMIT) {
			channel.get(source.trim(),destDir.trim(),null,ChannelSftp.OVERWRITE);
			return true;
		} else {
			SftpFileLoadingMonitor monitor = null;
			if (owner instanceof Frame)
				monitor = new SftpFileLoadingMonitor((Frame)owner,sourceFile,false);
			else 
				monitor = new SftpFileLoadingMonitor((Dialog)owner,sourceFile,false);
			try {
				channel.get(source.trim(),destDir.trim(),monitor,ChannelSftp.OVERWRITE);
			} finally {
				monitor.setVisible(false);
				if (monitor.isDisplayable())
					monitor.dispose();
			}
			return !monitor.isAborted();
		}
	}
	
	//--------------------------------------------------------------------------------
	/** Creates all directories contained by <code>dirPath</code> on the remote computer
	 *  which is hasn't existed yet.
	 * @param startDir the method creates the directories to this
	 * @param dirPath a path of directory
	 * @throws SftpException if any problem occurs
	 */
	public void createPath(String startDir, String dirPath) throws SftpException {
		if (startDir == null || "".equals(startDir.trim()))
			throw new IllegalArgumentException("'startDir' is invalid");
		if (dirPath == null || "".equals(dirPath.trim()))
			throw new IllegalArgumentException("'dirPath' is invalid");
		if (channel == null || session == null)
			throw new SftpException(ChannelSftp.SSH_FX_CONNECTION_LOST,"No connection");
		String _path = dirPath.trim().replace('\\','/');
		if (_path.startsWith("/"))
			_path = _path.substring(1);
		if (_path.endsWith("/"))
			_path = _path.substring(0,_path.length()-1);
		String[] dirs = createPrefixes(_path);
		for (String dir : dirs) {
			try {
				channel.mkdir(startDir + "/" + dir);
			} catch (SftpException e) {
				if (e.id == ChannelSftp.SSH_FX_BAD_MESSAGE)
					continue;
				throw e;
			}
		}
	}
	
	//--------------------------------------------------------------------------------
	/** Deletes directories specified by <code>path</code>. 
	 * @param path path of the deletable element
	 * @throws SftpException if any problem occurs
	 */
	@SuppressWarnings("unchecked")
	public void clean(String path) throws SftpException {
		if (path == null || "".equals(path.trim()))
			throw new IllegalArgumentException("'path' is invalid");
		if (channel == null || session == null)
			throw new SftpException(ChannelSftp.SSH_FX_CONNECTION_LOST,"No connection");
		Vector<LsEntry> files = channel.ls(path.trim());
		for (LsEntry entry : files) {
			if (!entry.getAttrs().isDir() && !entry.getAttrs().isLink())
				channel.rm(path.trim() + "/" + entry.getFilename());
			else if (entry.getFilename().equals(".") || entry.getFilename().equals(".."))
				continue;
			else if (entry.getAttrs().isDir())
				clean(path.trim() + "/" + entry.getFilename());
		}
		channel.rmdir(path.trim());
	}
	
	//--------------------------------------------------------------------------------
	/** Deletes a file specified by the parameter
	 * @param path path of the deletable file
	 * @throws SftpException if any problem occurs
	 */
	public void removeFile(String path) throws SftpException {
		if (path == null || "".equals(path.trim()))
			throw new IllegalArgumentException("'path' is invalid");
		if (channel == null || session == null)
			throw new SftpException(ChannelSftp.SSH_FX_CONNECTION_LOST,"No connection");
		channel.rm(path.trim());
	}
	
	//=================================================================================
	// private methods
	
//	//--------------------------------------------------------------------------------
//	private Vector<LsEntry> filterContent(Vector<LsEntry> original) {
//		if (original == null) 
//			return null;
//		Vector<LsEntry> result = new Vector<LsEntry>();
//		for (LsEntry entry : original) {
//			if (entry.getAttrs().isDir())
//				result.add(entry);
//		}
//		return result;
//	}
//	
//	//--------------------------------------------------------------------------------
//	// precondition: dir != null
//	private String calcNewId(String prefix,Vector<LsEntry> dirs) {
//		int index = prefix.length();
//		int newId = -1;
//		for (LsEntry entry : dirs) {
//			String suffix = entry.getFilename();
//			if (suffix.length() <= index) continue;
//			suffix = suffix.substring(index);
//			try {
//				int id = Integer.parseInt(suffix);
//				if (newId < id)
//					newId = id;
//			} catch (NumberFormatException e) {
//				continue;
//			}
//		}
//		return prefix + String.valueOf(++newId); 
//	}
	
	//---------------------------------------------------------------------------------
	/** Returns the absoulte path that belongs to the path <code>path</code>. */
	private String absolutePath(String path) {
		try {
			String _path = path.replace('\\',File.separatorChar);
			_path = path.replace('/',File.separatorChar);
			String lcwd = (new File(".")).getCanonicalPath();
			if((new File(_path)).isAbsolute()) return path;
			if(lcwd.endsWith(File.separator)) return lcwd + _path;
			return lcwd + File.separator + _path;
		} catch (IOException e) {
			// never happens
			throw new IllegalStateException();
		}
	}
	
	//--------------------------------------------------------------------------------
	/** Returns an array that contains relative paths of all directories that are
	 *  contained by <code>path</code>.<p>
	 *  For example: from path <em>a/b/c</em>, this method creates three path:<br>
	 *  <ul>
	 *  <li><em>a</em></li>
	 *  <li><em>a/b</em></li>
	 *  <li><em>a/b/c</em></li>
	 *  </ul> 
	 */
	private String[] createPrefixes(String path) {
		List<String> result = new ArrayList<String>();
		String[] parts = path.split("/");
		for (int i = 0;i < parts.length - 1;++i) {
			String res = "";
			for (int j = 0;j <= i;++j)
				res += parts[j] + "/";
			result.add(res.substring(0,res.length() - 1));
		}
		result.add(path);
		return result.toArray(new String[0]);
	}
	
	//================================================================================
	// nested classes
	
	/** This class stores authentication information of the SFTP connection. */
	private class NotInteractiveUserInfo implements UserInfo {

		//----------------------------------------------------------------------------
		public String getPassword() { return password; }
		
		//----------------------------------------------------------------------------
		public String getPassphrase() {	return passphrase; }
		
		//----------------------------------------------------------------------------
		public boolean promptPassphrase(String arg0) { return true;	}
		public boolean promptPassword(String arg0) { return true; }
		public boolean promptYesNo(String arg0) { return true;	}
		public void showMessage(String arg0) {}
	}
}
