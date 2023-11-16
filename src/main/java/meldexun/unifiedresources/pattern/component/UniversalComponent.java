package meldexun.unifiedresources.pattern.component;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import it.unimi.dsi.fastutil.chars.CharSet;
import meldexun.unifiedresources.pattern.ComponentMatch;

public class UniversalComponent extends Component {

	private final CharSet disallowedChars;

	public UniversalComponent(@Nullable CharSet disallowedChars) {
		this.disallowedChars = disallowedChars != null ? new CharOpenHashSet(disallowedChars) : null;
	}

	@Override
	public void matches(CharSequence input, @Nullable ComponentMatch parent, int position, Map<String, String> table, List<ComponentMatch> matches) {
		for (int l = 0; l <= input.length() - position; l++) {
			if (this.disallowedChars != null && l > 0 && this.disallowedChars.contains(input.charAt(position + l - 1)))
				break;
			matches.add(new ComponentMatch(parent, position, l));
		}
	}

}
