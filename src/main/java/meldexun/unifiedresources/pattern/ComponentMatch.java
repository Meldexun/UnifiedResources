package meldexun.unifiedresources.pattern;

import javax.annotation.Nullable;

public class ComponentMatch {

	private final ComponentMatch parent;
	private final int position;
	private final int length;
	@Nullable
	private final String key;

	public ComponentMatch(@Nullable ComponentMatch parent, int position, int length) {
		this(parent, position, length, null);
	}

	public ComponentMatch(@Nullable ComponentMatch parent, int position, int length, String key) {
		this.parent = parent;
		this.position = position;
		this.length = length;
		this.key = key;
	}

	public ComponentMatch getParent() {
		return this.parent;
	}

	public int getStart() {
		return this.position;
	}

	public int getEnd() {
		return this.position + this.length;
	}

	public int getLength() {
		return this.length;
	}

	public String getKey() {
		return this.key;
	}

}
