package meldexun.unifiedresources.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.util.ResourceLocation;

@Mixin(RecipeManager.class)
public interface RecipeManagerMixin {

	@Accessor(value = "recipes")
	Map<IRecipeType<?>, Map<ResourceLocation, IRecipe<?>>> recipes();

}
