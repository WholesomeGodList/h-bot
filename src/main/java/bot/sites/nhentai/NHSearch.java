package bot.sites.nhentai;

import bot.commands.Info;
import bot.modules.DBHandler;
import bot.modules.EmbedGenerator;
import bot.modules.TagList;
import net.dv8tion.jda.api.entities.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NHSearch {
	private static final Logger logger = LogManager.getLogger(NHSearch.class);

	private static final String BASE_URL = "https://nhentai.net/search/?q=";

	public static void runSearch(MessageChannel channel, User author, String query, boolean restrict, int pages, DBHandler database) {
		channel.sendMessage(EmbedGenerator.createAlertEmbed("Searching...", "Please wait while the search is being done")).queue(
				success -> success.delete().queueAfter(5, TimeUnit.SECONDS)
		);

		// Query preprocessing
		query = query.trim();
		logger.info("Running search with query " + query);
		String urlQuery = generateUrl(query, restrict);

		logger.info("Search URL: " + urlQuery);

		if (TagList.getIllegalTags().contains(query)) {
			channel.sendMessage("***FBI OPEN UP***").queue();
			return;
		}

		final String fquery = query;

		Pattern gallery = Pattern.compile("/g/(\\d{1,6})/?");

		ArrayList<String> allResults = new ArrayList<>();

		for (int currentPage = 1; currentPage <= pages; currentPage++) {
			try {
				String curUrlQuery = urlQuery + currentPage;
				logger.info("Current page: " + (currentPage));

				Document doc = Jsoup.connect(curUrlQuery).get();

				ArrayList<String> curResults = doc.select("a[href]").stream().map(n -> n.attr("abs:href"))
						.map(gallery::matcher).filter(Matcher::find)
						.map(s -> "https://nhentai.net/g/" + s.group(1) + "/")
						.collect(Collectors.toCollection(ArrayList::new));

				// If no new results were found, we stop here
				if (curResults.isEmpty()) {
					break;
				}

				allResults.addAll(curResults);
			} catch (HttpStatusException e) {
				logger.info("Error: HTTP status " + e.getStatusCode());
			} catch (IOException e) {
				logger.info("Exception occured in search. Query: " + urlQuery);
				e.printStackTrace();
			}
		}

		// We have actually already filtered out everything using the search URL

		// At this point, allResults is our final search results. Start returning the results.
		if (allResults.isEmpty()) {
			MessageEmbed noResultsAlert = EmbedGenerator.createAlertEmbed("Search Results", "No results found!");
			channel.sendMessage(noResultsAlert).queue();
		} else if (allResults.size() == 1) {
			// There's only one result, send an info embed for it.
			try {
				channel.sendMessage(Info.getDoujinInfoEmbed(new NHFetcher(allResults.get(0), database))).queue();
			} catch (IOException e) {
				logger.info("Something went wrong when making the search info embed.");
				e.printStackTrace();
			}
		} else if (allResults.size() <= 10) {
			// Relatively small link pile, send it in the chat
			channel.sendMessage(EmbedGenerator.createAlertEmbed("Search Results", "Results found: " + allResults.size())).queue();
			channel.sendMessage("Full results:\n" +
					allResults.stream().map(str -> "<" + str + ">").collect(Collectors.joining("\n"))).queue();
		} else {
			// Big link pile, send it in DMs
			if (channel.getType() == ChannelType.TEXT) {
				channel.sendMessage(EmbedGenerator.createAlertEmbed("Search Results", "More than 10 results - sending the results to your DMs!")).complete();
				author.openPrivateChannel().queue(
						pm -> sendResults(pm, allResults, fquery)
				);
			} else {
				sendResults(channel, allResults, fquery);
			}
		}
	}

	private static void sendResults(MessageChannel channel, ArrayList<String> allResults, String query) {
		channel.sendMessage(EmbedGenerator.createAlertEmbed("Search Results", "Results found: " + allResults.size())).queue();

		StringBuilder current = new StringBuilder();

		current.append("Full results for `").append(query).append("`:");
		while (!allResults.isEmpty()) {
			current.append("\n<").append(allResults.get(0)).append(">");
			allResults.remove(0);
			if (current.length() > 1500) {
				channel.sendMessage(current.toString()).queue();
				current = new StringBuilder();
			}
		}


		if (channel.getType() == ChannelType.PRIVATE) {
			PrivateChannel pm = (PrivateChannel) channel;
			pm.sendMessage(current.toString()).queue(
					success -> pm.close().queue()
			);
		} else {
			channel.sendMessage(current.toString()).queue();
		}
	}

	private static String generateUrl(String query, boolean restrict) {
		HashSet<String> allTags = TagList.getBadTags();
		if (restrict) {
			allTags.addAll(TagList.nonWholesomeTagsWithoutQuery(query));
		}
		allTags.addAll(TagList.getIllegalTags());

		query += " english";

		return BASE_URL + URLEncoder.encode(query + " -tag:\"" + String.join("\" -tag:\"", allTags) + "\"", StandardCharsets.UTF_8) + "&sort=popular&page=";
	}
}
