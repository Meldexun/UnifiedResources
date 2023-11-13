package meldexun.unifiedresources.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import meldexun.unifiedresources.UnifiedResources;
import meldexun.unifiedresources.api.IRecipeMutableResult;
import meldexun.unifiedresources.config.UnifiedResourcesConfig;
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
					outputsUpdated++;
					if (UnifiedResourcesConfig.SERVER_CONFIG.debug.get()) {
						LOGGER.info("Recipe: Replaced {} with {} in {}", stack.getItem().getRegistryName(), newStack.getItem().getRegistryName(), recipe.getId());
					}
				}
			}
		} else {
			for (RecipeOutputFixer recipeOutputFixer : RecipeOutputFixer.getRecipeOutputFixers(recipe.getClass())) {
				recipeOutputFixer.fixRecipeOutput(recipe, recipe);
			}
		}
	}

}
