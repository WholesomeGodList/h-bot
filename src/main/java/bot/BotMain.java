package bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class BotMain {
	private static final Logger logger = LogManager.getLogger(BotMain.class);
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static JDA myBot;

	public static void main(String[] args) {
		try {
			logger.info("Log file found!");
			ArrayList<GatewayIntent> intents = new ArrayList<>();
			intents.add(GatewayIntent.GUILD_MESSAGES);
			intents.add(GatewayIntent.GUILD_MESSAGE_REACTIONS);
			intents.add(GatewayIntent.DIRECT_MESSAGES);
			intents.add(GatewayIntent.DIRECT_MESSAGE_REACTIONS);

			myBot = JDABuilder.create(BotConfig.BOT_TOKEN, intents)
					.setChunkingFilter(ChunkingFilter.NONE)
					.addEventListeners(new Wiretap())
					.setAutoReconnect(true)
					.setActivity(Activity.watching("hentai | >help"))
					.build()
					.awaitReady();


			logger.info("Bot has started!");
		} catch (LoginException | InterruptedException e) {
			e.printStackTrace();
		}

		//scheduler.scheduleAtFixedRate(new hHook(), 0, 15, TimeUnit.MINUTES);
	}

	public static JDA getMyBot() {
		return myBot;
	}
}