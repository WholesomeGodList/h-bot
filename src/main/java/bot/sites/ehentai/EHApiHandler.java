package bot.sites.ehentai;

import org.apache.http.client.HttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class EHApiHandler {
	private enum Method {
		GDATA,
		GTOKEN
	}

	/**
	 * A simple wrapper class for the JSON payloads used in the e-hentai API for gallery information retrieval.
	 *
	 * The JSON formats are as follows:
	 * <h1>Payload:</h1>
	 * <pre> {@code
	 * {
	 *     "method":"gdata",
	 *     "namespace":"1",
	 *     "gidlist": [
	 *          [gallery ID, gallery token],
	 *          [gallery ID, gallery token],
	 *          ... (up to 25 times)
	 *     ]
	 * }} </pre>
	 * <br>
	 * The gallery ID (an int) and gallery token (a str made of 0-9a-f with length 10)
	 * can be retrieved from URLs, like so:
	 * https://e-hentai.org/g/{gallery token}/{gallery ID}/
	 * <br>
	 * <h2>Response:</h2>
	 * <pre> {@code
	 * {
	 *     "gmetadata": [
	 *          {Info about entry 1},
	 *          {Info about entry 2},
	 *          ... (as many as you requested)
	 *     ]
	 * }} </pre>
	 */
	private static class EHGalleryPayload {
		private JSONArray gid;

		public EHGalleryPayload() {
			gid = new JSONArray();
		}

		/**
		 * Adds an entry to the payload.
		 * @param galleryId The gallery ID of the entry.
		 * @param galleryToken The gallery token of the entry.
		 */
		public void addEntry(int galleryId, String galleryToken) {
			JSONArray entry = new JSONArray();
			entry.put(galleryId, galleryToken);

			gid.put(entry);
		}

		/**
		 * Clears the payload.
		 */
		public void clear() {
			gid = new JSONArray();
		}

		public JSONObject getPayload() {
			JSONObject payload = getEmptyGalleryPayload();
			payload.put("gidlist", gid);
			return payload;
		}

		public boolean isEmpty() {
			return gid.isEmpty();
		}

		public int getSize() {
			return gid.length();
		}

		private static JSONObject getEmptyGalleryPayload() {
			JSONObject payload = new JSONObject();
			payload.put("method", "gdata");
			payload.put("namespace", "1");

			return payload;
		}
	}

	/**
	 * A simple wrapper class for the JSON payloads used in the e-hentai API for gallery token retrieval from a page link.
	 *
	 * The JSON formats are as follows:
	 * <h1>Payload:</h1>
	 * <pre> {@code
	 * {
	 *     "method":"gtoken",
	 *     "pagelist": [
	 *          [galleryId, pageId, pageNum],
	 *          [galleryId, pageId, pageNum],
	 *          ... (up to 25 times)
	 *     ]
	 * }}
	 * </pre>
	 * <br>
	 * The gallery ID (an int), page ID (a str made of 0-9a-f length 10) and page number (an int)
	 * can be retrieved from URLs, like so:
	 * https://e-hentai.org/s/{page ID}/{gallery ID}-{page number}/
	 *
	 * <h2>Response:</h2>
	 * <pre> {@code
	 * {
	 *     "tokenlist": [
	 *         {"token": your gallery token},
	 *         {"token": your gallery token},
	 *         ... (as many as you requested)
	 *     ]
	 * }} </pre>
	 */
	private static class EHPagePayload {

	}
	private HttpClient client;
	private ArrayList<String>

	public EHApiHandler() {

	}

	public CompletableFuture<JSONObject> sendRequest() {

	}
}
