package bot.sites;

import java.time.Instant;
import java.util.HashSet;

public interface SiteFetcher {
	String getUrl();

	String getThumbnailUrl();

	Instant getTimePosted();

	int getPages();

	String getTitle();

	String getTitleJapanese();

	String getLanguage();

	HashSet<String> getTags();

	HashSet<String> getParodies();

	HashSet<String> getChars();

	HashSet<String> getArtists();

	HashSet<String> getGroups();
}
