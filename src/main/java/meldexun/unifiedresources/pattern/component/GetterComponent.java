package meldexun.unifiedresources.pattern.component;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import meldexun.unifiedresources.pattern.ComponentMatch;

public class GetterComponent extends Component {

	private final String key;

	public GetterComponent(String key) {
		this.key = key;
	}

	@Override
	public void matches(CharSequence input, @Nullable ComponentMatch parent, int position, Map<String, String> table, List<ComponentMatch> matches) {
		String value = table.get(this.key);
		if (value == null) {
			return;
		}
		if (!StringComponent.matching(input, position, value)) {
			return;
		}
		matches.add(new ComponentMatch(parent, position, value.length()));
	}

}
