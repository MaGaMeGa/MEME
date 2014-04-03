package ai.aitia.testing.matcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;

public class NetLogoResultMatcher extends TypeSafeMatcher<File> {
	
	//====================================================================================================
	// members
	
    protected File fileToMatch;
    
    //====================================================================================================
	// methods

    //----------------------------------------------------------------------------------------------------
	public NetLogoResultMatcher(final File fileToMatch) {
    	super();
		this.fileToMatch = fileToMatch;
	}

	//----------------------------------------------------------------------------------------------------
	public void describeTo(final Description description) {
		description.appendText("result matches");
	}

	//----------------------------------------------------------------------------------------------------
	@Override
	public boolean matchesSafely(final File otherFile) {
		if (!(fileToMatch.exists() && fileToMatch.isFile() && otherFile.exists() && otherFile.isFile()))
			return false;
		
		try {
			if (fileToMatch.getCanonicalPath().equals(otherFile.getCanonicalPath())) 
				return true;
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		}
		
		BufferedReader readerOne = null;
		BufferedReader readerTwo = null;
		try {
			readerOne = new BufferedReader(new FileReader(fileToMatch));
			readerTwo = new BufferedReader(new FileReader(otherFile));
			String lineOne = null, lineTwo = null;
			while ((lineOne = readerOne.readLine()) != null & (lineTwo = readerTwo.readLine()) != null) { // one & is not a mistake
				if ((lineOne.trim().startsWith("\"Timestamp:") && lineTwo.trim().startsWith("\"Timestamp:")) ||
					(lineOne.trim().startsWith("\"End time:") && lineTwo.trim().startsWith("\"End time:")) ||
					lineOne.trim().equals(lineTwo.trim())) continue;
				return false;
			}
			return (lineOne == null && lineTwo == null);
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (final IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				if (readerOne != null)
					readerOne.close();
				if (readerTwo != null)
					readerTwo.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	//----------------------------------------------------------------------------------------------------
	@Factory
	public static <T> Matcher<File> equalsInResult(final File file) {
		return new NetLogoResultMatcher(file);
	}
}
