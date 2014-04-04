package ai.aitia.meme.paramsweep.platform.mason.recording.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@link Submodel} annotation should decorate members of a Simulation class (a model class). 
 * 
 * @author Rajmund Bocsi
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
@Inherited
public @interface Submodel {
	/**
	 * A list of classes that can be the dynamic type of the member. This list is the domain of the {@link Submodel} parameter. If the list is empty then
	 * the framework will try to find all descendant classes (or implementors if the field static type is an interface).    
	 */
	Class<?>[] value() default {};
}