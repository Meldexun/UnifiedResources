package meldexun.unifiedresources;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import meldexun.unifiedresources.config.UnifiedResourcesConfig;
import meldexun.unifiedresources.reflection.ReflectionField;
import meldexun.unifiedresources.util.RecipeFixer;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ITagCollection;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;

@Mod(UnifiedResources.MODID)
public class UnifiedResources {

	public static final String MODID = "unified_resources";
	private static final Logger LOGGER = LogManager.getLogger();

	private static final ReflectionField<CompoundNBT> FIELD_CAP_NBT = new ReflectionField<>(ItemStack.class, "capNBT", "capNBT");
	private static final Map<ITag<Item>, List<Item>> TAG_2_SORTED_VALUES_MAP = new ConcurrentHashMap<>();

	public UnifiedResources() {
		ModLoadingContext.get().registerConfig(Type.SERVER, UnifiedResourcesConfig.SERVER_SPEC);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onAddReloadListenerEvent(AddReloadListenerEvent event) {
		event.addListener((stage, resourceManager, profiler1, profiler2, executor1, executor2) -> CompletableFuture.completedFuture(null).thenCompose(stage::wait).thenAcceptAsync((obj) -> {
			TAG_2_SORTED_VALUES_MAP.clear();
		}, executor2));
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onTagsUpdatedEvent(TagsUpdatedEvent.CustomTagTypes event) {
		RecipeFixer.checkRecipes();
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onServerAboutToStartEvent(FMLServerAboutToStartEvent event) {
		RecipeFixer.checkRecipes();
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onEntityJoinWorldEvent(EntityJoinWorldEvent event) {
		if (event.getEntity().level.isClientSide()) {
			return;
		}
		if (!(event.getEntity() instanceof ItemEntity)) {
			return;
		}
		ItemEntity itemEntity = (ItemEntity) event.getEntity();
		ItemStack stack = itemEntity.getItem();
		ItemStack newStack = getReplacement(stack);

		if (newStack != null) {
			itemEntity.setItem(newStack);
			UnifiedResources.onItemReplaced("ItemEntity", stack, newStack);
		}
	}

	@Nullable
	public static ItemStack getReplacement(ItemStack stack) {
		Item item = stack.getItem();
		Item newItem = getReplacement(item);
		if (newItem == null || newItem == item) {
			return null;
		}
		ItemStack newStack = new ItemStack(newItem, stack.getCount(), FIELD_CAP_NBT.get(stack));
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
					Item newItem = getReplacement(tag);

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

	public static void onItemReplaced(String type, ItemStack oldStack, ItemStack newStack) {
		onItemReplaced(type, oldStack.getItem(), newStack.getItem());
	}

	public static void onItemReplaced(String type, Item oldItem, Item newItem) {
		if (UnifiedResourcesConfig.SERVER_CONFIG.debug.get()) {
			LOGGER.info("{}: Replaced {} with {}", type, oldItem.getRegistryName(), newItem.getRegistryName());
		}
	}

}
