package meldexun.unifiedresources.util;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import meldexun.unifiedresources.UnifiedResources;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

public class RecipeOutputFixerItemStack extends RecipeOutputFixer {

	private static final Logger LOGGER = LogManager.getLogger();

	public RecipeOutputFixerItemStack(Field field) {
		super(field);
	}

	@Override
	public void fixRecipeOutput(IRecipe<?> recipe, Object obj) {
		try {
			ItemStack stack = (ItemStack) this.field.get(obj);
			if (stack == null) {
				return;
			}
			ItemStack newStack = UnifiedResources.getReplacement(stack);
			if (newStack != null) {
				this.field.set(obj, newStack);
				RecipeFixer.onRecipeOutputReplaced(stack, newStack);
			}
		} catch (Exception e) {
			LOGGER.error(String.format("%s %s %s:", recipe.getId(), recipe.getClass().getName(), this.field.getName()), e);
		}
	}

}
