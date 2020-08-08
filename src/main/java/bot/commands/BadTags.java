package bot.commands;

import bot.modules.TagList;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.lang3.tuple.ImmutableTriple;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

public class BadTags {
	/**
	 * Gets the bad tag embed with all the bad tags inside it.
	 * @return The bad tag embed.
	 */
	public static MessageEmbed getBadTagEmbed() {
		HashSet<String> badTags = TagList.getBadTags();
		HashSet<String> unwholesomeTags = TagList.getUnwholesomeTags();

		EmbedBuilder badTagEmbed = new EmbedBuilder();
		ImmutableTriple<String, String, String> badTagFields = tripartition(badTags);
		ImmutableTriple<String, String, String> unwholesomeTagFields = tripartition(unwholesomeTags);

		badTagEmbed.setAuthor("Bad Tags", null, "https://i.redd.it/fkg9yip5yyl21.png");

		addTagFields(badTagEmbed, badTagFields, "The current bad tags:");
		badTagEmbed.addField("--------", "", false);
		addTagFields(badTagEmbed, unwholesomeTagFields, "The current warning tags:");

		badTagEmbed.addField("Illegal tags:", "shotacon\n lolicon\n oppai loli", false);
		badTagEmbed.addField("You can help!", "If you have any tags that you want to add" +
				" to the warning tags list, please tell me (in the bot server (botinfo))!" +
				" I'll add them if I think they're necessary.", false);

		badTagEmbed.setFooter("Built by Stinggyray#1000", "https://images.emojiterra.com/twitter/v12/512px/1f914.png");

		return badTagEmbed.build();
	}

	/**
	 * Adds the three tag fields to the message embed.
	 * @param badTagEmbed The embed to add the fields to.
	 * @param fields An ImmutableTriple representing the field data.
	 * @param name The name of the fields.
	 */
	private static void addTagFields(EmbedBuilder badTagEmbed, ImmutableTriple<String, String, String> fields, String name) {
		badTagEmbed.addField(name, fields.getLeft(), true);
		badTagEmbed.addField(" ", fields.getMiddle(), true);
		badTagEmbed.addField(" ", fields.getRight(), true);
	}

	/**
	 * Partitions a HashSet with length 3 into 3 strings, each made up of 1/3 of the original length.
	 * @param tags The tags to partition.
	 * @return An ImmutableTriple holding 3 strings, each holding 1/3 of the original tags.
	 */
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
