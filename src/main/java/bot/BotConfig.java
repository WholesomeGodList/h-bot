package bot;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BotConfig {
	//This just loads the config of the bot.
	public static final String BOT_TOKEN = readRequiredConfigValue("token", "ERROR: Config file not found. Please make sure that config.json is in the same folder as hbot.jar.");
	public static final int HOOK_FREQUENCY = Integer.parseInt(readRequiredConfigValue("hooktime", "ERROR: Hook duration not found. Please make sure config.json is in the same folder as hbot.jar."));

	private static String readRequiredConfigValue(String key, String errorMessage) {
		try {
			InputStream is = new FileInputStream("./config.json");
			JSONObject bruh = new JSONObject(new JSONTokener(is));
			return bruh.getString(key);
		} catch (IOException e) {
			System.err.println(errorMessage);
			System.err.println("Stopping bot...");
			System.exit(1);
			return "If this somehow enters the code I'm suing Java";
		}
	}
}
