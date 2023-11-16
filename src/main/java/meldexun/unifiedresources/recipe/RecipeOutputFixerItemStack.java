package meldexun.unifiedresources.recipe;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import meldexun.unifiedresources.ItemReplacer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

public class RecipeOutputFixerItemStack extends RecipeOutputFixer {

	private static final Logger LOGGER = LogManager.getLogger();

	public RecipeOutputFixerItemStack(Field field) {
		super(field);
	}

	@Override
	public void fixRecipeOutput(IRecipe<?> recipe, Object scope) {
		try {
			ItemStack stack = (ItemStack) this.field.get(scope);
			if (stack == null) {
				return;
			}
			ItemStack newStack = ItemReplacer.getReplacement(stack);
			if (newStack != null) {
				this.field.set(scope, newStack);
				RecipeFixer.onRecipeOutputReplaced(stack, newStack);
			}
		} catch (Exception e) {
			LOGGER.error(String.format("%s %s %s:", recipe.getId(), recipe.getClass().getName(), this.field.getName()), e);
		}
	}

}
