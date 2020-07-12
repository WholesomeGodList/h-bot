package bot;

import bot.commands.BadTags;
import bot.commands.Help;
import bot.commands.Info;
import bot.commands.Tags;
import bot.modules.DBHandler;
import bot.modules.EmbedGenerator;
import bot.modules.Validator;
import bot.sites.SiteFetcher;
import bot.sites.ehentai.EHApiHandler;
import bot.sites.ehentai.EHFetcher;
import bot.sites.ehentai.EHSearch;
import bot.sites.nhentai.NHFetcher;
import bot.sites.nhentai.NHSearch;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Wiretap extends ListenerAdapter {
	private static final Logger logger = LogManager.getLogger(Wiretap.class);
	private static HashMap<String, ImmutablePair<String, SiteFetcher>> suspects;
	private final DBHandler database;
	private final EHApiHandler handler;

	/**
	 * Creates a Wiretap listener to listen to messages.
	 * The DBHandler will handle all the communication with databases.
	 */
	public Wiretap(DBHandler database, EHApiHandler handler) {
		this.database = database;
		this.handler = handler;

		suspects = new HashMap<>();
	}

	public static void registerSuspect(String messageId, String authorId, SiteFetcher fetcher) {
		suspects.put(messageId, new ImmutablePair<>(authorId, fetcher));
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
		String args = (argSplitter.length > 1) ? argSplitter[1] : null;

		if (!Validator.isCommand(command)) {
			// Not a command
			return;
		}

		// Everything the bot does needs to be in NSFW channels
		if (event.isFromGuild() && !event.getTextChannel().isNSFW()) {
			channel.sendMessage(EmbedGenerator.createAlertEmbed("Bot Alert", "This channel is not NSFW", "This bot can only be used in NSFW channels!")).queue();
			return;
		}

		logger.info("Command received: " + command);
		try {
			switch (command) {
				case "help" -> {
					channel.sendTyping().queue();
					channel.sendMessage(Help.getHelpEmbed()).queue();
				}
				case "botinfo" -> {
					channel.sendTyping().queue();
					channel.sendMessage(Help.getInfoEmbed()).queue();
				}
				case "info" -> {
					channel.sendTyping().queue();
					Info.sendInfo(channel, args, event.getAuthor(), handler, database);
				}
				case "tags" -> {
					channel.sendTyping().queue();
					Tags.sendTags(channel, args, handler, database);
				}
				case "badtags", "warningtags" -> {
					channel.sendTyping().queue();
					channel.sendMessage(BadTags.getBadTagEmbed()).queue();
				}
				case "searcheh", "deepsearcheh" -> {
					channel.sendTyping().queue();

					boolean restrict = true;
					if(args != null && args.startsWith("-n ")) {
						args = args.substring(3);
						restrict = false;
					}

					EHSearch.runSearch(channel, event.getAuthor(), args, restrict, command.equals("searcheh") ? 4 : 10, database);
				}
				case "search", "deepsearch" -> {
					channel.sendTyping().queue();

					boolean restrict = true;
					if(args != null && args.startsWith("-n ")) {
						args = args.substring(3);
						restrict = false;
					}

					NHSearch.runSearch(channel, event.getAuthor(), args, restrict, command.equals("search") ? 4 : 10, database);
				}
				case "addhook" -> {
					if(event.getMember() == null) {
						return;
					}
					if (event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
						channel.sendMessage("New doujins from the telecom will now be relayed here").queue();
						database.addHook(event.getChannel().getId(), event.getGuild().getId());
					} else {
						channel.sendMessage(EmbedGenerator.createAlertEmbed("Bot Alert", "You do not have the permission Manage Server!")).queue();
					}
				}
				case "removehook" -> {
					if(event.getMember() == null) {
						return;
					}
					if (event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
						channel.sendMessage("New doujins from the telecom will no longer be relayed here").queue();
						database.removeHook(event.getChannel().getId());
					} else {
						channel.sendMessage(EmbedGenerator.createAlertEmbed("Bot Alert", "You do not have the permission Manage Server!")).queue();
					}
				}
				case "setprefix" -> {
					if(event.getMember() == null) {
						return;
					}
					if (event.getMember().hasPermission(Permission.MANAGE_SERVER)) {
						if(args == null) {
							channel.sendMessage("Please specify a prefix.").queue();
							return;
						}
						try {
							if(args.trim().length() > 10) {
								channel.sendMessage("That prefix is too long. Please shorten it to 10 characters or less.").queue();
								return;
							}
							database.setPrefix(args.trim(), event.getGuild().getId());
							channel.sendMessage("Your prefix has been set to `" + args.trim() +"`.").queue();
						} catch (SQLException e) {
							channel.sendMessage("Something went wrong when setting your prefix. Please try again.").queue();
							logger.error("Prefix was not set properly.");
							e.printStackTrace();
						}
					} else {
						channel.sendMessage(EmbedGenerator.createAlertEmbed("Bot Alert", "You do not have the permission Manage Server!")).queue();
					}
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

		if(event.getUser() == null || event.getUser().isBot()) {
			return;
		}

		// Handle any wackiness
		if (event.getUser() == null) {
			return;
		}

		MessageChannel channel = event.getChannel();

		ImmutablePair<String, SiteFetcher> cur = suspects.get(event.getMessageId());
		String authorId = cur.getLeft();

		if (event.getUserId().equals(authorId)) {
			// Let's check the reaction...
			String reaction = event.getReaction().getReactionEmote().getAsCodepoints();

			if (reaction.equals("U+2705")) {
				// Checkmark
				logger.info("Checkmark reaction detected. Sending info embed...");
				channel.deleteMessageById(messageId).queue();
				channel.sendTyping().queue();

				MessageEmbed embed;
				if (cur.getRight() instanceof NHFetcher) {
					embed = Info.getDoujinInfoEmbed((NHFetcher) cur.getRight());
				} else if (cur.getRight() instanceof EHFetcher) {
					embed = Info.getDoujinInfoEmbed((EHFetcher) cur.getRight());
				} else {
					// This should never happen
					logger.error("Something impossible happened.");
					return;
				}

				channel.sendMessage(embed).queue();
				suspects.remove(messageId);
			} else if (reaction.equals("U+274c")) {
				// X
				logger.info("X reaction detected. Closing...");
				channel.deleteMessageById(messageId).queue();
				suspects.remove(messageId);
			} else {
				// Not a valid reaction. Yeet it.
				logger.info("Invalid reaction. Deleting...");
				if(channel.getType() == ChannelType.TEXT) {
					event.getReaction().removeReaction(event.getUser()).queue();
				}
			}
		} else {
			logger.info("Wrong person. Deleting...");
			// Wrong dude. Yeet the reaction into the sun.
			if(channel.getType() == ChannelType.TEXT) {
				event.getReaction().removeReaction(event.getUser()).queue();
			}
		}
	}
}
