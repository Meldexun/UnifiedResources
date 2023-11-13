package meldexun.unifiedresources.util;

import java.lang.reflect.Field;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import meldexun.unifiedresources.ItemReplacer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

public class RecipeOutputFixerList extends RecipeOutputFixer {

	private static final Logger LOGGER = LogManager.getLogger();

	public RecipeOutputFixerList(Field field) {
		super(field);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void fixRecipeOutput(IRecipe<?> recipe, Object scope) {
		try {
			List<Object> outputs = (List<Object>) this.field.get(scope);
			if (outputs == null) {
				return;
			}
			for (int i = 0; i < outputs.size(); i++) {
				Object output = outputs.get(i);
				if (Item.class.equals(output.getClass())) {
					Item newItem = ItemReplacer.getReplacement((Item) output);
					if (newItem != null) {
						outputs.set(i, newItem);
						RecipeFixer.onRecipeOutputReplaced((Item) output, newItem);
					}
				} else if (ItemStack.class.equals(output.getClass())) {
					ItemStack newStack = ItemReplacer.getReplacement((ItemStack) output);
					if (newStack != null) {
						outputs.set(i, newStack);
						RecipeFixer.onRecipeOutputReplaced((ItemStack) output, newStack);
					}
				} else {
					for (RecipeOutputFixer recipeOutputFixer : RecipeOutputFixers.getRecipeOutputFixers(output.getClass())) {
						recipeOutputFixer.fixRecipeOutput(recipe, output);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error(String.format("%s %s %s:", recipe.getId(), recipe.getClass().getName(), this.field.getName()), e);
		}
	}

}
