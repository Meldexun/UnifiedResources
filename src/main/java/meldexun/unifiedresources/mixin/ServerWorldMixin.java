package meldexun.unifiedresources.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import meldexun.unifiedresources.ItemReplacer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.server.ServerWorld;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

	@Inject(method = "add", at = @At("HEAD"))
	private void onAdd(Entity entity, CallbackInfo info) {
		if (!(entity instanceof ItemEntity)) {
			return;
		}

		ItemEntity itemEntity = (ItemEntity) entity;
		ItemStack stack = itemEntity.getItem();
		ItemStack newStack = ItemReplacer.getReplacement(stack);

		if (newStack != null) {
			itemEntity.setItem(newStack);
			ItemReplacer.onItemReplaced("ItemEntity", stack, newStack);
		}
	}

}
