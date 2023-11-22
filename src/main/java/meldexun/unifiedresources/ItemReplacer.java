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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meldexun.unifiedresources.pattern.SplittingPattern;
import meldexun.unifiedresources.util.CollectorUtil;
import meldexun.unifiedresources.util.JsonUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;

public class ItemReplacer {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final Path UNIFICATION_RULES_FILE = Paths.get("config/" + UnifiedResources.MODID + ".json");
	private static final Gson GSON = new Gson();
	private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();
	private static final List<UnificationRule> UNIFICATION_RULES = new ObjectArrayList<>();
	private static final List<SplittingPattern<Item>> IGNORED_ITEM_FILTERS = new ObjectArrayList<>();
	private static final List<SplittingPattern<ResourceLocation>> IGNORED_TAG_FILTERS = new ObjectArrayList<>();
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

		List<UnificationRule> newUnificationRules;
		List<SplittingPattern<Item>> newIgnoredItemFilters;
		List<SplittingPattern<ResourceLocation>> newIgnoredTagFilters;
		try {
			newUnificationRules = JsonUtil.<JsonObject>stream(unificationRulesJson.getAsJsonArray("rules"))
					.map(UnificationRule::parseUnificationRule)
					.collect(CollectorUtil.toObjList());
			newIgnoredItemFilters = ItemReplacer.parseItemFilters(unificationRulesJson.getAsJsonArray("ignoredItemFilters"));
			newIgnoredTagFilters = ItemReplacer.parseTagFilters(unificationRulesJson.getAsJsonArray("ignoredTagFilters"));
		} catch (PatternSyntaxException e) {
			LOGGER.error("Failed parsing unification rules", e);
			return;
		}

		LOCK.writeLock().lock();
		try {
			REPLACEMENT_CACHE.clear();
			UNIFICATION_RULES.clear();
			IGNORED_ITEM_FILTERS.clear();
			IGNORED_TAG_FILTERS.clear();

			UNIFICATION_RULES.addAll(newUnificationRules);
			IGNORED_ITEM_FILTERS.addAll(newIgnoredItemFilters);
			IGNORED_TAG_FILTERS.addAll(newIgnoredTagFilters);

			debug = unificationRulesJson.has("debug") && unificationRulesJson.get("debug").getAsBoolean();
		} finally {
			LOCK.writeLock().unlock();
		}
	}

	public static List<SplittingPattern<Item>> parseItemFilters(JsonArray array) throws PatternSyntaxException {
		return ItemReplacer.parseFilters(array, ((Function<ResourceLocation, CharSequence>) ResourceLocation::toString).compose(Item::getRegistryName));
	}

	public static List<SplittingPattern<ResourceLocation>> parseTagFilters(JsonArray array) throws PatternSyntaxException {
		return ItemReplacer.parseFilters(array, ResourceLocation::toString);
	}

	public static <T> List<SplittingPattern<T>> parseFilters(JsonArray array, Function<T, CharSequence> inputPreprocessor) throws PatternSyntaxException {
		return Stream.of(array)
				.filter(Objects::nonNull)
				.<JsonElement>flatMap(JsonUtil::stream)
				.map(JsonElement::getAsString)
				.map(pattern -> SplittingPattern.compile(pattern, inputPreprocessor))
				.collect(CollectorUtil.toObjList());
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
				Item newItem = UnificationRule.findReplacement(k, UNIFICATION_RULES, IGNORED_ITEM_FILTERS, IGNORED_TAG_FILTERS);
				if (newItem == null || newItem == k) {
					return null;
				}
				return newItem;
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
