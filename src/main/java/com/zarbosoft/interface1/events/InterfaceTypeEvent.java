package com.zarbosoft.interface1.events;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pidgoon.events.Event;

@Configuration(name = "type")
public class InterfaceTypeEvent implements InterfaceEvent {

	public InterfaceTypeEvent(final String string) {
		this.value = string;
	}

	public InterfaceTypeEvent() {
	}

	@Configuration
	public String value;

	@Override
	public boolean matches(final Event event) {
		return event.getClass() == getClass() && (value == null || value.equals(((InterfaceTypeEvent) event).value));
	}

	@Override
	public String toString() {
		return String.format("(%s)", value == null ? "*" : value);
	}
}
