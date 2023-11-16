package meldexun.unifiedresources.util;

public class StringReader {

	private final String s;
	private int p;

	public StringReader(String s) {
		this.s = s;
	}

	public boolean hasNext() {
		return this.p < this.s.length();
	}

	public char next() {
		return this.s.charAt(this.p++);
	}

	public char peek() {
		return this.s.charAt(this.p);
	}

	public String getString() {
		return this.s;
	}

	public int getPosition() {
		return this.p;
	}

}
