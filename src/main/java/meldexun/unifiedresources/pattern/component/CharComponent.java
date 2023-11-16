package meldexun.unifiedresources.pattern.component;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import meldexun.unifiedresources.pattern.ComponentMatch;

public class CharComponent extends Component {

	private final char c;

	public CharComponent(char c) {
		this.c = c;
	}

	@Override
	public void matches(CharSequence input, @Nullable ComponentMatch parent, int position, Map<String, String> table, List<ComponentMatch> matches) {
		if (input.length() - position < 1) {
			return;
		}
		if (input.charAt(position) != this.c) {
			return;
		}
		matches.add(new ComponentMatch(parent, position, 1));
	}

}
