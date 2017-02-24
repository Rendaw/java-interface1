package com.zarbosoft.interface1;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.zarbosoft.rendaw.common.Pair;
import org.reflections.Reflections;

import java.lang.reflect.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.uncheck;
import static java.util.Arrays.stream;

public class Walk {
	public static String decideName(final Field field) {
		final Configuration annotation = field.getAnnotation(Configuration.class);
		if (annotation == null || annotation.name().equals(""))
			return field.getName();
		return annotation.name();
	}

	public static String decideName(final Class<?> klass) {
		final Configuration annotation = klass.getAnnotation(Configuration.class);
		if (annotation == null || annotation.name().equals(""))
			return klass.getName();
		return annotation.name();
	}

	public static class TypeInfo {

		public final Type inner;
		public final Type generic;
		public final Field field;

		public TypeInfo(final Type target) {
			if (target instanceof ParameterizedType) {
				this.generic = target;
				this.inner = ((ParameterizedType) target).getRawType();
			} else {
				this.inner = target;
				this.generic = null;
			}
			this.field = null;
		}

		public TypeInfo(final Field f) {
			this.field = f;
			this.inner = f.getType();
			this.generic = f.getGenericType();
		}
	}

	public interface Visitor<T> {

		T visitString(Field field);

		T visitInteger(Field field);

		T visitDouble(Field field);

		T visitBoolean(Field field);

		T visitEnum(Field field, Class<?> enumClass);

		T visitList(Field field, T inner);

		T visitSet(Field field, T inner);

		T visitMap(Field field, T inner);

		/**
		 * If this returns null, derived types will not be visited and visitAbstract will not be called.
		 *
		 * @param field
		 * @param klass
		 * @return
		 */
		T visitAbstractShort(Field field, Class<?> klass);

		T visitAbstract(Field field, Class<?> klass, List<Pair<Class<?>, T>> derived);

		/**
		 * If this returns null, fields will not be visited and visitConcrete will not be called.
		 *
		 * @param field
		 * @param klass
		 * @return
		 */
		T visitConcreteShort(Field field, Class<?> klass);

		T visitConcrete(Field field, Class<?> klass, List<Pair<Field, T>> fields);
	}

	public static <T> T walk(final Reflections reflections, final Class<?> root, final Visitor<T> visitor) {
		return implementationForType(reflections, new TypeInfo(root), visitor);
	}

	public static List<Pair<Enum<?>, Field>> enumValues(final Class<?> enumClass) {
		return stream(enumClass.getEnumConstants()).map(prevalue -> {
			final Enum<?> value = (Enum<?>) prevalue;
			final Field field = uncheck(() -> enumClass.getField(value.name()));
			return new Pair<Enum<?>, Field>(value, field);
		}).collect(Collectors.toList());
	}

	private static <T> T implementationForType(
			final Reflections reflections, final TypeInfo target, final Visitor<T> visitor
	) {
		if (target.inner == String.class) {
			return visitor.visitString(target.field);
		} else if ((target.inner == int.class) || (target.inner == Integer.class)) {
			return visitor.visitInteger(target.field);
		} else if ((target.inner == double.class) || (target.inner == Double.class)) {
			return visitor.visitDouble(target.field);
		} else if ((target.inner == boolean.class) || (target.inner == Boolean.class)) {
			return visitor.visitBoolean(target.field);
		} else if (((Class<?>) target.inner).isEnum()) {
			return visitor.visitEnum(target.field, (Class<?>) target.inner);
		} else if (List.class.isAssignableFrom((Class<?>) target.inner)) {
			if (target.generic == null)
				throw new AssertionError("Unparameterized list!");
			final Type innerType = ((ParameterizedType) target.generic).getActualTypeArguments()[0];
			return visitor.visitList(target.field,
					implementationForType(reflections, new TypeInfo(innerType), visitor)
			);
		} else if (java.util.Set.class.isAssignableFrom((Class<?>) target.inner)) {
			if (target.generic == null)
				throw new AssertionError("Unparameterized set!");
			final Type innerType = ((ParameterizedType) target.generic).getActualTypeArguments()[0];
			return visitor.visitSet(target.field, implementationForType(reflections, new TypeInfo(innerType), visitor));
		} else if (Map.class.isAssignableFrom((Class<?>) target.inner)) {
			if (target.generic == null)
				throw new AssertionError("Unparameterized map!");
			if (((ParameterizedType) target.generic).getActualTypeArguments()[0] != String.class)
				throw new AssertionError("Configurable maps must have String keys.");
			final Type innerType = ((ParameterizedType) target.generic).getActualTypeArguments()[1];
			return visitor.visitMap(target.field, implementationForType(reflections, new TypeInfo(innerType), visitor));
		} else if (((Class<?>) target.inner).getAnnotation(Configuration.class) != null) {
			if (((Class<?>) target.inner).isInterface() ||
					Modifier.isAbstract(((Class<?>) target.inner).getModifiers())) {
				final T out = visitor.visitAbstractShort(target.field, (Class<?>) target.inner);
				if (out != null)
					return out;
				final java.util.Set<String> subclassNames = new HashSet<>();
				return visitor.visitAbstract(target.field,
						(Class<?>) target.inner,
						Sets
								.difference(reflections.getSubTypesOf((Class<?>) target.inner), ImmutableSet.of(target))
								.stream()
								.map(s -> (Class<?>) s)
								.filter(s -> !Modifier.isAbstract(s.getModifiers()))
								.map(s -> {
									String name = decideName(s);
									if (subclassNames.contains(name))
										throw new IllegalArgumentException(String.format(
												"Specific type [%s] of polymorphic type [%s] is ambiguous.",
												name,
												target.inner
										));
									subclassNames.add(name);
									return new Pair<Class<?>, T>(s,
											(T) implementationForType(reflections, new TypeInfo(s), visitor)
									);
								})
								.collect(Collectors.toList())
				);
			} else {
				final Constructor<?> constructor;
				try {
					constructor = ((Class<?>) target.inner).getConstructor();
				} catch (final NoSuchMethodException e) {
					throw new AssertionError(String.format(
							"Class [%s] of field marked for serialization has no nullary constructor or constructor is not public.",
							target.inner
					));
				}
				final T out = visitor.visitConcreteShort(target.field, (Class<?>) target.inner);
				if (out != null)
					return out;
				final java.util.Set<Field> fields = new HashSet<>();
				Class<?> level = (Class<?>) target.inner;
				while (level.getSuperclass() != null) {
					stream(level.getDeclaredFields())
							.filter(f -> f.getAnnotation(Configuration.class) != null)
							.forEach(f -> {
								if ((f.getModifiers() & Modifier.PUBLIC) == 0)
									throw new AssertionError(String.format("Field %s marked for serialization is not public.",
											f
									));
								fields.add(f);
							});
					level = level.getSuperclass();
				}
				return visitor.visitConcrete(target.field, (Class<?>) target.inner, fields.stream().map(f -> {
					return new Pair<>(f, implementationForType(reflections, new TypeInfo(f), visitor));
				}).collect(Collectors.toList()));
			}
		}
		throw new AssertionError(String.format("Unconfigurable field of type or derived type [%s]", target.inner));
	}
}