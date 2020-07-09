package bot.modules;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

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
}
