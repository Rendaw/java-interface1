package com.zarbosoft.interface1.events;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pidgoon.events.Event;

@Configuration(name = "object-close")
public class InterfaceObjectCloseEvent implements InterfaceEvent {

	@Override
	public boolean matches(final Event event) {
		return event.getClass() == getClass();
	}

	@Override
	public String toString() {
		return String.format("OBJECT_CLOSE");
	}
}
