package com.zarbosoft.interface1.events;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pidgoon.events.MatchingEvent;

@Configuration(name = "primitive")
public class InterfacePrimitiveEvent implements InterfaceEvent {
	public InterfacePrimitiveEvent(final String value) {
		this.value = value;
	}

	public InterfacePrimitiveEvent() {
	}

	@Configuration
	public String value;

	@Override
	public boolean matches(final MatchingEvent event) {
		return event.getClass() == getClass() &&
				(value == null || value.equals(((InterfacePrimitiveEvent) event).value));
	}

	@Override
	public String toString() {
		return String.format("%s", value == null ? "*" : value);
	}
}
