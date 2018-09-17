package pl.shockah.iguana.bridge;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.MessageEvent;

import javax.annotation.Nonnull;

import lombok.Getter;
import pl.shockah.iguana.Configuration;
import pl.shockah.iguana.IguanaSession;
import pl.shockah.iguana.WebhookClientWrapper;

public class IrcChannelBridge {
	@Nonnull
	@Getter
	private final IguanaSession session;

	@Nonnull
	@Getter
	private final PircBotX ircBot;

	@Nonnull
	@Getter
	private final IrcServerBridge serverBridge;

	@Nonnull
	@Getter
	private final Configuration.IRC.Server.Channel ircChannelConfig;

	@Nonnull
	@Getter(lazy = true)
	private final TextChannel discordChannel = ircChannelConfig.getDiscordChannel(session.getDiscord());

	@Nonnull
	@Getter(lazy = true)
	private final Channel ircChannel = ircBot.getUserChannelDao().getChannel(ircChannelConfig.getName());

	@Nonnull
	@Getter(lazy = true)
	private final WebhookClientWrapper webhookClient = new WebhookClientWrapper(ircChannelConfig.getWebhook(session.getDiscord()).newClient().build());

	public IrcChannelBridge(@Nonnull IrcServerBridge serverBridge, @Nonnull Configuration.IRC.Server.Channel ircChannelConfig) {
		session = serverBridge.getSession();
		ircBot = serverBridge.getIrcBot();
		this.serverBridge = serverBridge;
		this.ircChannelConfig = ircChannelConfig;
	}

	public void onMessage(@Nonnull MessageEvent event) {
		Channel channel = event.getChannel();
		if (channel == null)
			return;

		User user = event.getUser();
		if (user == null)
			return;

		getWebhookClient().send(
				new WebhookMessageBuilder()
						.setUsername(user.getNick())
						.setContent(event.getMessage())
						.build()
		);
	}
}