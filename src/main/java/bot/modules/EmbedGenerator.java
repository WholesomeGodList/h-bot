package bot.modules;

import bot.sites.ehentai.EHFetcher;
import bot.sites.nhentai.NHFetcher;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.commons.text.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

public class EmbedGenerator {
	private static final Logger logger = LogManager.getLogger(EmbedGenerator.class);
	/**
	 * Creates a MessageEmbed with a random meme for the cursed numbers.
	 * @return A MessageEmbed with a meme about 177013.
	 */
	public static MessageEmbed cursedEmbed() {
		EmbedBuilder badNumbers = new EmbedBuilder();
		ArrayList<String> memeURLs = new ArrayList<>();
		memeURLs.add("https://cdn.discordapp.com/attachments/549278996915814423/609487232692387851/lobster_claw_handjob.jpg");
		memeURLs.add("https://i.imgur.com/W2DCqPt.jpg");
		memeURLs.add("https://i.imgur.com/9PmmmpU.png");
		memeURLs.add("https://media.discordapp.net/attachments/624457027095363596/692488610364260422/ca575fb.png?width=475&height=475");
		int randomURLNum = (int) (memeURLs.size() * Math.random());
		String randomURL = memeURLs.get(randomURLNum);
		badNumbers.setImage(randomURL);

		return badNumbers.build();
	}

	/**
	 * Creates a MessageEmbed with a header and content, with the bot's icon.
	 * @param header The header of the embed.
	 * @param content The content of the embed.
	 * @return A MessageEmbed with the corresponding header and content.
	 *
	 * @see EmbedGenerator#createAlertEmbed(String, String, String)
	 */
	public static MessageEmbed createAlertEmbed(String header, String content) {
		EmbedBuilder alert = new EmbedBuilder();
		alert.setAuthor(header, null, "https://i.redd.it/fkg9yip5yyl21.png");
		alert.setDescription(content);

		return alert.build();
	}

	/**
	 * Creates a MessageEmbed with a header, title, and content, with the bot's icon.
	 * @param header The header of the embed.
	 * @param title The title of the embed (just below the header).
	 * @param content The content of the embed.
	 * @return A MessageEmbed with the corresponding header, title, and content.
	 *
	 * @see EmbedGenerator#createAlertEmbed(String, String)
	 */
	public static MessageEmbed createAlertEmbed(String header, String title, String content) {
		EmbedBuilder alert = new EmbedBuilder();
		alert.setAuthor(header, null, "https://i.redd.it/fkg9yip5yyl21.png");
		alert.setTitle(title);
		alert.setDescription(content);

		return alert.build();
	}

	/**
	 * Creates an information embed for a doujin, given an nhentai fetcher.
	 * @param info The NHFetcher object with which to build the embed.
	 * @return A message embed containing all the information about a doujin.
	 */
	public static MessageEmbed getDoujinInfoEmbed(NHFetcher info) throws IOException {
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
		return list.stream().sorted().collect(Collectors.joining(", "));
	}
}
