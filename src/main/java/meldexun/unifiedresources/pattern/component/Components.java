package meldexun.unifiedresources.pattern.component;

import java.util.regex.PatternSyntaxException;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.chars.Char2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.chars.CharSet;
import meldexun.unifiedresources.util.CharCollector;
import meldexun.unifiedresources.util.StringReader;

public class Components {

	private static final Char2ObjectMap<ComponentFactory> COMPONENT_FACTORIES = new Char2ObjectOpenHashMap<>();
	static {
		Components.register('*', reader -> {
			Components.readValidate(reader, '*');
			CharSet disallowed = null;
			if (reader.hasNext() && reader.peek() == '[') {
				Components.readValidate(reader, '[');
				disallowed = Components.readUntil(reader, ']', CharCollector.TO_CHAR_SET);
			}
			return new UniversalComponent(disallowed);
		});
		Components.register('?', reader -> {
			Components.readValidate(reader, '?');
			Components.readValidate(reader, '{');
			String key = Components.readUntil(reader, '}', CharCollector.TO_STRING);
			CharSet disallowed = null;
			if (reader.hasNext() && reader.peek() == '[') {
				Components.readValidate(reader, '[');
				disallowed = Components.readUntil(reader, ']', CharCollector.TO_CHAR_SET);
			}
			return new SetterComponent(key, disallowed);
		});
		Components.register('#', reader -> {
			Components.readValidate(reader, '#');
			Components.readValidate(reader, '{');
			return new GetterComponent(Components.readUntil(reader, '}', CharCollector.TO_STRING));
		});
	}

	public static Component parseNextComponent(StringReader reader) throws PatternSyntaxException {
		ComponentFactory factory = COMPONENT_FACTORIES.get(reader.peek());
		if (factory != null) {
			return factory.create(reader);
		}

		StringBuilder sb = new StringBuilder();
		while (reader.hasNext() && !COMPONENT_FACTORIES.containsKey(reader.peek())) {
			sb.append(reader.next());
		}
		if (sb.length() == 1) {
			return new CharComponent(sb.charAt(0));
		}
		return new StringComponent(sb.toString());
	}

	private static void register(char identifier, ComponentFactory factory) {
		COMPONENT_FACTORIES.put(identifier, factory);
	}

	private static void readValidate(StringReader reader, char expected) {
		if (!reader.hasNext()) {
			throw new PatternSyntaxException("Expected character '" + expected + "' but reached end.", reader.getString(), reader.getPosition());
		}
		char c = reader.next();
		if (c != expected) {
			throw new PatternSyntaxException("Expected character '" + expected + "' but got '" + c + "'.", reader.getString(), reader.getPosition() - 1);
		}
	}

	private static <A, R> R readUntil(StringReader reader, char end, CharCollector<A, R> collector) {
		A a = collector.supplier()
				.get();
		while (true) {
			if (!reader.hasNext()) {
				throw new PatternSyntaxException("Missing closing character '" + end + "'.", reader.getString(), reader.getPosition());
			}
			char c = reader.next();
			if (c == end) {
				break;
			}
			collector.accumulator()
					.accept(a, c);
		}
		return collector.finisher()
				.apply(a);
	}

	private interface ComponentFactory {

		Component create(StringReader reader) throws PatternSyntaxException;

	}

}
