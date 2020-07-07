package bot;

import bot.commands.Help;
import bot.modules.DBHandler;
import bot.modules.EmbedGenerator;
import bot.modules.Validator;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Wiretap extends ListenerAdapter {
	private static final Logger logger = LogManager.getLogger(Wiretap.class);
	private static HashMap<String, String> suspects;
	private final DBHandler database;

	/**
	 * Creates a Wiretap listener to listen to messages.
	 * The DBHandler will handle all the communication with databases.
	 */
	public Wiretap() {
		database = new DBHandler();
	}

	public static void registerSuspect(String messageId, String authorId) {
		suspects.put(messageId, authorId);
	}

	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		// Someone sent something!

		// Ignore conditions
		if (event.getAuthor().isBot()) {
			return;
		}

		Message message = event.getMessage();
		String content = message.getContentRaw();
		MessageChannel channel = event.getChannel();

		if (content.isEmpty()) {
			return;
		}

		String prefix;

		if (event.isFromType(ChannelType.TEXT)) {
			String guildId = event.getGuild().getId();

			prefix = database.getPrefix(guildId);
		} else if (event.isFromType(ChannelType.PRIVATE)) {
			prefix = ">";
		} else {
			// It's from some mysterious channel
			// Just ignore it.
			return;
		}

		// Handle any <> / []
		Pattern pattern = Pattern.compile("[\\[<]\\s*(\\d+)\\s*[>\\]]");
		Matcher matcher = pattern.matcher(content);
		if (matcher.find()) {
			content = prefix + "info " + matcher.group(1);
			if (event.isFromType(ChannelType.TEXT) && !event.getTextChannel().isNSFW()) {
				return;
			}
		}

		// Prefix loaded
		// Check if it's a command
		if (!content.startsWith(prefix)) {
			return;
		}

		// Check if the command is even a command
		content = content.substring(prefix.length()).trim();
		String[] argSplitter = content.split(" ", 2);
		String command = argSplitter[0];
		String args = argSplitter[1];

		if (!Validator.isCommand(command)) {
			// Not a command
			return;
		}

		// Everything the bot does needs to be in NSFW channels
		if (event.isFromGuild() && !event.getTextChannel().isNSFW()) {
			channel.sendMessage(EmbedGenerator.createAlertEmbed("Bot Alert", "This channel is not NSFW", "This bot can only be used in NSFW channels!")).queue();
			return;
		}
		try {
			switch (command) {
				case "help" -> {
					channel.sendTyping().complete();
					channel.sendMessage(Help.getHelpEmbed()).queue();
				}
				case "botinfo" -> {
					channel.sendTyping().complete();
					channel.sendMessage(Help.getInfoEmbed()).queue();
				}
				case "info" -> {
					channel.sendTyping().complete();

				}
			}
		} catch (InsufficientPermissionException e) {
			logger.info("Insufficient permissions. Missing permission " + e.getPermission().name());
		} catch (Exception e) {
			logger.error("Something went wrong. Message:");
			logger.error(content);
			logger.error("From: " + message.getAuthor().getAsTag());
			e.printStackTrace();
		}
	}

	@Override
	public void onMessageReactionAdd(MessageReactionAddEvent event) {
		String messageId = event.getMessageId();

		// Is it a reaction to something I care about?
		if (!suspects.containsKey(messageId)) {
			return;
		}

		//
		if (event.getUser() == null) {
			return;
		}

		String authorId = suspects.get(event.getMessageId());
		if (event.getUserId().equals(authorId)) {
			// Let's check the reaction...
			String reaction = event.getReaction().getReactionEmote().getAsCodepoints();
			if (reaction.equals("U+2705")) {
				// Checkmark
				return;
			}
		} else {
			// Wrong dude. Yeet the reaction into the sun.
			event.getReaction().removeReaction().queue();
		}
	}
}
