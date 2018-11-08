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
import pl.shockah.iguana.command.impl.JoinCommand;
import pl.shockah.iguana.command.impl.OnlineCommand;
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
	private final DiscordFormatter discordFormatter;

	@Nonnull
	@Getter
	private final IrcFormatter ircFormatter = new IrcFormatter();

	public IguanaSession(@Nonnull Iguana app, @Nonnull Configuration configuration) throws Exception {
		this.app = app;
		this.configuration = configuration;
		discordFormatter = new DiscordFormatter(configuration.appearance);
		bridge = new IrcBridge(this);
		registerCommands();

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

						saveConfiguration();
					}).start();
				}
			}).addEventListener(bridge).setEnableShutdownHook(false).setToken(configuration.discord.getToken()).build();
		} catch (LoginException e) {
			throw new Exception(e);
		}
	}

	public void saveConfiguration() {
		try {
			app.saveConfigJson(configuration.write());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void registerCommands() {
		// channel-specific commands
		bridge.getCommandManager().register(new OnlineCommand(discordFormatter));

		// server-specific commands
		bridge.getCommandManager().register(new JoinCommand());
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