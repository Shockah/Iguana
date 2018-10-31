package pl.shockah.iguana.bridge;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;

import org.pircbotx.PircBotX;
import org.pircbotx.User;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

import lombok.Getter;
import pl.shockah.iguana.IguanaSession;
import pl.shockah.iguana.WebhookClientWrapper;
import pl.shockah.unicorn.UnexpectedException;
import pl.shockah.unicorn.collection.Either2;

public abstract class IrcMessageBridge {
	@Nonnull
	@Getter
	protected final IguanaSession session;

	@Nonnull
	@Getter
	protected final PircBotX ircBot;

	@Nonnull
	@Getter
	protected final IrcServerBridge serverBridge;

	@Nonnull
	@Getter(lazy = true)
	private final WebhookClientWrapper webhookClient = initializeWebhookClient();

	public IrcMessageBridge(@Nonnull IrcServerBridge serverBridge) {
		this.session = serverBridge.getSession();
		this.ircBot = serverBridge.getIrcBot();
		this.serverBridge = serverBridge;
	}

	@Nonnull
	protected abstract WebhookClientWrapper initializeWebhookClient();

	@Nonnull
	private Either2<String, BufferedImage> getFormattedIrcToDiscordMessage(@Nonnull String ircMessage) {
		ircMessage = ircMessage.replace(ircBot.getUserBot().getNick(), session.getConfiguration().discord.getOwnerUser(session.getDiscord()).getAsMention());
		return session.getDiscordFormatter().output(session.getIrcFormatter().parse(ircMessage, null), null);
	}

	@Nonnull
	protected String formatNick(@Nonnull User user) {
		return user.getNick();
	}

	protected void onMessage(@Nonnull User user, @Nonnull String message) {
		onMessage(user, message, false);
	}

	protected void onMessage(@Nonnull User user, @Nonnull String message, boolean notice) {
		try {
			WebhookMessageBuilder builder = new WebhookMessageBuilder()
					.setUsername(formatNick(user))
					.setAvatarUrl(session.getBridge().getAvatarUrl(user.getNick()));

			getFormattedIrcToDiscordMessage(message).apply(
					text -> {
						if (notice) {
							builder.addEmbeds(new EmbedBuilder()
									.setTitle("Notice")
									.setDescription(text)
									.build());
						} else {
							builder.setContent(text);
						}
					},
					image -> {
						try {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ImageIO.write(image, "png", baos);
							if (notice)
								builder.setContent("**Notice**");
							builder.addFile("formatted-irc-message.png", baos.toByteArray());
						} catch (IOException e) {
							throw new UnexpectedException(e);
						}
					}
			);

			getWebhookClient().send(builder.build());
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
}