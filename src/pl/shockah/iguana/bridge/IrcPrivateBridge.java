package pl.shockah.iguana.bridge;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.exception.DaoException;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import lombok.Getter;
import pl.shockah.iguana.Configuration;
import pl.shockah.iguana.IguanaSession;
import pl.shockah.iguana.WebhookClientWrapper;
import pl.shockah.iguana.command.CommandCall;
import pl.shockah.iguana.command.UserCommand;
import pl.shockah.iguana.format.irc.IrcFormattingConstants;
import pl.shockah.unicorn.UnexpectedException;
import pl.shockah.unicorn.collection.Either2;

public class IrcPrivateBridge {
	@Nonnull
	private static final float[] NICKNAME_LENGTH_LIGHTNESS = new float[] { 0.55f, 0.65f, 0.75f, 0.85f };

	@Nonnull
	private static final float[] NICKNAME_LENGTH_CHROMA = new float[] { 0.4f, 0.5f, 0.6f, 0.7f, 0.8f };

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
	private final Configuration.IRC.Server.Private ircPrivateConfig;

	@Nonnull
	@Getter(lazy = true)
	private final TextChannel discordChannel = ircPrivateConfig.getDiscordChannel(session.getDiscord());

//	@Nonnull
//	@Getter(lazy = true)
//	private final Channel ircChannel = ircBot.getUserChannelDao().getChannel(ircChannelConfig.getName());

	@Nonnull
	@Getter(lazy = true)
	private final WebhookClientWrapper webhookClient = new WebhookClientWrapper(ircPrivateConfig.getWebhook(session.getDiscord()).newClient().build());

	public IrcPrivateBridge(@Nonnull IrcServerBridge serverBridge, @Nonnull Configuration.IRC.Server.Private ircPrivateConfig) {
		session = serverBridge.getSession();
		ircBot = serverBridge.getIrcBot();
		this.serverBridge = serverBridge;
		this.ircPrivateConfig = ircPrivateConfig;
	}

	@Nonnull
	private Either2<String, BufferedImage> getFormattedIrcToDiscordMessage(@Nonnull String ircMessage) {
		ircMessage = ircMessage.replace(ircBot.getUserBot().getNick(), session.getConfiguration().discord.getOwnerUser(session.getDiscord()).getAsMention());
		return session.getDiscordFormatter().output(session.getIrcFormatter().parse(ircMessage, null), null);
	}

	@Nullable
	public User getCurrentUser() {
		if (ircPrivateConfig.isNickServAccount()) {
			return getServerBridge().nickServIdentityManager.getUserForAccount(ircPrivateConfig.getName());
		} else {
			try {
				return ircBot.getUserChannelDao().getUser(ircPrivateConfig.getName());
			} catch (DaoException e) {
				return null;
			}
		}
	}

	private void onPrivateMessage(@Nonnull User user, @Nonnull String message) {
		try {
			WebhookMessageBuilder builder = new WebhookMessageBuilder()
					.setUsername(user.getNick())
					.setAvatarUrl(session.getBridge().getAvatarUrl(user.getNick()));

			getFormattedIrcToDiscordMessage(message).apply(
					builder::setContent,
					image -> {
						try {
							ByteArrayOutputStream baos = new ByteArrayOutputStream();
							ImageIO.write(image, "png", baos);
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

	public void onPrivateMessage(@Nonnull PrivateMessageEvent event) {
		User user = event.getUser();
		if (user == null)
			return;

		onPrivateMessage(user, event.getMessage());
	}

	public void onPrivateAction(@Nonnull ActionEvent event) {
		Channel channel = event.getChannel();
		if (channel == null)
			return;

		User user = event.getUser();
		if (user == null)
			return;

		String italicizedMessage = event.getMessage();
		italicizedMessage = italicizedMessage.replace(IrcFormattingConstants.RESET, IrcFormattingConstants.RESET + IrcFormattingConstants.ITALIC);
		italicizedMessage = IrcFormattingConstants.ITALIC + italicizedMessage;
		onPrivateMessage(user, italicizedMessage);
	}

	public void onPrivateNotice(@Nonnull NoticeEvent event) {
		Channel channel = event.getChannel();
		if (channel == null)
			return;

		User user = event.getUser();
		if (user == null)
			return;

		WebhookMessageBuilder builder = new WebhookMessageBuilder()
				.setUsername(user.getNick())
				.setAvatarUrl(session.getBridge().getAvatarUrl(user.getNick()));

		getFormattedIrcToDiscordMessage(event.getMessage()).apply(
				text -> builder.addEmbeds(new EmbedBuilder()
						.setTitle("Notice")
						.setDescription(text)
						.build()),
				image -> {
					try {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(image, "png", baos);
						builder.setContent("**Notice**");
						builder.addFile("formatted-irc-message.png", baos.toByteArray());
					} catch (IOException e) {
						throw new UnexpectedException(e);
					}
				}
		);

		getWebhookClient().send(builder.build());
	}

	public void onDiscordMessage(@Nonnull GuildMessageReceivedEvent event) {
		User user = getCurrentUser();
		if (user == null) {
			getDiscordChannel().sendMessage(new EmbedBuilder()
					.setDescription("User is currently offline.")
					.build()).queue();
			return;
		}

		String discordMessage = event.getMessage().getContentDisplay().trim();
		if (!discordMessage.equals("")) {
			CommandCall call = getSession().getBridge().getCommandManager().parseCommandCall(discordMessage);
			if (call != null) {
				UserCommand command = getSession().getBridge().getCommandManager().getCommandForUserContext(call.commandName);
				if (command != null) {
					command.execute(this, event.getMessage(), call.input);
					return;
				}
			}

			String ircMessage = session.getIrcFormatter().output(session.getDiscordFormatter().parse(discordMessage, null), null);
			user.send().message(ircMessage);
		}

		for (Message.Attachment attachment : event.getMessage().getAttachments()) {
			String url = attachment.getUrl();
			if (url != null && !url.equals(""))
				user.send().message(String.format("[ Attachment: %s ]", url));
		}
	}
}