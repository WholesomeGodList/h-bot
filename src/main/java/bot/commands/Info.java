package bot.commands;

import bot.Wiretap;
import bot.modules.DBHandler;
import bot.modules.EmbedGenerator;
import bot.modules.TagChecker;
import bot.modules.Validator;
import bot.sites.ehentai.EHApiHandler;
import bot.sites.ehentai.EHFetcher;
import bot.sites.nhentai.NHFetcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Pattern;

import static bot.modules.Validator.siteValidate;

public class Info {
	private static final Logger logger = LogManager.getLogger(Info.class);

	public static void sendInfo(MessageChannel channel, String args, User author, EHApiHandler handler, DBHandler database) {
		if(Pattern.compile("^\\d+$").matcher(args).find()) {
			args = "https://nhentai.net/g/" + Integer.parseInt(args) + "/";
		}
		Validator.ArgType validate = siteValidate(args, channel);
		if(!validate.isValid()) {
			return;
		}
		try {
			ImmutablePair<TagChecker.TagStatus, HashSet<String>> checker;
			MessageEmbed embed;

			if (validate == Validator.ArgType.EHENTAI) {
				EHFetcher eh = new EHFetcher(args, handler, database);
				checker = TagChecker.tagCheck(eh.getTags(), true);
				embed = EmbedGenerator.getDoujinInfoEmbed(eh);
			} else if (validate == Validator.ArgType.NHENTAI) {
				NHFetcher nh = new NHFetcher(args, database);
				checker = TagChecker.tagCheck(nh.getTags(), true);
				embed = EmbedGenerator.getDoujinInfoEmbed(nh);
			} else {
				return;
			}

			switch(checker.getLeft()) {
				case OK -> {
					// It's good. Send the embed.
					channel.sendTyping().complete();
					channel.sendMessage(embed).queue();
				}
				case ILLEGAL -> // It's not legal. Send another embed.
						channel.sendMessage(EmbedGenerator.createAlertEmbed("Bot Alert",
								"This doujin violates the Discord ToS",
								"This doujin contained the following illegal tags:\n" +
										EmbedGenerator.display(checker.getRight()))).queue();
				case HAS_BAD_TAGS -> {
					EmbedBuilder alter = new EmbedBuilder(EmbedGenerator.createAlertEmbed("Bot Alert",
							"This doujin has potentially dangerous tags",
							"This doujin contained the following tags:\n" +
									EmbedGenerator.display(checker.getRight())));
					alter.addField("Continue?", "To continue, press the checkmark.", false);

					channel.sendMessage(alter.build()).queue(
							success -> {
								success.addReaction("U+2705").queue();
								success.addReaction("U+274C").queue();
								Wiretap.registerSuspect(success.getId(), success.getAuthor().getId());
							}
					);
				}
			}
		} catch (HttpStatusException e) {
			channel.sendMessage("Can't find page: returned error code " + e.getStatusCode()).queue();
		} catch (IOException e) {
			channel.sendMessage("An error occurred. Please try again, or ping my owner if this persists.").queue();
			e.printStackTrace();
		}
	}
}
