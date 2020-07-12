package bot.sites.ehentai;

import bot.commands.Info;
import bot.modules.DBHandler;
import bot.modules.EmbedGenerator;
import bot.modules.TagChecker;
import net.dv8tion.jda.api.entities.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EHSearch {
	private static final Logger logger = LogManager.getLogger(EHSearch.class);
	private static final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

	private static final String BASE_URL = "https://e-hentai.org/?f_cats=1017&f_search=";

	public static void runSearch(MessageChannel channel, User author, String query, boolean restrict, int pages, EHApiHandler handler, DBHandler database) {
		// Query preprocessing
		Pattern gallery = Pattern.compile("https?://e[x\\-]hentai\\.org/g/(\\d+)/([\\da-f]{10})/");
		query = query.trim();
		String urlQuery = generateUrl(query);

		ArrayList<ImmutablePair<String, CompletableFuture<EHFetcher>>> queries = new ArrayList<>();

		for (int currentPage = 0; currentPage < pages; currentPage++) {
			try {
				String curUrlQuery = urlQuery + currentPage; // yes, E-Hentai's pages are zero indexed
				logger.info("Current page: " + (currentPage + 1));
				logger.info("Query: " + curUrlQuery);

				Document doc = Jsoup.connect(curUrlQuery).get();

				int querySize = queries.size();

				doc.select("a[href]").stream().map(e -> e.attr("abs:href"))
						.filter(gallery.asMatchPredicate())
						.forEachOrdered(str -> queries.add(new ImmutablePair<>(str, new CompletableFuture<>())));

				// If no new results were added, we can stop here
				if(querySize == queries.size()) {
					break;
				}
			} catch (HttpStatusException e) {
				logger.info("Error: HTTP status " + e.getStatusCode());
			} catch (IOException e) {
				logger.info("Exception occured in search. Query: " + urlQuery);
				e.printStackTrace();
			}
		}

		// Submit all promises for resolution simultaneously (using EHApiHandler)
		for (ImmutablePair<String, CompletableFuture<EHFetcher>> curPair : queries) {
			CompletableFuture<EHFetcher> curPromise = curPair.getRight();
			String curUrl = curPair.getLeft();

			executor.submit(
					() -> {
						try {
							curPromise.complete(new EHFetcher(curUrl, handler, database));
						} catch (IOException e) {
							curPromise.completeExceptionally(e);
						}
					});
		}

		// Find all the successful promises (filtering out anything that threw an exception), filter out anything with bad tags, and return them
		ArrayList<EHFetcher> fetchers = queries.stream().map(ImmutablePair::getRight)
				.map(EHSearch::joinWithoutExceptions).filter(Objects::nonNull)
				.filter(fetcher -> TagChecker.tagCheck(fetcher.getTags(), restrict).getLeft() == TagChecker.TagStatus.OK)
				.collect(Collectors.toCollection(ArrayList::new));

		// At this point, fetchers is our final search results. Start returning the results.
		if(fetchers.isEmpty()) {
			MessageEmbed noResultsAlert = EmbedGenerator.createAlertEmbed("Search Results", "No results found!");
			channel.sendMessage(noResultsAlert).queue();
		}
		else if(fetchers.size() == 1) {
			// There's only one result, send an info embed for it.
			channel.sendMessage(Info.getDoujinInfoEmbed(fetchers.get(0))).queue();
		}
		else if(fetchers.size() <= 10) {
			// Relatively small link pile, send it in the chat
			channel.sendMessage(EmbedGenerator.createAlertEmbed("Search Results", "Results found: " + fetchers.size())).queue();
			channel.sendMessage("Full results:\n" +
					fetchers.stream().map(fetcher -> "<" + fetcher.getUrl() + ">").collect(Collectors.joining("\n"))).queue();
		}
		else {
			// Big link pile, send it in DMs
			if(channel.getType() == ChannelType.TEXT) {
				channel.sendMessage(EmbedGenerator.createAlertEmbed("Search", "More than 10 results - sending the results to your DMs!")).complete();
				author.openPrivateChannel().queue(
						pm -> {
							pm.sendMessage(EmbedGenerator.createAlertEmbed("Search Results", "Results found: " + fetchers.size())).queue();
							pm.sendMessage("Full results:\n" +
									fetchers.stream().map(fetcher -> "<" + fetcher.getUrl() + ">").collect(Collectors.joining("\n"))).queue(
											success -> pm.close().queue()
							);
						}
				);
			}
			else {
				channel.sendMessage(EmbedGenerator.createAlertEmbed("Search Results", "Results found: " + fetchers.size())).queue();
				channel.sendMessage("Full results:\n" +
						fetchers.stream().map(fetcher -> "<" + fetcher.getUrl() + ">").collect(Collectors.joining("\n"))).queue();
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

	private static String generateUrl(String query) {
		query += " language:english";
		return BASE_URL + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&page=";
	}
}
