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
		helpMsg.addField("Core Commands",
				"""
						All nhentai commands work with just numbers!
						- help: Displays the help message
						- botinfo: Displays information about the bot / useful links
						- tags [link]: Returns tags for a link
						- getpage [link] [page]: Gets a page link for an nhentai doujin
						- info [link]: Returns info about a doujin
						- badtags/warningtags: Lists the tags you'll be warned about
						""", false);
		helpMsg.addField("Config Commands",
				"""
						- addhook: Registers this channel as a hook for the new doujin feed
						- delhook: Unregisters this channel as a hook for the new doujin feed
						- setprefix: Sets the prefix for this server
						""",
				false);
		helpMsg.addField("Search Commands",
				"""     
						- random [flags]:
						```
						-e: include non-english results
						-nbt: no bad tags allowed in results
						-w: no non-wholesome tags allowed in results (more restrictive than -nbt)
						-i: no incest/inseki
						-ya / yu: no yaoi / yuri
						```
						- search [-n] [query]: Queries for up to 100 doujins, and returns the ones it finds without any non-wholesome and warning tags (>badtags)
						- deepsearch [-n] [query]: Queries for up to 250 doujins instead of 100. Usually not necessary.
						- searcheh: >search but for e-hentai
						- deepsearcheh: >deepsearch but for e-hentai
						```
						-n: non-restrictive (works for both search and deepsearch) (no longer blocks warning tags, just non-wholesome tags)
						```
						"""
				, false);
		helpMsg.addField("More Questions?",
				"""
					If you have more questions about the bot, check the FAQ [here](https://github.com/WholesomeGodList/h-bot).
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
						**[GitHub Repo](https://github.com/WholesomeGodList/h-bot)**
						**[Invite the bot](https://discordapp.com/api/oauth2/authorize?client_id=608816072057159713&permissions=93248&scope=bot)**
						**[Our Patreon](https://patreon.com/WholesomeGodList)**
						""", false);
		infoEmbed.setFooter("Built by Stinggyray#1000", "https://images.emojiterra.com/twitter/v12/512px/1f914.png");
		infoEmbed.setTimestamp(Instant.now());

		return infoEmbed.build();
	}
}
