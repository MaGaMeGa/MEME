/**
 * 
 */
package ai.aitia.meme.paramsweep.platform.mason.recording.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link RecorderSource} annotation specifies that the annotated field or a method return value should be recorded by a recorder.
 * 
 * @author Tamás Máhr
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
@Inherited
public @interface RecorderSource {
	/**
	 * The name of the data source. This is used in the {@link Recorder} annotation to link the recorder to the data source, and this will be written
	 * into the output file as the column header for this data source.
	 */
	String value() default "";
	
	/**
	 * In case of a collection or array being annotated for recording, this attribute provides an upper limit on the length of the collection. The
	 * recorder file will contain this number of columns for this source. Either this or the {@link #collectionLengthMember()} attribute should be set
	 * when recording collections or arrays.
	 */
	int collectionLength() default 0;
	
	/**
	 * In case of a collection or array being annotated for recording, this attribute provides the name of a field or a method (ending in ()) in the
	 * class annotated with {@link Recorder}. This field or method is used by the framework to query the upper limit on the length of the collection.
	 * Note that it is queried only once after the creation of the object of the given class (supposedly the model). Either this or the
	 * {@link #collectionLength()} attribute should be set for recording collections or arrays. @see {@link #collectionLength()}.
	 */
	String collectionLengthMember() default "";
	
	/**
	 * In case of a collection or array being annotated for recording, this attribute provides a value to be printed for no data. It defaults to
	 * 'NaN', which is parsed as double. If you want integer values in the recorded files, specify an integer value here!
	 */
	String NAFiller() default "NaN";

	/**
	 * In case of a collection or array being annotated for recording, this attribute provides the type of the objects in the collection or array. It
	 * is necessary to provide this attribute only if the annotated type is not a generic type with a specified type parameter (e.g.
	 * {@literal List<Integer>} vs. Bag). Whenever possible, the framework will devise the inner type of generic types.
	 */
	Class<?> innerType() default Object.class;
	
	/**
	 * In case of a collection or array being annotated for recording, and the type of the objects in the collection or array is not a primitive type
	 * or String, this attribute provides the name of a public member to get the value from. If the value of this attribute ends in "()", it is
	 * interpreted as a method, otherwise as a field. If this attribute is not set, the class of the objects in the collection or array is scanned for
	 * {@link RecorderSource} annotated members.
	 */
	String member() default "";
}
