package meldexun.unifiedresources;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ResourceLocation;

public class ItemReplacer {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final Path UNIFICATION_RULES_FILE = Paths.get("config/" + UnifiedResources.MODID + ".json");
	private static final Gson GSON = new Gson();
	private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();
	private static final List<UnificationRule> UNIFICATION_RULES = new ArrayList<>();
	private static final List<Predicate<Item>> IGNORED_ITEM_FILTERS = new ArrayList<>();
	private static final List<Predicate<ResourceLocation>> IGNORED_TAG_FILTERS = new ArrayList<>();
	private static final Map<Item, Item> REPLACEMENT_CACHE = new ConcurrentHashMap<>();
	private static final Field FIELD_capNBT;
	static {
		try {
			FIELD_capNBT = ItemStack.class.getDeclaredField("capNBT");
			FIELD_capNBT.setAccessible(true);
		} catch (Exception e) {
			throw new UnsupportedOperationException("Failed to find capNBT field", e);
		}
	}

	private static boolean debug;

	public static void loadUnificationRules() {
		if (!Files.exists(UNIFICATION_RULES_FILE)) {
			try {
				Files.createDirectories(UNIFICATION_RULES_FILE.getParent());
				try (InputStream in = new BufferedInputStream(ItemReplacer.class.getResourceAsStream("/config/" + UnifiedResources.MODID + ".json"));
						OutputStream out = new BufferedOutputStream(Files.newOutputStream(UNIFICATION_RULES_FILE))) {
					int b;
					while ((b = in.read()) != -1) {
						out.write(b);
					}
				}
			} catch (IOException e) {
				LOGGER.error("Failed copying default config from mod jar into config folder {}", UNIFICATION_RULES_FILE, e);
			}
		}

		JsonObject unificationRulesJson;
		try (Reader reader = new InputStreamReader(new BufferedInputStream(Files.newInputStream(UNIFICATION_RULES_FILE)))) {
			unificationRulesJson = GSON.fromJson(reader, JsonObject.class);
		} catch (IOException e) {
			LOGGER.error("Failed reading config file {}", UNIFICATION_RULES_FILE, e);
			return;
		}

		List<UnificationRule> newUnificationRules = ItemReplacer.<JsonObject>stream(unificationRulesJson.getAsJsonArray("rules"))
				.map(rule -> {
					List<Predicate<Item>> originalItemFilters = ItemReplacer.parseResourceLocationFilters(rule.getAsJsonArray("originalItemFilters"))
							.map(predicate -> ItemReplacer.mapThenTest(predicate, Item::getRegistryName))
							.collect(Collectors.toList());
					List<Predicate<ResourceLocation>> originalTagFilters = ItemReplacer.parseResourceLocationFilters(rule.getAsJsonArray("originalTagFilters"))
							.collect(Collectors.toList());
					List<Predicate<Item>> replacementItemFilters = ItemReplacer.parseResourceLocationFilters(rule.getAsJsonArray("replacementItemFilters"))
							.map(predicate -> ItemReplacer.mapThenTest(predicate, Item::getRegistryName))
							.collect(Collectors.toList());
					return new UnificationRule(originalItemFilters, originalTagFilters, replacementItemFilters);
				})
				.collect(Collectors.toList());

		List<Predicate<Item>> newIgnoredItemFilters = ItemReplacer.stream(unificationRulesJson.getAsJsonArray("ignoredItemFilters"))
				.map(ItemReplacer::parseResourceLocationFilter)
				.map(predicate -> ItemReplacer.mapThenTest(predicate, Item::getRegistryName))
				.collect(Collectors.toList());

		List<Predicate<ResourceLocation>> newIgnoredTagFilters = ItemReplacer.stream(unificationRulesJson.getAsJsonArray("ignoredTagFilters"))
				.map(ItemReplacer::parseResourceLocationFilter)
				.collect(Collectors.toList());

		LOCK.writeLock().lock();
		try {
			REPLACEMENT_CACHE.clear();
			UNIFICATION_RULES.clear();
			IGNORED_ITEM_FILTERS.clear();
			IGNORED_TAG_FILTERS.clear();

			UNIFICATION_RULES.addAll(newUnificationRules);
			IGNORED_ITEM_FILTERS.addAll(newIgnoredItemFilters);
			IGNORED_TAG_FILTERS.addAll(newIgnoredTagFilters);

			debug = unificationRulesJson.has("debug") && unificationRulesJson.get("debug")
					.getAsBoolean();
		} finally {
			LOCK.writeLock().unlock();
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends JsonElement> Stream<T> stream(JsonArray jsonArray) {
		return StreamSupport.stream(jsonArray.spliterator(), false)
				.map(e -> (T) e);
	}

	private static Stream<Predicate<ResourceLocation>> parseResourceLocationFilters(JsonArray array) {
		return Stream.of(array)
				.filter(Objects::nonNull)
				.<JsonElement>flatMap(ItemReplacer::stream)
				.map(ItemReplacer::parseResourceLocationFilter);
	}

	private static Predicate<ResourceLocation> parseResourceLocationFilter(JsonElement regexJson) {
		return ItemReplacer.mapThenTest(Pattern.compile(regexJson.getAsString())
				.asPredicate(), ResourceLocation::toString);
	}

	private static <T, R> Predicate<R> mapThenTest(Predicate<T> predicate, Function<R, T> mappingFunction) {
		return t -> predicate.test(mappingFunction.apply(t));
	}

	@Nullable
	public static ItemStack getReplacement(ItemStack stack) {
		Item item = stack.getItem();
		Item newItem = ItemReplacer.getReplacement(item);
		if (newItem == null || newItem == item) {
			return null;
		}
		ItemStack newStack = new ItemStack(newItem, stack.getCount(), ItemReplacer.getCapNBT(stack));
		newStack.setTag(stack.getTag());
		return newStack;
	}

	@Nullable
	public static Item getReplacement(Item item) {
		LOCK.readLock().lock();
		try {
			return REPLACEMENT_CACHE.computeIfAbsent(item, k -> {
				if (IGNORED_ITEM_FILTERS.stream()
						.anyMatch(ignoredItemFilter -> ignoredItemFilter.test(item))) {
					return null;
				}
				for (UnificationRule rule : UNIFICATION_RULES) {
					if (rule.getOriginalItemFilters()
							.stream()
							.noneMatch(originalItemFilter -> originalItemFilter.test(item))) {
						continue;
					}
					for (Predicate<ResourceLocation> originalTagFilter : rule.getOriginalTagFilters()) {
						for (ResourceLocation tag : item.getTags()) {
							if (IGNORED_TAG_FILTERS.stream()
									.anyMatch(ignoredTagFilter -> ignoredTagFilter.test(tag))) {
								continue;
							}
							if (!originalTagFilter.test(tag)) {
								continue;
							}
							for (Predicate<Item> replacementItemFilter : rule.getReplacementItemFilters()) {
								for (Item tagItem : ItemTags.getAllTags()
										.getTag(tag)
										.getValues()) {
									if (!replacementItemFilter.test(tagItem)) {
										continue;
									}
									return tagItem;
								}
							}
						}
					}
				}
				return null;
			});
		} finally {
			LOCK.readLock().unlock();
		}
	}

	@Nullable
	private static CompoundNBT getCapNBT(ItemStack stack) {
		try {
			return (CompoundNBT) FIELD_capNBT.get(stack);
		} catch (ReflectiveOperationException e) {
			throw new UnsupportedOperationException("Failed to get capNBT field", e);
		}
	}

	public static void onItemReplaced(String type, ItemStack oldStack, ItemStack newStack) {
		ItemReplacer.onItemReplaced(type, oldStack.getItem(), newStack.getItem());
	}

	public static void onItemReplaced(String type, Item oldItem, Item newItem) {
		if (debug) {
			LOGGER.info("{}: Replaced {} with {}", type, oldItem.getRegistryName(), newItem.getRegistryName());
		}
	}

}
