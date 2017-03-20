package com.zarbosoft.interface1.path;

import com.zarbosoft.interface1.events.InterfaceArrayOpenEvent;
import com.zarbosoft.interface1.events.InterfaceEvent;
import com.zarbosoft.interface1.events.InterfaceObjectOpenEvent;
import com.zarbosoft.rendaw.common.DeadCode;

public class InterfaceRootPath extends InterfacePath {

	public InterfaceRootPath() {
	}

	@Override
	public InterfacePath value() {
		throw new DeadCode();
	}

	@Override
	public InterfacePath key(final String data) {
		throw new DeadCode();
	}

	@Override
	public InterfacePath type() {
		throw new DeadCode();
	}

	@Override
	public InterfacePath push(final InterfaceEvent e) {
		if (e.getClass() == InterfaceObjectOpenEvent.class) {
			return new InterfaceObjectPath(this);
		} else if (e.getClass() == InterfaceArrayOpenEvent.class) {
			return new InterfaceArrayPath(this);
		}
		return this;
	}

	@Override
	public String toString() {
		return "";
	}
}
