package meldexun.unifiedresources.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import meldexun.unifiedresources.UnifiedResources;
import meldexun.unifiedresources.api.IRecipeMutableResult;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

public class RecipeFixer {

	private static final Logger LOGGER = LogManager.getLogger();

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
					RecipeFixer.onRecipeOutputReplaced(stack, newStack);
				}
			}
		} else {
			for (RecipeOutputFixer recipeOutputFixer : RecipeOutputFixers.getRecipeOutputFixers(recipe.getClass())) {
				recipeOutputFixer.fixRecipeOutput(recipe, recipe);
			}
		}
	}

	public static void onRecipeOutputReplaced(ItemStack oldStack, ItemStack newStack) {
		RecipeFixer.onRecipeOutputReplaced(oldStack.getItem(), newStack.getItem());
	}

	public static void onRecipeOutputReplaced(Item oldItem, Item newItem) {
		outputsUpdated++;
		UnifiedResources.onItemReplaced("Recipe", oldItem, newItem);
	}

}
