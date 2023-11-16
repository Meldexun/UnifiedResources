package meldexun.unifiedresources.util;

import java.util.function.Function;
import java.util.function.Supplier;

import it.unimi.dsi.fastutil.chars.CharOpenHashSet;
import it.unimi.dsi.fastutil.chars.CharSet;

public interface CharCollector<A, R> {

	CharCollector<?, String> TO_STRING = CharCollector.create(StringBuilder::new, StringBuilder::append, StringBuilder::toString);
	CharCollector<?, CharSet> TO_CHAR_SET = CharCollector.create(CharOpenHashSet::new, CharSet::add);

	static <A, R> CharCollector<A, R> create(Supplier<A> supplier, ObjCharConsumer<A> accumulator) {
		return CharCollector.create(supplier, accumulator, CollectorUtil.castingIdentity());
	}

	static <A, R> CharCollector<A, R> create(Supplier<A> supplier, ObjCharConsumer<A> accumulator, Function<A, R> finisher) {
		return new CharCollector<A, R>() {

			@Override
			public Supplier<A> supplier() {
				return supplier;
			}

			@Override
			public ObjCharConsumer<A> accumulator() {
				return accumulator;
			}

			@Override
			public Function<A, R> finisher() {
				return finisher;
			}

		};
	}

	Supplier<A> supplier();

	ObjCharConsumer<A> accumulator();

	Function<A, R> finisher();

}
