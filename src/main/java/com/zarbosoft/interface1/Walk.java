package com.zarbosoft.interface1;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.zarbosoft.rendaw.common.Common;
import com.zarbosoft.rendaw.common.Pair;
import org.reflections.Reflections;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	public static String decideEnumName(final Enum value) {
		return decideName(uncheck(() -> value.getClass().getField(value.name())));
	}

	public static boolean required(final Field field) {
		final Configuration annotation = field.getAnnotation(Configuration.class);
		return !annotation.optional();
	}

	public static List<Pair<Enum<?>, Field>> enumValues(final Class<?> enumClass) {
		return stream(enumClass.getEnumConstants()).map(prevalue -> {
			final Enum<?> value = (Enum<?>) prevalue;
			final Field field = uncheck(() -> enumClass.getField(value.name()));
			return new Pair<Enum<?>, Field>(value, field);
		}).collect(Collectors.toList());
	}

	private static Stream<Field> getFields(final Class<?> klass) {
		class FieldIterator implements Iterator<Class<?>> {
			private Class<?> at;

			public FieldIterator(final Class<?> klass) {
				at = klass;
			}

			@Override
			public boolean hasNext() {
				return at != null;
			}

			@Override
			public Class<?> next() {
				final Class<?> out = at;
				at = at.getSuperclass();
				return out;
			}
		}
		return Common
				.stream(new FieldIterator(klass))
				.flatMap(klass2 -> stream(klass2.getDeclaredFields()))
				.filter(f -> f.getAnnotation(Configuration.class) != null)
				.map(f -> {
					if ((f.getModifiers() & Modifier.PUBLIC) == 0)
						throw new AssertionError(String.format("Field %s marked for serialization is not public.", f));
					return f;
				});
	}

	public static class TypeInfo {

		public final Type type;
		public final TypeInfo[] parameters;
		public final Field field;

		public TypeInfo(final Type target) {
			if (target instanceof ParameterizedType) {
				this.type = ((ParameterizedType) target).getRawType();
				parameters = stream(((ParameterizedType) target).getActualTypeArguments())
						.map(type1 -> new TypeInfo(type1))
						.toArray(TypeInfo[]::new);
			} else {
				this.type = target;
				this.parameters = null;
			}
			this.field = null;
		}

		public TypeInfo(final Type type, final TypeInfo... parameter) {
			this.type = type;
			this.parameters = parameter;
			this.field = null;
		}

		public TypeInfo(final Field f) {
			this.field = f;
			this.type = f.getType();
			if (f.getGenericType() instanceof ParameterizedType)
				this.parameters = stream(((ParameterizedType) f.getGenericType()).getActualTypeArguments())
						.map(type1 -> new TypeInfo(type1))
						.toArray(TypeInfo[]::new);
			else
				this.parameters = null;
		}
	}

	private static class Context<T> {
		public final Reflections reflections;
		public final Visitor<T> visitor;
		public Set<Class<?>> seen = new HashSet<>();

		private Context(final Reflections reflections, final Visitor<T> visitor) {
			this.reflections = reflections;
			this.visitor = visitor;
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

		T visitAbstract(Field field, Class<?> klass, List<Pair<Class<?>, T>> derived);

		T visitConcreteShort(Field field, Class<?> klass);

		void visitConcrete(Field field, Class<?> klass, List<Pair<Field, T>> fields);

		default T visitOther(final Field field, final Class<?> otherClass) {
			throw new AssertionError(String.format("Uninterfacable field of type or derived type [%s]", otherClass));
		}
	}

	public interface ObjectVisitor {

		void visitString(String value);

		void visitInteger(Integer value);

		void visitDouble(Double value);

		void visitBoolean(Boolean value);

		void visitEnum(Enum value);

		void visitListStart(List value);

		void visitListEnd(List value);

		void visitSetStart(Set value);

		void visitSetEnd(Set value);

		void visitMapStart(Map value);

		void visitKeyBegin(String key);

		void visitKeyEnd(String key);

		void visitMapEnd(Map value);

		boolean visitAbstractBegin(Class<?> klass, Object value);

		void visitAbstractEnd(Class<?> klass, Object value);

		boolean visitConcreteBegin(Class<?> klass, Object value);

		void visitFieldBegin(Field field, Object value);

		void visitFieldEnd(Field field, Object value);

		void visitConcreteEnd(Class<?> klass, Object value);

		default void visitOther(final Object value) {
			throw new AssertionError(String.format("Uninterfacable field of type or derived type [%s]",
					value.getClass()
			));
		}
	}

	public static <T> T walk(
			final Reflections reflections, final TypeInfo root, final Visitor<T> visitor
	) {
		return implementationForType(new Context<>(reflections, visitor), root);
	}

	private static <T> T implementationForType(
			final Context<T> context, final TypeInfo target
	) {
		if (target.type == String.class) {
			return context.visitor.visitString(target.field);
		} else if ((target.type == int.class) || (target.type == Integer.class)) {
			return context.visitor.visitInteger(target.field);
		} else if ((target.type == double.class) || (target.type == Double.class)) {
			return context.visitor.visitDouble(target.field);
		} else if ((target.type == boolean.class) || (target.type == Boolean.class)) {
			return context.visitor.visitBoolean(target.field);
		} else if (((Class<?>) target.type).isEnum()) {
			return context.visitor.visitEnum(target.field, (Class<?>) target.type);
		} else if (List.class.isAssignableFrom((Class<?>) target.type)) {
			if (target.parameters == null)
				throw new AssertionError("Unparameterized list!");
			return context.visitor.visitList(target.field, implementationForType(context, target.parameters[0]));
		} else if (java.util.Set.class.isAssignableFrom((Class<?>) target.type)) {
			if (target.parameters == null)
				throw new AssertionError("Unparameterized set!");
			return context.visitor.visitSet(target.field, implementationForType(context, target.parameters[0]));
		} else if (Map.class.isAssignableFrom((Class<?>) target.type)) {
			if (target.parameters == null)
				throw new AssertionError("Unparameterized map!");
			if (target.parameters.length != 2)
				throw new AssertionError("Map does not have exactly 2 parameters!");
			if (target.parameters[0].type != String.class)
				throw new AssertionError("Interfacable maps must have String keys.");
			return context.visitor.visitMap(target.field, implementationForType(context, target.parameters[1]));
		} else if (((Class<?>) target.type).getAnnotation(Configuration.class) != null) {
			if (((Class<?>) target.type).isInterface() ||
					Modifier.isAbstract(((Class<?>) target.type).getModifiers())) {
				final java.util.Set<String> subclassNames = new HashSet<>();
				return context.visitor.visitAbstract(target.field, (Class<?>) target.type, Sets
						.difference(context.reflections.getSubTypesOf((Class<?>) target.type),
								ImmutableSet.of(target)
						)
						.stream()
						.map(s -> (Class<?>) s)
						.filter(s -> !Modifier.isAbstract(s.getModifiers()))
						.filter(s -> s.getAnnotation(Configuration.class) != null)
						.map(s -> {
							String name = decideName(s);
							if (subclassNames.contains(name))
								throw new IllegalArgumentException(String.format(
										"Specific type [%s] of polymorphic type [%s] is ambiguous.",
										name,
										target.type
								));
							subclassNames.add(name);
							return new Pair<Class<?>, T>(s, (T) implementationForType(context, new TypeInfo(s)));
						})
						.collect(Collectors.toList()));
			} else {
				final Constructor<?> constructor;
				try {
					constructor = ((Class<?>) target.type).getConstructor();
				} catch (final NoSuchMethodException e) {
					throw new AssertionError(String.format(
							"Interface class [%s] has no nullary constructor or constructor is not public (maybe the class isn't static).",
							target.type
					));
				}
				if (!context.seen.contains(target.type)) {
					context.seen.add((Class<?>) target.type);
					context.visitor.visitConcrete(target.field,
							(Class<?>) target.type,
							getFields((Class<?>) target.type).map(f -> {
								return new Pair<>(f, implementationForType(context, new TypeInfo(f)));
							}).collect(Collectors.toList())
					);
				}
				return context.visitor.visitConcreteShort(target.field, (Class<?>) target.type);
			}
		}
		return context.visitor.visitOther(target.field, (Class<?>) target.type);
	}

	public static void walk(final TypeInfo target, final Object value, final ObjectVisitor visitor) {
		if (target.type == String.class) {
			visitor.visitString((String) value);
		} else if ((target.type == int.class) || (target.type == Integer.class)) {
			visitor.visitInteger((Integer) value);
		} else if ((target.type == double.class) || (target.type == Double.class)) {
			visitor.visitDouble((Double) value);
		} else if ((target.type == boolean.class) || (target.type == Boolean.class)) {
			visitor.visitBoolean((Boolean) value);
		} else if (((Class<?>) target.type).isEnum()) {
			visitor.visitEnum((Enum) value);
		} else if (List.class.isAssignableFrom((Class<?>) target.type)) {
			if (target.parameters == null)
				throw new AssertionError("Unparameterized list!");
			visitor.visitListStart((List) value);
			for (final Object subvalue : (List<?>) value) {
				walk(target.parameters[0], subvalue, visitor);
			}
			visitor.visitListEnd((List) value);
		} else if (java.util.Set.class.isAssignableFrom((Class<?>) target.type)) {
			if (target.parameters == null)
				throw new AssertionError("Unparameterized set!");
			visitor.visitSetStart((Set) value);
			for (final Object subvalue : (Set<?>) value) {
				walk(target.parameters[0], subvalue, visitor);
			}
			visitor.visitSetEnd((Set) value);
		} else if (Map.class.isAssignableFrom((Class<?>) target.type)) {
			if (target.parameters == null)
				throw new AssertionError("Unparameterized map!");
			if (target.parameters[0].type != String.class)
				throw new AssertionError("Interfacable maps must have String keys.");
			visitor.visitMapStart((Map) value);
			for (final Map.Entry<String, ?> subvalue : ((Map<String, ?>) value).entrySet()) {
				visitor.visitKeyBegin(subvalue.getKey());
				walk(target.parameters[1], subvalue.getValue(), visitor);
				visitor.visitKeyEnd(subvalue.getKey());
			}
			visitor.visitMapEnd((Map) value);
		} else if (((Class<?>) target.type).getAnnotation(Configuration.class) != null) {
			if (((Class<?>) target.type).isInterface() ||
					Modifier.isAbstract(((Class<?>) target.type).getModifiers())) {
				final boolean enter = visitor.visitAbstractBegin((Class<?>) target.type, value);
				if (enter) {
					walk(new TypeInfo(value.getClass()), value, visitor);
					visitor.visitAbstractEnd((Class<?>) target.type, value);
				}
			} else {
				final boolean enter = visitor.visitConcreteBegin((Class<?>) target.type, value);
				if (enter) {
					getFields((Class<?>) target.type).forEach(field -> {
						final Object subvalue = uncheck(() -> field.get(value));
						visitor.visitFieldBegin(field, subvalue);
						walk(new TypeInfo(field), subvalue, visitor);
						visitor.visitFieldEnd(field, subvalue);
					});
					visitor.visitConcreteEnd((Class<?>) target.type, value);
				}
			}
		} else
			visitor.visitOther(value);
	}

	public static class DefaultVisitor<T> implements Visitor<T> {
		@Override
		public T visitString(final Field field) {
			return null;
		}

		@Override
		public T visitInteger(final Field field) {
			return null;
		}

		@Override
		public T visitDouble(final Field field) {
			return null;
		}

		@Override
		public T visitBoolean(final Field field) {
			return null;
		}

		@Override
		public T visitEnum(final Field field, final Class<?> enumClass) {
			return null;
		}

		@Override
		public T visitList(final Field field, final T inner) {
			return null;
		}

		@Override
		public T visitSet(final Field field, final T inner) {
			return null;
		}

		@Override
		public T visitMap(final Field field, final T inner) {
			return null;
		}

		@Override
		public T visitAbstract(
				final Field field, final Class<?> klass, final List<Pair<Class<?>, T>> derived
		) {
			return null;
		}

		@Override
		public T visitConcreteShort(final Field field, final Class<?> klass) {
			return null;
		}

		@Override
		public void visitConcrete(
				final Field field, final Class<?> klass, final List<Pair<Field, T>> fields
		) {
		}

		@Override
		public T visitOther(final Field field, final Class<?> otherClass) {
			return null;
		}
	}
}