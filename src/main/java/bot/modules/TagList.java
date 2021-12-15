package bot.modules;

import java.util.Arrays;
import java.util.HashSet;

public class TagList {
	/**
	 * The list of tags that are the highest severity.
	 * These will always be warned about / excluded from searches.
	 */
	private static final HashSet<String> badTags = new HashSet<>(
			Arrays.asList(
					"netorare", "netori", "scat", "bestiality", "gigantic", "drugs", "blackmail", "horse",
					"vore", "guro", "nose hook", "blood", "cheating", "dog", "pig", "corruption", "mind control",
					"vomit", "bbm", "cannibalism", "tentacles", "rape", "snuff", "moral degeneration", "mind break", "humiliation",
					"chikan", "ryona", "cum bath", "infantilism", "unbirth", "abortion",
					"eye penetration", "urethra insertion", "chloroform", "parasite", "public use", "petrification", "necrophilia",
					"brain fuck", "daughter", "torture", "birth", "minigirl", "menstruation", "anorexic", "age regression",
					"shrinking", "giantess", "navel fuck", "possession", "miniguy", "nipple fuck", "cbt", "low scat", "dicknipples",
					"nipple birth", "monkey", "nazi", "triple anal", "triple vaginal", "diaper", "aunt", "mother", "father", 
					"niece", "uncle", "grandfather", "grandmother", "granddaughter", "insect", "anal birth", "skinsuit", "vacbed",
					"sleeping"
			)
	);

	/**
	 * The list of unwholesome tags.
	 * These are slightly lower severity than the bad tags.
	 */
	private static final HashSet<String> unwholesomeTags = new HashSet<>(
			Arrays.asList(
					"amputee", "futanari", "gender bender", "human on furry", "group", "lactation", "femdom",
					"ffm threesome", "double penetration", "gag", "harem", "strap-on", "inflation", "mmf threesome", "enema",
					"bukkake", "bbw", "dick growth", "huge breasts", "slave", "gaping", "pegging", "smegma",
					"triple penetration", "prolapse", "human pet", "foot licking", "milking", "bondage", "multiple penises",
					"asphyxiation", "stuck in wall", "human cattle", "clit growth", "ttf threesome", "phimosis", "glory hole",
					"eggs", "incest", "urination", "prostitution", "fisting", "piss drinking", "inseki", "feminization",
					"old lady", "old man", "mmm threesome", "fff threesome", "all the way through", "farting"
			)
	);

	/**
	 * The highest severity of tags.
	 * A doujin having any of these tags forbids the bot from saying anything about it.
	 * These tags probably violate the Discord ToS.
	 */
	private static final HashSet<String> illegalTags = new HashSet<>(
			Arrays.asList(
					"lolicon", "shotacon", "oppai loli", "low shotacon", "low lolicon"
			)
	);

	/**
	 * Returns a copy of the illegal tags.
	 *
	 * @return The illegal tags.
	 */
	public static HashSet<String> getIllegalTags() {
		return new HashSet<>(illegalTags);
	}

	/**
	 * Returns a copy of the bad tags.
	 *
	 * @return The bad tags.
	 */
	public static HashSet<String> getBadTags() {
		return new HashSet<>(badTags);
	}

	/**
	 * Returns a copy of the unwholesome tags.
	 *
	 * @return The unwholesome tags.
	 */
	public static HashSet<String> getUnwholesomeTags() {
		return new HashSet<>(unwholesomeTags);
	}

	/**
	 * Returns a copy of the unwholesome tags, WITHOUT any tags contained
	 * within the query.
	 *
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
