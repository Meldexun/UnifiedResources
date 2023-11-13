package meldexun.unifiedresources.util;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import meldexun.unifiedresources.UnifiedResources;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

public class RecipeOutputFixerArray extends RecipeOutputFixer {

	private static final Logger LOGGER = LogManager.getLogger();

	public RecipeOutputFixerArray(Field field) {
		super(field);
	}

	@Override
	public void fixRecipeOutput(IRecipe<?> recipe, Object scope) {
		try {
			Object[] stacks = (Object[]) this.field.get(scope);
			if (stacks == null) {
				return;
			}
			if (stacks.length == 0) {
				return;
			}
			if (stacks[0].getClass() != ItemStack.class) {
				for (Object obj1 : stacks) {
					List<RecipeOutputFixer> recipeOutputFixers = RecipeOutputFixer.getRecipeOutputFixers(obj1.getClass());
					for (RecipeOutputFixer recipeOutputFixer : recipeOutputFixers) {
						recipeOutputFixer.fixRecipeOutput(recipe, obj1);
					}
				}
			} else {
				for (int i = 0; i < stacks.length; i++) {
					ItemStack stack = (ItemStack) stacks[i];
					ItemStack newStack = UnifiedResources.getReplacement(stack);
					if (newStack != null) {
						stacks[i] = newStack;
						RecipeFixer.onRecipeOutputReplaced(stack, newStack);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error(String.format("%s %s %s:", recipe.getId(), recipe.getClass().getName(), this.field.getName()), e);
		}
	}

}
