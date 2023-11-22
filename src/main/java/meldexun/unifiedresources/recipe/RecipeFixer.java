package meldexun.unifiedresources.recipe;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import meldexun.unifiedresources.ItemReplacer;
import meldexun.unifiedresources.api.IRecipeMutableResult;
import meldexun.unifiedresources.util.CollectorUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.util.ResourceLocation;

public class RecipeFixer {

	private static final Logger LOGGER = LogManager.getLogger();

	private static AtomicInteger recipesChecked = new AtomicInteger();
	private static AtomicInteger outputsUpdated = new AtomicInteger();

	public synchronized static void checkRecipes(Map<IRecipeType<?>, Map<ResourceLocation, IRecipe<?>>> recipes) {
		long time = System.currentTimeMillis();
		ItemReplacer.loadUnificationRules();
		recipesChecked.set(0);
		outputsUpdated.set(0);
		recipes.values()
				.stream()
				.map(Map::values)
				.flatMap(Collection::stream)
				.collect(CollectorUtil.toObjList())
				.parallelStream()
				.forEach(RecipeFixer::checkRecipe);
		time = System.currentTimeMillis() - time;
		LOGGER.info("Checking {} recipes and unifying {} outputs took {} milliseconds", recipesChecked, outputsUpdated, time);
	}

	private static void checkRecipe(IRecipe<?> recipe) {
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
		recipesChecked.getAndIncrement();
	}

	public static void onRecipeOutputReplaced(ItemStack oldStack, ItemStack newStack) {
		RecipeFixer.onRecipeOutputReplaced(oldStack.getItem(), newStack.getItem());
	}

	public static void onRecipeOutputReplaced(Item oldItem, Item newItem) {
		outputsUpdated.getAndIncrement();
		ItemReplacer.onItemReplaced("Recipe", oldItem, newItem);
	}

}
