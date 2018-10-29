package pl.shockah.iguana.bridge;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.TextChannel;

import org.pircbotx.Channel;
import org.pircbotx.InputParser;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.cap.EnableCapHandler;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.ConnectEvent;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.ServerResponseEvent;
import org.pircbotx.hooks.events.TopicEvent;
import org.pircbotx.hooks.managers.ThreadedListenerManager;
import org.pircbotx.snapshot.UserChannelDaoSnapshot;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.AccessLevel;
import lombok.Getter;
import pl.shockah.iguana.Configuration;
import pl.shockah.iguana.IguanaSession;
import pl.shockah.iguana.irc.AccountNotifyEvent;
import pl.shockah.iguana.irc.ExtendedJoinEvent;
import pl.shockah.iguana.irc.IrcInputParser;
import pl.shockah.iguana.irc.IrcListenerAdapter;
import pl.shockah.iguana.irc.NickServIdentityManager;
import pl.shockah.util.ReadWriteMap;

public class IrcServerBridge {
	@Nonnull
	@Getter
	private final IguanaSession session;

	@Nullable
	private PircBotX ircBot;

	@Nonnull
	@Getter
	private final Configuration.IRC.Server ircServerConfig;

	@Nonnull
	@Getter
	private final ReadWriteMap<Configuration.IRC.Server.Channel, IrcChannelBridge> channels = new ReadWriteMap<>(new HashMap<>());

	@Nonnull
	@Getter
	private final ReadWriteMap<Configuration.IRC.Server.Private, IrcPrivateBridge> privateBridges = new ReadWriteMap<>(new HashMap<>());

	@Nonnull
	@Getter(lazy = true)
	private final Category discordChannelCategory = ircServerConfig.getDiscordChannelCategory(session.getDiscord());

	@Nonnull
	@Getter(lazy = true)
	private final TextChannel discordManagementChannel = ircServerConfig.getDiscordManagementChannel(session.getDiscord());

	@Nonnull
	@Getter(lazy = true)
	private final ListenerAdapter ircListener = new IrcListener();

	@Nonnull
	@Getter
	private final Thread ircThread = new Thread(() -> {
		try {
			getIrcBot().startBot();
		} catch (IOException | IrcException e) {
			throw new RuntimeException(e);
		}
	});

	@Getter(value = AccessLevel.PRIVATE, lazy = true)
	private final boolean availableWhoX = ircBot.getServerInfo().isWhoX();

	@Nonnull
	protected final NickServIdentityManager nickServIdentityManager = new NickServIdentityManager();

	public IrcServerBridge(@Nonnull IguanaSession session, @Nonnull Configuration.IRC.Server ircServerConfig) {
		this.session = session;
		this.ircServerConfig = ircServerConfig;
	}

	public synchronized void start() {
		ThreadedListenerManager listenerManager = new ThreadedListenerManager();
		listenerManager.addListener(getIrcListener());

		org.pircbotx.Configuration.Builder ircConfigBuilder = new org.pircbotx.Configuration.Builder()
				.addServer(ircServerConfig.getHost(), ircServerConfig.getPort())
				.setName(ircServerConfig.getNickname())
				.setAutoNickChange(true)
				.setAutoReconnect(true)
				.setListenerManager(listenerManager)
				.addCapHandler(new EnableCapHandler("extended-join", true))
				.addCapHandler(new EnableCapHandler("account-notify", true))
				.setBotFactory(new org.pircbotx.Configuration.BotFactory() {
					@Override
					public InputParser createInputParser(PircBotX bot) {
						return new IrcInputParser(bot);
					}
				});

		for (Configuration.IRC.Server.Channel ircChannel : ircServerConfig.getChannels()) {
			if (ircChannel.getPassword() == null)
				ircConfigBuilder.addAutoJoinChannel(ircChannel.getName());
			else
				ircConfigBuilder.addAutoJoinChannel(ircChannel.getName(), ircChannel.getPassword());
		}

		if (ircServerConfig.getNickServLogin() != null) {
			ircConfigBuilder.setNickservNick(ircServerConfig.getNickServLogin());
			ircConfigBuilder.setNickservPassword(ircServerConfig.getNickServPassword());
			ircConfigBuilder.setNickservDelayJoin(true);
		}

		ircBot = new PircBotX(ircConfigBuilder.buildConfiguration());

		for (Configuration.IRC.Server.Channel ircChannelConfig : ircServerConfig.getChannels()) {
			channels.put(ircChannelConfig, new IrcChannelBridge(this, ircChannelConfig));
		}

		ircThread.start();
	}

	public synchronized void stop() {
		PircBotX bot = getIrcBot();
		bot.stopBotReconnect();
		bot.close();
	}

	@Nonnull
	public synchronized PircBotX getIrcBot() {
		if (ircBot == null)
			throw new IllegalStateException("PircBotX not yet started.");
		return ircBot;
	}

	@Nonnull
	protected final IrcChannelBridge getChannelBridge(@Nonnull Channel channel) {
		return channels.readOperation(channels -> {
			for (Map.Entry<Configuration.IRC.Server.Channel, IrcChannelBridge> entry : channels.entrySet()) {
				if (entry.getKey().getName().equals(channel.getName()))
					return entry.getValue();
			}
			throw new IllegalStateException();
		});
	}

	@Nonnull
	protected final IrcPrivateBridge getPrivateBridge(@Nonnull User user) {
		return privateBridges.writeOperation(privateBridges -> {
			for (Map.Entry<Configuration.IRC.Server.Private, IrcPrivateBridge> entry : privateBridges.entrySet()) {
				String nick = user.getNick();
				String account = nickServIdentityManager.getAccountForUser(user);

				if (entry.getKey().isNickServAccount()) {
					if (entry.getKey().getName().equals(account))
						return entry.getValue();
				} else {
					if (entry.getKey().getName().equals(nick))
						return entry.getValue();
				}
			}

			// TODO: create and return new
			throw new IllegalStateException();
		});
	}

	private class IrcListener extends IrcListenerAdapter {
		@Override
		public void onConnect(ConnectEvent event) throws Exception {
			super.onConnect(event);

			getDiscordManagementChannel().sendMessage(new EmbedBuilder()
					.setColor(session.getConfiguration().appearance.events.getConnectedColor())
					.setDescription(String.format("Connected to `%s`.", ircServerConfig.getHost()))
					.setTimestamp(Instant.now())
					.build()).queue();
		}

		@Override
		public void onDisconnect(DisconnectEvent event) throws Exception {
			super.onDisconnect(event);

			getDiscordManagementChannel().sendMessage(new EmbedBuilder()
					.setColor(session.getConfiguration().appearance.events.getDisconnectedColor())
					.setDescription(String.format("Disconnected from `%s`.", ircServerConfig.getHost()))
					.setTimestamp(Instant.now())
					.build()).queue();
		}

		@Override
		public void onMessage(MessageEvent event) throws Exception {
			super.onMessage(event);

			Channel channel = event.getChannel();
			if (channel == null)
				return;

			getChannelBridge(channel).onChannelMessage(event);
		}

		@Override
		public void onAction(ActionEvent event) throws Exception {
			super.onAction(event);

			Channel channel = event.getChannel();
			if (channel == null)
				return;

			getChannelBridge(channel).onChannelAction(event);
		}

		@Override
		public void onNotice(NoticeEvent event) throws Exception {
			super.onNotice(event);

			Channel channel = event.getChannel();
			if (channel == null)
				return;

			getChannelBridge(channel).onChannelNotice(event);
		}

		@Override
		public void onPrivateMessage(PrivateMessageEvent event) throws Exception {
			super.onPrivateMessage(event);

			getPrivateBridge(event.getUser()).onPrivateMessage(event);
		}

		@Override
		public void onJoin(JoinEvent event) throws Exception {
			super.onJoin(event);

			if (event.getBot().getUserBot().equals(event.getUser())) {
				if (isAvailableWhoX())
					event.getBot().sendRaw().rawLine(String.format("WHO %s %%na", event.getChannel().getName()));
				getChannelBridge(event.getChannel()).onSelfJoin(event);
			} else {
				getChannelBridge(event.getChannel()).onJoin(event);
			}
		}

		@Override
		public void onPart(PartEvent event) throws Exception {
			super.onPart(event);

			User user = event.getUser();

			if (event.getBot().getUserBot().equals(user))
				return;

			if (user.getChannels().isEmpty() || (user.getChannels().size() == 1 && user.getChannels().first().equals(event.getChannel())))
				nickServIdentityManager.updateAccount(user, null);

			getChannelBridge(event.getChannel()).onPart(event);
		}

		@Override
		public void onQuit(QuitEvent event) throws Exception {
			super.onQuit(event);

			if (event.getBot().getUserBot().equals(event.getUser()))
				return;

			nickServIdentityManager.updateAccount(event.getUser(), null);

			UserChannelDaoSnapshot dao = event.getUserChannelDaoSnapshot();
			if (dao == null)
				return;

			channels.iterateValues(bridge -> {
				Channel snapshotChannel = dao.getChannel(bridge.getIrcChannel().getName());
				if (snapshotChannel.getUsers().contains(event.getUser()))
					bridge.onQuit(event);
			});
		}

		@Override
		public void onNickChange(NickChangeEvent event) throws Exception {
			super.onNickChange(event);

			if (event.getBot().getUserBot().equals(event.getUser()))
				return;

			nickServIdentityManager.updateAccount(event.getNewNick(), nickServIdentityManager.getAccountForUser(event.getOldNick()));
			nickServIdentityManager.updateAccount(event.getOldNick(), null);

			channels.iterateValues(bridge -> {
				if (bridge.getIrcChannel().getUsers().contains(event.getUser()))
					bridge.onNickChange(event);
			});
		}

		@Override
		public void onTopic(TopicEvent event) throws Exception {
			super.onTopic(event);

			Channel channel = event.getChannel();
			if (channel == null)
				return;

			getChannelBridge(channel).onTopic(event);
		}

		@Override
		public void onExtendedJoin(ExtendedJoinEvent event) {
			super.onExtendedJoin(event);
			nickServIdentityManager.updateAccount(event.getUser(), event.getAccount());
		}

		@Override
		public void onAccountNotify(AccountNotifyEvent event) {
			super.onAccountNotify(event);
			nickServIdentityManager.updateAccount(event.getUser(), event.getAccount());
		}

		@Override
		public void onServerResponse(ServerResponseEvent event) throws Exception {
			super.onServerResponse(event);
			if (event.getCode() != 354)
				return;

			List<String> response = event.getParsedResponse();
			if (response.size() == 3)
				nickServIdentityManager.updateAccount(response.get(1), response.get(2));
		}
	}
}