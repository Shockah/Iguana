package pl.shockah.iguana;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.managers.ThreadedListenerManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;

import lombok.Getter;

public class IguanaSession {
	@Nonnull
	@Getter
	private final Configuration configuration;

	@Nonnull
	@Getter
	private final JDA discord;

	@Nonnull
	private final Map<Configuration.IRC.Server, PircBotX> modifiableIrcMap = new HashMap<>();

	@Nonnull
	@Getter
	private final Map<Configuration.IRC.Server, PircBotX> ircMap = Collections.unmodifiableMap(modifiableIrcMap);

	@Nonnull
	private final Map<PircBotX, Configuration.IRC.Server> modifiableReverseIrcMap = new LinkedHashMap<>();

	@Nonnull
	@Getter
	private final Map<PircBotX, Configuration.IRC.Server> reverseIrcMap = Collections.unmodifiableMap(modifiableReverseIrcMap);

	public IguanaSession(@Nonnull Configuration configuration) throws SessionException {
		this.configuration = configuration;

		try {
			discord = new JDABuilder(AccountType.BOT).setToken(configuration.discord.getToken()).build();
		} catch (LoginException e) {
			throw new SessionException(e);
		}

		for (Configuration.IRC.Server ircServer : configuration.irc.getServers()) {
			org.pircbotx.Configuration ircConfiguration = new org.pircbotx.Configuration.Builder()
					.addServer(ircServer.getHost(), ircServer.getPort())
					.setName(ircServer.getNickname())
					.setAutoNickChange(true)
					.setListenerManager(new ThreadedListenerManager() {{
						addListener(new IrcListener(IguanaSession.this));
					}})
					.buildConfiguration();
			PircBotX ircBot = new PircBotX(ircConfiguration);
			modifiableIrcMap.put(ircServer, ircBot);
			modifiableReverseIrcMap.put(ircBot, ircServer);
		}
	}

	public static class SessionException extends Exception {
		public SessionException() {
			super();
		}

		public SessionException(@Nonnull Throwable throwable) {
			super(throwable);
		}
	}
}