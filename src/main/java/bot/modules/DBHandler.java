package bot.modules;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.sql.*;
import java.util.HashMap;

public class DBHandler {
	private final HashMap<String, String> prefixes;
	private Connection connfig;
	private Connection cacheconn;
	private static final Logger logger = LogManager.getLogger(DBHandler.class);

	public DBHandler() {
		prefixes = new HashMap<>();

		File config = new File("./config.db");
		String configUrl = "jdbc:sqlite:config.db";
		if(!config.exists()) {
			logger.info("Config database not found. Creating a new database...");
			try {
				connfig = DriverManager.getConnection(configUrl);
				if(connfig != null) {
					logger.info("Config database created.");
					logger.info("Now running setup commands...");

					Statement create = connfig.createStatement();

					create.execute("CREATE TABLE prefixes (guild_id INTEGER PRIMARY KEY, prefix text)");
				}
			} catch (SQLException e) {
				logger.error("Config database creation failed. Error details:");
				logger.error(e.getStackTrace());
			}
		} else try {
			connfig = DriverManager.getConnection(configUrl);
			if (connfig != null) {
				logger.info("Connected to config database.");
			}
		} catch (SQLException e) {
			logger.error("Config database connection failed. Error details:");
			logger.error(e.getStackTrace());
		}

		File cache = new File("./cache.db");
		String cacheUrl = "jdbc:sqlite:cache.db";
		if(!cache.exists()) {
			logger.info("Config database not found. Creating a new database...");
			try {
				cacheconn = DriverManager.getConnection(cacheUrl);
				if(cacheconn != null) {
					logger.info("Cache database created.");
					logger.info("Now running setup commands...");
					Statement create = cacheconn.createStatement();
					create.execute("CREATE TABLE ehentai (url text, data blob, timecached integer)");
					create.execute("CREATE TABLE nhentai (number integer, data blob, timecached integer)");
				}
			} catch (SQLException e) {
				logger.error("Config database creation failed. Error details:");
				logger.error(e.getStackTrace());
			}
		} else try {
			cacheconn = DriverManager.getConnection(cacheUrl);
			if (cacheconn != null) {
				logger.info("Connected to config database.");
			}
		} catch (SQLException e) {
			logger.error("Config database connection failed. Error details:");
			logger.error(e.getStackTrace());
		}
	}

	public String getPrefix(String guildId) {
		String prefix;

		if (prefixes.containsKey(guildId)) {
			prefix = prefixes.get(guildId);
		} else {
			try {
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
}
