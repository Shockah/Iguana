package pl.shockah.iguana.irc;

import org.pircbotx.PircBotX;
import org.pircbotx.User;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import pl.shockah.unicorn.collection.Box;
import pl.shockah.unicorn.collection.ReadWriteList;

public class WhoisManager extends IrcListenerAdapter {
	public static final long DEFAULT_SYNC_REQUEST_TIMEOUT = 5000L;

	@Nonnull
	public final PircBotX bot;

	@Nonnull
	private final ReadWriteList<Request> userRequests = new ReadWriteList<>(new ArrayList<>());

	public WhoisManager(@Nonnull PircBotX bot) {
		this.bot = bot;
	}

	public void asyncRequest(@Nonnull User user, @Nonnull AsyncHandler handler) {
		asyncRequest(user.getNick(), handler);
	}

	public void asyncRequest(@Nonnull String nick, @Nonnull AsyncHandler handler) {
		userRequests.add(new Request(nick, handler));
		bot.sendRaw().rawLine(String.format("WHOIS %s", nick));
	}

	@Nullable
	public Whois2Event syncRequest(@Nonnull User user) {
		return syncRequest(user.getNick(), DEFAULT_SYNC_REQUEST_TIMEOUT);
	}

	@Nullable
	public Whois2Event syncRequest(@Nonnull User user, long timeout) {
		return syncRequest(user.getNick(), timeout);
	}

	@Nullable
	public Whois2Event syncRequest(@Nonnull String nick) {
		return syncRequest(nick, DEFAULT_SYNC_REQUEST_TIMEOUT);
	}

	@Nullable
	public Whois2Event syncRequest(String nick, long timeout) {
		CountDownLatch latch = new CountDownLatch(1);
		Box<Whois2Event> box = new Box<>();
		asyncRequest(nick, e -> {
			box.value = e;
			latch.countDown();
		});
		try {
			latch.await(timeout, TimeUnit.MILLISECONDS);
		} catch (Exception ignored) {
		}
		return box.value;
	}

	@Override
	public void onWhois2(Whois2Event e) {
		userRequests.iterateAndWrite((request, it) -> {
			if (e.getNick().equalsIgnoreCase(request.nick)) {
				request.handler.handle(e);
				it.remove();
				it.stop();
			}
		});
	}

	public interface AsyncHandler {
		void handle(@Nonnull Whois2Event event);
	}

	private static class Request {
		@Nonnull
		public final String nick;

		@Nonnull
		public final AsyncHandler handler;

		public Request(@Nonnull String nick, @Nonnull AsyncHandler handler) {
			this.nick = nick;
			this.handler = handler;
		}
	}
}