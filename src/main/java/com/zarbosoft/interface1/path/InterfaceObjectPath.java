package com.zarbosoft.interface1.path;

public class InterfaceObjectPath extends InterfacePath {

	private String key;

	public InterfaceObjectPath(final InterfacePath parent) {
		this.parent = parent;
	}

	public InterfaceObjectPath(final InterfacePath parent, final String key) {
		this.parent = parent;
		this.key = key;
	}

	@Override
	public InterfacePath value() {
		return this;
	}

	@Override
	public InterfacePath key(final String data) {
		return new InterfaceObjectPath(parent, data);
	}

	@Override
	public InterfacePath type() {
		return this;
	}

	@Override
	public String toString() {
		return String.format("%s/%s", parent == null ? "" : parent.toString(), key == null ? "" : key);
	}
}
