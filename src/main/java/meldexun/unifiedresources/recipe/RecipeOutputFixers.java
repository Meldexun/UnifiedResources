package meldexun.unifiedresources.recipe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Util;

public class RecipeOutputFixers {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final Map<Class<?>, List<RecipeOutputFixer>> RECIPE_OUTPUT_FIXERS = new ConcurrentHashMap<>();
	private static final Set<String> VANILLA_RECIPE_RESULT_NAMES = Util.make(new HashSet<>(), set -> {
		set.add("field_222143_e"); // AbstractCookingRecipe.result
		set.add("field_222132_b"); // SingleItemRecipe.result
		set.add("field_234839_c_"); // SmithingRecipe.result
		set.add("field_77575_e"); // ShapedRecipe.result
		set.add("field_77580_a"); // ShapelessRecipe.result
	});

	public static List<RecipeOutputFixer> getRecipeOutputFixers(Class<?> classToCheck) {
		List<RecipeOutputFixer> cachedRecipeOutputFixers = RECIPE_OUTPUT_FIXERS.get(classToCheck);
		if (cachedRecipeOutputFixers != null) {
			return cachedRecipeOutputFixers;
		}

		RecipeOutputFixers.ensureSuperClassCached(classToCheck);
		return RECIPE_OUTPUT_FIXERS.computeIfAbsent(classToCheck, k -> {
			if (Object.class.equals(k)) {
				return Collections.emptyList();
			}

			List<RecipeOutputFixer> recipeOutputFixers = new ObjectArrayList<>();
			for (Field field : classToCheck.getDeclaredFields()) {
				RecipeOutputFixer recipeOutputFixer = RecipeOutputFixers.create(field);
				if (recipeOutputFixer != null) {
					recipeOutputFixers.add(recipeOutputFixer);
				}
			}
			recipeOutputFixers.addAll(RECIPE_OUTPUT_FIXERS.get(k.getSuperclass()));
			((ObjectArrayList<RecipeOutputFixer>) recipeOutputFixers).trim();
			return recipeOutputFixers;
		});
	}

	private static void ensureSuperClassCached(Class<?> classToCheck) {
		Class<?> superClass = classToCheck.getSuperclass();
		if (superClass != null && !RECIPE_OUTPUT_FIXERS.containsKey(superClass)) {
			RecipeOutputFixers.getRecipeOutputFixers(superClass);
		}
	}

	@Nullable
	private static RecipeOutputFixer create(Field field) {
		if (Modifier.isStatic(field.getModifiers())) {
			return null;
		}

		if (!VANILLA_RECIPE_RESULT_NAMES.contains(field.getName())
				&& !StringUtils.containsIgnoreCase(field.getName(), "result")
				&& !StringUtils.containsIgnoreCase(field.getName(), "output")) {
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
