package bot.commands;

import bot.Wiretap;
import bot.modules.DBHandler;
import bot.modules.EmbedGenerator;
import bot.modules.TagChecker;
import bot.modules.Validator;
import bot.sites.NotFoundException;
import bot.sites.SiteFetcher;
import bot.sites.ehentai.EHApiHandler;
import bot.sites.ehentai.EHFetcher;
import bot.sites.godlist.WHFetcher;
import bot.sites.nhentai.NHFetcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.HttpStatusException;

import java.awt.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static bot.modules.Validator.siteValidate;

public class Info {
	private static final Logger logger = LogManager.getLogger(Info.class);

	public static void sendInfo(MessageChannel channel, String args, User author, EHApiHandler handler, DBHandler database) {
		if(args != null && Pattern.compile("^\\d+$").matcher(args).find()) {
			args = "https://nhentai.net/g/" + Integer.parseInt(args) + "/";
		}
		Validator.ArgType validate = siteValidate(args, channel);
		if(!validate.isValid()) {
			return;
		}

		try {
			if(validate == Validator.ArgType.GODLIST) {
				if(args == null) {
					// Something's gone seriously wrong.
					logger.error("Bruh how is args null?????");
					return;
				}
				WHFetcher wh = new WHFetcher(Integer.parseInt(args.substring(1)));
				channel.sendMessage(getDoujinInfoEmbed(wh)).queue();

				if(Validator.getArgType(wh.getLink()) == Validator.ArgType.NHENTAI) {
					channel.sendTyping().complete();
					args = wh.getLink();
					validate = Validator.ArgType.NHENTAI;
				}
				else {
					return;
				}
			}

			ImmutablePair<TagChecker.TagStatus, HashSet<String>> checker;
			MessageEmbed embed;

			EHFetcher eh = null;
			NHFetcher nh = null;

			if (validate == Validator.ArgType.EHENTAI) {
				eh = new EHFetcher(args, handler, database);
				checker = TagChecker.tagCheck(eh.getTags(), true);
				embed = getDoujinInfoEmbed(eh);
			} else if (validate == Validator.ArgType.NHENTAI) {
				nh = new NHFetcher(args, database);
				checker = TagChecker.tagCheck(nh.getTags(), true);
				embed = getDoujinInfoEmbed(nh);
			} else {
				return;
			}

			switch (checker.getLeft()) {
				case OK -> {
					logger.info("No problems found. Sending info embed...");
					// It's good. Send the embed.
					channel.sendTyping().complete();
					channel.sendMessage(embed).queue();
				}
				case ILLEGAL -> {// It's not legal. Send another embed.
					logger.info("It's not legal.");
					channel.sendMessage(EmbedGenerator.createAlertEmbed("Bot Alert",
							"This doujin violates the Discord ToS",
							"This doujin contained the following illegal tags:\n" +
									display(checker.getRight()))).queue();
				}
				case HAS_BAD_TAGS -> {
					logger.info("Problems found. Sending warning embed...");

					EmbedBuilder alter = new EmbedBuilder(EmbedGenerator.createAlertEmbed("Bot Alert",
							"This doujin has potentially dangerous tags",
							"This doujin contained the following tags:\n" +
									display(checker.getRight())));
					alter.addField("Continue?", "To continue, press the checkmark.", false);

					SiteFetcher fetcher = switch (validate) {
						case EHENTAI:
							yield eh;
						case NHENTAI:
							yield nh;
						default:
							yield null;
					};

					channel.sendMessage(alter.build()).queue(
							success -> {
								success.addReaction("U+2705").queue();
								success.addReaction("U+274C").queue();
								Wiretap.registerSuspect(success.getId(), author.getId(), fetcher);
							}
					);
				}
			}
		} catch (NotFoundException e) {
			channel.sendMessage("Something went wrong. Error message: " + (e.getMessage().isEmpty() ? "None" : e.getMessage())).queue();
		} catch (HttpStatusException e) {
			channel.sendMessage("Can't find page: returned error code " + e.getStatusCode()).queue();
		} catch (IOException e) {
			channel.sendMessage("An error occurred. Please try again, or ping my owner if this persists.").queue();
			e.printStackTrace();
		}
	}
	
	public static MessageEmbed getDoujinInfoEmbed(WHFetcher info) {
		EmbedBuilder whInfo = new EmbedBuilder();
		whInfo.setColor(Color.BLACK);

		whInfo.setAuthor("Entry Info", null, "https://i.redd.it/fkg9yip5yyl21.png");
		whInfo.setTitle(info.getTitle(), info.getLink());
		whInfo.setDescription("by " + info.getAuthor());
		whInfo.addField("Warning", info.getWarning(), true);
		whInfo.addField("Tier", info.getTier(), true);
		whInfo.addField("Parody", info.getParody(), true);

		whInfo.addField("Pages", ""+info.getPages(), false);
		whInfo.addField("Tags", display(info.getTags()), false);

		whInfo.setFooter("ID: #" + info.getId());

		return whInfo.build();
	}

	/**
	 * Creates an information embed for a doujin, given an nhentai fetcher.
	 * @param info The NHFetcher object with which to build the embed.
	 * @return A message embed containing all the information about a doujin.
	 */
	public static MessageEmbed getDoujinInfoEmbed(NHFetcher info) {
		EmbedBuilder nhInfo = new EmbedBuilder();
		nhInfo.setColor(Color.BLACK);

		nhInfo.setAuthor("Doujin Info", null, "https://i.redd.it/fkg9yip5yyl21.png");
		nhInfo.setTitle(info.getTitle(), info.getUrl());
		nhInfo.setDescription("by " + WordUtils.capitalize(display(info.getArtists())));
		nhInfo.setImage(info.getThumbnailUrl());

		nhInfo.addField("Language", info.getLanguage(), true);
		nhInfo.addField("Japanese Title", info.getTitleJapanese(), true);
		if (!info.getParodies().isEmpty()) {
			nhInfo.addField("Parody", display(info.getParodies()), true);
			if (!info.getChars().isEmpty()) {
				nhInfo.addField("Characters", display(info.getChars()), true);
			}
		}
		nhInfo.addField("Tags", display(info.getTags()), false);

		nhInfo.setFooter(info.getPages() + " pages | Favorites: " + info.getFavorites() + " | Uploaded:");
		nhInfo.setTimestamp(info.getTimePosted());

		return nhInfo.build();
	}

	/**
	 * Creates an information embed for a doujin, given an ehentai fetcher.
	 * @param info The EHFetcher object with which to build the embed.
	 * @return A message embed containing all the information about a doujin.
	 */
	public static MessageEmbed getDoujinInfoEmbed(EHFetcher info) {
		EmbedBuilder ehInfo = new EmbedBuilder();
		ehInfo.setColor(Color.BLACK);

		ehInfo.setAuthor("Doujin Info", null, "https://cdn.discordapp.com/attachments/607405329206083585/706310427491041390/e-hentaihex.png");
		ehInfo.setTitle(info.getTitle(), info.getUrl());
		ehInfo.setDescription("by " + display(info.getArtists()));
		ehInfo.setImage(info.getThumbnailUrl());

		ehInfo.addField("Language", WordUtils.capitalize(info.getLanguage()), true);
		ehInfo.addField("Japanese Title", info.getTitleJapanese(), true);
		if (!info.getParodies().isEmpty()) {
			ehInfo.addField("Parody", display(info.getParodies()), true);
			if (!info.getChars().isEmpty()) {
				ehInfo.addField("Characters", display(info.getChars()), true);
			}
		}
		ehInfo.addField("Category", info.getCategory().toString(), false);
		ehInfo.addField("Male Tags", display(info.getMaleTags()), true);
		ehInfo.addField("Female Tags", display(info.getFemaleTags()), true);
		ehInfo.addField("Misc Tags", display(info.getMiscTags()), true);

		ehInfo.setFooter(info.getPages() + " pages | Rating: " + info.getRating() + " | Uploaded:", null);
		ehInfo.setTimestamp(info.getTimePosted());

		return ehInfo.build();
	}

	public static String display(HashSet<String> list) {
		return list.isEmpty() ? "None" : list.stream().sorted().collect(Collectors.joining(", "));
	}
}
