package meldexun.unifiedresources.util;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class JsonUtil {

	@SuppressWarnings("unchecked")
	public static <T extends JsonElement> Stream<T> stream(JsonArray jsonArray) {
		return StreamSupport.stream(jsonArray.spliterator(), false)
				.map(e -> (T) e);
	}

}
