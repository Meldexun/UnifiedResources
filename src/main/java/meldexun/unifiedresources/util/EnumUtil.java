package meldexun.unifiedresources.util;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public class EnumUtil {

	public static <E extends Enum<E>> Set<E> immutableEnumSet(E e1) {
		return Collections.unmodifiableSet(EnumSet.of(e1));
	}

	public static <E extends Enum<E>> Set<E> immutableEnumSet(E e1, E e2) {
		return Collections.unmodifiableSet(EnumSet.of(e1, e2));
	}

	public static <E extends Enum<E>> Set<E> immutableEnumSet(E e1, E e2, E e3) {
		return Collections.unmodifiableSet(EnumSet.of(e1, e2, e3));
	}

	public static <E extends Enum<E>> Set<E> immutableEnumSet(E e1, E e2, E e3, E e4) {
		return Collections.unmodifiableSet(EnumSet.of(e1, e2, e3, e4));
	}

	public static <E extends Enum<E>> Set<E> immutableEnumSet(E e1, E e2, E e3, E e4, E e5) {
		return Collections.unmodifiableSet(EnumSet.of(e1, e2, e3, e4, e5));
	}

	@SafeVarargs
	public static <E extends Enum<E>> Set<E> immutableEnumSet(E first, E... rest) {
		return Collections.unmodifiableSet(EnumSet.of(first, rest));
	}

}
