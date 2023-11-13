package meldexun.unifiedresources.util;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import meldexun.unifiedresources.UnifiedResources;
import net.minecraft.item.Item;
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
			Object[] outputs = (Object[]) this.field.get(scope);
			if (outputs == null) {
				return;
			}
			for (int i = 0; i < outputs.length; i++) {
				Object output = outputs[i];
				if (Item.class.equals(output.getClass())) {
					Item newItem = UnifiedResources.getReplacement((Item) output);
					if (newItem != null) {
						outputs[i] = newItem;
						RecipeFixer.onRecipeOutputReplaced((Item) output, newItem);
					}
				} else if (ItemStack.class.equals(output.getClass())) {
					ItemStack newStack = UnifiedResources.getReplacement((ItemStack) output);
					if (newStack != null) {
						outputs[i] = newStack;
						RecipeFixer.onRecipeOutputReplaced((ItemStack) output, newStack);
					}
				} else {
					for (RecipeOutputFixer recipeOutputFixer : RecipeOutputFixer.getRecipeOutputFixers(output.getClass())) {
						recipeOutputFixer.fixRecipeOutput(recipe, output);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error(String.format("%s %s %s:", recipe.getId(), recipe.getClass().getName(), this.field.getName()), e);
		}
	}

}
