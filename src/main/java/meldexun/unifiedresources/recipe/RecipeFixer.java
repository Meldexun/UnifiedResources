package meldexun.unifiedresources.recipe;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import meldexun.unifiedresources.ItemReplacer;
import meldexun.unifiedresources.api.IRecipeMutableResult;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.server.MinecraftServer;

public class RecipeFixer {

	private static final Logger LOGGER = LogManager.getLogger();

	private static int recipesChecked = 0;
	private static int outputsUpdated = 0;

	public static void checkRecipes(MinecraftServer server) {
		if (server == null) {
			return;
		}
		synchronized (server) {
			recipesChecked = 0;
			outputsUpdated = 0;
			long t = System.currentTimeMillis();
			ItemReplacer.loadUnificationRules();
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
				ItemStack newStack = ItemReplacer.getReplacement(stack);

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
		ItemReplacer.onItemReplaced("Recipe", oldItem, newItem);
	}

}
