package com.zarbosoft.interface1.path;

public class InterfaceArrayPath extends InterfacePath {

	private boolean type = false;
	private int index = -1;

	public InterfaceArrayPath(final InterfacePath parent) {
		this.parent = parent;
	}

	public InterfaceArrayPath(final InterfacePath parent, final boolean type, final int index) {
		this.parent = parent;
		this.type = type;
		this.index = index;
	}

	@Override
	public InterfacePath value() {
		if (this.type)
			return new InterfaceArrayPath(parent, false, index);
		else
			return new InterfaceArrayPath(parent, false, index + 1);
	}

	@Override
	public InterfacePath key(final String data) {
		return this;
	}

	@Override
	public InterfacePath type() {
		return new InterfaceArrayPath(parent, true, index + 1);
	}

	@Override
	public String toString() {
		return String.format("%s/%s",
				parent == null ? "" : parent.toString(),
				index == -1 ? "" : ((Integer) index).toString()
		);
	}
}
