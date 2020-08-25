package bot.sites.ehentai;

import bot.commands.Info;
import bot.modules.DBHandler;
import bot.modules.EmbedGenerator;
import bot.modules.TagChecker;
import bot.modules.TagList;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EHSearch {
	private static final Logger logger = LogManager.getLogger(EHSearch.class);
	private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

	private static final String BASE_URL = "https://e-hentai.org/?f_cats=1017&f_search=";

	public static void runSearch(MessageChannel channel, User author, String query, boolean restrict, int pages, DBHandler database) {
		channel.sendMessage(EmbedGenerator.createAlertEmbed("Searching...", "Please wait while the search is being done")).queue(
				success -> success.delete().queueAfter(5, TimeUnit.SECONDS)
		);

		// Query preprocessing
		query = query.trim();
		String urlQuery = generateUrl(query);

		if (TagList.getIllegalTags().contains(query)) {
			channel.sendMessage("***FBI OPEN UP***").queue();
			return;
		}

		final String fquery = query;

		Pattern gallery = Pattern.compile("https?://e[x\\-]hentai\\.org/g/(\\d+)/([\\da-f]{10})/");

		ArrayList<EHFetcher> fetchers = new ArrayList<>();

		for (int currentPage = 0; currentPage < pages; currentPage++) {
			try {
				ArrayList<ImmutablePair<Integer, String>> queries = new ArrayList<>();

				String curUrlQuery = urlQuery + currentPage; // yes, E-Hentai's pages are zero indexed
				logger.info("Current page: " + (currentPage + 1));
				logger.info("Query: " + curUrlQuery);

				Document doc = Jsoup.connect(curUrlQuery).get();

				doc.select("a[href]").stream().map(e -> e.attr("abs:href"))
						.filter(gallery.asMatchPredicate()).map(gallery::matcher).filter(Matcher::find)
						.forEachOrdered(m -> queries.add(new ImmutablePair<>(Integer.parseInt(m.group(1)), m.group(2))));

				// If no new results were found, we stop here
				if (queries.isEmpty()) {
					break;
				}

				// Query the E-Hentai API
				EHApiHandler.EHGalleryPayload payload = new EHApiHandler.EHGalleryPayload();

				queries.forEach(s -> payload.addEntry(s.getLeft(), s.getRight()));

				JSONObject data = EHApiHandler.EHApiRequest(payload.getPayload());
				if (data.has("error")) {
					logger.info("Some error happened: " + data.getString("error"));
					continue;
				}

				JSONArray metadatas = data.getJSONArray("gmetadata");
				ArrayList<EHFetcher> tempResults = new ArrayList<>();

				for (int i = 0; i < metadatas.length(); i++) {
					tempResults.add(new EHFetcher(metadatas.getJSONObject(i), database));
				}

				tempResults.stream().filter(fetcher ->
						TagChecker.tagCheck(fetcher.getTags(), restrict, fquery).getLeft() == TagChecker.TagStatus.OK)
						.forEachOrdered(fetchers::add);

				Thread.sleep(1000);
			} catch (HttpStatusException e) {
				logger.info("Error: HTTP status " + e.getStatusCode());
			} catch (IOException | InterruptedException e) {
				logger.info("Exception occured in search. Query: " + urlQuery);
				e.printStackTrace();
			}
		}

		// At this point, fetchers is our final search results. Start returning the results.
		if (fetchers.isEmpty()) {
			MessageEmbed noResultsAlert = EmbedGenerator.createAlertEmbed("Search Results", "No results found!");
			channel.sendMessage(noResultsAlert).queue();
		} else if (fetchers.size() == 1) {
			// There's only one result, send an info embed for it.
			channel.sendMessage(Info.getDoujinInfoEmbed(fetchers.get(0))).queue();
		} else if (fetchers.size() <= 10) {
			// Relatively small link pile, send it in the chat
			channel.sendMessage(EmbedGenerator.createAlertEmbed("Search Results", "Results found: " + fetchers.size())).queue();
			channel.sendMessage("Full results:\n" +
					fetchers.stream().map(fetcher -> "<" + fetcher.getUrl() + ">").collect(Collectors.joining("\n"))).queue();
		} else {
			// Big link pile, send it in DMs
			if (channel.getType() == ChannelType.TEXT) {
				channel.sendMessage(EmbedGenerator.createAlertEmbed("Search Results", "More than 10 results - sending the results to your DMs!")).complete();
				author.openPrivateChannel().queue(
						pm -> sendResults(pm, fetchers, fquery)
				);
			} else {
				channel.sendMessage(EmbedGenerator.createAlertEmbed("Search Results", "Results found: " + fetchers.size())).queue();
				sendResults(channel, fetchers, fquery);
			}
		}
	}

	// Simple wrapper method for future.join
	private static EHFetcher joinWithoutExceptions(CompletableFuture<EHFetcher> future) {
		try {
			return future.join();
		} catch (Exception e) {
			logger.error("Something went wrong with the joining of an EHFetcher. Printing stack trace...");
			e.printStackTrace();

			return null;
		}
	}

	private static void sendResults(MessageChannel channel, ArrayList<EHFetcher> fetchers, String query) {
		channel.sendMessage(EmbedGenerator.createAlertEmbed("Search Results", "Results found: " + fetchers.size())).queue();

		StringBuilder current = new StringBuilder();

		current.append("Full results for `").append(query).append("`:");
		while (!fetchers.isEmpty()) {
			current.append("\n<").append(fetchers.get(0).getUrl()).append(">");
			fetchers.remove(0);
			if (current.length() > 1500) {
				channel.sendMessage(current.toString()).complete();
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

	private static String generateUrl(String query) {
		query += " language:english";
		return BASE_URL + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&page=";
	}
}
