package bot.sites.mangadex;

import bot.modules.DBHandler;
import bot.sites.NotFoundException;
import com.google.common.collect.HashBiMap;
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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Map.entry;

public class MDFetcher {

	private static final Logger logger = LogManager.getLogger(MDFetcher.class);
	private static final String API_URL = "https://api.mangadex.org/v2/";

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
	private static final HashBiMap<String, String> languages = HashBiMap.create(Map.ofEntries(
			entry("sa", "Arabic"),
			entry("bd", "Bengali"),
			entry("bg", "Bulgarian"),
			entry("mm", "Burmese"),
			entry("ct", "Catalan"),
			entry("cn", "Chinese (Simplified)"),
			entry("hk", "Chinese (Traditional)"),
			entry("cz", "Czech"),
			entry("dk", "Danish"),
			entry("nl", "Dutch"),
			entry("gb", "English"),
			entry("ph", "Filipino"),
			entry("fi", "Finnish"),
			entry("fr", "French"),
			entry("de", "German"),
			entry("gr", "Greek"),
			entry("il", "Hebrew"),
			entry("in", "Hindi"),
			entry("hu", "Hungarian"),
			entry("id", "Indonesian"),
			entry("it", "Italian"),
			entry("jp", "Japanese"),
			entry("kr", "Korean"),
			entry("lt", "Lithuanian"),
			entry("my", "Malay"),
			entry("mn", "Mongolian"),
			entry("", "Other"),
			entry("ir", "Persian"),
			entry("pl", "Polish"),
			entry("br", "Portuguese (Brazil)"),
			entry("pt", "Portuguese (Portugal)"),
			entry("ro", "Romanian"),
			entry("ru", "Russian"),
			entry("rs", "Serbo-Croatian"),
			entry("es", "Spanish (Spain)"),
			entry("mx", "Spanish (Latin America)"),
			entry("se", "Swedish"),
			entry("th", "Thai"),
			entry("tr", "Turkish"),
			entry("ua", "Ukrainian"),
			entry("vn", "Vietnamese")
	));
	private static HashMap<Integer, ImmutableTriple<String, String, String>> tagCache = null;

	private static final HttpClientConnectionManager connManager = new PoolingHttpClientConnectionManager();
	// Manga identifiers
	private final String url;
	private final int id;

	//--------------------------------
	// INSTANCE FIELDS
	//--------------------------------
	// Manga title
	private String title;

	// Basic information
	private HashSet<String> authors;
	private HashSet<String> artists;
	private String demographic;
	private String status;
	private String language;
	private String fullDescription;
	private String description;

	// Gallery tags
	private HashSet<String> allTags;
	private HashSet<String> format;
	private HashSet<String> genre;
	private HashSet<String> theme;
	private HashSet<String> content;

	// Gallery metadata
	private int totalChapters;
	private ArrayList<Chapter> recentChapters;
	private String thumbnail;
	private boolean isHentai;
	private int comments;
	private int views;
	private int follows;
	private double rating;

	private Instant lastUpdated;

	public MDFetcher(String url, @Nullable DBHandler database) throws IOException {
		this.url = url;

		Pattern idPattern = Pattern.compile("https://mangadex\\.org/title/(\\d+)/");
		Matcher idMatcher = idPattern.matcher(url);

		if (idMatcher.find()) {
			this.id = Integer.parseInt(idMatcher.group(1));
		} else {
			// Something has gone seriously wrong
			logger.error("ID not found: from URL " + url);
			throw new NotFoundException("ID not found");
		}

		if (database != null) {
			boolean cached = database.loadFromCache(this);
			if (cached) {
				// Found it in the caches (and the data has already been loaded). We can stop here.
				return;
			}
		}

		loadFields(MDApiRequest(id + "?include=chapters"), database);
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
		if (response.getInt("code") != 200) {
			logger.error("Something went wrong with the HTTP status code.");
			logger.error(response.toString(4));
			throw new NotFoundException("MangaDex request failed");
		}

		JSONObject data = response.getJSONObject("data").getJSONObject("manga");
		title = data.getString("title");

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

		totalChapters = response.getJSONObject("data").getJSONArray("chapters").length();
		recentChapters = response.getJSONObject("data").getJSONArray("chapters").toList()
				.stream().map(o -> (JSONObject) o).map(obj -> new Chapter(
					//TODO fill this in
				)).collect(Collectors.toCollection(ArrayList::new));

		//TODO fix caching
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

	class Chapter {
		private int id;
		private String hash;
		private String title;
		private String volume;
		private String chapter;
		private String language;
		private ArrayList<Integer> groups;
		private Instant timestamp;
		private int threadId;
		private int comments;
		private int views;

		public Chapter(int id, String hash, String title, String volume, String chapter, String language, ArrayList<Integer> groups, Instant timestamp, int threadId, int comments, int views) {
			this.id = id;
			this.hash = hash;
			this.title = title;
			this.volume = volume;
			this.chapter = chapter;
			this.language = language;
			this.groups = groups;
			this.timestamp = timestamp;
			this.threadId = threadId;
			this.comments = comments;
			this.views = views;
		}

		//TODO: Remove this (temporary)
		public Chapter() {

		}

		@Override
		public String toString() {
			//TODO: Convert to string format for Markdown
		}

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public String getHash() {
			return hash;
		}

		public void setHash(String hash) {
			this.hash = hash;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getVolume() {
			return volume;
		}

		public void setVolume(String volume) {
			this.volume = volume;
		}

		public String getChapter() {
			return chapter;
		}

		public void setChapter(String chapter) {
			this.chapter = chapter;
		}

		public String getLanguage() {
			return language;
		}

		public void setLanguage(String language) {
			this.language = language;
		}

		public ArrayList<Integer> getGroups() {
			return groups;
		}

		public void setGroups(ArrayList<Integer> groups) {
			this.groups = groups;
		}

		public Instant getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(Instant timestamp) {
			this.timestamp = timestamp;
		}

		public int getThreadId() {
			return threadId;
		}

		public void setThreadId(int threadId) {
			this.threadId = threadId;
		}

		public int getComments() {
			return comments;
		}

		public void setComments(int comments) {
			this.comments = comments;
		}

		public int getViews() {
			return views;
		}

		public void setViews(int views) {
			this.views = views;
		}
	}

	//-----------------------------------------------------
	// The rest is just getters and setters.
	// Don't bother scrolling.
	//-----------------------------------------------------

	public String getUrl() {
		return url;
	}

	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public HashSet<String> getAuthors() {
		return authors;
	}

	public void setAuthors(HashSet<String> authors) {
		this.authors = authors;
	}

	public HashSet<String> getArtists() {
		return artists;
	}

	public void setArtists(HashSet<String> artists) {
		this.artists = artists;
	}

	public String getDemographic() {
		return demographic;
	}

	public void setDemographic(String demographic) {
		this.demographic = demographic;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getFullDescription() {
		return fullDescription;
	}

	public void setFullDescription(String fullDescription) {
		this.fullDescription = fullDescription;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public HashSet<String> getAllTags() {
		return allTags;
	}

	public void setAllTags(HashSet<String> allTags) {
		this.allTags = allTags;
	}

	public HashSet<String> getFormat() {
		return format;
	}

	public void setFormat(HashSet<String> format) {
		this.format = format;
	}

	public HashSet<String> getGenre() {
		return genre;
	}

	public void setGenre(HashSet<String> genre) {
		this.genre = genre;
	}

	public HashSet<String> getTheme() {
		return theme;
	}

	public void setTheme(HashSet<String> theme) {
		this.theme = theme;
	}

	public HashSet<String> getContent() {
		return content;
	}

	public void setContent(HashSet<String> content) {
		this.content = content;
	}

	public ArrayList<String> getRecentChapters() {
		return recentChapters;
	}

	public void setRecentChapters(ArrayList<String> recentChapters) {
		this.recentChapters = recentChapters();
	}

	public int getTotalChapters() {
		return totalChapters;
	}

	public void setTotalChapters(int totalChapters) {
		this.totalChapters = totalChapters;
	}

	public boolean isHentai() {
		return isHentai;
	}

	public void setHentai(boolean hentai) {
		isHentai = hentai;
	}

	public int getComments() {
		return comments;
	}

	public void setComments(int comments) {
		this.comments = comments;
	}

	public int getViews() {
		return views;
	}

	public void setViews(int views) {
		this.views = views;
	}

	public int getFollows() {
		return follows;
	}

	public void setFollows(int follows) {
		this.follows = follows;
	}

	public double getRating() {
		return rating;
	}

	public void setRating(double rating) {
		this.rating = rating;
	}

	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}
}
