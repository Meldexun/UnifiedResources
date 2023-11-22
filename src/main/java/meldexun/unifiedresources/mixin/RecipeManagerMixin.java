package meldexun.unifiedresources.mixin;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.google.gson.JsonElement;

import meldexun.unifiedresources.recipe.RecipeFixer;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

@Mixin(RecipeManager.class)
public class RecipeManagerMixin {

	@Shadow
	private Map<IRecipeType<?>, Map<ResourceLocation, IRecipe<?>>> recipes;

	@Inject(method = "apply", at = @At("RETURN"))
	private void onApply(Map<ResourceLocation, JsonElement> resources, IResourceManager resourceManager, IProfiler profiler, CallbackInfo info) {
		RecipeFixer.checkRecipes(this.recipes);
	}

}
