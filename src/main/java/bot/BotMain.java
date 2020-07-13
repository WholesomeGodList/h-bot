package bot;

import bot.modules.DBHandler;
import bot.sites.ehentai.EHApiHandler;
import bot.sites.nhentai.NHHook;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BotMain {
	private static final Logger logger = LogManager.getLogger(BotMain.class);
	private static JDA myBot;
	private static DBHandler database;
	private static EHApiHandler handler;
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	public static void main(String[] args) {
		try {
			logger.info("Log file found! Loading bot...");
			ArrayList<GatewayIntent> intents = new ArrayList<>();
			intents.add(GatewayIntent.GUILD_MESSAGES);
			intents.add(GatewayIntent.GUILD_MESSAGE_REACTIONS);
			intents.add(GatewayIntent.DIRECT_MESSAGES);
			intents.add(GatewayIntent.DIRECT_MESSAGE_REACTIONS);

			logger.info("Loading handlers...");
			Class.forName("org.sqlite.JDBC");

			database = new DBHandler();
			handler = new EHApiHandler();

			myBot = JDABuilder.create(BotConfig.BOT_TOKEN, intents)
					.disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOTE, CacheFlag.CLIENT_STATUS)
					.setChunkingFilter(ChunkingFilter.NONE)
					.addEventListeners(new Wiretap(database, handler))
					.setAutoReconnect(true)
					.setActivity(Activity.watching("hentai | >help"))
					.build()
					.awaitReady();

			logger.info("Bot has started!");

			scheduler.scheduleAtFixedRate(new NHHook(database, myBot), 0, BotConfig.HOOK_FREQUENCY, TimeUnit.MINUTES);
		} catch (LoginException | InterruptedException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}
}