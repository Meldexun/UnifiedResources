package meldexun.unifiedresources.util;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class CollectorUtil {

	public static final Set<Characteristics> CH_ID = EnumUtil.immutableEnumSet(Characteristics.IDENTITY_FINISH);
	public static final Set<Characteristics> CH_UNORDERED_ID = EnumUtil.immutableEnumSet(Characteristics.UNORDERED, Characteristics.IDENTITY_FINISH);

	public static class CollectorImpl<T, A, R> implements Collector<T, A, R> {

		private final Supplier<A> supplier;
		private final BiConsumer<A, T> accumulator;
		private final BinaryOperator<A> combiner;
		private final Function<A, R> finisher;
		private final Set<Characteristics> characteristics;

		public CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner, Function<A, R> finisher,
				Set<Characteristics> characteristics) {
			this.supplier = supplier;
			this.accumulator = accumulator;
			this.combiner = combiner;
			this.finisher = finisher;
			this.characteristics = characteristics;
		}

		public CollectorImpl(Supplier<A> supplier, BiConsumer<A, T> accumulator, BinaryOperator<A> combiner, Set<Characteristics> characteristics) {
			this(supplier, accumulator, combiner, CollectorUtil.castingIdentity(), characteristics);
		}

		@Override
		public BiConsumer<A, T> accumulator() {
			return this.accumulator;
		}

		@Override
		public Supplier<A> supplier() {
			return this.supplier;
		}

		@Override
		public BinaryOperator<A> combiner() {
			return this.combiner;
		}

		@Override
		public Function<A, R> finisher() {
			return this.finisher;
		}

		@Override
		public Set<Characteristics> characteristics() {
			return this.characteristics;
		}

	}

	@SuppressWarnings("unchecked")
	public static <I, R> Function<I, R> castingIdentity() {
		return i -> (R) i;
	}

	public static <T> Collector<T, ?, ObjectArrayList<T>> toObjList() {
		return CollectorUtil.toList(ObjectArrayList::new);
	}

	public static <T> Collector<T, ?, ObjectOpenHashSet<T>> toObjSet() {
		return CollectorUtil.toSet(ObjectOpenHashSet::new);
	}

	public static <T, R extends List<T>> Collector<T, ?, R> toList(Supplier<R> supplier) {
		return CollectorUtil.toCollection(supplier, CH_ID);
	}

	public static <T, R extends Set<T>> Collector<T, ?, R> toSet(Supplier<R> supplier) {
		return CollectorUtil.toCollection(supplier, CH_UNORDERED_ID);
	}

	public static <T, R extends Collection<T>> Collector<T, ?, R> toCollection(Supplier<R> supplier, Set<Characteristics> characteristics) {
		return new CollectorImpl<>(supplier, Collection::add, (c1, c2) -> {
			c1.addAll(c2);
			return c1;
		}, characteristics);
	}

}
