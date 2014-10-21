package lars.simplehttpserver.helper;

import java.net.URI;
import java.util.HashMap;

public class URIUtils {
	public static HashMap<String, String> parseQuery(URI uri) {
		HashMap<String, String> result = new HashMap<String, String>();
		String query = uri.getQuery();
		if (query != null) {
			String[] keyValues = query.split("&");
			for (int i = 0; i < keyValues.length; i++) {
				String[] keyValue = keyValues[i].split("=");
				result.put(keyValue[0], keyValue[1]);
			}
		}
		return result;
	}
}
