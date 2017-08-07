package com.zarbosoft.interface1;

import com.zarbosoft.interface1.events.InterfaceEvent;
import com.zarbosoft.interface1.events.ReadEventGrammar;
import com.zarbosoft.pidgoon.events.Grammar;
import com.zarbosoft.pidgoon.events.Parse;
import com.zarbosoft.rendaw.common.Pair;
import org.reflections.Reflections;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.stream.Stream;

public class Events {
	public static <T> Stream<T> parseMultiple(
			final Reflections reflections,
			final Walk.TypeInfo typeInfo,
			final Stream<Pair<? extends InterfaceEvent, Object>> data
	) {
		final HashSet<Type> seen = new HashSet<>();
		final Grammar grammar = ReadEventGrammar.buildGrammar(reflections, typeInfo);
		return new Parse<T>().grammar(grammar).errorHistory(5).parseMultiple(data.map(pair -> pair));
	}

	public static <T> T parse(
			final Reflections reflections,
			final Walk.TypeInfo typeInfo,
			final Stream<Pair<? extends InterfaceEvent, Object>> data
	) {
		final HashSet<Type> seen = new HashSet<>();
		final Grammar grammar = ReadEventGrammar.buildGrammar(reflections, typeInfo);
		return new Parse<T>().grammar(grammar).errorHistory(5).parse(data.map(pair -> pair));
	}
}
