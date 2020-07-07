package bot.commands;

import bot.Wiretap;
import bot.modules.EmbedGenerator;
import bot.modules.TagChecker;
import bot.modules.Validator;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Pattern;

import static bot.modules.Validator.siteValidate;

public class Info {
	private static final Logger logger = LogManager.getLogger(Info.class);

	public static void sendInfo(MessageChannel channel, String args, User author) {
		if(Pattern.compile("^\\d+$").matcher(args).find()) {
			args = "https://nhentai.net/g/" + Integer.parseInt(args) + "/";
		}
		Validator.ArgType validate = siteValidate(args, channel);
		if(!validate.isValid()) {
			return;
		}
		try {
			HashSet<String> checkedTags;

			if (args.get(1).replaceAll("-", "x").contains("exhentai")) {
				EHFetcher tagGetter = new EHFetcher(args.get(1));
				checkedTags = TagChecker.tagCheckWithWarnings(tagGetter.getTags());
			} else {
				SoupPitcher tagGetter = new SoupPitcher(args.get(1));
				checkedTags = TagChecker.tagCheckWithWarnings(tagGetter.getTags());
			}
			if (checkedTags.isEmpty()) {
				channel.sendTyping().complete();
				channel.sendMessage(InfoBuilder.getInfoEmbed(args.get(1))).queue();
			} else if (checkedTags.get(0).equals("lolicon") || checkedTags.get(0).equals("shotacon")) {
				channel.sendMessage(EmbedGenerator.createAlertEmbed("Bot Alert", "This doujin violates the Discord ToS", "This doujin contained the following illegal tags:\n" + String.join(", ", checkedTags))).queue();
			} else {
				EmbedBuilder alter = new EmbedBuilder(EmbedGenerator.createAlertEmbed("Bot Alert",
						"This doujin has potentially dangerous tags",
						"This doujin contained the following tags:\n" + String.join(", ", checkedTags)));
				alter.addField("Continue?", "To continue, press the checkmark.", false);

				channel.sendMessage(alter.build()).queue(
						success -> {
							success.addReaction("U+2705").queue();
							success.addReaction("U+274C").queue()

							Wiretap.registerSuspect(success.getId(), success.getAuthor().getId());
						}
				);
			}
		} catch (HttpStatusException e) {
			channel.sendMessage("Can't find page: returned error code " + e.getStatusCode()).queue();
		} catch (IOException e) {
			channel.sendMessage("An error occurred. Please try again, or ping my owner if this persists.").queue();
			e.printStackTrace();
		}
	}
}
