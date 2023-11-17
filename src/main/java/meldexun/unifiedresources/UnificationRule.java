package meldexun.unifiedresources;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import meldexun.unifiedresources.util.CollectorUtil;
import net.minecraft.item.Item;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;

public class UnificationRule {

	private final List<Predicate<Item>> originalItemFilters;
	private final List<Predicate<ResourceLocation>> originalTagFilters;
	private final List<Predicate<Item>> replacementItemFilters;

	public UnificationRule(List<Predicate<Item>> originalItemFilters, List<Predicate<ResourceLocation>> originalTagFilters,
			List<Predicate<Item>> replacementItemFilters) {
		this.originalItemFilters = ImmutableList.copyOf(originalItemFilters);
		this.originalTagFilters = ImmutableList.copyOf(originalTagFilters);
		this.replacementItemFilters = ImmutableList.copyOf(replacementItemFilters);
	}

	public static UnificationRule parseUnificationRule(JsonObject json) throws PatternSyntaxException {
		List<Predicate<Item>> originalItemFilters = ItemReplacer.parseFilters(json.getAsJsonArray("originalItemFilters"))
				.map(filter -> ItemReplacer.mapThenTest(filter, Item::getRegistryName))
				.collect(CollectorUtil.toObjList());
		List<Predicate<ResourceLocation>> originalTagFilters = ItemReplacer.parseFilters(json.getAsJsonArray("originalTagFilters"))
				.collect(CollectorUtil.toObjList());
		List<Predicate<Item>> replacementItemFilters = ItemReplacer.parseFilters(json.getAsJsonArray("replacementItemFilters"))
				.map(filter -> ItemReplacer.mapThenTest(filter, Item::getRegistryName))
				.collect(CollectorUtil.toObjList());
		return new UnificationRule(originalItemFilters, originalTagFilters, replacementItemFilters);
	}

	@Nullable
	public Item findReplacement(Item item, List<Predicate<ResourceLocation>> ignoredTagFilters) {
		if (this.originalItemFilters.stream()
				.noneMatch(originalItemFilter -> originalItemFilter.test(item))) {
			return null;
		}
		for (Predicate<ResourceLocation> originalTagFilter : this.originalTagFilters) {
			for (ResourceLocation tag : item.getTags()) {
				if (ignoredTagFilters.stream()
						.anyMatch(ignoredTagFilter -> ignoredTagFilter.test(tag))) {
					continue;
				}
				if (!originalTagFilter.test(tag)) {
					continue;
				}
				for (Predicate<Item> replacementItemFilter : this.replacementItemFilters) {
					for (Item tagItem : ItemTags.getAllTags()
							.getTag(tag)
							.getValues()) {
						if (!replacementItemFilter.test(tagItem)) {
							continue;
						}
						return tagItem;
					}
				}
			}
		}
		return null;
	}

}
