package meldexun.unifiedresources.pattern;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class PatternMatch {

	private final CharSequence input;
	private final ComponentMatch lastComponentMatch;

	public PatternMatch(CharSequence input, ComponentMatch lastComponentMatch) {
		this.input = input;
		this.lastComponentMatch = lastComponentMatch;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("PatternResult[");

		List<ComponentMatch> matchList = new ObjectArrayList<>();
		ComponentMatch current = this.lastComponentMatch;
		while (current != null) {
			matchList.add(0, current);
			current = current.getParent();
		}

		sb.append(matchList.stream()
				.map(m -> this.input.subSequence(m.getStart(), m.getEnd()))
				.collect(Collectors.joining(", ")));

		sb.append("]");
		return sb.toString();
	}

	public Map<String, String> computeVariableMap() {
		return PatternMatch.computeVariableMap(this.input, this.lastComponentMatch);
	}

	public Map<String, String> computeVariableMap(Supplier<Map<String, String>> generator) {
		return PatternMatch.computeVariableMap(this.input, this.lastComponentMatch, generator);
	}

	public static Map<String, String> computeVariableMap(CharSequence input, ComponentMatch lastComponentMatch) {
		return PatternMatch.computeVariableMap(input, lastComponentMatch, Object2ObjectOpenHashMap::new);
	}

	public static Map<String, String> computeVariableMap(CharSequence input, ComponentMatch lastComponentMatch, Supplier<Map<String, String>> generator) {
		Map<String, String> variableMap = null;
		ComponentMatch current = lastComponentMatch;
		while (current != null) {
			if (current.getKey() != null) {
				if (variableMap == null) {
					variableMap = generator.get();
				}
				variableMap.put(current.getKey(), input.subSequence(current.getStart(), current.getEnd())
						.toString());
			}
			current = current.getParent();
		}
		if (variableMap == null) {
			return Collections.emptyMap();
		}
		return variableMap;
	}

	public CharSequence getInput() {
		return this.input;
	}

	public ComponentMatch getLastComponentMatch() {
		return this.lastComponentMatch;
	}

}
