package com.zarbosoft.interface1.path;

import com.zarbosoft.interface1.events.*;

/**
 * A class to describe paths in a document describing a three of Java objects.
 */
public abstract class InterfacePath {

	public InterfacePath parent;

	public abstract InterfacePath value();

	public abstract InterfacePath key(String data);

	public abstract InterfacePath type();

	public InterfacePath pop() {
		return parent;
	}

	public InterfacePath push(final InterfaceEvent e) {
		if (e.getClass() == InterfaceArrayOpenEvent.class) {
			return new InterfaceArrayPath(value());
		} else if (e.getClass() == InterfaceArrayCloseEvent.class) {
			return pop();
		} else if (e.getClass() == InterfaceObjectOpenEvent.class) {
			return new InterfaceObjectPath(value());
		} else if (e.getClass() == InterfaceObjectCloseEvent.class) {
			return pop();
		} else if (e.getClass() == InterfaceKeyEvent.class) {
			return key(((InterfaceKeyEvent) e).value);
		} else if (e.getClass() == InterfaceTypeEvent.class) {
			return type();
		} else if (e.getClass() == InterfacePrimitiveEvent.class) {
			return value();
		} else if (e.getClass() == InterfaceOtherEvent.class) {
			return value();
		} else
			throw new AssertionError(String.format("Unknown luxem event type [%s]", e.getClass()));
	}
}
