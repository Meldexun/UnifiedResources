package meldexun.unifiedresources.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import meldexun.unifiedresources.UnifiedResources;
import meldexun.unifiedresources.api.IRecipeMutableResult;
import meldexun.unifiedresources.config.UnifiedResourcesConfig;
import meldexun.unifiedresources.reflection.ReflectionField;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.AbstractCookingRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipe;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.item.crafting.SingleItemRecipe;
import net.minecraft.item.crafting.SmithingRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class RecipeFixer {

	private static final Logger LOGGER = LogManager.getLogger();

	private static final ReflectionField<ItemStack> COOKING_RECIPE__RESULT = new ReflectionField<>(AbstractCookingRecipe.class, "field_222143_e", "result");
	private static final ReflectionField<ItemStack> SINGLE_ITEM_RECIPE__RESULT = new ReflectionField<>(SingleItemRecipe.class, "field_222132_b", "result");
	private static final ReflectionField<ItemStack> SMITHING_RECIPE__RESULT = new ReflectionField<>(SmithingRecipe.class, "field_234839_c_", "result");
	private static final ReflectionField<ItemStack> SHAPED_RECIPE__RESULT = new ReflectionField<>(ShapedRecipe.class, "field_77575_e", "result");
	private static final ReflectionField<ItemStack> SHAPELESS_RECIPE__RESULT = new ReflectionField<>(ShapelessRecipe.class, "field_77580_a", "result");

	public static int recipesChecked = 0;
	public static int outputsUpdated = 0;

	public static void checkRecipes() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) {
			return;
		}
		synchronized (server) {
			recipesChecked = 0;
			outputsUpdated = 0;
			long t = System.currentTimeMillis();
			for (IRecipe<?> recipe : server.getRecipeManager().getRecipes()) {
				RecipeFixer.fixRecipe(recipe);
				recipesChecked++;
			}
			LOGGER.info("Checking {} recipes and unifying {} outputs took {} milliseconds", recipesChecked, outputsUpdated, System.currentTimeMillis() - t);
		}
	}

	private static void fixRecipe(IRecipe<?> recipe) {
		if (recipe instanceof IRecipeMutableResult) {
			for (int i = 0; i < ((IRecipeMutableResult) recipe).getResultItemCount(); i++) {
				ItemStack stack = ((IRecipeMutableResult) recipe).getResultItem(i);
				ItemStack newStack = UnifiedResources.getReplacement(stack);

				if (newStack != null) {
					((IRecipeMutableResult) recipe).setResultItem(newStack, i);
					outputsUpdated++;
					if (UnifiedResourcesConfig.SERVER_CONFIG.debug.get()) {
						LOGGER.info("Recipe: Replaced {} with {} in {}", stack.getItem().getRegistryName(), newStack.getItem().getRegistryName(), recipe.getId());
					}
				}
			}
		} else if (recipe instanceof AbstractCookingRecipe) {
			fixVanillaRecipe(recipe, COOKING_RECIPE__RESULT);
		} else if (recipe instanceof SingleItemRecipe) {
			fixVanillaRecipe(recipe, SINGLE_ITEM_RECIPE__RESULT);
		} else if (recipe instanceof SmithingRecipe) {
			fixVanillaRecipe(recipe, SMITHING_RECIPE__RESULT);
		} else if (recipe instanceof ShapedRecipe) {
			fixVanillaRecipe(recipe, SHAPED_RECIPE__RESULT);
		} else if (recipe instanceof ShapelessRecipe) {
			fixVanillaRecipe(recipe, SHAPELESS_RECIPE__RESULT);
		} else {
			for (RecipeOutputFixer recipeOutputFixer : RecipeOutputFixer.getRecipeOutputFixers(recipe.getClass())) {
				recipeOutputFixer.fixRecipeOutput(recipe, recipe);
			}
		}
	}

	private static void fixVanillaRecipe(IRecipe<?> recipe, ReflectionField<ItemStack> resultAccessor) {
		ItemStack stack = resultAccessor.get(recipe);
		ItemStack newStack = UnifiedResources.getReplacement(stack);

		if (newStack != null) {
			resultAccessor.set(recipe, newStack);
			outputsUpdated++;
			if (UnifiedResourcesConfig.SERVER_CONFIG.debug.get()) {
				LOGGER.info("Recipe: Replaced {} with {} in {}", stack.getItem().getRegistryName(), newStack.getItem().getRegistryName(), recipe.getId());
			}
		}
	}

}
