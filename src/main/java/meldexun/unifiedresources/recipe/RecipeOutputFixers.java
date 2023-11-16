package meldexun.unifiedresources.recipe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public class RecipeOutputFixers {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final Map<Class<?>, List<RecipeOutputFixer>> RECIPE_OUTPUT_FIXERS = new HashMap<>();

	public static List<RecipeOutputFixer> getRecipeOutputFixers(Class<?> classToCheck) {
		if (RECIPE_OUTPUT_FIXERS.containsKey(classToCheck)) {
			return RECIPE_OUTPUT_FIXERS.get(classToCheck);
		}
		ArrayList<RecipeOutputFixer> recipeOutputFixerList = new ArrayList<>();
		if (classToCheck != Object.class) {
			for (Field field : classToCheck.getDeclaredFields()) {
				RecipeOutputFixer recipeOutputFixer = RecipeOutputFixers.create(field);
				if (recipeOutputFixer != null) {
					recipeOutputFixerList.add(recipeOutputFixer);
				}
			}
			recipeOutputFixerList.addAll(RecipeOutputFixers.getRecipeOutputFixers(classToCheck.getSuperclass()));
		}
		recipeOutputFixerList.trimToSize();
		RECIPE_OUTPUT_FIXERS.put(classToCheck, recipeOutputFixerList);
		return recipeOutputFixerList;
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

		try {
			if (type == Item.class) {
				field.setAccessible(true);
				return new RecipeOutputFixerItem(field);
			}
			if (type == ItemStack.class) {
				field.setAccessible(true);
				return new RecipeOutputFixerItemStack(field);
			}
			if (type.isArray() && !type.getComponentType().isPrimitive()) {
				field.setAccessible(true);
				return new RecipeOutputFixerArray(field);
			}
			if (List.class.isAssignableFrom(type)) {
				field.setAccessible(true);
				return new RecipeOutputFixerList(field);
			}
		} catch (Exception e) {
			LOGGER.error("Failed creating recipe output fixer for field: {} {} {}", field.getDeclaringClass(), field.getName(), type, e);
		}

		return null;
	}

}
