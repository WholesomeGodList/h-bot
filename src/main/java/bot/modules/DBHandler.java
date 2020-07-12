package bot.modules;

import bot.sites.ehentai.EHFetcher;
import bot.sites.nhentai.NHFetcher;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class DBHandler {
	private final HashMap<String, String> prefixes;
	private static final Logger logger = LogManager.getLogger(DBHandler.class);

	private static final BasicDataSource config = new BasicDataSource();
	private static final BasicDataSource cache = new BasicDataSource();

	/**
	 * Creates and sets up a local SQL server if it doesn't exist already. It will also initialize all necessary connections.
	 */
	public DBHandler() {
		prefixes = new HashMap<>();
		Connection connectionConfig;

		//Creating database for command configs for each server
		File configFile = new File("./config.db");
		String configUrl = "jdbc:sqlite:config.db";
		if(!configFile.exists()) {
			logger.info("Config database not found. Creating a new database...");
			try {
				connectionConfig = DriverManager.getConnection(configUrl);
				if(connectionConfig != null) {
					logger.info("Config database created.");
					logger.info("Now running setup commands...");

					Statement create = connectionConfig.createStatement();

					create.execute("CREATE TABLE prefixes (guild_id INTEGER PRIMARY KEY, prefix text)");
					create.execute(" CREATE TABLE hookchannels (guild_id INTEGER PRIMARY KEY, channel_id INTEGER);");
				}
			} catch (SQLException e) {
				logger.error("Config database creation failed. Error details:");
				logger.error(e.getStackTrace());
			}
		}
		try {
			//Initializing connections pool
			config.setUrl("jdbc:sqlite:config.db");
			config.setMinIdle(2);
			config.setMaxIdle(5);
			config.setMaxTotal(15);

			Connection conn = config.getConnection();
			if(conn != null) {
				logger.info("Connected to config database.");
				conn.close();
			}
		} catch (SQLException e) {
			logger.error("Config database connection failed. Error details:");
			logger.error(e.getStackTrace());
		}

		Connection cacheConn;

		//Creating database for caching website data.
		File cacheFile = new File("./cache.db");
		String cacheUrl = "jdbc:sqlite:cache.db";
		if(!cacheFile.exists()) {
			logger.info("Config database not found. Creating a new database...");
			try {
				cacheConn = DriverManager.getConnection(cacheUrl);
				if(cacheConn != null) {
					logger.info("Cache database created.");
					logger.info("Now running setup commands...");
					Statement create = cacheConn.createStatement();

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
		}
		try {
			//Initializing connections pool
			cache.setUrl("jdbc:sqlite:cache.db");
			cache.setMinIdle(2);
			cache.setMaxIdle(5);
			cache.setMaxTotal(30);

			Connection conn = cache.getConnection();
			if (conn != null) {
				logger.info("Connected to cache database.");
				conn.close();
			}

		} catch (SQLException e) { //fuck
			logger.error("Config database connection failed. Error details:");
			logger.error(e.getStackTrace());
		}
	}

	/**
	 * Gets the prefix for a guild.
	 * Internally caches prefixes in memory to prevent excessive SQL queries.
	 * @param guildId The ID of the guild to which the get the prefix of.
	 * @return The prefix of the guild.
	 */
	public String getPrefix(String guildId) {
		String prefix;

		if (prefixes.containsKey(guildId)) {
			prefix = prefixes.get(guildId);
		} else {
			try (Connection configConn = config.getConnection()) {
				//Create connection and fetch if internal cache doesn't have it

				PreparedStatement stmt = configConn.prepareStatement("SELECT prefix FROM prefixes WHERE guild_id=?");
				//fuck you bobby
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

	public void setPrefix(String prefix, String guildId) throws SQLException {
		try (Connection configConn = config.getConnection()) {
			prefixes.remove(guildId);

			PreparedStatement deleteOldPrefix = configConn.prepareStatement("DELETE FROM prefixes WHERE guild_id=?");
			deleteOldPrefix.setLong(1, Long.parseLong(guildId));
			deleteOldPrefix.execute();

			PreparedStatement stmt = configConn.prepareStatement("INSERT INTO prefixes VALUES (?, ?)");
			stmt.setLong(1, Long.parseLong(guildId));
			stmt.setString(2, prefix);

			stmt.execute();

			prefixes.put(guildId, prefix);
		}
	}

	public void addHook(String channelId, String guildId) throws SQLException {
		try (Connection configConn = config.getConnection()) {
			PreparedStatement stmt = configConn.prepareStatement("INSERT INTO hookchannels VALUES (?, ?)");
			stmt.setLong(1, Long.parseLong(guildId));
			stmt.setString(2, channelId);

			stmt.execute();
		}
	}

	public void removeHook(String channelId) throws SQLException {
		try (Connection configConn = config.getConnection()) {
			PreparedStatement stmt = configConn.prepareStatement("DELETE FROM hookchannels WHERE channel_id=?");
			stmt.setLong(1, Long.parseLong(channelId));

			stmt.execute();
		}
	}

	public ArrayList<TextChannel> getHookChannels(JDA jda) {
		try (Connection configConn = config.getConnection()) {
			Statement stmt = configConn.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM hookchannels");

			ArrayList<TextChannel> results = new ArrayList<>();
			while (rs.next()) {
				Guild curGuild = jda.getGuildById(rs.getString(1));
				if(curGuild == null) {
					continue;
				}
				results.add(curGuild.getTextChannelById(rs.getString(2)));
			}
			stmt.close();
			return results;
		} catch (SQLException e) {
			logger.error("Something went wrong when retrieving the hook channels.");
			e.printStackTrace();
			return null;
		}
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
		try (Connection cacheConn = cache.getConnection()) {
			PreparedStatement guillotine = cacheConn.prepareStatement("SELECT * FROM ehentai WHERE url=?");
			guillotine.setString(1, load.getUrl());

			ResultSet rs = guillotine.executeQuery();
			//execute the nobles

			if(rs.next()) {
				// If the entry is older than 14 days...
				if(rs.getLong("timecached") < (Instant.now().getEpochSecond() - (86400 * 14))) {

					PreparedStatement coronavirus = cacheConn.prepareStatement("DELETE FROM ehentai WHERE url=?");
					//Execute the old queries!

					coronavirus.setString(1, load.getUrl());
					coronavirus.execute();

					coronavirus.close();
					//i wish we could do this too

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

				load.setArtists(loadSetFromTable("ehartists", url, cacheConn));
				load.setGroups(loadSetFromTable("ehgroups", url, cacheConn));
				load.setParodies(loadSetFromTable("ehparodies", url, cacheConn));
				load.setChars(loadSetFromTable("ehchars", url, cacheConn));
				load.setMaleTags(loadSetFromTable("ehmaletags", url, cacheConn));
				load.setFemaleTags(loadSetFromTable("ehfemaletags", url, cacheConn));
				load.setMiscTags(loadSetFromTable("ehmisctags", url, cacheConn));
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
		try (Connection cacheConn = cache.getConnection()) {
			PreparedStatement guillotine = cacheConn.prepareStatement("SELECT * FROM nhentai WHERE url=?");
			guillotine.setString(1, load.getUrl());

			ResultSet rs = guillotine.executeQuery();
			//execute the nobles!

			if(rs.next()) {
				// If the entry is older than 14 days...
				if(rs.getLong("timecached") < (Instant.now().getEpochSecond() - (86400 * 14))) {
					PreparedStatement coronavirus = cacheConn.prepareStatement("DELETE FROM nhentai WHERE url=?");
					//execute the old queries!
					coronavirus.setString(1, load.getUrl());
					coronavirus.execute();

					coronavirus.close();
					//i wish
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

				load.setArtists(loadSetFromTable("nhartists", url, cacheConn));
				load.setGroups(loadSetFromTable("nhgroups", url, cacheConn));
				load.setParodies(loadSetFromTable("nhparodies", url, cacheConn));
				load.setChars(loadSetFromTable("nhchars", url, cacheConn));
				load.setTags(loadSetFromTable("nhtags", url, cacheConn));
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
	 * Caches the data currently inside the passed EHFetcher to internal storage.
	 * @param data The EHFetcher with data to be cached.
	 */
	public void cache(EHFetcher data) {
		try (Connection cacheConn = cache.getConnection()) {
			PreparedStatement stmt = cacheConn.prepareStatement("INSERT INTO ehentai VALUES " +
					"(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

			//insert necessary values and tell bobby to fuck off
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

			insertSetIntoTable(data.getArtists(),"ehartists", url, cacheConn);
			insertSetIntoTable(data.getGroups(),"ehgroups", url, cacheConn);
			insertSetIntoTable(data.getParodies(),"ehparodies", url, cacheConn);
			insertSetIntoTable(data.getChars(),"ehchars", url, cacheConn);
			insertSetIntoTable(data.getMaleTags(),"ehmaletags", url, cacheConn);
			insertSetIntoTable(data.getFemaleTags(),"ehfemaletags", url, cacheConn);
			insertSetIntoTable(data.getMiscTags(),"ehmisctags", url, cacheConn);

			logger.info("Caching completed.");
		} catch (SQLException e) {
			logger.error("An error occurred during the cache insertion.");
			e.printStackTrace();
		}
	}

	/**
	 * Caches the data currently inside the passed NHFetcher to internal storage.
	 * @param data The NHFetcher with data to be cached.
	 */
	public void cache(NHFetcher data) {
		try (Connection cacheConn = cache.getConnection()) {
			PreparedStatement stmt = cacheConn.prepareStatement("INSERT INTO nhentai VALUES " +
					"(?, ?, ?, ?, ?, ?, ?, ?, ?)");

			//insert necessary values and tell bobby to fuck off
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

			insertSetIntoTable(data.getArtists(),"nhartists", url, cacheConn);
			insertSetIntoTable(data.getGroups(),"nhgroups", url, cacheConn);
			insertSetIntoTable(data.getParodies(),"nhparodies", url, cacheConn);
			insertSetIntoTable(data.getChars(),"nhchars", url, cacheConn);
			insertSetIntoTable(data.getTags(),"nhtags", url, cacheConn);

			logger.info("Caching completed.");
		} catch (SQLException e) {
			logger.error("An error occurred during the cache insertion.");
			e.printStackTrace();
		}
	}

	private HashSet<String> loadSetFromTable(String tableName, String url, Connection conn) throws SQLException {
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

	private void insertSetIntoTable(HashSet<String> tags, String tableName, String url, Connection conn) throws SQLException {
		//bobby cant touch tableName so we're good
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
