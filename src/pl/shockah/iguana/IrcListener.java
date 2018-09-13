package pl.shockah.iguana;

import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.Webhook;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;

import org.pircbotx.Channel;
import org.pircbotx.User;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

public class IrcListener extends ListenerAdapter {
	@Nonnull
	private final IguanaSession session;

	@Nonnull
	private final Map<Webhook, WebhookClient> webhookClientCache = Collections.synchronizedMap(new HashMap<>());

	public IrcListener(@Nonnull IguanaSession session) {
		this.session = session;
	}

	@Nonnull
	private TextChannel getDiscordChannel(@Nonnull Channel ircChannel) {
		Configuration.IRC.Server ircServerConfig = session.getReverseIrcMap().get(ircChannel.getBot());
		Configuration.IRC.Server.Channel ircChannelConfig = ircServerConfig.getChannels().stream()
				.filter(channelConfig -> channelConfig.getName().equals(ircChannel.getName()))
				.findFirst().orElseThrow(IllegalStateException::new);
		return ircChannelConfig.getDiscordChannel(session.getDiscord());
	}

	@Nonnull
	private WebhookClient getWebhookClient(@Nonnull Channel ircChannel) {
		Configuration.IRC.Server ircServerConfig = session.getReverseIrcMap().get(ircChannel.getBot());
		Configuration.IRC.Server.Channel ircChannelConfig = ircServerConfig.getChannels().stream()
				.filter(channelConfig -> channelConfig.getName().equals(ircChannel.getName()))
				.findFirst().orElseThrow(IllegalStateException::new);
		Webhook webhook = ircChannelConfig.getWebhook(session.getDiscord());
		return webhookClientCache.computeIfAbsent(webhook, key -> webhook.newClient().build());
	}

	@Override
	public void onMessage(MessageEvent event) throws Exception {
		Channel ircChannel = event.getChannel();
		if (ircChannel == null)
			return;

		User ircUser = event.getUser();
		if (ircUser == null)
			return;

		getWebhookClient(ircChannel).send(
				new WebhookMessageBuilder()
						.setUsername(ircUser.getNick())
						.setContent(event.getMessage())
						.build()
		);
	}
}