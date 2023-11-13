package meldexun.unifiedresources;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import meldexun.unifiedresources.config.UnifiedResourcesConfig;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ITagCollection;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;

public class ItemReplacer {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final Field FIELD_capNBT;
	static {
		try {
			FIELD_capNBT = ItemStack.class.getDeclaredField("capNBT");
			FIELD_capNBT.setAccessible(true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	public static final Map<ITag<Item>, List<Item>> TAG_2_SORTED_VALUES_MAP = new ConcurrentHashMap<>();

	@Nullable
	public static ItemStack getReplacement(ItemStack stack) {
		Item item = stack.getItem();
		Item newItem = ItemReplacer.getReplacement(item);
		if (newItem == null || newItem == item) {
			return null;
		}
		ItemStack newStack = new ItemStack(newItem, stack.getCount(), ItemReplacer.getCapNBT(stack));
		newStack.setTag(stack.getTag());
		return newStack;
	}

	@Nullable
	public static Item getReplacement(Item item) {
		Set<ResourceLocation> itemTags = item.getTags();
		List<? extends String> tagsToUnify = UnifiedResourcesConfig.SERVER_CONFIG.tagsToUnify.get();
		List<? extends String> tagsToIgnore = UnifiedResourcesConfig.SERVER_CONFIG.tagsToIgnore.get();

		for (ResourceLocation itemTag : itemTags) {
			String s = itemTag.toString();
			for (String s1 : tagsToUnify) {
				if (s.startsWith(s1) && !tagsToIgnore.contains(s)) {
					ITagCollection<Item> allTags = ItemTags.getAllTags();
					ITag<Item> tag = allTags.getTag(itemTag);
					Item newItem = ItemReplacer.getReplacement(tag);

					if (newItem != null) {
						return newItem;
					}
				}
			}
		}

		return null;
	}

	@Nullable
	public static Item getReplacement(ITag<Item> tag) {
		List<Item> values = TAG_2_SORTED_VALUES_MAP.computeIfAbsent(tag, key -> {
			List<? extends String> priorityModIds = UnifiedResourcesConfig.SERVER_CONFIG.modPriority.get();
			List<Item> sortedValues = new ArrayList<>(tag.getValues());
			sortedValues.sort((item1, item2) -> {
				ResourceLocation registryName1 = item1.getRegistryName();
				ResourceLocation registryName2 = item2.getRegistryName();
				int i1 = priorityModIds.indexOf(registryName1.getNamespace());
				int i2 = priorityModIds.indexOf(registryName2.getNamespace());
				if (i1 != -1 && (i2 == -1 || i1 < i2)) {
					return -1;
				}
				if (i2 != -1 && (i1 == -1 || i2 < i1)) {
					return 1;
				}
				return item1.getRegistryName().compareNamespaced(item2.getRegistryName());
			});
			return sortedValues;
		});

		if (values.isEmpty()) {
			return null;
		}

		return values.get(0);
	}

	@Nullable
	private static CompoundNBT getCapNBT(ItemStack stack) {
		try {
			return (CompoundNBT) FIELD_capNBT.get(stack);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	public static void onItemReplaced(String type, ItemStack oldStack, ItemStack newStack) {
		ItemReplacer.onItemReplaced(type, oldStack.getItem(), newStack.getItem());
	}

	public static void onItemReplaced(String type, Item oldItem, Item newItem) {
		if (UnifiedResourcesConfig.SERVER_CONFIG.debug.get()) {
			LOGGER.info("{}: Replaced {} with {}", type, oldItem.getRegistryName(), newItem.getRegistryName());
		}
	}

}
