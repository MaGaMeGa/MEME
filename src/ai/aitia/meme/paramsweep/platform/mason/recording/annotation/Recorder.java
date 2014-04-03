/**
 * 
 */
package ai.aitia.meme.paramsweep.platform.mason.recording.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link Recorder} annotation should decorate a Simulation class (a model class). 
 * 
 * @author Tamás Máhr
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Recorder {
	/**
	 * The name of the output file.
	 */
	String value();
	
	/**
	 * The {@link RecordTime} enum lists the different options when specified values are recorded during a simulation. Values are not necessarily
	 * output to a file when recorded. When recorded values are written to a file is governed by the {@link Recorder#outputAt()} attribute.
	 * 
	 * @author Tamás Máhr
	 * 
	 */
	public enum RecordTime {
		/**
		 * Values are recorded only once at the end of the simulation.
		 */
		END_OF_RUN,

		/**
		 * Values are recordedat the end of each iteration (simulation step)
		 */
		END_OF_ITERATION,

		/**
		 * Recorded values are output at every nth simulation. n is set by the recordingInterval attribute.
		 */
		EVERY_NTH_ITERATION,
		
		/**
		 * Recorded values are output when the condition given in the condition attribute evaluates to true.
		 */
		CONDITIONAL;
		
		public String getMemeValue(){
			switch (this){
			case END_OF_RUN:
				return "RUN";
				
			case END_OF_ITERATION:
				return "ITERATION";
				
			case EVERY_NTH_ITERATION:
				return "ITERATION_INTERVAL";
				
			case CONDITIONAL:
				return "CONDITION";
				
			default:
				throw new IllegalStateException();
			}
		}
	}
	
	/**
	 * The {@link OutputTime} enum lists the different options when recorded values are written to a file.
	 * 
	 * @author Tamás Máhr
	 *
	 */
	public enum OutputTime {
		/**
		 * Recorded values are written to a file immediately after recording.
		 */
		SAME_AS_RECORD_TIME,

		/**
		 * Recorded values are written to a file only at the end of the simulation. If recording happens more frequently, recorded values are stored in memory.
		 */
		END_OF_RUN,

		/**
		 * Recorded values are written to a file at every nth iteration.
		 */
		EVERY_NTH_ITERATION;

		public String getMemeValue(){
			switch(this){
			case SAME_AS_RECORD_TIME:
				return "RECORD";
				
			case END_OF_RUN:
				return "RUN";
				
			case EVERY_NTH_ITERATION:
				return "ITERATION_INTERVAL";
				
			default:
				throw new IllegalStateException();
			}
		}
	}
	
	/**
	 * The time when values should be collected. It defaults to {@link RecordTime#END_OF_RUN}.
	 */
	RecordTime recordAt() default RecordTime.END_OF_RUN;
	
	/**
	 * The time when recorded values should be written to a file. It defaults to {@link OutputTime#SAME_AS_RECORD_TIME}.
	 */
	OutputTime outputAt() default OutputTime.SAME_AS_RECORD_TIME;
	
	/**
	 * If the recordAt attribute is {@link RecordTime#EVERY_NTH_ITERATION}, this attribute specifies n. It defaults to 1.0.
	 */
	int recordingInterval() default 1;
	
	/**
	 * If the outputAt attribute is {@link OutputTime#EVERY_NTH_ITERATION}, this attribute specifies n. It defaults to 1.0.
	 */
	int outputInterval() default 1;
	
	/**
	 * If the recordAt attribute is {@link RecordTime#CONDITIONAL}, this attribute specifies the condition. It should be a proper Java boolean expression, in the scope of the model.
	 */
	String condition() default "";
	
	/**
	 * A list of names that correspond to {@link RecorderSource} values. Please note, that you can record only such sources that are available from
	 * the model by object reference (not necessarily directly).
	 * <p>
	 * In the list, you can specify simply the name of a {@link RecorderSource} value, or you can specify an aggregate operation on the value in the
	 * form 'func(name)'. Currently, you can specify the following aggregate operations:
	 * <ul>
	 * <li>max(name)
	 * <li>mean(name)
	 * <li>median(name)
	 * <li>min(name)
	 * <li>product(name)
	 * <li>standardDeviation(name)
	 * <li>variance(name)
	 * </ul>
	 * 
	 * If an aggregate operation is specified, then the recorder expects a collection of values to be collected under the specified name. It can
	 * happen in two ways. Either the annotated method returns a collection of values, or there are a collection of objects available from the model
	 * with an annotated (field or method) value.
	 * 
	 */
	String[] sources() default {};
}
