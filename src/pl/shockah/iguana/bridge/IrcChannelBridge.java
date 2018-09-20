package pl.shockah.iguana.bridge;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.hooks.events.JoinEvent;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.NickChangeEvent;
import org.pircbotx.hooks.events.PartEvent;
import org.pircbotx.hooks.events.QuitEvent;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import lombok.Getter;
import pl.shockah.iguana.Configuration;
import pl.shockah.iguana.IguanaSession;
import pl.shockah.iguana.WebhookClientWrapper;
import pl.shockah.unicorn.UnexpectedException;
import pl.shockah.unicorn.collection.Either2;
import pl.shockah.unicorn.color.LCHColorSpace;

public class IrcChannelBridge {
	@Nonnull
	private static final String ALLOWED_NONALPHANUMERIC_NICKNAME_CHARACTERS = "[\\]^_-{|}";

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

	public IrcChannelBridge(@Nonnull IrcServerBridge serverBridge, @Nonnull Configuration.IRC.Server.Channel ircChannelConfig) {
		session = serverBridge.getSession();
		ircBot = serverBridge.getIrcBot();
		this.serverBridge = serverBridge;
		this.ircChannelConfig = ircChannelConfig;
	}

	private float getAtReversingRepeatingIndex(@Nonnull float[] array, int index) {
		int maxIndex = array.length * 2 - 2;
		int semiIndex = index % maxIndex;
		if (semiIndex < array.length)
			return array[semiIndex];
		else
			return array[maxIndex - semiIndex];
	}

	@Nonnull
	private LCHColorSpace getLchBackgroundColorForNickname(@Nonnull String nickname) {
		StringBuilder sb = new StringBuilder(nickname.toLowerCase());
		for (int i = 0; i < sb.length(); i++) {
			if (ALLOWED_NONALPHANUMERIC_NICKNAME_CHARACTERS.indexOf(sb.charAt(i)) != -1)
				sb.deleteCharAt(i--);
		}
		for (int i = sb.length() - 1; i >= 0; i++) {
			char c = sb.charAt(i);
			if (c >= '0' && c <= '9')
				sb.deleteCharAt(i);
			else
				break;
		}

		return new LCHColorSpace(
				getAtReversingRepeatingIndex(NICKNAME_LENGTH_LIGHTNESS, nickname.length() - 1) * 100f,
				getAtReversingRepeatingIndex(NICKNAME_LENGTH_CHROMA, nickname.length() - 1) * 133f,
				(sb.toString().hashCode() % 100) / 100f
		);
	}

	@Nonnull
	private LCHColorSpace getLchTextColorForBackgroundColor(@Nonnull LCHColorSpace backgroundColor) {
		LCHColorSpace result = new LCHColorSpace(backgroundColor.l, backgroundColor.c, backgroundColor.h);
		if (backgroundColor.l < 50f)
			result.l += 40f;
		else
			result.l -= 40f;
		return result;
	}

	@Nullable
	public String getAvatarUrl(@Nonnull String nickname) {
		LCHColorSpace lchBackgroundColor = getLchBackgroundColorForNickname(nickname);
		LCHColorSpace lchTextColor = getLchTextColorForBackgroundColor(lchBackgroundColor);
		return session.getConfiguration().discord.getAvatarUrl(nickname, lchBackgroundColor.toRGB(), lchTextColor.toRGB());
	}

	@Nonnull
	private String getFullIrcNickname(@Nonnull User user) {
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
	private Either2<String, Image> getFormattedIrcToDiscordMessage(@Nonnull String ircMessage) {
		ircMessage = ircMessage.replace(ircBot.getUserBot().getNick(), session.getConfiguration().discord.getOwnerUser(session.getDiscord()).getAsMention());
		return Either2.first(ircMessage);
	}

	public void onMessage(@Nonnull MessageEvent event) {
		Channel channel = event.getChannel();
		if (channel == null)
			return;

		User user = event.getUser();
		if (user == null)
			return;

		WebhookMessageBuilder builder = new WebhookMessageBuilder()
				.setUsername(getFullIrcNickname(user))
				.setAvatarUrl(getAvatarUrl(user.getNick()));

		getFormattedIrcToDiscordMessage(event.getMessage()).apply(
				builder::setContent,
				image -> {
					try {
						BufferedImage swingImage = SwingFXUtils.fromFXImage(image, null);
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						ImageIO.write(swingImage, "png", baos);
						builder.addFile("formatted-irc-message.png", baos.toByteArray());
					} catch (IOException e) {
						throw new UnexpectedException(e);
					}
				}
		);

		getWebhookClient().send(builder.build());
	}

	public void onJoin(@Nonnull JoinEvent event) {
		User user = event.getUser();
		if (user == null)
			return;

		getDiscordEventChannel().sendMessage(
				new EmbedBuilder()
						.setColor(new Color(30, 200, 30))
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
						.setColor(new Color(200, 30, 30))
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
						.setColor(new Color(200, 30, 30))
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
						.setColor(new Color(127, 127, 127))
						.setDescription(String.format("**%s** (`%s!%s@%s`) is now known as **%s**.", event.getOldNick(), user.getNick(), user.getLogin(), user.getHostname(), event.getNewNick()))
						.setTimestamp(Instant.now())
						.build()
		).queue();
	}
}