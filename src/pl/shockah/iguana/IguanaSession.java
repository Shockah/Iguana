package pl.shockah.iguana;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;

import lombok.Getter;
import pl.shockah.iguana.bridge.IrcBridge;
import pl.shockah.iguana.bridge.IrcServerBridge;
import pl.shockah.iguana.format.discord.DiscordFormatter;
import pl.shockah.iguana.format.irc.IrcFormatter;

public class IguanaSession {
	@Nonnull
	@Getter
	private final Iguana app;

	@Nonnull
	@Getter
	private final Configuration configuration;

	@Nonnull
	@Getter
	private final JDA discord;

	@Nonnull
	@Getter
	private final IrcBridge bridge;

	@Nonnull
	@Getter
	private final DiscordFormatter discordFormatter = new DiscordFormatter();

	@Nonnull
	@Getter
	private final IrcFormatter ircFormatter = new IrcFormatter();

	public IguanaSession(@Nonnull Iguana app, @Nonnull Configuration configuration) throws Exception {
		this.app = app;
		this.configuration = configuration;
		bridge = new IrcBridge(this);

		try {
			discord = new JDABuilder(AccountType.BOT).addEventListener(new ListenerAdapter() {
				@Override
				public void onReady(ReadyEvent event) {
					super.onReady(event);

					for (Configuration.IRC.Server ircServerConfig : configuration.irc.getServers()) {
						IrcServerBridge serverBridge = new IrcServerBridge(IguanaSession.this, ircServerConfig);
						serverBridge.start();
						bridge.getServers().put(ircServerConfig, serverBridge);
					}

					new Thread(() -> {
						bridge.prepareIrcTask().run();

						bridge.getServers().readOperation(servers -> {
							for (Configuration.IRC.Server ircServerConfig : servers.keySet()) {
								bridge.prepareIrcServerTask(ircServerConfig).run();
							}
						});

						try {
							app.saveConfigJson(configuration.write());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}).start();
				}
			}).addEventListener(bridge).setToken(configuration.discord.getToken()).build();
		} catch (LoginException e) {
			throw new Exception(e);
		}
	}

	public static class Exception extends java.lang.Exception {
		public Exception() {
			super();
		}

		public Exception(@Nonnull Throwable throwable) {
			super(throwable);
		}
	}
}