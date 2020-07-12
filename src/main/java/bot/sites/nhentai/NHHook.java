package bot.sites.nhentai;

import bot.commands.Info;
import bot.modules.DBHandler;
import bot.modules.TagChecker;
import bot.sites.NotFoundException;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NHHook implements Runnable {
	private static final Logger logger = LogManager.getLogger();
	private final DBHandler database;
	private final JDA jda;

	public NHHook(DBHandler database, JDA jda) {
		this.database = database;
		this.jda = jda;
	}

	@Override
	public void run() {
		try {
			runHook();
		} catch (Exception e) {
			logger.error("Exception in h-hook thrown:\n" + Arrays.toString(e.getStackTrace()));
		}
	}

	public void runHook() {
		logger.info("Now running hook");

		int latest = getLatestNumber();
		logger.info("Current number: " + getLastNumber());
		logger.info("Latest number: " + latest);
		if (getLastNumber() == latest || getLastNumber() == -1) {
			logger.info("Hook finished.");
			return;
		}

		ArrayList<TextChannel> hooks = database.getHookChannels(jda).stream().filter(Objects::nonNull)
				.collect(Collectors.toCollection(ArrayList::new));

		ArrayList<String> links = new ArrayList<>();

		for (int i = getLastNumber() + 1; i <= latest; i++) {
			links.add("https://nhentai.net/g/" + i + "/");
		}

		ArrayList<NHFetcher> filteredDoujins = new ArrayList<>();

		for (String cur : links) {
			try {
				Thread.sleep(200);
				NHFetcher fetcher = new NHFetcher(cur, database);
				if (TagChecker.tagCheck(fetcher.getTags(), true).getLeft() == TagChecker.TagStatus.OK
						&& fetcher.getLanguage().equals("English")
						&& fetcher.getTags().size() >= 3) {
					filteredDoujins.add(fetcher);
					logger.info("Link found: " + cur);
				}
			} catch (Exception e) {
				logger.info("Exception occurred in checking an NHFetcher:!");
				e.printStackTrace();
			}
		}

		logger.info("Building and sending info embeds...");
		for (NHFetcher cur : filteredDoujins) {
			try {
				MessageEmbed hookEmbed = Info.getDoujinInfoEmbed(cur);
				for (TextChannel curChannel : hooks) {
					try {
						curChannel.sendMessage(hookEmbed).queue(
								success -> {
									success.addReaction("U+2B06").queue();
									success.addReaction("U+2B07").queue();
								}
						);
					}
					catch (Exception e) {
						logger.info("Probably failed to send something. Skipping this channel...");
					}
				}
			} catch (Exception e) {
				logger.info("Something went wrong with the info embed.");
			}
		}

		setLastNumber(latest);
		logger.info("Hook finished.");
	}

	private static int getLatestNumber() {
		try {
			Document doc = Jsoup.connect("https://nhentai.net/").get();
			Elements links = doc.select("div .index-container").last().select("a[href]");

			Pattern pattern = Pattern.compile("/g/(\\d{1,6})/?");

			return links.stream().map(e -> e.attr("href")).map(pattern::matcher)
					.filter(Matcher::find).mapToInt(m -> Integer.parseInt(m.group(1))).findFirst().orElseThrow();
		} catch (HttpStatusException e) {
			logger.error("Error happened when getting latest number! This should NOT happen");
			logger.error("HTTP status code: " + e.getStatusCode());
		} catch (IOException e) {
			e.printStackTrace();
		}
		throw new NotFoundException("Latest number not found.");
	}

	private static void setLastNumber(int latest) {
		try {
			InputStream is = new FileInputStream(new File("./hook.json"));
			JSONObject setter = new JSONObject(new JSONTokener(is));
			setter.put("lastNumber", latest);
			FileWriter file = new FileWriter("./hook.json");
			file.write(setter.toString(4));
			file.close();
		} catch (IOException e) {
			logger.info("Last number set unsuccessfully.");
		}
	}

	private static int getLastNumber() {
		try {
			InputStream is = new FileInputStream(new File("./hook.json"));
			JSONObject getter = new JSONObject(new JSONTokener(is));
			return getter.getInt("lastNumber");
		} catch (IOException e) {
			logger.info("Last number was not gotten.");
		}
		return -1;
	}
}
