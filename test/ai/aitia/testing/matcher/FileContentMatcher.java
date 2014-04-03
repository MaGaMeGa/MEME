package ai.aitia.testing.matcher;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.junit.internal.matchers.TypeSafeMatcher;

public class FileContentMatcher extends TypeSafeMatcher<File> {
	
    protected File fileToMatch;

    public FileContentMatcher(File fileToMatch) {
    	super();
		this.fileToMatch = fileToMatch;
	}

	public void describeTo(Description description) {
		description.appendText("file content matches");
	}

	@Override
	public boolean matchesSafely(File otherFile) {
		if (!(fileToMatch.exists() && fileToMatch.isFile() && otherFile.exists() && otherFile.isFile()))
			return false;
		try {
			if (fileToMatch.getCanonicalPath().equals(otherFile.getCanonicalPath())) 
				return true;
		} catch (IOException e1) {
			e1.printStackTrace();
			return false;
		}
		BufferedInputStream streamOne = null;
		BufferedInputStream streamTwo = null;
		try {
			streamOne = new BufferedInputStream(new FileInputStream(fileToMatch));
			streamTwo = new BufferedInputStream(new FileInputStream(otherFile));
			int a = 0; int b = 0;
			while ((a = streamOne.read()) != -1 && (b = streamTwo.read()) != -1) {
				if (a != b) return false;
			}
			if (a == -1) b = streamTwo.read();
			if (a == b) return true;
			else return false;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				streamOne.close();
				streamTwo.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Factory
	public static <T> Matcher<File> equalsInFileContent(File f) {
		return new FileContentMatcher(f);
	}
}
