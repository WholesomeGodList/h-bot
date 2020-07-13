package bot.commands;

import bot.modules.DBHandler;
import bot.modules.Validator;
import bot.sites.ehentai.EHApiHandler;
import bot.sites.ehentai.EHFetcher;
import bot.sites.godlist.WHFetcher;
import bot.sites.nhentai.NHFetcher;
import net.dv8tion.jda.api.entities.MessageChannel;
import org.jsoup.HttpStatusException;

import java.io.IOException;
import java.util.HashSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static bot.modules.Validator.siteValidate;

public class Tags {
	public static void sendTags(MessageChannel channel, String args, EHApiHandler handler, DBHandler database) {
		if (args != null && Pattern.compile("^\\d+$").matcher(args).find()) {
			args = "https://nhentai.net/g/" + Integer.parseInt(args) + "/";
		}
		Validator.ArgType validate = siteValidate(args, channel);
		if (!validate.isValid()) {
			return;
		}

		try {
			if (validate == Validator.ArgType.NHENTAI) {
				NHFetcher taginator = new NHFetcher(args, database);
				channel.sendMessage("Tags for " + taginator.getTitle() + ":\n" + display(taginator.getTags())).queue();
			} else if (validate == Validator.ArgType.EHENTAI) {
				EHFetcher taginator = new EHFetcher(args, handler, database);

				String msg = "Tags for " + taginator.getTitle() + ":\n" +
						"Male tags:\n" +
						display(taginator.getMaleTags()) + "\n\n" +
						"Female tags:\n" +
						display(taginator.getFemaleTags()) + "\n\n" +
						"Misc tags:\n" +
						display(taginator.getMiscTags());

				channel.sendMessage(msg).queue();
			} else if (validate == Validator.ArgType.GODLIST) {
				if(args == null) {
					return;
				}
				WHFetcher taginator = new WHFetcher(Integer.parseInt(args.substring(1)));
				String msg = "Tags for " + taginator.getTitle() + ":\n" +
						"God List tags:\n" +
						display(taginator.getTags());

				if(Validator.getArgType(taginator.getLink()) == Validator.ArgType.NHENTAI) {
					msg += "\n\nnhentai tags:\n" +
							display(new NHFetcher(taginator.getLink(), database).getTags());
				}

				channel.sendMessage(msg).queue();
			}
		} catch (HttpStatusException e) {
			channel.sendMessage("Can't find linked page: returned error code " + e.getStatusCode()).queue();
		} catch (IOException e) {
			channel.sendMessage("An error occurred. Please try again, or ping my owner if this persists.").queue();
			e.printStackTrace();
		}
	}

	public static String display(HashSet<String> list) {
		return list.isEmpty() ? "`None`" : "`" + list.stream().sorted().collect(Collectors.joining("` `")) + "`";
	}
}
