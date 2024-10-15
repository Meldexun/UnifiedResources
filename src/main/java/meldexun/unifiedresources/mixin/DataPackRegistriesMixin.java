package meldexun.unifiedresources.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import meldexun.unifiedresources.recipe.RecipeFixer;
import net.minecraft.resources.DataPackRegistries;

@Mixin(DataPackRegistries.class)
public class DataPackRegistriesMixin {

	@Inject(method = "updateGlobals", at = @At("RETURN"))
	private void onUpdateGlobals(CallbackInfo info) {
		RecipeFixer.checkRecipes(((RecipeManagerMixin) ((DataPackRegistries) (Object) this).getRecipeManager()).recipes());
	}

}
