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

public class RecipeOutputFixers {

	private static final Map<Class<?>, List<RecipeOutputFixer>> RECIPE_CLASS_2_RECIPE_OUTPUT_FIXERS = new HashMap<>();

	public static List<RecipeOutputFixer> getRecipeOutputFixers(Class<?> classToCheck) {
		if (RECIPE_CLASS_2_RECIPE_OUTPUT_FIXERS.containsKey(classToCheck)) {
			return RECIPE_CLASS_2_RECIPE_OUTPUT_FIXERS.get(classToCheck);
		}
		ArrayList<RecipeOutputFixer> list = new ArrayList<>();
		if (classToCheck != Object.class) {
			for (Field field : classToCheck.getDeclaredFields()) {
				RecipeOutputFixer recipeOutputFixer = RecipeOutputFixers.create(field);
				if (recipeOutputFixer != null) {
					list.add(recipeOutputFixer);
				}
			}
			list.addAll(RecipeOutputFixers.getRecipeOutputFixers(classToCheck.getSuperclass()));
		}
		list.trimToSize();
		RECIPE_CLASS_2_RECIPE_OUTPUT_FIXERS.put(classToCheck, list);
		return list;
	}

	@Nullable
	private static RecipeOutputFixer create(Field field) {
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
		} else if (type.isArray() && !type.getComponentType().isPrimitive()) {
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
