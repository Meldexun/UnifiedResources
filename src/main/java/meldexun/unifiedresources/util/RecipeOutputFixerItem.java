package meldexun.unifiedresources.util;

import java.lang.reflect.Field;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import meldexun.unifiedresources.UnifiedResources;
import meldexun.unifiedresources.config.UnifiedResourcesConfig;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;

public class RecipeOutputFixerItem extends RecipeOutputFixer {

	private static final Logger LOGGER = LogManager.getLogger();

	public RecipeOutputFixerItem(Field field) {
		super(field);
	}

	@Override
	public void fixRecipeOutput(IRecipe<?> recipe, Object obj) {
		try {
			Item item = (Item) this.field.get(obj);
			if (item == null) {
				return;
			}
			Item newItem = UnifiedResources.getReplacement(item);
			if (newItem != null) {
				this.field.set(obj, newItem);
				RecipeFixer.outputsUpdated++;
				if (UnifiedResourcesConfig.SERVER_CONFIG.debug.get()) {
					LOGGER.info("Recipe: Replaced {} with {} in {}", item.getItem().getRegistryName(), newItem.getItem().getRegistryName(), recipe.getId());
				}
			}
		} catch (Exception e) {
			LOGGER.error(String.format("%s %s %s:", recipe.getId(), recipe.getClass().getName(), this.field.getName()), e);
		}
	}

}
