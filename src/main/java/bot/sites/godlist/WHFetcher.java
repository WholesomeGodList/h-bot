package bot.sites.godlist;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.HttpClientConnectionManager;
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
import java.time.Instant;
import java.util.HashSet;
import java.util.Random;

public class WHFetcher {
	private static final Logger logger = LogManager.getLogger(WHFetcher.class);
	private static final String API_URL = "https://wholesomelist.com/api/list";
	private static final HttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
	private static JSONObject cachedTable = null;
	private static long lastCached;

	private final String url;
	private final int id;
	private final String link;

	private final String title;
	private final String author;
	private final String parody;
	private final String warning;
	private final String tier;
	private final int pages;

	private final HashSet<String> tags;

	public WHFetcher(int id) throws IOException {
		this.id = id;

		JSONObject table = getCachedTable();
		JSONObject entry = getEntry(id, table);

		url = "https://wholesomelist.com/list#" + id;
		link = entry.getString("link");

		title = entry.getString("title");
		author = entry.getString("author");
		parody = entry.getString("parody");
		warning = entry.getString("warning");
		tier = entry.getString("tier");
		pages = Integer.parseInt(entry.getString("pages"));

		JSONArray tagArray = entry.getJSONArray("tags");
		tags = new HashSet<>();

		for (int i = 0; i < tagArray.length(); i++) {
			tags.add(tagArray.getString(i));
		}
	}

	public static JSONObject getEntry(int id, JSONObject table) {
		return table.getJSONArray("table").getJSONObject(id - 1);
	}

	public static int getRandomNumber() throws IOException {
		Random ran = new Random();
		return ran.nextInt(getCachedTable().getJSONArray("table").length()) + 1;
	}

	private static JSONObject getCachedTable() throws IOException {
		// If the cache is null or older than 10 minutes, update it
		if (cachedTable == null || lastCached < (Instant.now().getEpochSecond() - 600)) {
			cachedTable = WHApiRequest();
			lastCached = Instant.now().getEpochSecond();
		}

		return cachedTable;
	}

	public static JSONObject WHApiRequest() throws IOException {
		HttpClient connect = HttpClients.custom().setConnectionManager(connManager).build();
		return WHApiRequest(connect);
	}

	public static JSONObject WHApiRequest(HttpClient connect) throws IOException {
		HttpGet get = new HttpGet(API_URL);
		HttpResponse apiResponse = connect.execute(get);

		HttpEntity entity = apiResponse.getEntity();

		logger.info("Response received. Processing...");

		// Unescape any wacky HTML character sequences (like &#039;)
		BufferedReader buf = new BufferedReader(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8));
		JSONObject jsonResponse = new JSONObject(new JSONTokener(Parser.unescapeEntities(buf.readLine(), false)));
		buf.close();

		EntityUtils.consume(entity);

		logger.debug(jsonResponse.toString(4));

		return jsonResponse;
	}

	public String getUrl() {
		return url;
	}

	public int getId() {
		return id;
	}

	public String getLink() {
		return link;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthor() {
		return author;
	}

	public String getParody() {
		return parody;
	}

	public String getWarning() {
		return warning;
	}

	public String getTier() {
		return tier;
	}

	public int getPages() {
		return pages;
	}

	public HashSet<String> getTags() {
		return tags;
	}
}
