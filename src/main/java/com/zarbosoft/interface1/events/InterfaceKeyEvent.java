package com.zarbosoft.interface1.events;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pidgoon.events.Event;

@Configuration(name = "key")
public class InterfaceKeyEvent implements InterfaceEvent {
	public InterfaceKeyEvent(final String string) {
		value = string;
	}

	public InterfaceKeyEvent() {
	}

	@Configuration
	public String value;

	@Override
	public boolean matches(final Event event) {
		return event.getClass() == getClass() && (value == null || value.equals(((InterfaceKeyEvent) event).value));
	}

	@Override
	public String toString() {
		return String.format("%s:", value == null ? "*" : value);
	}
}
