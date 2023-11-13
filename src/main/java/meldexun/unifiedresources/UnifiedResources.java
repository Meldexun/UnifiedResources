package meldexun.unifiedresources;

import java.util.concurrent.CompletableFuture;

import meldexun.unifiedresources.config.UnifiedResourcesConfig;
import meldexun.unifiedresources.util.RecipeFixer;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

@Mod(UnifiedResources.MODID)
public class UnifiedResources {

	public static final String MODID = "unified_resources";

	public UnifiedResources() {
		ModLoadingContext.get().registerConfig(Type.SERVER, UnifiedResourcesConfig.SERVER_SPEC);
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onAddReloadListenerEvent(AddReloadListenerEvent event) {
		event.addListener((stage, resourceManager, profiler1, profiler2, executor1, executor2) -> CompletableFuture.completedFuture(null).thenCompose(stage::wait).thenAcceptAsync((obj) -> {
			ItemReplacer.TAG_2_SORTED_VALUES_MAP.clear();
		}, executor2));
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
