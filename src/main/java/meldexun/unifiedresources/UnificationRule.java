package meldexun.unifiedresources;

import java.util.List;
import java.util.function.Predicate;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class UnificationRule {

	private final List<Predicate<Item>> originalItemFilters;
	private final List<Predicate<ResourceLocation>> originalTagFilters;
	private final List<Predicate<Item>> replacementItemFilters;

	public UnificationRule(List<Predicate<Item>> originalItemFilters, List<Predicate<ResourceLocation>> originalTagFilters,
			List<Predicate<Item>> replacementItemFilters) {
		this.originalItemFilters = originalItemFilters;
		this.originalTagFilters = originalTagFilters;
		this.replacementItemFilters = replacementItemFilters;
	}

	public List<Predicate<Item>> getOriginalItemFilters() {
		return originalItemFilters;
	}

	public List<Predicate<ResourceLocation>> getOriginalTagFilters() {
		return originalTagFilters;
	}

	public List<Predicate<Item>> getReplacementItemFilters() {
		return replacementItemFilters;
	}

}
