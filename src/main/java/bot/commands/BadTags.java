package bot.commands;

import bot.modules.TagList;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class BadTags {
	public static MessageEmbed badTagEmbed() {
		HashSet<String> badTags = TagList.getBadTags();
		HashSet<String> nonWholesomeTags = TagList.getUnwholesomeTags();

		return new EmbedBuilder().build();
	}

	private static ImmutableTriple<String, String, String> tripartition(HashSet<String> tags) {
		ArrayList<String> list = new ArrayList<>(tags);
		Collections.sort(list);

		int s = list.size();

		return new ImmutableTriple<>(
				String.join("\n", list.subList(0, s / 3 + 1)),
				String.join("\n", list.subList(s / 3 + 1, 2 * s / 3 + 1)),
				String.join("\n", list.subList(2 * s / 3 + 1, s))
		);
	}
}
