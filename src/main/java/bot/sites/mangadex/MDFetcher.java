package bot.sites.mangadex;

import bot.modules.DBHandler;
import bot.sites.NotFoundException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
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
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.parser.Parser;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MDFetcher {

	private static final Logger logger = LogManager.getLogger(MDFetcher.class);
	private static final String API_URL = "https://api.mangadex.org/";
	private static final Pattern mangadexPattern = Pattern.compile("https?://mangadex\\.org/title/([a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89aAbB][a-f0-9]{3}-[a-f0-9]{12})(?>.*?)");


	private static final ArrayList<String> statuses = new ArrayList<>(
			Arrays.asList(
					"", "Ongoing", "Completed", "Cancelled", "Hiatus"
			)
	);
	private static final ArrayList<String> demographics = new ArrayList<>(
			Arrays.asList(
					"", "Shounen", "Shoujo", "Seinen", "Josei"
			)
	);
	private static HashMap<Integer, ImmutableTriple<String, String, String>> tagCache = null;

	private static final HttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();

	// Manga identifiers
	private final String url;
	private final String uuid;

	//--------------------------------
	// INSTANCE FIELDS
	//--------------------------------
	// Manga titles
	private String title;
	private String altTitle;
	private String japaneseTitle;

	// Basic information
	private HashSet<String> authors;
	private HashSet<String> artists;
	private String demographic;
	private String status;
	private String originalLanguage;
	private String fullDescription;
	private String description;

	// Gallery tags
	private HashSet<String> allTags;
	private HashSet<String> format;
	private HashSet<String> genre;
	private HashSet<String> theme;
	private HashSet<String> content;

	private Instant lastUpdated;

	public MDFetcher(String url, @Nullable DBHandler database) throws IOException {
		this.url = url;

		Matcher mangadexMatcher = mangadexPattern.matcher(url);

		if (mangadexMatcher.find()) {
			this.uuid = mangadexMatcher.group(1);
		} else {
			// Something has gone seriously wrong
			logger.error("UUID not found: from URL " + url);
			throw new NotFoundException("UUID not found");
		}

		if (database != null) {
			boolean cached = database.loadFromCache(this);
			if (cached) {
				// Found it in the caches (and the data has already been loaded). We can stop here.
				return;
			}
		}

		loadFields(MDApiRequest("manga/" + uuid + "?includes[]=artist&includes[]=author&includes[]=cover_art"), database);
	}

	public static JSONObject MDApiRequest(String endpoint) throws IOException {
		HttpClient connect = HttpClients.custom().setConnectionManager(connManager).build();
		return MDApiRequest(endpoint, connect);
	}
	public static JSONObject MDApiRequest(String endpoint, HttpClient connect) throws IOException {
		HttpGet get = new HttpGet(API_URL + endpoint);
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

	private void loadFields(JSONObject response, @Nullable DBHandler database) {
		if (!response.getString("result").equals("ok")) {
			logger.error("Something went wrong with the HTTP status code.");
			logger.error(response.toString(4));
			throw new NotFoundException("MangaDex request failed");
		}

		JSONObject data = response.getJSONObject("data").getJSONObject("attributes");

		StreamSupport.stream(data.getJSONArray("altTitles").spliterator(), false).filter((o) ->
				(o instanceof JSONObject) && ((JSONObject) o).has("en"))
				.findFirst()
				.ifPresentOrElse(
						(o) -> altTitle = ((JSONObject) o).getString("en"),
						() -> altTitle = "None"
				);

		try {
			title = data.getJSONObject("title").getString("en");
		} catch (Exception e) {
			title = altTitle;
			altTitle = "None";
		}

		authors = data.getJSONArray("author").toList().stream().map(Object::toString)
				.collect(Collectors.toCollection(HashSet::new));
		artists = data.getJSONArray("artist").toList().stream().map(Object::toString)
				.collect(Collectors.toCollection(HashSet::new));

		demographic = demographics.get(data.getJSONObject("publication").getInt("demographic"));
		status = statuses.get(data.getJSONObject("publication").getInt("status"));

		if(data.getString("lastChapter") != null) {
			status += "Final: ";
			if(data.getString("lastVolume") != null) {
				status += "Vol. " + data.getString("lastVolume") + " ";
			}
			status += "Ch. " + data.getString("lastChapter");
		}
		language = languages.get(data.getJSONObject("publication").getString("language"));
		fullDescription = convertBBCode(data.getString("description"));
		description = fullDescription.split("\\R\\R")[0];

		format = new HashSet<>();
		genre = new HashSet<>();
		theme = new HashSet<>();
		content = new HashSet<>();
		allTags = new HashSet<>();

		if (tagCache == null) {
			JSONObject tagResp;
			try {
				tagResp = MDApiRequest("tag");
			} catch (IOException e) {
				logger.error("Connection to Mangadex Tag API endpoint failed");
				throw new NotFoundException("MangaDex Tag API request failed");
			}

			tagCache = new HashMap<>(tagResp.getJSONObject("data")
					.keySet().stream().map(s -> tagResp.getJSONObject("data").getJSONObject(s)).map(
							s -> new ImmutablePair<>(s.getInt("id"), new ImmutableTriple<>(
									s.getString("name"), s.getString("group"), s.getString("description"))))
					.collect(Collectors.toMap(ImmutablePair::getLeft, ImmutablePair::getRight)));
		}
		data.getJSONArray("tags").toList().stream().map(i -> (int) i).map(tagCache::get).forEach(tagData -> {
			switch (tagData.getMiddle()) {
				case "Format" -> format.add(tagData.getLeft());
				case "Genre" -> genre.add(tagData.getLeft());
				case "Theme" -> theme.add(tagData.getLeft());
				case "Content" -> content.add(tagData.getLeft());
			}
			allTags.add(tagData.getLeft());
		});
		//TODO fix caching2
	}

	private String convertBBCode(String str) {
		str = str.replaceAll("\\[/?b]", "**")
				.replaceAll("\\[/?i]", "*")
				.replaceAll("\\[/?u]", "_")
				.replaceAll("\\[/?s]", "~~")
				.replaceAll("\\[/?h]", "")
				.replaceAll("\\[/?spoiler]", "||")
				.replaceAll("\\[/?code]", "`")
				.replaceAll("\\[hr]", "\r\n")
				.replaceAll("\\[*](.*?)\\[/*]", "- $1")
				.replaceAll("\\[url=(.*?)](.*?)\\[/url]", "[$2]($1)");

		return str;
	}

	//-----------------------------------------------------
	// The rest is just getters and setters.
	// Don't bother scrolling.
	//-----------------------------------------------------


}
