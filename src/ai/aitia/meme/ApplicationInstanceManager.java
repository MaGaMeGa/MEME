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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 *  Source: http://www.rbgrn.net/content/43-java-single-application-instance
 *  @author Robert Green
 *  Modified by Rajmund Bocsi
 */
public class ApplicationInstanceManager {
	
	//====================================================================================================
	// "members"

    private static ApplicationInstanceListener subListener;

    /** Randomly chosen, but static, high socket number */
    public static final int SINGLE_INSTANCE_NETWORK_SOCKET = MEMEApp.userPrefs.getInt(UserPrefs.SINGLE_INSTANCE_PORT,44331);

    /** Must end with newline */
    public static final String SINGLE_INSTANCE_SHARED_KEY = "$$NewMEMEInstance$$\n";
    
    //====================================================================================================
	// methods

    /**
     * Registers this instance of the application.
     *
     * @return true if first instance, false if not.
     */
    public static boolean registerInstance() {
    	// returnValueOnError should be true if lenient (allows app to run on network error) or false if strict.
    	final boolean returnValueOnError = true;
    	// try to open network socket
    	// if success, listen to socket for new instance message, return true
    	// if unable to open, connect to existing and send new instance message, return false
    	try {
    		final ServerSocket socket = new ServerSocket(SINGLE_INSTANCE_NETWORK_SOCKET,10,InetAddress.getLocalHost());
    		MEMEApp.logError("Listening for application instances on socket " + SINGLE_INSTANCE_NETWORK_SOCKET);
    		final Thread instanceListenerThread = new Thread(new Runnable() {
    
    			//====================================================================================================
    			// methods

    			//----------------------------------------------------------------------------------------------------
    			public void run() {
    				boolean socketClosed = false;
    				while (!socketClosed) {
    					if (socket.isClosed()) 
    						socketClosed = true;
    					else {
    						try {
    							final Socket client = socket.accept();
    							final BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
    							final String message = in.readLine();
    							if (SINGLE_INSTANCE_SHARED_KEY.trim().equals(message.trim())) {
    								MEMEApp.logError("Shared key matched - new application instance found");
    								fireNewInstance();
    							}
    							in.close();
    							client.close();
    						} catch (final IOException e) {
    							socketClosed = true;
    						}
    					}	
    				}
    			}
    		});
    		instanceListenerThread.setName("MEME-Single-Instance-Listener-Thread");
    		instanceListenerThread.start();
    		// listen
    	} catch (final UnknownHostException e) {
    		MEMEApp.logException(e);
    		return returnValueOnError;
    	} catch (final IOException e) {
    		try {
    			final Socket clientSocket = new Socket(InetAddress.getLocalHost(),SINGLE_INSTANCE_NETWORK_SOCKET);
    			final OutputStream out = clientSocket.getOutputStream();
    			out.write(SINGLE_INSTANCE_SHARED_KEY.getBytes());
    			out.close();
    			clientSocket.close();
    			return false;
    		} catch (final UnknownHostException e1) {
    			return returnValueOnError;
    		} catch (IOException e1) {
    			return returnValueOnError;
    		}
    	}
    	return true;
    }

    //----------------------------------------------------------------------------------------------------
	public static void setApplicationInstanceListener(final ApplicationInstanceListener listener) { subListener = listener; }

    //====================================================================================================
	// assistance methods
	
	//----------------------------------------------------------------------------------------------------
	private static void fireNewInstance() {
		if (subListener != null) 
			subListener.newInstanceCreated();
	}
	
	//----------------------------------------------------------------------------------------------------
	private ApplicationInstanceManager() {}
}
