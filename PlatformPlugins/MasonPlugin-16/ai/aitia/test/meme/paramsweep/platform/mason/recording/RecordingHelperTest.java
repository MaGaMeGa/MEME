/**
 * 
 */
package ai.aitia.test.meme.paramsweep.platform.mason.recording;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import sim.engine.SimState;
import sim.util.Bag;
import ai.aitia.meme.paramsweep.batch.output.RecorderInfo;
import ai.aitia.meme.paramsweep.platform.mason.recording.RecordingHelper;
import ai.aitia.meme.paramsweep.platform.mason.recording.annotation.Recorder;
import ai.aitia.meme.paramsweep.platform.mason.recording.annotation.RecorderSource;
import ai.aitia.meme.paramsweep.platform.mason.recording.annotation.Recorder.RecordTime;
import ai.aitia.meme.paramsweep.utils.SimulationException;

/**
 * @author Tamás Máhr
 *
 */
public class RecordingHelperTest {

	@Recorder(value="recordingtest.txt", recordAt=RecordTime.END_OF_ITERATION, sources={"intValue", "doubleValue", "intArray", "doubleArray",
			"avg(doubleArray)", "sum(doubleArray)", "intMethod", "doubleMethod", "intArrayMethod", "doubleArrayMethod", "beanArrayDouble"})
	private static class TestModel extends SimState {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static final int ARRAY_LENGTH = 5;
		
		public int getArrayLength(){
			return ARRAY_LENGTH;
		}
		
		@RecorderSource("intValue")
		protected int intValue;
		
		@RecorderSource("doubleValue")
		protected double doubleValue;
		
		@RecorderSource(value = "intArray", collectionLength = ARRAY_LENGTH)
		protected int[] intArray = new int[ARRAY_LENGTH];
		
		@RecorderSource(value = "doubleArray", collectionLength = ARRAY_LENGTH)
		protected double[] doubleArray = new double[ARRAY_LENGTH];
		
		@RecorderSource("intMethod")
		protected int intMethod(){
			return intValue;
		}
		
		@RecorderSource("doubleMethod")
		protected double doubleMethod(){
			return doubleValue;
		}
		
		@RecorderSource(value="intArrayMethod", collectionLength=ARRAY_LENGTH)
		protected int[] intArrayMethod(){
			return intArray;
		}
		
		@RecorderSource(value="doubleArrayMethod", collectionLengthMember="getArrayLength()")
		protected double[] doubleArrayMethod(){
			return doubleArray;
		}
		
//		@RecorderSource(value="beanIntValue", innerType=TestBean.class, member="intValue")
//		protected TestBean bean = new TestBean();
		
//		@RecorderSource(value="beanArrayDouble", innerType=TestBean.class, member="doubleValue", collectionLengthMember="getArrayLength()")
//		protected Bag beanArray = new Bag();

//		protected TestBean[] beanArray = new TestBean[TestModel.ARRAY_LENGTH];

		@RecorderSource(value="beanArrayDouble", member="doubleValue", collectionLengthMember="getArrayLength()")
		protected List<TestBean> beanArray = new ArrayList<RecordingHelperTest.TestBean>();
		
//		@RecorderSource(value="beanIntMethod", innerType=TestBean.class, member="intMethod()")
//		protected TestBean getTestBean(){
//			return bean;
//		}
		
//		@RecorderSource(value="beanDoubleMethod", innerType=TestBean.class, member="doubleMethod()", collectionLength=ARRAY_LENGTH)
//		protected TestBean[] getTestBeanArray(){
//			return beanArray;
//		}
		
		public TestModel(long seed) {
			super(seed);
			
			for (int i = 0 ; i < ARRAY_LENGTH ; i++){
				beanArray.add(new TestBean());
				intArray[i] = i;
				doubleArray[i] = 2.0 * i;
			}
		}
	}
	
	private static class TestBean {
		public int intValue;
		
		public double doubleValue;
		
		public int[] intArray = new int[TestModel.ARRAY_LENGTH];
		
		public double[] doubleArray;
		
		public int intMethod(){
			return intValue;
		}
		
		public double doubleMethod(){
			return doubleValue;
		}
		
		public int[] intArrayMethod(){
			return intArray;
		}
		
		public double[] doubleArrayMethod(){
			return doubleArray;
		}
	}
	
	/**
	 * Test method for {@link ai.aitia.meme.paramsweep.platform.mason.recording.RecordingHelper#getRecorders(sim.engine.SimState)}.
	 */
	@Test
	public void testGetRecorders() {
		TestModel testModel = new TestModel(0);
		testModel.start();
		
		RecordingHelper.newInstance(testModel);
		
		List<RecorderInfo> recorders = RecordingHelper.getRecorders(testModel);
		
		assertEquals(1, recorders.size());
		
		RecorderInfo recorderInfo = recorders.get(0);
		
		assertEquals(testModel.getClass().getAnnotation(Recorder.class).sources().length, recorderInfo.getRecordables().size());
		
		RecordingHelper.getInstance().scheduleRecording(testModel);
		
		testModel.schedule.step(testModel);
		testModel.schedule.step(testModel);
		testModel.finish();
		
		RecordingHelper.getInstance().closeRecorder();
		
		File resultFile = recorderInfo.getOutputFile();
		
		assertTrue(resultFile.exists());
		
	}

}
