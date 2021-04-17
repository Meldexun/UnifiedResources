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

import meldexun.unifiedresources.api.IRecipeMutableResult;
import meldexun.unifiedresources.config.UnifiedResourcesConfig;
import meldexun.unifiedresources.reflection.ReflectionField;
import meldexun.unifiedresources.util.RecipeOutputFixer;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.AbstractCookingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.item.crafting.SingleItemRecipe;
import net.minecraft.item.crafting.SmithingRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.server.MinecraftServer;
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
import net.minecraftforge.fml.server.ServerLifecycleHooks;

@Mod(UnifiedResources.MODID)
public class UnifiedResources {

	public static final String MODID = "unified_resources";
	private static final Logger LOGGER = LogManager.getLogger();

	private static final ReflectionField<ItemStack> COOKING_RECIPE__RESULT = new ReflectionField<>(AbstractCookingRecipe.class, "field_222143_e", "result");
	private static final ReflectionField<ItemStack> SINGLE_ITEM_RECIPE__RESULT = new ReflectionField<>(SingleItemRecipe.class, "field_222132_b", "result");
	private static final ReflectionField<ItemStack> SMITHING_RECIPE__RESULT = new ReflectionField<>(SmithingRecipe.class, "field_234839_c_", "result");
	private static final ReflectionField<ItemStack> SHAPED_RECIPE__RESULT = new ReflectionField<>(ShapedRecipe.class, "field_77575_e", "result");
	private static final ReflectionField<ItemStack> SHAPELESS_RECIPE__RESULT = new ReflectionField<>(ShapelessRecipe.class, "field_77580_a", "result");

	private static final ReflectionField<CompoundNBT> FIELD_CAP_NBT = new ReflectionField<>(ItemStack.class, "capNBT", "capNBT");
	private static final Map<ITag<Item>, List<Item>> TAG_2_SORTED_VALUES_MAP = new ConcurrentHashMap<>();
	public static int recipesChecked = 0;
	public static int outputsUpdated = 0;

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
		this.checkRecipes();
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onServerAboutToStartEvent(FMLServerAboutToStartEvent event) {
		this.checkRecipes();
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
			if (UnifiedResourcesConfig.SERVER_CONFIG.debug.get()) {
				LOGGER.info("ItemEntity: Replaced {} with {}", stack.getItem().getRegistryName(), newStack.getItem().getRegistryName());
			}
		}
	}

	private void checkRecipes() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) {
			return;
		}
		synchronized (server) {
			recipesChecked = 0;
			outputsUpdated = 0;
			long t = System.currentTimeMillis();
			for (IRecipe<?> recipe : server.getRecipeManager().getRecipes()) {
				this.fixRecipe(recipe);
				recipesChecked++;
			}
			LOGGER.info("Checking {} recipes and unifying {} outputs took {} milliseconds", recipesChecked, outputsUpdated, System.currentTimeMillis() - t);
		}
	}

	private void fixRecipe(IRecipe<?> recipe) {
		if (recipe instanceof IRecipeMutableResult) {
			for (int i = 0; i < ((IRecipeMutableResult) recipe).getResultItemCount(); i++) {
				ItemStack stack = ((IRecipeMutableResult) recipe).getResultItem(i);
				ItemStack newStack = getReplacement(stack);

				if (newStack != null) {
					((IRecipeMutableResult) recipe).setResultItem(newStack, i);
					outputsUpdated++;
					if (UnifiedResourcesConfig.SERVER_CONFIG.debug.get()) {
						LOGGER.info("Recipe: Replaced {} with {} in {}", stack.getItem().getRegistryName(), newStack.getItem().getRegistryName(), recipe.getId());
					}
				}
			}
		} else if (recipe instanceof AbstractCookingRecipe) {
			this.fixVanillaRecipe(recipe, COOKING_RECIPE__RESULT);
		} else if (recipe instanceof SingleItemRecipe) {
			this.fixVanillaRecipe(recipe, SINGLE_ITEM_RECIPE__RESULT);
		} else if (recipe instanceof SmithingRecipe) {
			this.fixVanillaRecipe(recipe, SMITHING_RECIPE__RESULT);
		} else if (recipe instanceof ShapedRecipe) {
			this.fixVanillaRecipe(recipe, SHAPED_RECIPE__RESULT);
		} else if (recipe instanceof ShapelessRecipe) {
			this.fixVanillaRecipe(recipe, SHAPELESS_RECIPE__RESULT);
		} else {
			for (RecipeOutputFixer recipeOutputFixer : RecipeOutputFixer.getRecipeOutputFixers(recipe.getClass())) {
				recipeOutputFixer.fixRecipeOutput(recipe, recipe);
			}
		}
	}

	private void fixVanillaRecipe(IRecipe<?> recipe, ReflectionField<ItemStack> resultAccessor) {
		ItemStack stack = resultAccessor.get(recipe);
		ItemStack newStack = getReplacement(stack);

		if (newStack != null) {
			resultAccessor.set(recipe, newStack);
			outputsUpdated++;
			if (UnifiedResourcesConfig.SERVER_CONFIG.debug.get()) {
				LOGGER.info("Recipe: Replaced {} with {} in {}", stack.getItem().getRegistryName(), newStack.getItem().getRegistryName(), recipe.getId());
			}
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

}
