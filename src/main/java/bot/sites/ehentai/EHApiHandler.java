package bot.sites.ehentai;

import bot.sites.NotFoundException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.parser.Parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class EHApiHandler {
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
	public static class EHGalleryPayload {
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
			entry.put(galleryId);
			entry.put(galleryToken);

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

		public int size() {
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
		private JSONArray pages;

		public EHPagePayload() {
			pages = new JSONArray();
		}

		/**
		 * Adds an entry to the payload.
		 * @param galleryId The gallery ID of the entry.
		 * @param pageId The ID of the page.
		 * @param pageNum The page number.
		 */
		public void addEntry(int galleryId, String pageId, int pageNum) {
			JSONArray entry = new JSONArray();
			entry.put(galleryId);
			entry.put(pageId);
			entry.put(pageNum);

			pages.put(entry);
		}

		/**
		 * Clears the payload.
		 */
		public void clear() {
			pages = new JSONArray();
		}

		public JSONObject getPayload() {
			JSONObject payload = getEmptyPagePayload();
			payload.put("pagelist", pages);
			return payload;
		}

		public boolean isEmpty() {
			return pages.isEmpty();
		}

		public int size() {
			return pages.length();
		}

		private static JSONObject getEmptyPagePayload() {
			JSONObject payload = new JSONObject();
			payload.put("method", "gtoken");

			return payload;
		}
	}

	//initialize HTTP threading objects
	private final ArrayList<Pair<EHFetcher, CompletableFuture<JSONObject>>> queries;
	private volatile boolean sleeping;
	private final ThreadPoolExecutor executor;
	private static final HttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();

	private static final Logger logger = LogManager.getLogger(EHApiHandler.class);
	private static final String API_URL = "https://api.e-hentai.org/api.php";

	public EHApiHandler() {
		executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
		queries = new ArrayList<>();
		sleeping = false;
		logger.info("API handler initialized.");
	}

	// Simple mutex to ensure sleeping never gets modified twice at the same time
	private synchronized void setSleeping(boolean status) {
		sleeping = status;
	}

	/**
	 * Packages EHFetcher queries in bits of 25 at a time to minimalize traffic/bandwidth. Avoids the E-Hentai rate limit.
	 *
	 */
	public void runGalleryQuery() {
		logger.info("Now compiling the current gallery queries...");
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Make sure we clean out the relevant parts of queries before setting sleeping to false, to ensure
		// there are no repeat queries

		int maxSize = Math.min(queries.size(), 25);
		ArrayList<Pair<EHFetcher, CompletableFuture<JSONObject>>> curQueries = new ArrayList<>(queries.subList(0, maxSize));
		queries.subList(0, maxSize).clear();

		if(!queries.isEmpty()) {
			// If there are still queries to be run, run another query afterwards
			logger.info("There are still queries remaining. Running gallery queries again.");
			setSleeping(true);
			executor.submit(
					this::runGalleryQuery
			);
		}

		setSleeping(false);
		//allow next batch of queries

		EHGalleryPayload payload = new EHGalleryPayload();

		//build package
		for (Pair<EHFetcher, CompletableFuture<JSONObject>> cur : curQueries) {
			EHFetcher curData = cur.getLeft();
			payload.addEntry(curData.getGalleryId(), curData.getGalleryToken());
		}

		try {
			// Get response
			JSONObject response = EHApiRequest(payload.getPayload());
			//logger.info(response.toString(4));

			JSONArray gmetas = response.getJSONArray("gmetadata");

			//Iterate through response data and resolve them individually
			for(int i = 0; i < gmetas.length(); i++) {
				JSONObject curResponse = gmetas.getJSONObject(i);

				if (curResponse.has("error")) {
					// There's an error.
					logger.info("An error happened in the payload: " + curResponse.getString("error"));

					for(Pair<EHFetcher, CompletableFuture<JSONObject>> cur : curQueries) {
						if(cur.getLeft().getGalleryId() == curResponse.getInt("gid")) {
							cur.getRight().completeExceptionally(new NotFoundException(curResponse.getString("error")));
						}
					}
				}
				String curGalleryToken = curResponse.getString("token");

				for(Pair<EHFetcher, CompletableFuture<JSONObject>> cur : curQueries) {
					if(cur.getLeft().getGalleryToken().equals(curGalleryToken)) {
						cur.getRight().complete(curResponse);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();

			for(Pair<EHFetcher, CompletableFuture<JSONObject>> cur : curQueries) {
				cur.getRight().completeExceptionally(e);
			}
		}
	}

	/**
	 * Submits a query to the E-Hentai API for information about a doujin.
	 * @param data An EHFetcher containing a Gallery ID and Gallery Token (that will be used to submit the query).
	 * @return A CompletableFuture object that will resolve with the data from E-Hentai.
	 */
	public CompletableFuture<JSONObject> galleryQuery(EHFetcher data) {
		CompletableFuture<JSONObject> promise = new CompletableFuture<>();
		queries.add(new ImmutablePair<>(data, promise));

		if(!sleeping) {
			setSleeping(true);
			startGalleryQuery();
		}
		return promise;
	}

	/**
	 * Kickstarts query fetch thread.
	 */
	private synchronized void startGalleryQuery() {
		executor.submit(
				this::runGalleryQuery
		);
	}

	/**
	 * Sends a basic request.
	 * @param payload
	 * @return
	 * @throws IOException
	 */
	public static JSONObject EHApiRequest(JSONObject payload) throws IOException {
		HttpClient connect = HttpClients.custom().setConnectionManager(connManager).build();
		return EHApiRequest(payload, connect);
	}

	/**
	 * Sends a basic request using a specific HttpClient.
	 * @param payload
	 * @param connect
	 * @return
	 * @throws IOException
	 */
	public static JSONObject EHApiRequest(JSONObject payload, HttpClient connect) throws IOException {
		logger.info("Sending payload...");
		logger.info(payload.toString(4));

		StringEntity payloadEntity = new StringEntity(payload.toString(), ContentType.APPLICATION_JSON);

		HttpPost post = new HttpPost(API_URL);
		post.setEntity(payloadEntity);

		HttpResponse apiResponse = connect.execute(post);

		HttpEntity entity = apiResponse.getEntity();

		logger.info("Response received. Processing...");

		// Unescape any wacky HTML character sequences (like &#039;)
		BufferedReader buf = new BufferedReader(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8));
		JSONObject jsonResponse = new JSONObject(new JSONTokener(Parser.unescapeEntities(buf.readLine(), false)));
		buf.close();

		EntityUtils.consume(entity);

		logger.info(jsonResponse.toString(4));

		return jsonResponse;
	}
}
