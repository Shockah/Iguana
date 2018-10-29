package pl.shockah.iguana.bridge;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.UserHostmask;
import org.pircbotx.hooks.events.ActionEvent;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.NoticeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;
import org.pircbotx.hooks.events.TopicEvent;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

import lombok.Getter;
import pl.shockah.iguana.Configuration;
import pl.shockah.iguana.IguanaSession;
import pl.shockah.iguana.WebhookClientWrapper;
import pl.shockah.iguana.command.ChannelCommand;
import pl.shockah.iguana.command.CommandCall;
import pl.shockah.iguana.format.irc.IrcFormattingConstants;
import pl.shockah.unicorn.UnexpectedException;
import pl.shockah.unicorn.collection.Either2;

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
	private final TextChannel discordEventChannel = ircChannelConfig.getDiscordEventChannel(session.getDiscord());

	@Nonnull
	@Getter(lazy = true)
	private final Channel ircChannel = ircBot.getUserChannelDao().getChannel(ircChannelConfig.getName());

	@Nonnull
	@Getter(lazy = true)
	private final WebhookClientWrapper webhookClient = new WebhookClientWrapper(ircChannelConfig.getWebhook(session.getDiscord()).newClient().build());

	@Nonnull
	public static final Color defaultOtherActionColor = new Color(127, 127, 127);

	public IrcChannelBridge(@Nonnull IrcServerBridge serverBridge, @Nonnull Configuration.IRC.Server.Channel ircChannelConfig) {
		session = serverBridge.getSession();
		ircBot = serverBridge.getIrcBot();
		this.serverBridge = serverBridge;
		this.ircChannelConfig = ircChannelConfig;
	}

	@Nonnull
	public String getFullIrcNickname(@Nonnull User user) {
		String nickname = user.getNick();
		if (getIrcChannel().hasVoice(user))
			nickname = String.format("+%s", nickname);
		else if (getIrcChannel().isHalfOp(user))
			nickname = String.format("%%%s", nickname);
		else if (getIrcChannel().isOp(user))
			nickname = String.format("@%s", nickname);
		return nickname;
	}

	@Nonnull
	private Either2<String, BufferedImage> getFormattedIrcToDiscordMessage(@Nonnull String ircMessage) {
		ircMessage = ircMessage.replace(ircBot.getUserBot().getNick(), session.getConfiguration().discord.getOwnerUser(session.getDiscord()).getAsMention());
		return session.getDiscordFormatter().output(session.getIrcFormatter().parse(ircMessage, null), null);
	}

	private void onChannelMessage(@Nonnull User user, @Nonnull String message) {
		try {
			WebhookMessageBuilder builder = new WebhookMessageBuilder()
					.setUsername(getFullIrcNickname(user))
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

	public void onChannelMessage(@Nonnull MessageEvent event) {
		Channel channel = event.getChannel();
		if (channel == null)
			return;

		User user = event.getUser();
		if (user == null)
			return;

		onChannelMessage(user, event.getMessage());
	}

	public void onChannelAction(@Nonnull ActionEvent event) {
		Channel channel = event.getChannel();
		if (channel == null)
			return;

		User user = event.getUser();
		if (user == null)
			return;

		String italicizedMessage = event.getMessage();
		italicizedMessage = italicizedMessage.replace(IrcFormattingConstants.RESET, IrcFormattingConstants.RESET + IrcFormattingConstants.ITALIC);
		italicizedMessage = IrcFormattingConstants.ITALIC + italicizedMessage;
		onChannelMessage(user, italicizedMessage);
	}

	public void onChannelNotice(@Nonnull NoticeEvent event) {
		Channel channel = event.getChannel();
		if (channel == null)
			return;

		User user = event.getUser();
		if (user == null)
			return;

		WebhookMessageBuilder builder = new WebhookMessageBuilder()
				.setUsername(getFullIrcNickname(user))
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

	public void onSelfJoin(@Nonnull JoinEvent event) {
		Channel channel = event.getChannel();
		if (channel == null)
			return;

		if (!channel.getTopic().equals(getDiscordChannel().getTopic()))
			getDiscordChannel().getManager().setTopic(channel.getTopic()).queue();
	}

	public void onJoin(@Nonnull JoinEvent event) {
		User user = event.getUser();
		if (user == null)
			return;

		getDiscordEventChannel().sendMessage(
				new EmbedBuilder()
						.setColor(session.getConfiguration().appearance.userInfo.getJoinColor())
						.setDescription(String.format("**%s** (`%s!%s@%s`) joined.", user.getNick(), user.getNick(), user.getLogin(), user.getHostname()))
						.setTimestamp(Instant.now())
						.build()
		).queue();
	}

	public void onPart(@Nonnull PartEvent event) {
		User user = event.getUser();
		if (user == null)
			return;

		getDiscordEventChannel().sendMessage(
				new EmbedBuilder()
						.setColor(session.getConfiguration().appearance.userInfo.getLeaveColor())
						.setDescription(String.format("**%s** (`%s!%s@%s`) left.", user.getNick(), user.getNick(), user.getLogin(), user.getHostname()))
						.setTimestamp(Instant.now())
						.build()
		).queue();
	}

	public void onQuit(@Nonnull QuitEvent event) {
		User user = event.getUser();
		if (user == null)
			return;

		getDiscordEventChannel().sendMessage(
				new EmbedBuilder()
						.setColor(session.getConfiguration().appearance.userInfo.getLeaveColor())
						.setDescription(String.format("**%s** (`%s!%s@%s`) has quit.", user.getNick(), user.getNick(), user.getLogin(), user.getHostname()))
						.setTimestamp(Instant.now())
						.build()
		).queue();
	}

	public void onNickChange(@Nonnull NickChangeEvent event) {
		User user = event.getUser();
		if (user == null)
			return;

		getDiscordEventChannel().sendMessage(
				new EmbedBuilder()
						.setColor(session.getConfiguration().appearance.userInfo.getNickChangeColor())
						.setDescription(String.format("**%s** (`%s!%s@%s`) is now known as **%s**.", event.getOldNick(), user.getNick(), user.getLogin(), user.getHostname(), event.getNewNick()))
						.setTimestamp(Instant.now())
						.build()
		).queue();
	}

	public void onTopic(@Nonnull TopicEvent event) {
		Channel channel = event.getChannel();
		if (channel == null)
			return;

		UserHostmask user = event.getUser();
		if (user == null)
			return;

		getDiscordChannel().getManager().setTopic(event.getTopic()).queue();

		getDiscordChannel().sendMessage(
				new EmbedBuilder()
						.setColor(defaultOtherActionColor)
						.setDescription(String.format("**%s** (`%s!%s@%s`) changed the topic.", user.getNick(), user.getNick(), user.getLogin(), user.getHostname()))
						.addField("Old", event.getOldTopic().equals("") ? "<empty>" : event.getOldTopic(), false)
						.addField("New", event.getTopic().equals("") ? "<empty>" : event.getTopic(), false)
						.build()
		).queue();
	}

	public void onDiscordMessage(@Nonnull GuildMessageReceivedEvent event) {
		String discordMessage = event.getMessage().getContentDisplay().trim();
		if (!discordMessage.equals("")) {
			CommandCall call = getSession().getBridge().getCommandManager().parseCommandCall(discordMessage);
			if (call != null) {
				ChannelCommand command = getSession().getBridge().getCommandManager().getCommandForChannelContext(call.commandName);
				if (command != null) {
					command.execute(this, event.getMessage(), call.input);
					return;
				}
			}

			String ircMessage = session.getIrcFormatter().output(session.getDiscordFormatter().parse(discordMessage, null), null);
			getIrcChannel().send().message(ircMessage);
		}

		for (Message.Attachment attachment : event.getMessage().getAttachments()) {
			String url = attachment.getUrl();
			if (url != null && !url.equals(""))
				getIrcChannel().send().message(String.format("[ Attachment: %s ]", url));
		}
	}
}