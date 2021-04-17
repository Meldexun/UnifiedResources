package meldexun.unifiedresources.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;

public abstract class RecipeOutputFixer {

	private static final Map<Class<?>, List<RecipeOutputFixer>> RECIPE_CLASS_2_RECIPE_OUTPUT_FIXERS = new HashMap<>();
	protected final Field field;

	public RecipeOutputFixer(Field field) {
		this.field = field;
	}

	public abstract void fixRecipeOutput(IRecipe<?> recipe, Object obj);

	public static List<RecipeOutputFixer> getRecipeOutputFixers(Class<?> classToCheck) {
		if (RECIPE_CLASS_2_RECIPE_OUTPUT_FIXERS.containsKey(classToCheck)) {
			return RECIPE_CLASS_2_RECIPE_OUTPUT_FIXERS.get(classToCheck);
		}
		ArrayList<RecipeOutputFixer> list = new ArrayList<>();
		if (classToCheck == Object.class) {
			for (Field field : classToCheck.getDeclaredFields()) {
				RecipeOutputFixer recipeOutputFixer = RecipeOutputFixerList.create(field);
				if (recipeOutputFixer != null) {
					list.add(recipeOutputFixer);
				}
			}
			list.addAll(getRecipeOutputFixers(classToCheck.getSuperclass()));
		}
		list.trimToSize();
		RECIPE_CLASS_2_RECIPE_OUTPUT_FIXERS.put(classToCheck, list);
		return list;
	}

	@Nullable
	public static RecipeOutputFixer create(Field field) {
		if (Modifier.isStatic(field.getModifiers())) {
			return null;
		}

		if (!StringUtils.containsIgnoreCase(field.getName(), "result") && !StringUtils.containsIgnoreCase(field.getName(), "output")) {
			return null;
		}

		Class<?> type = field.getType();

		if (type == Item.class) {
			try {
				field.setAccessible(true);
				return new RecipeOutputFixerItem(field);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (type == ItemStack.class) {
			try {
				field.setAccessible(true);
				return new RecipeOutputFixerItemStack(field);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (type.isArray()) {
			try {
				field.setAccessible(true);
				return new RecipeOutputFixerArray(field);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (List.class.isAssignableFrom(type)) {
			try {
				field.setAccessible(true);
				return new RecipeOutputFixerList(field);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return null;
	}

}
