# H-Bot
[![Java Version](https://img.shields.io/badge/JDK%20Version-14-blue)](https://openjdk.java.net/projects/jdk/14/)
[![GPLv3 license](https://img.shields.io/github/license/WholesomeGodList/h-bot)](http://perso.crans.org/besson/LICENSE.html)
[![Releases](https://img.shields.io/github/v/release/WholesomeGodList/h-bot)](https://github.com/WholesomeGodList/h-bot-old/releases)
![build](https://github.com/WholesomeGodList/h-bot/workflows/Gradle%20CI/badge.svg)

# Public Invite Link

**Coming soon!**

# Bot Information

**The bot is undergoing a full-scale rewrite to improve scalability. Once I complete the rewrite, I will release a public invite link.**

Discord bot built using the wonderful [JDA library](https://github.com/DV8FromTheWorld/JDA) that provides info about nhentai (AND NOW E-HENTAI!) doujins.
Focused on features that find wholesome, vanilla doujins (we are a vanilla server after all), and warns about any potentially objectionable tags.

Join our Discord for support and vanilla doujins!

[discord-invite]: https://discord.gg/FQCR6qu
[discord-shield]: https://discordapp.com/api/guilds/624457027095363594/widget.png
[ ![discord-shield][] ][discord-invite]

# Contributing
Go ahead and contribute if you'd like. I'll review it and probably merge it. If you need documentation, just join the Discord and ask me what anything does.
This rewrite is mostly documented, but any questions are still welcome.

# Functionality
As I've stated, this bot has a *focus on wholesomeness*. This means that it will warn you about things like netorare, and will also have a suite of wholesomeness-focused commands and utilities. As for the functionality,
H-Bot comes equipped with a lot of commands, plus a bootleg webhook for nhentai. It also conforms with Discord's terms of service by blocking any doujins with the lolicon or shotacon tags.

The commands (MOST OF THESE ARE NOT IMPLEMENTED YET! Rewrites take time!) are as follows:

- help - Your run-of-the-mill help command. Will display a useful embed of all the commands you can do.
- tags - Gets all the tags of a doujin.
- getpage - Gets the direct image link of a page of any doujin.
- info - Gets the information of a doujin and packages it in a neat embed. Will also warn you about any potentially objectionable tags so you can avoid surprise NTR.
- random - Gets a random doujin (by default restricted to English). There are a variety of tags you can use to blacklist certain types of doujins.
  - -e - allows non-english results
  - -nbt - does not allow any bad tags (can be found through the badtags command)
  - -w - does not allow any bad tags OR warning tags (can be found through the badtags command)
  - -i - does not allow incest / inseki
  - -ya / -yu - does not allow yaoi / yuri
- badtags / warningtags - DMs the user a list of the bad tags / warning tags.
- supportedsites / sites - Gives a list of the supported sites (currently only nhentai; planned expansion to exhentai)
- search - Searches nhentai with the provided query, looking through 100 doujins sorting by popular, and returns the ones that do not contain any bad tags or warning tags AND have 3 or more tags, along with a random info embed
- deepsearch - search, but with 250 doujins instead of 100. Will take a very long time.
  - -n (works with both search and deepsearch) - nonrestrictive search, which relaxes the requirement to only not containing the bad tags.
- searcheh - search but for e-hentai
- deepsearcheh - deepsearch but for e-hentai

### Webhook

The webhook is a feature of the bot that provides a webhook functionality with nhentai. Every 15 minutes, it will run a scheduled scan of the latest doujins, and automatically upload the wholesome ones (no bad or warning tags, and at least 3 tags) to the hook channels.

Commands to set it up:
- addhook - Registers the channel as a webhook channel (not shown in help, requires Manage Server permission)
- removehook - Unregisters the channel as a webhook channel (not shown in help, requires Manage Server permission)

# Dependencies (These are built into the jar already)
- [JDA](https://github.com/DV8FromTheWorld/JDA)
- [Log4j2](https://logging.apache.org/log4j/2.x/)
- [Jsoup](https://jsoup.org/)
- [JSON-Java](https://github.com/stleary/JSON-java)
- [SQLite3 JDBC Driver](https://github.com/xerial/sqlite-jdbc)
- [Apache Commons DBCP](https://commons.apache.org/proper/commons-dbcp/)
- [Apache Commons Text](https://commons.apache.org/proper/commons-text/)
- [Apache Commons HttpComponents](https://hc.apache.org/)

# Setup
If you would like to self-host this bot for your own servers, you're going to need a few steps.

### Create the Discord bot

First off, you should [create the Discord bot](https://discordapp.com/developers). Click on New Application, name the bot whatever you want, attach a bot user, and invite it to your server from the OAuth2 tab (with the following permissions integer: 125952, meaning that in the URL that says `https://discordapp.com/api/oauth2/authorize?`, you should have the flag `&permissions=125952`.)

If you get confused by this, there are plenty of image tutorials on the Internet. Creating the Discord bot is a pretty involved process at first, so don't feel too bad.

After this, save the bot token provided to you in the Bot tab somewhere (you'll need it later). Also make sure to keep this a secret - if someone else has access to this, they have access to your bot.

Once you have the bot in your server, you are ready for the next step.

### Setting up the bot locally

To get started, you should make a folder somewhere, and download whatever version of H-Bot you want from Releases.

Download hbot.jar here: [![Releases](https://img.shields.io/github/v/release/WholesomeGodList/h-bot)](https://github.com/WholesomeGodList/h-bot/releases)

To run, the bot expects 1 file. (Another one's optional.)
- config.json
- log4j2.xml (if you care about logging)

All of these files can be found in the `samples` folder. Go ahead and download them and place them in the **same directory** as the bot's jar file. For config.json, make sure that you replace the `"botToken": "Your bot token here"` with the bot token I told you to save earlier. This is essential to make sure your bot actually starts up.

Once you're done with all of that, your bot is ready to start.

### Startup
Since I do use preview features in the source code, you won't be able to just run it using java -jar. This means this also requires JDK 13. If you don't know how to set up JDK 13, I recommend going to [AdoptOpenJDK and installing JDK 13](https://adoptopenjdk.net/?variant=openjdk13&jvmVariant=hotspot) through their easy-to-use binaries.

To run the bot, you need to run the following command:
```
java --enable-preview -jar hbot.jar
```

This means if you are using Windows, you can make a Batch file:
```
@ECHO OFF
java --enable-preview -jar hbot.jar
pause
EXIT
```

This will work as long as you put it in the same folder as the H-Bot jar. Make sure the bot is running using JDK 13. If it's not, this will not work. To ensure that it is running with JDK 13, instead of using `java`, you could instead use:
```
<your_path_to_java_14>\bin\java.exe --enable-preview -jar hbot.jar
```

For Windows and AdoptOpenJDK users, that path will be:
```
"C:\Program Files\AdoptOpenJDK\<JDK/JRE 14 folder>\bin\java.exe
```

Once you have all of this set up, you're pretty much good to go! One last thing.

### The Telecom
If you want this telecom (functionality explained in the Functionality section) in your server, you're going to have to perform just a few extra steps.

Make sure you have the Manage Server role (which you should if you invited the bot) if you want to set up a telecom channel.

Create any channel / repurpose any existing channel, then use the command `addhook` (with default prefix, that would be `>addhook`).

If you need to delete this hook for any reason, go to that same channel and use the command `removehook`.

### Congrats!
That's all the setup you need to do. Have fun with the bot!
