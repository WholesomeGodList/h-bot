package bot.sites.ehentai;

import bot.modules.DBHandler;
import bot.sites.SiteFetcher;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nullable;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static bot.sites.ehentai.EHApiHandler.EHApiRequest;

public class EHFetcher implements SiteFetcher {
	private final static Logger logger = LogManager.getLogger(EHFetcher.class);

	/**
	 * An enum representing the different categories
	 * of entries in e-hentai.
	 */
	public enum Category {
		DOUJINSHI("Doujinshi"),
		MANGA("Manga"),
		ARTIST_CG("Artist CG"),
		GAME_CG("Game CG"),
		WESTERN("Western"),
		IMAGE_SET("Image Set"),
		NON_H("Non-H"),
		COSPLAY("Cosplay"),
		ASIAN_PORN("Asian Porn"),
		MISC("Misc"),
		PRIVATE("Private");

		private final String name;
		Category(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}
	}

	/**
	 * Creates a new EHFetcher object and populates the fields with information.
	 * @param url The e-hentai URL to fetch information about.
	 * @param handler An EHApiHandler object to use to fetch the information from.
	 * @param database A DBHandler object (to get the cache from). If null, caching will be disabled.
	 * @throws IOException If something goes wrong with the fetching, an IOException is thrown.
	 */
	public EHFetcher(String url, EHApiHandler handler, @Nullable DBHandler database) throws IOException {
		// Handle URL
		url = url.trim().replace("http://", "https://");
		this.url = url;

		Pattern galleryPattern = Pattern.compile("https?://e[x\\-]hentai\\.org/g/(\\d+)/([\\da-f]{10})/");
		Pattern pagePattern = Pattern.compile("https?://e[x\\-]hentai\\.org/s/([\\da-f]{10})/(\\d+)-(\\d+)/");

		Matcher galleryMatcher = galleryPattern.matcher(url);
		Matcher pageMatcher = pagePattern.matcher(url);

		if (galleryMatcher.find()) {
			galleryId = Integer.parseInt(galleryMatcher.group(1));
			galleryToken = galleryMatcher.group(2);
		} else if (pageMatcher.find()){
			//TODO: Make this also handled in EHApiHandler if the bot gets too large
			String pageId = pageMatcher.group(1);
			galleryId = Integer.parseInt(pageMatcher.group(2));
			int pageNum = Integer.parseInt(pageMatcher.group(3));

			JSONObject payload = new JSONObject();
			payload.put("method", "gtoken");

			JSONArray pageContainer = new JSONArray();
			JSONArray page = new JSONArray();

			page.put(galleryId);
			page.put(pageId);
			page.put(pageNum);

			pageContainer.put(page);

			payload.put("pagelist", pageContainer);

			galleryToken = EHApiRequest(payload).getJSONArray("tokenlist").getJSONObject(0).getString("token");
		}
		CompletableFuture<JSONObject> data = handler.galleryQuery(this);
		try {
			JSONObject galleryData = data.get();
			loadFields(galleryData);
		} catch (ExecutionException | InterruptedException e) {
			logger.error("Fetching failed - exception details:");
			e.printStackTrace();
		}
	}

	//--------------------------------
	// INSTANCE FIELDS
	//--------------------------------

	// Gallery identifiers
	private final String url;
	private int galleryId;
	private String galleryToken;

	// Gallery titles
	private String title;
	private String titleJapanese;

	// Basic information
	private HashSet<String> artists;
	private HashSet<String> groups;
	private HashSet<String> parodies;
	private HashSet<String> chars;
	private String language;

	// Gallery tags
	private HashSet<String> maleTags;
	private HashSet<String> femaleTags;
	private HashSet<String> miscTags;

	// Gallery metadata
	private Category category;
	private int pages;
	private String uploader;
	private String thumbnailUrl;
	private double rating;
	private Instant timePosted;

	private void loadFields(JSONObject data) {
		Pattern titleRegex = Pattern.compile("^(?:\\s*(?:=.*?=|<.*?>|\\[.*?]|\\(.*?\\)|\\{.*?})\\s*)*(?:[^\\[|\\](){}<>]*\\s*\\|\\s*)?([^\\[|\\](){}<>]*?)(?:\\s*(?:=.*?=|<.*?>|\\[.*?]|\\(.*?\\)|\\{.*?})\\s*)*$");
		Matcher titleMatcher = titleRegex.matcher(data.getString("title"));
		Matcher japaneseTitleMatcher = titleRegex.matcher(data.getString("title_jpn"));

		title = titleMatcher.find() ? titleMatcher.group(1).trim() : data.getString("title");
		titleJapanese = japaneseTitleMatcher.find() ? japaneseTitleMatcher.group(1).trim() : data.getString("title_jpn");

		HashSet<String> allTags = new HashSet<>();

		for(Object cur : data.getJSONArray("tags")) {
			allTags.add(cur.toString());
		}

		artists = searchList("artist:(.*)$", allTags);
		groups = searchList("group:(.*)$", allTags);
		parodies = searchList("parody:(.*)$", allTags);
		chars = searchList("character:(.*)$", allTags);
		language = WordUtils.capitalize(searchList("language:(.*)$", allTags)
				.stream().filter((str) -> !(str.contains("translated") || str.contains("rewrite")))
				.findFirst().orElse(null));

		maleTags = searchList("^male:(.*)$", allTags);
		femaleTags = searchList("female:(.*)$", allTags);
		miscTags = searchList("^([^:]*)$", allTags);

		String categoryName = data.getString("category");
		category = Arrays.stream(Category.values())
				.filter((c) -> c.toString().equals(categoryName)).findFirst().orElse(null);
		pages = Integer.parseInt(data.getString("filecount").trim());
		uploader = data.getString("uploader");
		thumbnailUrl = data.getString("thumb");
		rating = Double.parseDouble(data.getString("rating").trim());
		timePosted = Instant.ofEpochSecond(Long.parseLong(data.getString("posted").trim()));
	}

	private HashSet<String> searchList(String regex, HashSet<String> list) {
		HashSet<String> results = new HashSet<>();

		Pattern re = Pattern.compile(regex);
		for(String cur : list) {
			Matcher matcher = re.matcher(cur);
			if(matcher.find()) {
				results.add(matcher.group(1));
			}
		}

		return results;
	}

	@Override
	public HashSet<String> getTags() {
		HashSet<String> tags = new HashSet<>();
		tags.addAll(getMaleTags());
		tags.addAll(getFemaleTags());
		tags.addAll(getMiscTags());

		return tags;
	}

	//-----------------------------------------------------
	// The rest is just getters and setters.
	// Don't bother scrolling.
	//-----------------------------------------------------

	public int getGalleryId() {
		return galleryId;
	}

	public String getGalleryToken() {
		return galleryToken;
	}

	@Override
	public String getUrl() {
		return url;
	}

	@Override
	public Instant getTimePosted() {
		return timePosted;
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

	public HashSet<String> getMaleTags() {
		return maleTags;
	}

	public HashSet<String> getFemaleTags() {
		return femaleTags;
	}

	public HashSet<String> getMiscTags() {
		return miscTags;
	}

	public Category getCategory() {
		return category;
	}

	@Override
	public int getPages() {
		return pages;
	}

	public String getUploader() {
		return uploader;
	}

	public String getThumbnailUrl() {
		return thumbnailUrl;
	}

	public double getRating() {
		return rating;
	}

	public void setGalleryId(int galleryId) {
		this.galleryId = galleryId;
	}

	public void setGalleryToken(String galleryToken) {
		this.galleryToken = galleryToken;
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

	public void setMaleTags(HashSet<String> maleTags) {
		this.maleTags = maleTags;
	}

	public void setFemaleTags(HashSet<String> femaleTags) {
		this.femaleTags = femaleTags;
	}

	public void setMiscTags(HashSet<String> miscTags) {
		this.miscTags = miscTags;
	}

	public void setCategory(Category category) {
		this.category = category;
	}

	public void setCategoryByName(String categoryName) {
		category = Arrays.stream(Category.values())
				.filter((c) -> c.toString().equals(categoryName)).findFirst().orElse(null);
	}


	public void setPages(int pages) {
		this.pages = pages;
	}

	public void setUploader(String uploader) {
		this.uploader = uploader;
	}

	public void setThumbnailUrl(String thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public void setRating(double rating) {
		this.rating = rating;
	}

	public void setTimePosted(Instant timePosted) {
		this.timePosted = timePosted;
	}
}
