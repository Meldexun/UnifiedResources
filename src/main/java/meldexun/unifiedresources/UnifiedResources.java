package meldexun.unifiedresources;

import meldexun.unifiedresources.util.RecipeFixer;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

@Mod(UnifiedResources.MODID)
public class UnifiedResources {

	public static final String MODID = "unified_resources";

	public UnifiedResources() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onTagsUpdatedEvent(TagsUpdatedEvent.CustomTagTypes event) {
		RecipeFixer.checkRecipes(ServerLifecycleHooks.getCurrentServer());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onServerAboutToStartEvent(FMLServerAboutToStartEvent event) {
		RecipeFixer.checkRecipes(ServerLifecycleHooks.getCurrentServer());
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onEntityJoinWorldEvent(EntityJoinWorldEvent event) {
		if (event.getEntity().level.isClientSide()) {
			return;
		}
		if (!(event.getEntity() instanceof ItemEntity)) {
			return;
		}
		ItemEntity itemEntity = (ItemEntity) event.getEntity();
		ItemStack stack = itemEntity.getItem();
		ItemStack newStack = ItemReplacer.getReplacement(stack);

		if (newStack != null) {
			itemEntity.setItem(newStack);
			ItemReplacer.onItemReplaced("ItemEntity", stack, newStack);
		}
	}

}
