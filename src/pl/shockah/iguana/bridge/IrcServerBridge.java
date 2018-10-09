package pl.shockah.iguana.bridge;

import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.TextChannel;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.managers.ThreadedListenerManager;
import org.pircbotx.snapshot.UserChannelDaoSnapshot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import lombok.Getter;
import pl.shockah.iguana.Configuration;
import pl.shockah.iguana.IguanaSession;
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
	@Getter(lazy = true)
	private final Category discordChannelCategory = ircServerConfig.getDiscordChannelCategory(session.getDiscord());

	@Nonnull
	@Getter(lazy = true)
	private final TextChannel discordManagementChannel = ircServerConfig.getDiscordManagementChannel(session.getDiscord());

	@Nonnull
	@Getter(lazy = true)
	private final ListenerAdapter ircListener = new IrcListener();

	private volatile boolean running = false;

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
				.setListenerManager(listenerManager);

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

		new Thread(() -> {
			try {
				ircBot.startBot();
			} catch (IOException | IrcException e) {
				throw new RuntimeException(e);
			}
		}).start();
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
				if (entry.getKey().getName().equals(channel.getName())) {
					return entry.getValue();
				}
			}
			throw new IllegalStateException();
		});
	}

	private class IrcListener extends ListenerAdapter {
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
		public void onJoin(JoinEvent event) throws Exception {
			super.onJoin(event);

			if (event.getBot().getUserBot().equals(event.getUser()))
				return;

			getChannelBridge(event.getChannel()).onJoin(event);
		}

		@Override
		public void onPart(PartEvent event) throws Exception {
			super.onPart(event);

			if (event.getBot().getUserBot().equals(event.getUser()))
				return;

			getChannelBridge(event.getChannel()).onPart(event);
		}

		@Override
		public void onQuit(QuitEvent event) throws Exception {
			super.onQuit(event);

			if (event.getBot().getUserBot().equals(event.getUser()))
				return;

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

			channels.iterateValues(bridge -> {
				if (bridge.getIrcChannel().getUsers().contains(event.getUser()))
					bridge.onNickChange(event);
			});
		}
	}
}