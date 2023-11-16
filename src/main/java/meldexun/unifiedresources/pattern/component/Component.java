package meldexun.unifiedresources.pattern.component;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import meldexun.unifiedresources.pattern.ComponentMatch;

public abstract class Component {

	public abstract void matches(CharSequence input, @Nullable ComponentMatch parent, int position, Map<String, String> table, List<ComponentMatch> matches);

}
