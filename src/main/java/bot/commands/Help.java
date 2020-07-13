package bot.commands;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.awt.*;
import java.time.Instant;

public class Help {
	//Just the help message.
	public static MessageEmbed getHelpEmbed() {
		EmbedBuilder helpMsg = new EmbedBuilder();
		helpMsg.setColor(Color.BLACK);
		helpMsg.setAuthor("H-Bot", null, "https://i.redd.it/fkg9yip5yyl21.png");
		helpMsg.setTitle("For easily accessing info from nhentai and e-hentai");
		helpMsg.setFooter("Built by Stinggyray#1000", "https://images.emojiterra.com/twitter/v12/512px/1f914.png");
		helpMsg.addField("**Bot Usage**", """
						The sites this bot supports are:
						[Wholesome Hentai God List](https://wholesomelist.com) (accepted formats: #1234)
						[nhentai](https://nhentai.net) (accepted formats: https://nhentai.net/g/258133, 258133)
						[e-hentai/exhentai](https://e-hentai.org) (accepted formats: https://e[x/-]hentai.org/...)
						
						All core commands can be used with all of these sites.
						Search commands are site-specific.
						
						Abbreviations can also be used to check info of a doujin.
						Enclosing the numbers (#1234 for the god list or 123456 for nhentai)
						in `[]` or `{}` is equivalent to the info command.
						
						This bot also works in DMs. Feel free to use it in DMs (the prefix is always `>` in DMs).
						""", false);
		helpMsg.addField("Config / Information Commands",
				"""
						- help: Displays the help message
						- botinfo: Displays information about the bot / useful links
						- addhook: Registers this channel as a hook for the new doujin feed
						- removehook: Unregisters this channel as a hook for the new doujin feed
						- setprefix: Sets the prefix for this server
						- badtags/warningtags: Lists the tags you'll be warned about
						""",
				false);
		helpMsg.addField("Core Commands",
				"""
						- tags [link]: Returns tags for a link
						- info [link]: Returns info about a doujin
						""", false);
		helpMsg.addField("Search Commands",
				"""     
						- random: Returns a random doujin from the Wholesome Hentai God List.
						- search [-n] [query]: Queries for up to 100 doujins, and returns the ones it finds without any non-wholesome and warning tags (>badtags)
						- deepsearch [-n] [query]: Queries for up to 250 doujins instead of 100. Usually not necessary.
						- searcheh [-n] [query]: Search but for e-hentai. Does not find nearly as many doujins. Just use regular search unless you have a reason not to.
						- deepsearcheh [-n] [query]: Deepsearch but for e-hentai. Same advice applies.
						```
						-n: non-restrictive (works for both search and deepsearch) (no longer blocks warning tags, just non-wholesome tags)
						```
						"""
				, false);
		helpMsg.addField("Questions / Suggestions",
				"""
                        If you're confused about the commands, the command syntax, and how to use them, a very in-depth guide is available [here](https://github.com/WholesomeGodList/h-bot/wiki/Commands).
						If you have more questions about the bot, check the FAQ [here](https://github.com/WholesomeGodList/h-bot/wiki/FAQ).
						If that doesn't resolve your question, or if you have a suggestion, [join our Discord](https://discord.com/invite/FQCR6qu) and I'll be happy to help.
						""", false);
		helpMsg.setTimestamp(Instant.now());

		return helpMsg.build();
	}

	public static MessageEmbed getInfoEmbed() {
		EmbedBuilder infoEmbed = new EmbedBuilder();
		infoEmbed.setColor(Color.BLACK);
		infoEmbed.setAuthor("H-Bot", null, "https://i.redd.it/fkg9yip5yyl21.png");
		infoEmbed.setTitle("H-Bot Information");
		infoEmbed.addField("Bot Purpose",
				"""
						This bot is meant to help fetch information about doujins easily, with a focus on wholesomeness.
						It was originally built for a small-scale release for the wholesome hentai god list.
												
						It has since been rewritten to allow scalability and be far more efficient.
						""", false);
		infoEmbed.addField("Useful Links",
				"""
						**[The Wholesome Hentai God List](https://wholesomelist.com)**
						**[Discord Server](https://discord.com/invite/FQCR6qu)**
						**[GitHub Repository](https://github.com/WholesomeGodList/h-bot)**
						**[GitHub Repository Wiki Page](https://github.com/WholesomeGodList/h-bot/wiki/Home)**
						**[Invite the bot](https://discord.com/api/oauth2/authorize?client_id=608816072057159713&permissions=93248&scope=bot)**
						**[Our Patreon](https://patreon.com/WholesomeGodList)**
						""", false);
		infoEmbed.setFooter("Built by Stinggyray#1000", "https://images.emojiterra.com/twitter/v12/512px/1f914.png");
		infoEmbed.setTimestamp(Instant.now());

		return infoEmbed.build();
	}
}
