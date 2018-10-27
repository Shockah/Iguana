package pl.shockah.iguana.irc;

import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.NoticeEvent;

public class IrcListenerAdapter extends ListenerAdapter {
	@Override
	public void onEvent(Event event) throws Exception {
		if (event instanceof ExtendedJoinEvent)
			onExtendedJoin((ExtendedJoinEvent)event);
		else if (event instanceof AccountNotifyEvent)
			onAccountNotify((AccountNotifyEvent)event);

		if (event instanceof NoticeEvent) {
			NoticeEvent e = (NoticeEvent)event;
			if (e.getUser() == null || e.getUser().getServer() == null || e.getUser().getServer().equals("")) {
				onServerNotice(new ServerNoticeEvent(e.getBot(), e.getUserHostmask(), e.getUser(), e.getMessage()));
				return;
			}
		}

		super.onEvent(event);
	}

	public void onExtendedJoin(ExtendedJoinEvent event) { }

	public void onAccountNotify(AccountNotifyEvent event) { }

	public void onServerNotice(ServerNoticeEvent event) { }
}