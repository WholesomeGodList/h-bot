package bot.sites.nhentai;

import bot.modules.DBHandler;
import bot.sites.SiteFetcher;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class NHFetcher implements SiteFetcher {
	private static final Logger logger = LogManager.getLogger(NHFetcher.class);

	/**
	 * Creates a new NHFetcher object and populates the fields with information.
	 *
	 * @param url      The nhentai URL to fetch information about.
	 * @param database A DBHandler object (to get the cache from). If null, caching will be disabled.
	 * @throws IOException If something goes wrong with the fetching, an IOException is thrown.
	 */
	public NHFetcher(String url, @Nullable DBHandler database) throws IOException {
		logger.debug("Connecting to " + url);
		this.url = url;

		if (database != null) {
			boolean cached = database.loadFromCache(this);
			if (cached) {
				// Found it in the caches (and the data has already been loaded). We can stop here.
				return;
			}
		}

		Document doc = Jsoup.connect(url).get();

		// Load in the fields
		loadFields(doc, database);
	}
	//--------------------------------
	// INSTANCE FIELDS
	//--------------------------------

	// Doujin identifiers
	private final String url;

	// Doujin titles
	private String title;
	private String titleJapanese;

	// Basic information
	private HashSet<String> artists;
	private HashSet<String> groups;
	private HashSet<String> parodies;
	private HashSet<String> chars;
	private String language;

	// Doujin tags
	private HashSet<String> tags;

	// Doujin metadata
	private int pages;
	private String thumbnailUrl;
	private int favorites;
	private Instant timePosted;

	private void loadFields(Document doc, @Nullable DBHandler database) {
		// Oh boy, parsing time.
		Pattern titleRegex = Pattern.compile(
				"^(?:\\s*(?:=.*?=|<.*?>|\\[.*?]|\\(.*?\\)|\\{.*?})\\s*)*" +
				"(?:[^\\[|\\](){}<>=]*\\s*\\|\\s*)?([^\\[|\\](){}<>=]*?)" +
				"(?:\\s*(?:=.*?=|<.*?>|\\[.*?]|\\(.*?\\)|\\{.*?})\\s*)*$"
		);

		String title = doc.select("h1.title").first() == null ? "None" : doc.select("h1.title").first().text().trim();
		String titleJapanese = doc.select("h2.title").first() == null ? "None" : doc.select("h2.title").first().text().trim();

		Matcher titleMatcher = titleRegex.matcher(title);
		Matcher japaneseTitleMatcher = titleRegex.matcher(titleJapanese);

		this.title = titleMatcher.find() ? titleMatcher.group(1).trim().isEmpty() ? title : titleMatcher.group(1).trim() : title;
		this.titleJapanese = japaneseTitleMatcher.find() ? japaneseTitleMatcher.group(1).trim().isEmpty() ? titleJapanese : japaneseTitleMatcher.group(1).trim() : titleJapanese;

		artists = tagSearch("/artist/(.*?)/?$", doc).stream()
				.map(WordUtils::capitalize).collect(Collectors.toCollection(HashSet::new));
		groups = tagSearch("/group/(.*?)/?$", doc);
		parodies = tagSearch("/parody/(.*?)/?$", doc);
		chars = tagSearch("/character/(.*?)/?$", doc);
		language = WordUtils.capitalize(tagSearch("/language/(.*?)/?$", doc).stream()
				.filter((str) -> !(str.contains("translated") || str.contains("rewrite")))
				.findFirst().orElse(null));

		tags = tagSearch("/tag/(.*?)/?$", doc);

		// here come some nasty one liners
		pages = Integer.parseInt(doc.select("span.tags").select("a").stream().filter(div ->
				div.attr("href").contains("pages")).findFirst().orElseThrow()
				.select("span.name").text());
		thumbnailUrl = doc.select("div#cover").select("img").first().attr("data-src");
		favorites = Integer.parseInt(Pattern.compile("\\((\\d+)\\)").matcher(
				doc.select("span.nobold").last().text())
				.results().map(m -> m.group(1)).findFirst().orElseThrow());
		timePosted = Instant.parse(doc.select("time").first().attr("datetime"));

		if (database != null) {
			// Cache the data loaded
			logger.debug("Caching entry...");
			database.cache(this);
		}
	}

	private HashSet<String> tagSearch(String pattern, Document doc) {
		return doc.select("a[href~=" + pattern + "]").stream().map(link -> link.select("span.name").text())
				.collect(Collectors.toCollection(HashSet::new));
	}

	//-----------------------------------------------------
	// The rest is just getters and setters.
	// Don't bother scrolling.
	//-----------------------------------------------------

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public String getTitle() {
		return title;
	}

	@Override
	public String getTitleJapanese() {
		return titleJapanese;
	}

	@Override
	public HashSet<String> getArtists() {
		return artists;
	}

	@Override
	public HashSet<String> getGroups() {
		return groups;
	}

	@Override
	public HashSet<String> getParodies() {
		return parodies;
	}

	@Override
	public HashSet<String> getChars() {
		return chars;
	}

	@Override
	public String getLanguage() {
		return language;
	}

	@Override
	public HashSet<String> getTags() {
		return tags;
	}

	@Override
	public int getPages() {
		return pages;
	}

	@Override
	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public int getFavorites() {
		return favorites;
	}

	@Override
	public Instant getTimePosted() {
		return timePosted;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setTitleJapanese(String titleJapanese) {
		this.titleJapanese = titleJapanese;
	}

	public void setArtists(HashSet<String> artists) {
		this.artists = artists;
	}

	public void setGroups(HashSet<String> groups) {
		this.groups = groups;
	}

	public void setParodies(HashSet<String> parodies) {
		this.parodies = parodies;
	}

	public void setChars(HashSet<String> chars) {
		this.chars = chars;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public void setTags(HashSet<String> tags) {
		this.tags = tags;
	}

	public void setPages(int pages) {
		this.pages = pages;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public void setFavorites(int favorites) {
		this.favorites = favorites;
	}

	public void setTimePosted(Instant timePosted) {
		this.timePosted = timePosted;
	}
}
