package com.zarbosoft.interface1.events;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.interface1.Walk;
import com.zarbosoft.pidgoon.AbortParse;
import com.zarbosoft.pidgoon.Node;
import com.zarbosoft.pidgoon.events.*;
import com.zarbosoft.pidgoon.internal.Helper;
import com.zarbosoft.pidgoon.nodes.Reference;
import com.zarbosoft.pidgoon.nodes.Repeat;
import com.zarbosoft.pidgoon.nodes.Sequence;
import com.zarbosoft.pidgoon.nodes.Union;
import com.zarbosoft.rendaw.common.Pair;
import com.zarbosoft.rendaw.common.Tuple;
import org.reflections.Reflections;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.zarbosoft.rendaw.common.Common.uncheck;

public class ReadEventGrammar {
	public static Grammar buildGrammar(final Reflections reflections, final Walk.TypeInfo root) {
		final HashSet<Object> seen = new HashSet<>();
		final Grammar grammar = new Grammar();
		grammar.add("root", new Union().add(Walk.walk(reflections, root, new Walk.Visitor<Node>() {
			@Override
			public Node visitString(final Field field) {
				return new Operator(new MatchingEventTerminal(new InterfacePrimitiveEvent(null)), s -> {
					final InterfacePrimitiveEvent event = (InterfacePrimitiveEvent) s.top();
					return s.pushStack(event.value);
				});
			}

			@Override
			public Node visitInteger(final Field field) {
				return new Operator(new MatchingEventTerminal(new InterfacePrimitiveEvent(null)), s -> {
					final InterfacePrimitiveEvent event = (InterfacePrimitiveEvent) s.top();
					try {
						return s.pushStack(Integer.valueOf(event.value));
					} catch (final NumberFormatException e) {
						throw new AbortParse(e);
					}
				});
			}

			@Override
			public Node visitDouble(final Field field) {
				return new Operator(new MatchingEventTerminal(new InterfacePrimitiveEvent(null)), s -> {
					final InterfacePrimitiveEvent event = (InterfacePrimitiveEvent) s.top();
					try {
						return s.pushStack(Double.valueOf(event.value));
					} catch (final NumberFormatException e) {
						throw new AbortParse(e);
					}
				});
			}

			@Override
			public Node visitBoolean(final Field field) {
				return new Operator(new MatchingEventTerminal(new InterfacePrimitiveEvent(null)), s -> {
					final InterfacePrimitiveEvent event = (InterfacePrimitiveEvent) s.top();
					if (event.value.equals("true"))
						return s.pushStack(true);
					else if (event.value.equals("false"))
						return s.pushStack(false);
					else
						throw new AbortParse(String.format("Invalid value [%s]", event.value));
				});
			}

			@Override
			public Node visitEnum(final Field field, final Class<?> enumClass) {
				final Union union = new Union();
				Walk.enumValues(enumClass).forEach(pair -> {
					union.add(new Operator(new MatchingEventTerminal(new InterfacePrimitiveEvent(Walk.decideName(pair.second))),
							store -> store.pushStack(pair.first)
					));
				});
				return union;
			}

			@Override
			public Node visitList(final Field field, final Node inner) {
				return new Sequence()
						.add(new Operator(new MatchingEventTerminal(new InterfaceArrayOpenEvent()),
								s -> s.pushStack(0)
						))
						.add(new Repeat(new Operator(inner, s -> {
							Object temp = s.stackTop();
							s = (Store) s.popStack();
							Integer count = s.stackTop();
							s = (Store) s.popStack();
							return s.pushStack(temp).pushStack(count + 1);
						})))
						.add(new Operator(new MatchingEventTerminal(new InterfaceArrayCloseEvent()), s -> {
							final List out = new ArrayList();
							s = (Store) Helper.stackPopSingleList(s, out::add);
							Collections.reverse(out);
							return s.pushStack(out);
						}));
			}

			@Override
			public Node visitSet(final Field field, final Node inner) {
				return new Sequence()
						.add(new Operator(new MatchingEventTerminal(new InterfaceArrayOpenEvent()),
								s -> s.pushStack(0)
						))
						.add(new Repeat(new Operator(inner, s -> {
							Object temp = s.stackTop();
							s = (Store) s.popStack();
							Integer count = s.stackTop();
							s = (Store) s.popStack();
							return s.pushStack(temp).pushStack(count + 1);
						})))
						.add(new Operator(new MatchingEventTerminal(new InterfaceArrayCloseEvent()), s -> {
							final Set out = new HashSet();
							s = (Store) Helper.stackPopSingleList(s, (Consumer<Object>) out::add);
							return s.pushStack(out);
						}));
			}

			@Override
			public Node visitMap(final Field field, final Node inner) {
				return new Sequence()
						.add(new Operator(new MatchingEventTerminal(new InterfaceObjectOpenEvent()),
								s -> s.pushStack(0)
						))
						.add(new Repeat(new Sequence()
								.add(new Operator(new MatchingEventTerminal(new InterfaceKeyEvent(null)),
										store -> store.pushStack(((InterfaceKeyEvent) store.top()).value)
								))
								.add(new Operator(inner, Helper::stackDoubleElement))))
						.add(new Operator(new MatchingEventTerminal(new InterfaceObjectCloseEvent()), s -> {
							final Map out = new HashMap();
							s = (Store) Helper.<Pair<String, Object>>stackPopSingleList(s,
									p -> out.put(p.first, p.second)
							);
							return s.pushStack(out);
						}));
			}

			@Override
			public Node visitAbstract(
					final Field field, final Class<?> klass, final List<Pair<Class<?>, Node>> derived
			) {
				final Class<?> def;
				if (field != null)
					def = field.getAnnotation(Configuration.class).typeless();
				else
					def = null;
				final Tuple<Object> key =
						new Tuple<>(klass, derived.stream().map(p -> p.first).collect(Collectors.toSet()), def);
				if (!seen.contains(key)) {
					seen.add(key);
					final Set<String> subclassNames = new HashSet<>();
					final Union out = new Union();
					derived.stream().forEach(s -> {
						if (s.first.equals(def))
							out.add(s.second);
						out.add(new Sequence()
								.add(new MatchingEventTerminal(new InterfaceTypeEvent(Walk
										.decideName(s.first)
										.toLowerCase())))
								.add(s.second));
					});
					grammar.add(key, out);
				}
				return new Reference(key);
			}

			@Override
			public Node visitConcreteShort(final Field field, final Class<?> klass) {
				return new Reference(klass.getTypeName());
			}

			@Override
			public void visitConcrete(
					final Field field, final Class<?> klass, final List<Pair<Field, Node>> fields
			) {
				final Sequence seq = new Sequence();
				{
					seq.add(new Operator(new MatchingEventTerminal(new InterfaceObjectOpenEvent()),
							s -> s.pushStack(0)
					));
					final com.zarbosoft.pidgoon.nodes.Set set = new com.zarbosoft.pidgoon.nodes.Set();
					fields.forEach(f -> {
						set.add(new Operator(new Sequence()
								.add(new MatchingEventTerminal(new InterfaceKeyEvent(Walk.decideName(f.first))))
								.add(f.second), s -> {
							s = (Store) s.pushStack(f.first);
							return Helper.stackDoubleElement(s);
						}), fieldIsRequired(f.first));
					});
					seq.add(set);
					seq.add(new MatchingEventTerminal(new InterfaceObjectCloseEvent()));
				}
				final Node topNode;
				final List<Pair<Field, Node>> minimalFields2 =
						fields.stream().filter(f -> fieldIsRequired(f.first)).collect(Collectors.toList());
				final List<Pair<Field, Node>> minimalFields;
				if (minimalFields2.size() == 0)
					minimalFields = fields;
				else
					minimalFields = minimalFields2;
				if (minimalFields.size() == 1) {
					final Union temp = new Union();
					temp.add(seq);
					temp.add(new Operator(minimalFields.iterator().next().second, s -> {
						final Object value = s.stackTop();
						s = (Store) s.popStack();
						return s.pushStack(new Pair<>(value, minimalFields.iterator().next().first)).pushStack(1);
					}));
					topNode = temp;
				} else {
					topNode = seq;
				}
				grammar.add(klass.getTypeName(), new Operator(topNode, s -> {
					final Object out = uncheck(klass::newInstance);
					s = (Store) Helper.<Pair<Object, Field>>stackPopSingleList(s, (pair) -> {
						uncheck(() -> pair.second.set(out, pair.first));
					});
					return s.pushStack(out);
				}));
			}

			@Override
			public Node visitOther(final Field field, final Class<?> otherClass) {
				return new Operator(new MatchingEventTerminal(new MatchingEvent() {
					@Override
					public boolean matches(final MatchingEvent event) {
						return event instanceof InterfaceOtherEvent &&
								otherClass.isAssignableFrom(((InterfaceOtherEvent) event).value.getClass());
					}
				}), s -> {
					final InterfaceOtherEvent event = (InterfaceOtherEvent) s.top();
					return s.pushStack(event.value);
				});
			}
		})).add(new Operator(store -> store.pushStack(null))));
		return grammar;
	}

	private static boolean fieldIsRequired(final Field field) {
		if (Collection.class.isAssignableFrom(field.getType()))
			return false;
		if (Map.class.isAssignableFrom(field.getType()))
			return false;
		final Configuration annotation = field.getAnnotation(Configuration.class);
		if (annotation == null)
			return false;
		if (annotation.optional())
			return false;
		return true;
	}
}
