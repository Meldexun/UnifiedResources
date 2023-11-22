package meldexun.unifiedresources.util;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;

import net.minecraft.item.Item;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;

public class ItemTagHolder {

	private final ResourceLocation tagName;
	private final ITag<Item> tag;
	private List<Item> items;

	public ItemTagHolder(ResourceLocation tagName) {
		this(tagName, ItemTags.getAllTags()
				.getTag(tagName));
	}

	public ItemTagHolder(ResourceLocation tagName, ITag<Item> tag) {
		this.tagName = Objects.requireNonNull(tagName);
		this.tag = Objects.requireNonNull(tag);
	}

	public ResourceLocation getTagName() {
		return this.tagName;
	}

	public ITag<Item> getTag() {
		return this.tag;
	}

	public List<Item> getItems() {
		if (this.items == null) {
			this.items = ImmutableList.sortedCopyOf(Comparator.comparing(Item::getRegistryName, ResourceLocation::compareNamespaced), this.tag.getValues());
		}
		return this.items;
	}

}
