package meldexun.unifiedresources.pattern.component;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import meldexun.unifiedresources.pattern.ComponentMatch;

public class StringComponent extends Component {

	private final String value;

	public StringComponent(String value) {
		this.value = value;
	}

	@Override
	public void matches(CharSequence input, @Nullable ComponentMatch parent, int position, Map<String, String> table, List<ComponentMatch> matches) {
		if (!StringComponent.matching(input, position, this.value)) {
			return;
		}
		matches.add(new ComponentMatch(parent, position, this.value.length()));
	}

	public static boolean matching(CharSequence a, int offset, CharSequence b) {
		if (a.length() - offset < b.length()) {
			return false;
		}
		for (int i = 0; i < b.length(); i++) {
			if (a.charAt(offset + i) != b.charAt(i)) {
				return false;
			}
		}
		return true;
	}

}
