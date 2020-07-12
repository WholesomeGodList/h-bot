package bot.modules;

import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nullable;
import java.util.HashSet;

public class TagChecker {
	public enum TagStatus {
		ILLEGAL,
		HAS_BAD_TAGS,
		OK
	}

	/**
	 * Gets the hash set of all bad tags inside the tag HashSet given.
	 * If there are none, this method returns an empty HashSet.
	 * @param tags The tags you want to check.
	 * @param includeWarnings Whether to check warnings or not.
	 * @return The tags inside tags that were bad.
	 */
	public static ImmutablePair<TagStatus, HashSet<String>> tagCheck(HashSet<String> tags, boolean includeWarnings) {
		return tagCheck(tags, includeWarnings, null);
	}

	/**
	 * Gets the hash set of all bad tags inside the tag HashSet given.
	 * If there are none, this method returns an empty HashSet.
	 * @param tags The tags you want to check.
	 * @param includeWarnings Whether to check warnings or not.
	 * @param query A query to exclude from the tag check.
	 * @return The tags inside tags that were bad.
	 */
	public static ImmutablePair<TagStatus, HashSet<String>> tagCheck(HashSet<String> tags, boolean includeWarnings, @Nullable String query) {
		HashSet<String> badTags = TagList.getBadTags();
		if(includeWarnings) {
			badTags.addAll(query == null ? TagList.getUnwholesomeTags() : TagList.nonWholesomeTagsWithoutQuery(query));
		}

		HashSet<String> illegalTags = TagList.getIllegalTags();

		illegalTags.retainAll(tags);
		if(!illegalTags.isEmpty()) {
			return new ImmutablePair<>(TagStatus.ILLEGAL, illegalTags);
		}

		badTags.retainAll(tags);
		if(!badTags.isEmpty()) {
			return new ImmutablePair<>(TagStatus.HAS_BAD_TAGS, badTags);
		}
		return new ImmutablePair<>(TagStatus.OK, badTags);
	}
}
