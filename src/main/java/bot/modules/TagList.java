package bot.modules;

import java.util.HashSet;
import java.util.Arrays;

public class TagList {
	/**
	 * The list of tags that are the highest severity.
	 * These will always be warned about / excluded from searches.
	 */
	private static final HashSet<String> badTags = new HashSet<>(
			Arrays.asList(
					"netorare", "netori", "scat", "bestiality", "gigantic", "drugs", "blackmail", "horse", "fisting",
					"vore", "guro", "nose hook", "urination", "blood", "cheating", "dog", "pig", "corruption", "mind control",
					"vomit", "bbm", "cannibalism", "tentacles", "rape", "snuff", "moral degeneration", "mind break", "humiliation",
					"chikan", "ryona", "piss drinking", "prostitution", "cum bath", "infantilism", "unbirth", "abortion",
					"eye penetration", "urethra insertion", "chloroform", "parasite", "public use", "petrification", "necrophilia",
					"brain fuck", "daughter", "torture", "birth"
			)
	);

	/**
	 * The list of unwholesome tags.
	 * These are slightly lower severity than the bad tags.
	 */
	private static final HashSet<String> unwholesomeTags = new HashSet<>(
			Arrays.asList(
					"amputee", "futanari", "gender bender", "human on furry", "group", "lactation", "femdom",
					"ffm threesome", "double penetration", "gag", "harem", "collar", "strap-on", "inflation", "mmf threesome", "enema",
					"bukkake", "bbw", "dick growth", "big areolae", "huge breasts", "slave", "gaping", "shemale", "pegging",
					"triple penetration", "prolapse", "human pet", "foot licking", "milking", "bondage", "multiple penises",
					"asphyxiation", "stuck in wall", "human cattle", "clit growth", "ttf threesome", "phimosis", "glory hole",
					"eggs", "incest"
			)
	);

	/**
	 * The highest severity of tags.
	 * A doujin having any of these tags forbids the bot from saying anything about it.
	 * These tags probably violate the Discord ToS.
	 */
	private static final HashSet<String> illegalTags = new HashSet<>(
			Arrays.asList(
					"lolicon", "shotacon"
			)
	);

	/**
	 * Returns a copy of the illegal tags.
	 * @return The illegal tags.
	 */
	public static HashSet<String> getIllegalTags() {
		return new HashSet<>(illegalTags);
	}

	/**
	 * Returns a copy of the bad tags.
	 * @return The bad tags.
	 */
	public static HashSet<String> getBadTags() {
		return new HashSet<>(badTags);
	}

	/**
	 * Returns a copy of the unwholesome tags.
	 * @return The unwholesome tags.
	 */
	public static HashSet<String> getUnwholesomeTags() {
		return new HashSet<>(unwholesomeTags);
	}

	/**
	 * Returns a copy of the unwholesome tags, WITHOUT any tags contained
	 * within the query.
	 * @param query The query to be excluded from the tag list.
	 * @return The tag list without tags contained in the query.
	 */
	public static HashSet<String> nonWholesomeTagsWithoutQuery(String query) {
		HashSet<String> nonWT = getUnwholesomeTags();

		// Remove tag if query contains that tag
		nonWT.removeIf(query::contains);
		return nonWT;
	}
}
