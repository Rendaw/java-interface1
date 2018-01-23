package com.zarbosoft.interface1;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotate classes and fields to walk with this.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Configuration {
	/**
	 * A name when serialized if different from the canonical class name.
	 *
	 * @return
	 */
	String name() default "";

	/**
	 * For fields. Not required when deserializing if not present in data.
	 *
	 * @return
	 */
	boolean optional() default false;

	/**
	 * For fields.
	 * For polymorphic types, serialize only these derived types.
	 *
	 * @return
	 */
	Class<?>[] include() default {};

	/**
	 * For fields.
	 * For polymorphic types, do not deserialize these derived types.
	 *
	 * @return
	 */
	Class<?>[] exclude() default {};

	/**
	 * For fields.
	 * The default type of serialized polymorphic data if a type is not explicitly specified.
	 *
	 * @return
	 */
	Class<?> typeless() default Void.class;
}
