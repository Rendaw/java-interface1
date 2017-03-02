package com.zarbosoft.interface1;

import com.zarbosoft.rendaw.common.Pair;
import org.junit.Test;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class WalkTest {
	public static class BlankVisitor implements Walk.Visitor<String> {

		@Override
		public String visitString(final Field field) {
			throw new AssertionError("Wrong callback.");
		}

		@Override
		public String visitInteger(final Field field) {
			throw new AssertionError("Wrong callback.");
		}

		@Override
		public String visitDouble(final Field field) {
			throw new AssertionError("Wrong callback.");
		}

		@Override
		public String visitBoolean(final Field field) {
			throw new AssertionError("Wrong callback.");
		}

		@Override
		public String visitEnum(final Field field, final Class<?> enumClass) {
			throw new AssertionError("Wrong callback.");
		}

		@Override
		public String visitList(final Field field, final String inner) {
			throw new AssertionError("Wrong callback.");
		}

		@Override
		public String visitSet(final Field field, final String inner) {
			throw new AssertionError("Wrong callback.");
		}

		@Override
		public String visitMap(final Field field, final String inner) {
			throw new AssertionError("Wrong callback.");
		}

		@Override
		public String visitAbstractShort(final Field field, final Class<?> klass) {
			throw new AssertionError("Wrong callback.");
		}

		@Override
		public String visitAbstract(
				final Field field, final Class<?> klass, final List<Pair<Class<?>, String>> derived
		) {
			throw new AssertionError("Wrong callback.");
		}

		@Override
		public String visitConcreteShort(final Field field, final Class<?> klass) {
			throw new AssertionError("Wrong callback.");
		}

		@Override
		public String visitConcrete(
				final Field field, final Class<?> klass, final List<Pair<Field, String>> fields
		) {
			throw new AssertionError("Wrong callback.");
		}
	}

	@Test
	public void testPrimitiveList() {
		assertEquals(
				"ok",
				Walk.walk(new Reflections("com.zarbosoft.interface1"), List.class, String.class, new BlankVisitor() {
					@Override
					public String visitString(final Field field) {
						return "string";
					}

					@Override
					public String visitList(final Field field, final String inner) {
						assertEquals("string", inner);
						return "ok";
					}
				})
		);
	}
}
