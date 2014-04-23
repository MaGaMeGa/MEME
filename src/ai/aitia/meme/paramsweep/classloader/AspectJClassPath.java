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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javassist.ByteArrayClassPath;
import javassist.ClassPath;
import javassist.NotFoundException;

import org.apache.commons.io.IOUtils;
import org.aspectj.weaver.loadtime.ClassLoaderWeavingAdaptor;

import ai.aitia.meme.Logger;

/**
 * @author Tamás Máhr
 *
 */
public class AspectJClassPath implements ClassPath {

	String classPathString;
	ClassPath originalClassPath;
	ByteArrayClassPath newClassPath;
	ClassLoaderWeavingAdaptor weaver;
	
	public AspectJClassPath(final String classPathUrl, final ClassPath originalClassPath, final ClassLoaderWeavingAdaptor weaver){
		this.classPathString = classPathUrl;
		this.originalClassPath = originalClassPath;
		this.weaver = weaver;
	}
	
	/** {@inheritDoc} 
	 */
	@Override
	public InputStream openClassfile(String classname) throws NotFoundException {
		try {
			URL originalURL = originalClassPath.find(classname);
			if (originalURL == null){
				return null;
			}

			byte[] classBytes = IOUtils.toByteArray(originalClassPath.openClassfile(classname));
			
			try {
				if (! classname.startsWith("ai.aitia.meme")){
					weaver.weaveClass(classname, classBytes);
				}
			} catch (IOException e) {
				Logger.logExceptionCallStack(e);
				throw new RuntimeException(e);
			}
			
			return new ByteArrayInputStream(classBytes);
		} catch (IOException e) {
			throw new NotFoundException("Could not read the bytes of " + classname + " from " + originalClassPath, e);
		}
	}

	/** {@inheritDoc} 
	 */
	@Override
	public URL find(String classname) {
		return originalClassPath.find(classname);
	}

	/** {@inheritDoc} 
	 */
	@Override
	public void close() {
	}

	@Override
	public String toString() {
		return originalClassPath.toString();
	}
}
