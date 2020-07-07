package bot.sites.ehentai;

import bot.sites.SiteFetcher;
import org.apache.commons.text.WordUtils;
import org.json.JSONArray;
import org.json.JSONObject;

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

	public EHFetcher(String url, EHApiHandler handler) throws ExecutionException, InterruptedException, IOException {
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
		JSONObject galleryData = data.get();

		loadFields(galleryData);
	}

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
				.stream().filter((str) -> str.contains("translated")).findFirst().orElse(null));

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
	//
	// The rest are just autogenerated getters and setters.
	// Don't bother scrolling.
	//
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
}
