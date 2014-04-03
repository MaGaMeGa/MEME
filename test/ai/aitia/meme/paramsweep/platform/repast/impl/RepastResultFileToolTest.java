package ai.aitia.meme.paramsweep.platform.repast.impl;

import static ai.aitia.testing.matcher.FileContentMatcher.equalsInFileContent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Test;

import ai.aitia.meme.paramsweep.batch.param.ParameterInfo;

public class RepastResultFileToolTest {

	
	@BeforeClass
	public static void deleteExistingGeneratedFiles() throws URISyntaxException {
		File result = new File(RepastResultFileToolTest.class.getResource("/result.txt").toURI());
		File genHeader = new File(result.getParentFile(), "header_gen.txt");
		File genColumns = new File(result.getParentFile(), "columns_gen.txt");
		File genData = new File(result.getParentFile(), "data_gen.txt");
		File genFooter = new File(result.getParentFile(), "footer_gen.txt");
		if (genHeader.exists()) genHeader.delete();
		if (genColumns.exists()) genColumns.delete();
		if (genData.exists()) genData.delete();
		if (genFooter.exists()) genFooter.delete();
	}
	
	@Test
	public void testSplitFileHeader() throws URISyntaxException, IOException {
		File result = new File(this.getClass().getResource("/result.txt").toURI());
		File header = new File(this.getClass().getResource("/header.txt").toURI());
		File generatedHeader = new File(result.getParentFile(), "header_gen.txt");
		RepastResultFileTool fileTool = new RepastResultFileTool();
		fileTool.splitFile(result, true, generatedHeader, false, null, false, null, false, null, 0);
		assertThat(header, equalsInFileContent(generatedHeader));
	}
	
	@Test
	public void testSplitFileColumns() throws URISyntaxException, IOException {
		File result = new File(this.getClass().getResource("/result.txt").toURI());
		File columns = new File(this.getClass().getResource("/columns.txt").toURI());
		File generatedColumns = new File(result.getParentFile(), "columns_gen.txt");
		RepastResultFileTool fileTool = new RepastResultFileTool();
		fileTool.splitFile(result, false, null, true, generatedColumns, false, null, false, null, 15);
		assertThat(columns, equalsInFileContent(generatedColumns));
	}
	@Test
	public void testSplitFileData() throws URISyntaxException, IOException {
		File result = new File(this.getClass().getResource("/result.txt").toURI());
		File data = new File(this.getClass().getResource("/data.txt").toURI());
		File generatedData = new File(result.getParentFile(), "data_gen.txt");
		RepastResultFileTool fileTool = new RepastResultFileTool();
		fileTool.splitFile(result, false, null, false, null, true, generatedData, false, null, 15);
		assertThat(data, equalsInFileContent(generatedData));
	}
	@Test
	public void testSplitFileFooter() throws URISyntaxException, IOException {
		File result = new File(this.getClass().getResource("/result.txt").toURI());
		File footer = new File(this.getClass().getResource("/footer.txt").toURI());
		File generatedFooter = new File(result.getParentFile(), "footer_gen.txt");
		RepastResultFileTool fileTool = new RepastResultFileTool();
		fileTool.splitFile(result, false, null, false, null, false, null, true, generatedFooter, 0);
		assertThat(footer, equalsInFileContent(generatedFooter));
	}

	@Test
	public void testSplitFileOnlyData() throws URISyntaxException, FileNotFoundException, IOException {
		File result = new File(this.getClass().getResource("/result.txt").toURI());
		File data = new File(this.getClass().getResource("/data.txt").toURI());
		File generatedData = new File(result.getParentFile(), "data_gen.txt");
		RepastResultFileTool fileTool = new RepastResultFileTool();
		fileTool.splitFileOnlyData(result, data, 15);
		assertThat(data, equalsInFileContent(generatedData));
	}
	
	@Test
	public void testGetHeader() {
		String newline = System.getProperty("line.separator");
		ParameterInfo<Boolean> constBool = new ParameterInfo<Boolean>("abcd", "this is a constant boolean parameter", Boolean.TRUE);
		constBool.setValue(Boolean.FALSE);
		ParameterInfo<Integer> constInt = new ParameterInfo<Integer>("efgh", "this is a constant int parameter", Integer.valueOf(-1));
		constInt.setValue(Integer.valueOf(123));
		ParameterInfo<Double> constDouble = new ParameterInfo<Double>("ijkl", "this is a constant double parameter", Double.valueOf(-1.11));
		constDouble.setValue(Double.valueOf(1.23));
		ParameterInfo<Integer> listInt = new ParameterInfo<Integer>("mnop", "this is a list int parameter", Integer.valueOf(-2));
		listInt.setValues(Integer.valueOf(5),Integer.valueOf(6),Integer.valueOf(7),Integer.valueOf(8));
		ArrayList<ParameterInfo> paramComb = new ArrayList<ParameterInfo>();
		paramComb.add(constBool);
		paramComb.add(constInt);
		paramComb.add(constDouble);
		paramComb.add(listInt);
		RepastResultFileTool fileTool = new RepastResultFileTool();
		long millis_1983_01_24_05_00_00 = 412228800000L;
		String generatedHeader = fileTool.getHeader(paramComb, millis_1983_01_24_05_00_00);
		StringBuilder sb = new StringBuilder();
		sb.append("Timestamp: 1983.01.24. 05:00:00");
		sb.append(newline);
		sb.append("abcd: false");
		sb.append(newline);
		sb.append("efgh: 123");
		sb.append(newline);
		sb.append("ijkl: 1.23");
		sb.append(newline);
		sb.append(newline);
		sb.append(newline);
		String expectedHeader = sb.toString();
		assertEquals(expectedHeader, generatedHeader);
	}

	@Test
	public void testGetFooter() {
		String newline = System.getProperty("line.separator");
		RepastResultFileTool fileTool = new RepastResultFileTool();
		long millis_1983_01_24_05_00_00 = 412228800000L;
		String generatedFooter = fileTool.getFooter(new ArrayList<ParameterInfo>(), millis_1983_01_24_05_00_00);
		StringBuilder sb = new StringBuilder();
		sb.append(newline);
		sb.append("Timestamp: 1983.01.24. 05:00:00");
		String expectedFooter = sb.toString();
		assertEquals(expectedFooter, generatedFooter);
	}

}
