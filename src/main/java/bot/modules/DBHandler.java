package bot.modules;

import bot.sites.ehentai.EHFetcher;
import bot.sites.nhentai.NHFetcher;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;

public class DBHandler {
	private final HashMap<String, String> prefixes;
	private static final Logger logger = LogManager.getLogger(DBHandler.class);

	private static final BasicDataSource config = new BasicDataSource();
	private static final BasicDataSource cache = new BasicDataSource();

	public DBHandler() {
		prefixes = new HashMap<>();
		Connection connfig;

		File configFile = new File("./config.db");
		String configUrl = "jdbc:sqlite:config.db";
		if(!configFile.exists()) {
			logger.info("Config database not found. Creating a new database...");
			try {
				connfig = DriverManager.getConnection(configUrl);
				if(connfig != null) {
					logger.info("Config database created.");
					logger.info("Now running setup commands...");

					Statement create = connfig.createStatement();

					create.execute("CREATE TABLE prefixes (guild_id INTEGER PRIMARY KEY, prefix text)");
					create.execute(" CREATE TABLE hookchannels (guild_id INTEGER PRIMARY KEY, channel_id INTEGER);");
				}
			} catch (SQLException e) {
				logger.error("Config database creation failed. Error details:");
				logger.error(e.getStackTrace());
			}
		} else try {
			config.setUrl("jdbc:sqlite:config.db");
			config.setMinIdle(2);
			config.setMaxIdle(5);

			Connection conn = config.getConnection();
			if(conn != null) {
				logger.info("Connected to config database.");
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Config database connection failed. Error details:");
			logger.error(e.getStackTrace());
		}

		Connection cacheconn;

		File cacheFile = new File("./cache.db");
		String cacheUrl = "jdbc:sqlite:cache.db";
		if(!cacheFile.exists()) {
			logger.info("Config database not found. Creating a new database...");
			try {
				cacheconn = DriverManager.getConnection(cacheUrl);
				if(cacheconn != null) {
					logger.info("Cache database created.");
					logger.info("Now running setup commands...");
					Statement create = cacheconn.createStatement();

					// Create main tables
					create.execute("CREATE TABLE ehentai (url text, timecached integer, gallery_id integer," +
							" gallery_token text, title text, title_japanese text, language text, category text," +
							"pages integer, uploader text, thumb text, rating real, timeposted integer)");

					create.execute( "CREATE TABLE nhentai (url text, timecached integer, title text," +
							"title_japanese text, language text, pages integer," +
							" thumb text, favorites integer, timeposted integer)");

					// Now create all the small tables
					create.execute("CREATE TABLE ehmaletags (url text, tag text)");
					create.execute("CREATE TABLE ehfemaletags (url text, tag text)");
					create.execute("CREATE TABLE ehmisctags (url text, tag text)");
					create.execute("CREATE TABLE ehartists (url text, artist text)");
					create.execute("CREATE TABLE ehgroups (url text, \"group\" text)");
					create.execute("CREATE TABLE ehparodies (url text, parody text)");
					create.execute("CREATE TABLE ehchars (url text, character text)");

					create.execute("CREATE TABLE nhtags (url text, tag text)");
					create.execute("CREATE TABLE nhartists (url text, artist text)");
					create.execute("CREATE TABLE nhgroups (url text, \"group\" text)");
					create.execute("CREATE TABLE nhparodies (url text, parody text)");
					create.execute("CREATE TABLE nhchars (url text, character text)");
				}
			} catch (SQLException e) {
				logger.error("Config database creation failed. Error details:");
				logger.error(e.getStackTrace());
			}
		} else try {
			cache.setUrl("jdbc:sqlite:cache.db");
			cache.setMinIdle(2);
			cache.setMaxIdle(5);

			Connection conn = cache.getConnection();
			if (conn != null) {
				logger.info("Connected to cache database.");
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Config database connection failed. Error details:");
			logger.error(e.getStackTrace());
		}
	}

	/**
	 * Gets the prefix for a guild.
	 * Internally caches prefixes in memory to prevent SQL queries.
	 * @param guildId The ID of the guild to which the get the prefix of.
	 * @return The prefix of the guild.
	 */
	public String getPrefix(String guildId) {
		String prefix;

		if (prefixes.containsKey(guildId)) {
			prefix = prefixes.get(guildId);
		} else {
			try (Connection connfig = config.getConnection()) {
				PreparedStatement stmt = connfig.prepareStatement("SELECT prefix FROM prefixes WHERE guild_id=?");
				stmt.setLong(1, Long.parseLong(guildId));
				ResultSet rs = stmt.executeQuery();
				if (rs.next()) {
					prefix = rs.getString("prefix");
					prefixes.put(guildId, rs.getString("prefix"));
				} else {
					prefix = ">";
					prefixes.put(guildId, ">");
				}
			} catch (SQLException e) {
				logger.error("Prefix for server with ID " + guildId + " errored out. Defaulting to >...");
				prefix = ">";
				prefixes.put(guildId, ">");
				e.printStackTrace();
			}
		}

		return prefix;
	}

	/**
	 * Looks for a cached version of EHFetcher in the database. If there is one, it is used to populate all the fields
	 * of the passed EHFetcher object.
	 *
	 * If the cached version is too old (older than 2 weeks) it is thrown out and not used.
	 * @param load An EHFetcher object with which to load the cache into.
	 * @return Whether this value was found in the cache or not.
	 */
	public boolean loadFromCache(EHFetcher load) {
		try (Connection cacheconn = cache.getConnection()) {
			PreparedStatement stmt = cacheconn.prepareStatement("SELECT * FROM ehentai WHERE url=?");
			stmt.setString(1, load.getUrl());

			ResultSet rs = stmt.executeQuery();

			if(rs.next()) {
				// If the entry is older than 14 days...
				if(rs.getLong("timecached") < (Instant.now().getEpochSecond() - (86400 * 14))) {
					PreparedStatement deleter = cacheconn.prepareStatement("DELETE FROM ehentai WHERE url=?");
					deleter.setString(1, load.getUrl());
					deleter.execute();

					deleter.close();
					return false;
				}

				// We're good. Start processing.
				load.setGalleryId(rs.getInt("gallery_id"));
				load.setGalleryToken(rs.getString("gallery_token"));

				load.setTitle(rs.getString("title"));
				load.setTitleJapanese(rs.getString("title_japanese"));

				load.setLanguage(rs.getString("language"));
				load.setCategoryByName(rs.getString("category"));

				load.setPages(rs.getInt("pages"));
				load.setUploader(rs.getString("uploader"));
				load.setThumbnailUrl(rs.getString("thumb"));
				load.setRating(rs.getDouble("rating"));
				load.setTimePosted(Instant.ofEpochSecond(rs.getLong("timeposted")));

				String url = load.getUrl();

				// Load in all the HashSets

				load.setArtists(loadSet("ehartists", url, cacheconn));
				load.setGroups(loadSet("ehgroups", url, cacheconn));
				load.setParodies(loadSet("ehparodies", url, cacheconn));
				load.setChars(loadSet("ehchars", url, cacheconn));
				load.setMaleTags(loadSet("ehmaletags", url, cacheconn));
				load.setFemaleTags(loadSet("ehfemaletags", url, cacheconn));
				load.setMiscTags(loadSet("ehmisctags", url, cacheconn));
				return true;
			}
			else {
				// It's not in the cache.
				return false;
			}
		} catch (SQLException e) {
			logger.error("An error occurred during the cache query.");
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Looks for a cached version of NHFetcher in the database. If there is one, it is used to populate all the fields
	 * of the passed NHFetcher object.
	 *
	 * If the cached version is too old (older than 2 weeks) it is thrown out and not used.
	 * @param load An NHFetcher object with which to load the cache into.
	 * @return Whether this value was found in the cache or not.
	 */
	public boolean loadFromCache(NHFetcher load) {
		try (Connection cacheconn = cache.getConnection()) {
			PreparedStatement stmt = cacheconn.prepareStatement("SELECT * FROM nhentai WHERE url=?");
			stmt.setString(1, load.getUrl());

			ResultSet rs = stmt.executeQuery();

			if(rs.next()) {
				// If the entry is older than 14 days...
				if(rs.getLong("timecached") < (Instant.now().getEpochSecond() - (86400 * 14))) {
					PreparedStatement deleter = cacheconn.prepareStatement("DELETE FROM nhentai WHERE url=?");
					deleter.setString(1, load.getUrl());
					deleter.execute();

					deleter.close();
					return false;
				}

				// We're good. Start processing.
				load.setTitle(rs.getString("title"));
				load.setTitleJapanese(rs.getString("title_japanese"));
				load.setLanguage(rs.getString("language"));

				load.setPages(rs.getInt("pages"));
				load.setThumbnailUrl(rs.getString("thumb"));
				load.setFavorites(rs.getInt("favorites"));
				load.setTimePosted(Instant.ofEpochSecond(rs.getLong("timeposted")));

				String url = load.getUrl();

				// Load in all the HashSets

				load.setArtists(loadSet("nhartists", url, cacheconn));
				load.setGroups(loadSet("nhgroups", url, cacheconn));
				load.setParodies(loadSet("nhparodies", url, cacheconn));
				load.setChars(loadSet("nhchars", url, cacheconn));
				load.setTags(loadSet("nhtags", url, cacheconn));
				return true;
			}
			else {
				// It's not in the cache.
				return false;
			}
		} catch (SQLException e) {
			logger.error("An error occurred during the cache query.");
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Caches the data currently inside this EHFetcher.
	 * @param data The EHFetcher with data to be cached.
	 */
	public void cache(EHFetcher data) {
		try (Connection cacheconn = cache.getConnection()) {
			PreparedStatement stmt = cacheconn.prepareStatement("INSERT INTO ehentai VALUES " +
					"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
			stmt.setString(1, data.getUrl());
			stmt.setLong(2, Instant.now().getEpochSecond());
			stmt.setInt(3, data.getGalleryId());
			stmt.setString(4, data.getGalleryToken());
			stmt.setString(5, data.getTitle());
			stmt.setString(6, data.getTitleJapanese());
			stmt.setString(7, data.getLanguage());
			stmt.setString(8, data.getCategory().toString());
			stmt.setInt(9, data.getPages());
			stmt.setString(10, data.getUploader());
			stmt.setString(11, data.getThumbnailUrl());
			stmt.setDouble(12, data.getRating());
			stmt.setLong(13, data.getTimePosted().getEpochSecond());

			stmt.execute(); //jesus christ
			
			String url = data.getUrl();

			insertSet(data.getArtists(),"ehartists", url, cacheconn);
			insertSet(data.getGroups(),"ehgroups", url, cacheconn);
			insertSet(data.getParodies(),"ehparodies", url, cacheconn);
			insertSet(data.getChars(),"ehchars", url, cacheconn);
			insertSet(data.getMaleTags(),"ehmaletags", url, cacheconn);
			insertSet(data.getFemaleTags(),"ehfemaletags", url, cacheconn);
			insertSet(data.getMiscTags(),"ehmisctags", url, cacheconn);

			logger.info("Caching completed.");
		} catch (SQLException e) {
			logger.error("An error occurred during the cache insertion.");
			e.printStackTrace();
		}
	}

	/**
	 * Caches the data currently inside this NHFetcher.
	 * @param data The NHFetcher with data to be cached.
	 */
	public void cache(NHFetcher data) {
		try (Connection cacheconn = cache.getConnection()) {
			PreparedStatement stmt = cacheconn.prepareStatement("INSERT INTO nhentai VALUES " +
					"(?, ?, ?, ?, ?, ?, ?, ?, ?)");
			stmt.setString(1, data.getUrl());
			stmt.setLong(2, Instant.now().getEpochSecond());
			stmt.setString(3, data.getTitle());
			stmt.setString(4, data.getTitleJapanese());
			stmt.setString(5, data.getLanguage());
			stmt.setInt(6, data.getPages());
			stmt.setString(7, data.getThumbnailUrl());
			stmt.setInt(8, data.getFavorites());
			stmt.setLong(9, data.getTimePosted().getEpochSecond());

			stmt.execute(); //jesus christ v2

			String url = data.getUrl();

			insertSet(data.getArtists(),"nhartists", url, cacheconn);
			insertSet(data.getGroups(),"nhgroups", url, cacheconn);
			insertSet(data.getParodies(),"nhparodies", url, cacheconn);
			insertSet(data.getChars(),"nhchars", url, cacheconn);
			insertSet(data.getTags(),"nhtags", url, cacheconn);

			logger.info("Caching completed.");
		} catch (SQLException e) {
			logger.error("An error occurred during the cache insertion.");
			e.printStackTrace();
		}
	}

	// Simple helper to load a HashSet from a table.
	private HashSet<String> loadSet(String tableName, String url, Connection conn) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement("SELECT * FROM " + tableName + " WHERE url=?");
		stmt.setString(1, url);

		ResultSet rs = stmt.executeQuery();

		HashSet<String> results = new HashSet<>();
		while(rs.next()) {
			results.add(rs.getString(2));
		}
		stmt.close();
		return results;
	}

	// Simple helper to insert a HashSet into a table.
	private void insertSet(HashSet<String> tags, String tableName, String url, Connection conn) throws SQLException {
		PreparedStatement stmt = conn.prepareStatement("INSERT INTO " + tableName + " VALUES (?, ?)");
		for(String curTag : tags) {
			stmt.setString(1, url);
			stmt.setString(2, curTag);
			stmt.addBatch();
		}

		stmt.executeBatch();
		stmt.close();
	}
}
