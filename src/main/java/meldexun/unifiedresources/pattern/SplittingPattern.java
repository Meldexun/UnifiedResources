package meldexun.unifiedresources.pattern;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.PatternSyntaxException;

import com.google.common.collect.ImmutableList;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meldexun.unifiedresources.pattern.component.Component;
import meldexun.unifiedresources.pattern.component.Components;
import meldexun.unifiedresources.util.StringReader;

public class SplittingPattern<T> {

	private final List<Component> components;
	private final Function<T, CharSequence> inputPreprocessor;

	public SplittingPattern(List<Component> components, Function<T, CharSequence> inputPreprocessor) {
		this.components = ImmutableList.copyOf(components);
		this.inputPreprocessor = inputPreprocessor;
	}

	public static SplittingPattern<CharSequence> compile(String pattern) throws PatternSyntaxException {
		return SplittingPattern.compile(pattern, Function.identity());
	}

	public static <T> SplittingPattern<T> compile(String pattern, Function<T, CharSequence> inputPreprocessor) throws PatternSyntaxException {
		List<Component> components = new ObjectArrayList<>();
		StringReader reader = new StringReader(pattern);
		while (reader.hasNext()) {
			components.add(Components.parseNextComponent(reader));
		}
		return new SplittingPattern<>(components, inputPreprocessor);
	}

	private List<ComponentMatch> preliminaryMatches(CharSequence input, Map<String, String> variableMap) {
		List<ComponentMatch> prevMatches = new ObjectArrayList<>();
		List<ComponentMatch> nextMatches = new ObjectArrayList<>();
		prevMatches.add(null);
		for (Component component : this.components) {
			if (prevMatches.isEmpty())
				break;
			for (ComponentMatch parent : prevMatches) {
				component.matches(input, parent, parent != null ? parent.getEnd() : 0, variableMap, nextMatches);
			}
			prevMatches.clear();
			List<ComponentMatch> temp = prevMatches;
			prevMatches = nextMatches;
			nextMatches = temp;
		}
		return prevMatches;
	}

	private static boolean isValidMatch(CharSequence input, ComponentMatch match) {
		return match != null && match.getEnd() == input.length();
	}

	public List<PatternMatch> matches(T t) {
		return this.matches(t, Collections.emptyMap());
	}

	public List<PatternMatch> matches(T t, Map<String, String> variableMap) {
		CharSequence input = this.inputPreprocessor.apply(t);
		List<PatternMatch> matches = new ObjectArrayList<>();
		for (ComponentMatch match : this.preliminaryMatches(input, variableMap)) {
			if (!SplittingPattern.isValidMatch(input, match))
				continue;
			matches.add(new PatternMatch(input, match));
		}
		return matches;
	}

	public boolean hasAnyMatch(T t) {
		return this.hasAnyMatch(t, Collections.emptyMap());
	}

	public boolean hasAnyMatch(T t, Map<String, String> variableMap) {
		CharSequence input = this.inputPreprocessor.apply(t);
		for (ComponentMatch match : this.preliminaryMatches(input, variableMap)) {
			if (!SplittingPattern.isValidMatch(input, match))
				continue;
			return true;
		}
		return false;
	}

}
