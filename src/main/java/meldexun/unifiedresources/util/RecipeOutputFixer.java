package meldexun.unifiedresources.util;

import java.lang.reflect.Field;

import net.minecraft.item.crafting.IRecipe;

public abstract class RecipeOutputFixer {

	protected final Field field;

	public RecipeOutputFixer(Field field) {
		this.field = field;
	}

	public abstract void fixRecipeOutput(IRecipe<?> recipe, Object scope);

}
