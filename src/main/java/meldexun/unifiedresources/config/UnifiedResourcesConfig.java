package meldexun.unifiedresources.config;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;

public class UnifiedResourcesConfig {

	public static final ServerConfig SERVER_CONFIG;
	public static final ForgeConfigSpec SERVER_SPEC;
	static {
		final Pair<ServerConfig, ForgeConfigSpec> serverSpecPair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
		SERVER_CONFIG = serverSpecPair.getLeft();
		SERVER_SPEC = serverSpecPair.getRight();
	}

	public static class ServerConfig {

		public final ForgeConfigSpec.BooleanValue debug;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> modPriority;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> tagsToIgnore;
		public final ForgeConfigSpec.ConfigValue<List<? extends String>> tagsToUnify;

		public ServerConfig(ForgeConfigSpec.Builder builder) {
			this.debug = builder.comment("").define("debug", false);
			this.modPriority = builder.comment("Mods listed here will be preferred during the unify process. Entries at the beginning have the highest priority.").defineList("modPriority",
					Arrays.asList("minecraft"), o -> true);
			this.tagsToIgnore = builder.comment("Item tags which won't be unified. Useful when specifying a folder in 'tagsToUnify' but not all tags from that folder should be unified.").defineList("tagsToIgnore",
					Arrays.asList("forge:rods/all_metal"), o -> true);
			this.tagsToUnify = builder.comment("Item tags which will be unified. When leaving a '/' at the end that means that all tags inside that folder will be unified.").defineList("tagsToUnify",
					Arrays.asList( "forge:dusts/", "forge:gems/", "forge:ingots/",
					"forge:nuggets/", "forge:ores/", "forge:rods/",
					"forge:storage_blocks/"), o -> true);
		}

	}

}
