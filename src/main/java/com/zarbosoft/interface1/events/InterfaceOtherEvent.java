package com.zarbosoft.interface1.events;

import com.zarbosoft.interface1.Configuration;
import com.zarbosoft.pidgoon.events.MatchingEvent;
import com.zarbosoft.rendaw.common.DeadCode;

@Configuration(name = "other")
public class InterfaceOtherEvent implements InterfaceEvent {
	public InterfaceOtherEvent(final Object value) {
		this.value = value;
	}

	public InterfaceOtherEvent() {
	}

	@Configuration
	public Object value;

	@Override
	public boolean matches(final MatchingEvent event) {
		throw new DeadCode();
	}
}
