package meldexun.unifiedresources;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import meldexun.unifiedresources.pattern.PatternMatch;
import meldexun.unifiedresources.pattern.SplittingPattern;
import meldexun.unifiedresources.util.ItemTagHolder;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class UnificationRule {

	private final List<SplittingPattern<ResourceLocation>> originalTagFilters;
	private final List<SplittingPattern<Item>> originalItemFilters;
	private final List<SplittingPattern<Item>> replacementItemFilters;

	public UnificationRule(List<SplittingPattern<ResourceLocation>> originalTagFilters, List<SplittingPattern<Item>> originalItemFilters,
			List<SplittingPattern<Item>> replacementItemFilters) {
		this.originalTagFilters = ImmutableList.copyOf(originalTagFilters);
		this.originalItemFilters = ImmutableList.copyOf(originalItemFilters);
		this.replacementItemFilters = ImmutableList.copyOf(replacementItemFilters);
	}

	public static UnificationRule parseUnificationRule(JsonObject json) throws PatternSyntaxException {
		List<SplittingPattern<ResourceLocation>> originalTagFilters = ItemReplacer.parseTagFilters(json.getAsJsonArray("originalTagFilters"));
		List<SplittingPattern<Item>> originalItemFilters = ItemReplacer.parseItemFilters(json.getAsJsonArray("originalItemFilters"));
		List<SplittingPattern<Item>> replacementItemFilters = ItemReplacer.parseItemFilters(json.getAsJsonArray("replacementItemFilters"));
		return new UnificationRule(originalTagFilters, originalItemFilters, replacementItemFilters);
	}

	public static Item findReplacement(Item item, List<UnificationRule> rules, List<SplittingPattern<Item>> ignoredItemFilters,
			List<SplittingPattern<ResourceLocation>> ignoredTagFilters) {
		if (UnificationRule.isItemIgnored(item, ignoredItemFilters)) {
			return null;
		}
		List<ItemTagHolder> tags = UnificationRule.getNotIgnoredTags(item, ignoredTagFilters);
		if (tags.isEmpty()) {
			return null;
		}
		for (UnificationRule rule : rules) {
			Item r = rule.findReplacement(item, tags);
			if (r != null) {
				if (r == item) {
					return null;
				}
				return r;
			}
		}
		return null;
	}

	private static boolean isItemIgnored(Item item, List<SplittingPattern<Item>> ignoredItemFilters) {
		for (SplittingPattern<Item> filter : ignoredItemFilters) {
			if (filter.hasAnyMatch(item)) {
				return true;
			}
		}
		return false;
	}

	private static List<ItemTagHolder> getNotIgnoredTags(Item item, List<SplittingPattern<ResourceLocation>> ignoredTagFilters) {
		List<ItemTagHolder> notIgnoredTags = new ObjectArrayList<>();
		for (ResourceLocation tagName : item.getTags()) {
			if (UnificationRule.isTagIgnored(tagName, ignoredTagFilters)) {
				continue;
			}
			notIgnoredTags.add(new ItemTagHolder(tagName));
		}
		notIgnoredTags.sort(Comparator.comparing(ItemTagHolder::getTagName, ResourceLocation::compareNamespaced));
		return notIgnoredTags;
	}

	private static boolean isTagIgnored(ResourceLocation tagName, List<SplittingPattern<ResourceLocation>> ignoredTagFilters) {
		for (SplittingPattern<ResourceLocation> filter : ignoredTagFilters) {
			if (filter.hasAnyMatch(tagName)) {
				return true;
			}
		}
		return false;
	}

	private static class UnificationContext {

		private final Set<Pair<ResourceLocation, Map<String, String>>> usedTagFilterResults = new ObjectOpenHashSet<>();
		private final Set<Triple<ResourceLocation, Map<String, String>, Map<String, String>>> usedItemFilterResults = new ObjectOpenHashSet<>();

		public Collection<Map<String, String>> matches(SplittingPattern<ResourceLocation> filter, ResourceLocation tagName) {
				Collection<Map<String, String>> output = new ObjectArrayList<>();
				for (PatternMatch match : filter.matches(tagName)) {
					Map<String, String> variableMap = match.computeVariableMap();
					if (!this.usedTagFilterResults.add(Pair.of(tagName, variableMap)))
						continue;
					output.add(variableMap);
				}
				return output;
		}

		public Collection<Map<String, String>> matches(SplittingPattern<Item> filter, Item item, ResourceLocation tagName, Map<String, String> tagFilterResult) {
				Collection<Map<String, String>> output = new ObjectArrayList<>();
				for (PatternMatch match : filter.matches(item, tagFilterResult)) {
					Map<String, String> variableMap = match.computeVariableMap();
					if (!this.usedItemFilterResults.add(Triple.of(tagName, tagFilterResult, variableMap)))
						continue;
					output.add(variableMap);
				}
				return output;
		}

	}

	public Item findReplacement(Item item, List<ItemTagHolder> tags) {
		return this.findReplacement(item, tags, new UnificationContext());
	}

	private Item findReplacement(Item item, List<ItemTagHolder> tags, UnificationContext context) {
		for (SplittingPattern<ResourceLocation> originalTagFilter : this.originalTagFilters) {
			for (ItemTagHolder tag : tags) {
				for (Map<String, String> tagVariableMap : context.matches(originalTagFilter, tag.getTagName())) {
					Item r = this.findReplacement(item, tag, context, tagVariableMap);
					if (r != null) {
						return r;
					}
				}
			}
		}
		return null;
	}

	private Item findReplacement(Item item, ItemTagHolder tag, UnificationContext context, Map<String, String> tagVariableMap) {
		for (SplittingPattern<Item> originalItemFilter : this.originalItemFilters) {
			for (Map<String, String> itemVariableMap : context.matches(originalItemFilter, item, tag.getTagName(), tagVariableMap)) {
				Item r = this.findReplacement(tag, UnificationRule.combine(tagVariableMap, itemVariableMap));
				if (r != null) {
					return r;
				}
			}
		}
		return null;
	}

	private static <K, V> Map<K, V> combine(Map<K, V> m1, Map<K, V> m2) {
		Map<K, V> m3 = new Object2ObjectOpenHashMap<>();
		m3.putAll(m1);
		m3.putAll(m2);
		return m3;
	}

	private Item findReplacement(ItemTagHolder tag, Map<String, String> combinedVariableMap) {
		for (SplittingPattern<Item> replacementItemFilter : this.replacementItemFilters) {
			for (Item item : tag.getItems()) {
				if (replacementItemFilter.hasAnyMatch(item, combinedVariableMap)) {
					return item;
				}
			}
		}
		return null;
	}

}
