package bot.sites.nhentai;

import bot.commands.Info;
import bot.modules.DBHandler;
import bot.modules.EmbedGenerator;
import bot.modules.TagList;
import bot.sites.ehentai.EHSearch;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
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
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NHSearch {
	public static void main(String[] args) {
		System.out.println(generateUrl("sword art online"));
	}

	private static final Logger logger = LogManager.getLogger(EHSearch.class);
	private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

	private static final String BASE_URL = "https://nhentai.net/search/?q=";

	public static void runSearch(MessageChannel channel, User author, String query, boolean restrict, int pages, DBHandler database) {
		// Query preprocessing
		query = query.trim();
		String urlQuery = generateUrl(query);

		final String fquery = query;

		Pattern gallery = Pattern.compile("/g/(\\d{1,6})/?");

		ArrayList<String> allResults = new ArrayList<>();

		for (int currentPage = 1; currentPage <= pages; currentPage++) {
			try {

				String curUrlQuery = urlQuery + currentPage;
				logger.info("Current page: " + (currentPage));
				logger.info("Query: " + curUrlQuery);

				Document doc = Jsoup.connect(curUrlQuery).get();
				
				ArrayList<String> curResults = doc.select("a[href]").stream().map(n -> n.attr("abs:href"))
						.map(gallery::matcher).filter(Matcher::find)
						.map(s -> "https://nhentai.net/g/" + s.group(1) + "/")
						.collect(Collectors.toCollection(ArrayList::new));

				// If no new results were found, we stop here
				if(curResults.size() == 0) {
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
		if(allResults.isEmpty()) {
			MessageEmbed noResultsAlert = EmbedGenerator.createAlertEmbed("Search Results", "No results found!");
			channel.sendMessage(noResultsAlert).queue();
		}
		else if(allResults.size() == 1) {
			// There's only one result, send an info embed for it.
			try {
				channel.sendMessage(Info.getDoujinInfoEmbed(new NHFetcher(allResults.get(0), database))).queue();
			} catch (IOException e) {
				logger.info("Something went wrong when making the search info embed.");
				e.printStackTrace();
			}
		}
		else if(allResults.size() <= 10) {
			// Relatively small link pile, send it in the chat
			channel.sendMessage(EmbedGenerator.createAlertEmbed("Search Results", "Results found: " + allResults.size())).queue();
			channel.sendMessage("Full results:\n" +
					allResults.stream().map(str -> "<" + str + ">").collect(Collectors.joining("\n"))).queue();
		}
		else {
			// Big link pile, send it in DMs
			if(channel.getType() == ChannelType.TEXT) {
				channel.sendMessage(EmbedGenerator.createAlertEmbed("Search Results", "More than 10 results - sending the results to your DMs!")).complete();
				author.openPrivateChannel().queue(
						pm -> {
							pm.sendMessage(EmbedGenerator.createAlertEmbed("Search Results", "Results found: " + allResults.size())).queue();
							pm.sendMessage("Full results:\n" +
									allResults.stream().map(str -> "<" + str + ">").collect(Collectors.joining("\n"))).queue(
									success -> pm.close().queue()
							);
						}
				);
			}
			else {
				channel.sendMessage(EmbedGenerator.createAlertEmbed("Search Results", "Results found: " + allResults.size())).queue();
				channel.sendMessage("Full results:\n" +
						allResults.stream().map(str -> "<" + str + ">").collect(Collectors.joining("\n"))).queue();
			}
		}
	}

	private static String generateUrl(String query) {
		HashSet<String> allTags = TagList.getBadTags();
		allTags.addAll(TagList.nonWholesomeTagsWithoutQuery(query));
		allTags.addAll(TagList.getIllegalTags());

		query += " english";

		return BASE_URL + URLEncoder.encode(query + " -tag:\"" + String.join("\" -tag:\"", allTags) + "\"", StandardCharsets.UTF_8) + "&sort=popular&page=";
	}
}
