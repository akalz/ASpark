package com.lz.aspark.spark;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smack.provider.PrivacyProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.GroupChatInvitation;
import org.jivesoftware.smackx.OfflineMessageManager;
import org.jivesoftware.smackx.PrivateDataManager;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.bytestreams.socks5.provider.BytestreamsProvider;
import org.jivesoftware.smackx.filetransfer.FileTransfer;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferNegotiator;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.packet.ChatStateExtension;
import org.jivesoftware.smackx.packet.LastActivity;
import org.jivesoftware.smackx.packet.OfflineMessageInfo;
import org.jivesoftware.smackx.packet.OfflineMessageRequest;
import org.jivesoftware.smackx.packet.SharedGroupsInfo;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.provider.AdHocCommandDataProvider;
import org.jivesoftware.smackx.provider.DataFormProvider;
import org.jivesoftware.smackx.provider.DelayInformationProvider;
import org.jivesoftware.smackx.provider.DiscoverInfoProvider;
import org.jivesoftware.smackx.provider.DiscoverItemsProvider;
import org.jivesoftware.smackx.provider.MUCAdminProvider;
import org.jivesoftware.smackx.provider.MUCOwnerProvider;
import org.jivesoftware.smackx.provider.MUCUserProvider;
import org.jivesoftware.smackx.provider.MessageEventProvider;
import org.jivesoftware.smackx.provider.MultipleAddressesProvider;
import org.jivesoftware.smackx.provider.RosterExchangeProvider;
import org.jivesoftware.smackx.provider.StreamInitiationProvider;
import org.jivesoftware.smackx.provider.VCardProvider;
import org.jivesoftware.smackx.provider.XHTMLExtensionProvider;
import org.jivesoftware.smackx.search.UserSearch;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

public class SmackHelper {
	private XMPPConnection connection;
	private HandlerThread mSmackThread;
	public Handler mSmackHandler;
	public Handler mMainHandler;
	private FileTransferManager fileTransferManager;
	private MessageListener msgListener;

	private static class SmackHelperHolder {
		private static SmackHelper mSmackHelper = new SmackHelper();
	};

	public static SmackHelper getInstance() {
		return SmackHelperHolder.mSmackHelper;
	}

	private SmackHelper() {
		mSmackThread = new HandlerThread("smack");
		mSmackThread.start();
		mSmackHandler = new Handler(mSmackThread.getLooper());
		mMainHandler = new Handler();
	}

	/**
	 * 连接服务器
	 *
	 * @return 连接结果
	 */
	public void connect(String host, String port,
						final SparkResponseHandler handler) {
		mSmackHandler.post(new Runnable() {
			@Override
			public void run() {

				// configure(ProviderManager.getInstance());
				init();

				ConnectionConfiguration config = new ConnectionConfiguration(
						"192.168.56.1", 5222);
				/** 是否启用安全验证 */
				config.setSASLAuthenticationEnabled(false);
				/** 是否启用调试 */
				config.setDebuggerEnabled(true);

				// 允许自动连接
				// config.setReconnectionAllowed(true);
				// config.setSendPresence(true);
				// config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);

				/** 创建connection链接 */
				try {
					connection = new XMPPConnection(config);
					/** 建立连接 */
					connection.connect();
					mMainHandler.post(new Runnable() {

						@Override
						public void run() {
							handler.onSuccess();
						}
					});
				} catch (XMPPException e) {
					e.printStackTrace();
					mMainHandler.post(new Runnable() {
						@Override
						public void run() {
							handler.onFailure();
						}
					});
				}
			}
		});
	}

	/**
	 * 注册
	 *
	 * @param account
	 *            注册帐号
	 * @param password
	 *            注册密码
	 * @return 1、注册成功 0、服务器没有返回结果2、这个账号已经存在3、注册失败
	 */
	public String regist(String account, String password) {
		if (connection == null)
			return "0";
		Registration reg = new Registration();
		reg.setType(IQ.Type.SET);
		reg.setTo(connection.getServiceName());
		reg.setUsername(account);// 注意这里createAccount注册时，参数是username，不是jid，是“@”前面的部分。
		reg.setPassword(password);
		reg.addAttribute("android", "geolo_createUser_android");// 这边addAttribute不能为空，否则出错。
		PacketFilter filter = new AndFilter(new PacketIDFilter(
				reg.getPacketID()), new PacketTypeFilter(IQ.class));
		PacketCollector collector = connection.createPacketCollector(filter);
		connection.sendPacket(reg);
		IQ result = (IQ) collector.nextResult(SmackConfiguration
				.getPacketReplyTimeout());
		// Stop queuing results
		collector.cancel();// 停止请求results（是否成功的结果）
		if (result == null) {
			Log.e("Regist", "No response from server.");
			return "0";
		} else if (result.getType() == IQ.Type.RESULT) {
			return "1";
		} else { // if (result.getType() == IQ.Type.ERROR)
			if (result.getError().toString().equalsIgnoreCase("conflict(409)")) {
				Log.e("Regist", "IQ.Type.ERROR: "
						+ result.getError().toString());
				return "2";
			} else {
				Log.e("Regist", "IQ.Type.ERROR: "
						+ result.getError().toString());
				return "3";
			}
		}
	}

	/**
	 * 登录
	 *
	 * @param account
	 *            登录帐号
	 * @param password
	 *            登录密码
	 * @return
	 */
	public boolean login(String account, String password) {
		try {
			if (connection == null)
				return false;
			/** 登录 */
			connection.login(account, password, "Spark");
			recvFile();

			ChatManager cm = connection.getChatManager();
			cm.addChatListener(new ChatManagerListener() {

				@Override
				public void chatCreated(Chat chat, boolean createdLocally) {
					chat.addMessageListener(new MessageListener() {

						@Override
						public void processMessage(Chat chat, Message message) {
							if (msgListener != null) {
								msgListener.processMessage(chat, message);
							} else {
								if (message.getBody() != null) {
									Log.d(chat.getParticipant(),
											message.getBody());
								}
							}

						}
					});
				}
			});

			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * @param message
	 *            聊天信息
	 */
	public void chat(String jid, String message) {

		Chat chat = connection.getChatManager().getThreadChat(jid);
		if (chat == null) {
			try {
				connection.getChatManager().createChat(jid, null)
						.sendMessage(message);
			} catch (XMPPException e) {
				e.printStackTrace();
			}
		} else {
			try {
				chat.sendMessage(message);
			} catch (XMPPException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void setMessageListener(MessageListener messageListener) {
		msgListener = messageListener;
	}

	public boolean addUser(String jid, String name) {
		String[] groupsStrings = new String[] { "1", "2", "3" };
		try {
			connection.getRoster().createEntry(jid, name, groupsStrings);
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean deleteUser(String jid) {
		try {
			connection.getRoster().removeEntry(
					connection.getRoster().getEntry(jid));
		} catch (XMPPException e) {
			e.printStackTrace();
		}
		return false;
	}

	public void getRosterInfo() {
		Roster roster = connection.getRoster();
		for (Iterator i = roster.getEntries().iterator(); i.hasNext();) {
			System.out.println(i.next());
		}
	}

	public void updateStateMessage(String status) {
		Presence presence = new Presence(Presence.Type.available);
		presence.setStatus(status);
		connection.sendPacket(presence);
	}

	public void addRosterListener() {
		connection.getRoster().addRosterListener(new RosterListener() {

			@Override
			public void entriesAdded(Collection<String> arg0) {
				// TODO Auto-generated method stub
				System.out.println("--------EE:" + "entriesAdded");
			}

			@Override
			public void entriesDeleted(Collection<String> arg0) {
				// TODO Auto-generated method stub
				System.out.println("--------EE:" + "entriesDeleted");
			}

			@Override
			public void entriesUpdated(Collection<String> arg0) {
				// TODO Auto-generated method stub
				System.out.println("--------EE:" + "entriesUpdated");
			}

			@Override
			public void presenceChanged(Presence arg0) {
				// TODO Auto-generated method stub
				System.out.println("--------EE:" + "presenceChanged");
			}

		});
	}

	/**
	 * 修改密码
	 *
	 * @param connection
	 * @return
	 */
	public boolean changePassword(String pwd) {
		if (connection == null)
			return false;
		try {
			connection.getAccountManager().changePassword(pwd);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 更改用户状态
	 */
	public void setPresence(int code) {
		if (connection == null)
			return;
		Presence presence;
		switch (code) {
			case 0:
				presence = new Presence(Presence.Type.available);
				connection.sendPacket(presence);
				Log.v("state", "设置在线");
				break;
			case 1:
				presence = new Presence(Presence.Type.available);
				presence.setMode(Presence.Mode.chat);
				connection.sendPacket(presence);
				Log.v("state", "设置Q我吧");
				System.out.println(presence.toXML());
				break;
			case 2:
				presence = new Presence(Presence.Type.available);
				presence.setMode(Presence.Mode.dnd);
				connection.sendPacket(presence);
				Log.v("state", "设置忙碌");
				System.out.println(presence.toXML());
				break;
			case 3:
				presence = new Presence(Presence.Type.available);
				presence.setMode(Presence.Mode.away);
				connection.sendPacket(presence);
				Log.v("state", "设置离开");
				System.out.println(presence.toXML());
				break;
			case 4:
				Roster roster = connection.getRoster();
				Collection<RosterEntry> entries = roster.getEntries();
				for (RosterEntry entry : entries) {
					presence = new Presence(Presence.Type.unavailable);
					presence.setPacketID(Packet.ID_NOT_AVAILABLE);
					presence.setFrom(connection.getUser());
					presence.setTo(entry.getUser());
					connection.sendPacket(presence);
					System.out.println(presence.toXML());
				}
				// 向同一用户的其他客户端发送隐身状态
				presence = new Presence(Presence.Type.unavailable);
				presence.setPacketID(Packet.ID_NOT_AVAILABLE);
				presence.setFrom(connection.getUser());
				presence.setTo(StringUtils.parseBareAddress(connection.getUser()));
				connection.sendPacket(presence);
				Log.v("state", "设置隐身");
				break;
			case 5:
				presence = new Presence(Presence.Type.unavailable);
				connection.sendPacket(presence);
				Log.v("state", "设置离线");
				break;
			default:
				break;
		}
	}

	public static VCard getUserVCard(XMPPConnection connection, String user)
			throws XMPPException {
		VCard vcard = new VCard();
		vcard.load(connection, user);

		return vcard;
	}

	/**
	 * 获取用户头像信息
	 */
	public Bitmap getUserImage(String user) {
		Bitmap bitmap = null;
		try {
			VCard vcard = new VCard();
			vcard.load(connection, user);

			if (vcard == null || vcard.getAvatar() == null) {
				return null;
			}
			bitmap = BitmapFactory.decodeByteArray(vcard.getAvatar(), 0,
					vcard.getAvatar().length);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return bitmap;
	}

	/**
	 * 删除当前用户
	 *
	 * @param connection
	 * @return
	 */
	public boolean deleteAccount() {
		if (connection == null)
			return false;
		try {
			connection.getAccountManager().deleteAccount();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public void broadcast() throws XMPPException {

		Message newmsg = new Message();
		newmsg.setTo("shimiso@csdn.shimiso.com");
		newmsg.setSubject("重要通知");
		newmsg.setBody("今天下午2点60分有会！");
		newmsg.setType(Message.Type.headline);// normal支持离线
		connection.sendPacket(newmsg);
		connection.disconnect();
	}

	public void getOfflineBroadcast() throws XMPPException {
		OfflineMessageManager offlineManager = new OfflineMessageManager(
				connection);
		try {
			Iterator<org.jivesoftware.smack.packet.Message> it = offlineManager
					.getMessages();

			System.out.println(offlineManager.supportsFlexibleRetrieval());
			System.out.println("离线消息数量: " + offlineManager.getMessageCount());

			Map<String, ArrayList<Message>> offlineMsgs = new HashMap<String, ArrayList<Message>>();

			while (it.hasNext()) {
				org.jivesoftware.smack.packet.Message message = it.next();
				System.out
						.println("收到离线消息, Received from 【" + message.getFrom()
								+ "】 message: " + message.getBody());
				String fromUser = message.getFrom().split("/")[0];

				if (offlineMsgs.containsKey(fromUser)) {
					offlineMsgs.get(fromUser).add(message);
				} else {
					ArrayList<Message> temp = new ArrayList<Message>();
					temp.add(message);
					offlineMsgs.put(fromUser, temp);
				}
			}

			// 在这里进行处理离线消息集合......
			Set<String> keys = offlineMsgs.keySet();
			Iterator<String> offIt = keys.iterator();
			while (offIt.hasNext()) {
				String key = offIt.next();
				ArrayList<Message> ms = offlineMsgs.get(key);

				for (int i = 0; i < ms.size(); i++) {
					System.out.println("-->" + ms.get(i));
				}
			}

			offlineManager.deleteMessages();
		} catch (Exception e) {
			e.printStackTrace();
		}
		offlineManager.deleteMessages();// 删除所有离线消息
		Presence presence = new Presence(Presence.Type.available);
		connection.sendPacket(presence);// 上线了
		connection.disconnect();// 关闭连接

	}

	public void sendFile(String filename) throws XMPPException {
		Presence pre = connection.getRoster().getPresence(
				"lizhao@ety.akalz.com");
		System.out.println(pre);
		if (pre.getType() != Presence.Type.unavailable) {
			if (fileTransferManager == null) {
				initFileTransport();
			}
			FileTransferManager manager = new FileTransferManager(connection);
			// 创建输出的文件传输
			OutgoingFileTransfer transfer = manager
					.createOutgoingFileTransfer(pre.getFrom());
			// 发送文件
			transfer.sendFile(new File(filename), "图片");
			// while (!transfer.isDone()) {
			// if (transfer.getStatus() == FileTransfer.Status.in_progress) {
			// // 可以调用transfer.getProgress();获得传输的进度　
			// System.out.println(transfer.getStatus());
			// System.out.println(transfer.getProgress());
			// System.out.println(transfer.isDone());
			// }
			// }
		}

	}

	private void recvFile() throws XMPPException {
		if (fileTransferManager == null) {
			initFileTransport();
		}
		fileTransferManager.addFileTransferListener(new FileTransferListener() {
			@Override
			public void fileTransferRequest(
					FileTransferRequest fileTransferRequest) {
				IncomingFileTransfer transfer = fileTransferRequest.accept();
				File sdDir = Environment.getExternalStorageDirectory();
				String filePath = sdDir.toString() + "/"
						+ fileTransferRequest.getFileName();
				try {
					transfer.recieveFile(new File(filePath));
				} catch (XMPPException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public boolean initFileTransport() {
		if (connection == null) {
			return false;
		} else if (fileTransferManager != null) {
			return true;
		} else {
			fileTransferManager = new FileTransferManager(connection);
			ServiceDiscoveryManager sdm = ServiceDiscoveryManager
					.getInstanceFor(connection);
			if (sdm == null)
				sdm = new ServiceDiscoveryManager(connection);
			sdm.addFeature("http://jabber.org/protocol/disco#info");
			sdm.addFeature("jabber:iq:privacy");
			FileTransferNegotiator.setServiceEnabled(connection, true);
			return true;
		}
	}

	private void configure(ProviderManager pm) {

		pm.addIQProvider("query", "jabber:iq:private",

				new PrivateDataManager.PrivateDataIQProvider());

		try {

			pm.addIQProvider("query", "jabber:iq:time",

					Class.forName("org.jivesoftware.smackx.packet.Time"));

		} catch (ClassNotFoundException e) {

			Log.w("TestClient",

					"Can't load class for org.jivesoftware.smackx.packet.Time");

		}
		pm.addExtensionProvider("x", "jabber:x:roster",

				new RosterExchangeProvider());
		pm.addExtensionProvider("x", "jabber:x:event",

				new MessageEventProvider());
		pm.addExtensionProvider("active",

				"http://jabber.org/protocol/chatstates",

				new ChatStateExtension.Provider());
		pm.addExtensionProvider("composing",

				"http://jabber.org/protocol/chatstates",

				new ChatStateExtension.Provider());
		pm.addExtensionProvider("paused",

				"http://jabber.org/protocol/chatstates",

				new ChatStateExtension.Provider());

		pm.addExtensionProvider("inactive",

				"http://jabber.org/protocol/chatstates",

				new ChatStateExtension.Provider());
		pm.addExtensionProvider("gone",

				"http://jabber.org/protocol/chatstates",

				new ChatStateExtension.Provider());
		pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im",

				new XHTMLExtensionProvider());

		pm.addExtensionProvider("x", "jabber:x:conference",

				new GroupChatInvitation.Provider());
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#items",
				new DiscoverItemsProvider());

		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",

				new DiscoverInfoProvider());
		pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());
		pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user",
				new MUCUserProvider());
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin",
				new MUCAdminProvider());

		pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner",
				new MUCOwnerProvider());
		pm.addExtensionProvider("x", "jabber:x:delay",
				new DelayInformationProvider());
		try {

			pm.addIQProvider("query", "jabber:iq:version",

					Class.forName("org.jivesoftware.smackx.packet.Version"));

		} catch (ClassNotFoundException e) {

			// Not sure what's happening here.

		}
		pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());
		pm.addIQProvider("offline", "http://jabber.org/protocol/offline",

				new OfflineMessageRequest.Provider());
		pm.addExtensionProvider("offline",
				"http://jabber.org/protocol/offline",
				new OfflineMessageInfo.Provider());
		pm.addIQProvider("query", "jabber:iq:last", new LastActivity.Provider());
		pm.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());
		pm.addIQProvider("sharedgroup",
				"http://www.jivesoftware.org/protocol/sharedgroup",
				new SharedGroupsInfo.Provider());
		pm.addExtensionProvider("addresses",
				"http://jabber.org/protocol/address",
				new MultipleAddressesProvider());
		pm.addIQProvider("si", "http://jabber.org/protocol/si",
				new StreamInitiationProvider());
		pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams",
				new BytestreamsProvider());
		pm.addIQProvider("query", "jabber:iq:privacy", new PrivacyProvider());

	}

	private void init() {
		ProviderManager pm = ProviderManager.getInstance();
		// Private Data Storage
		pm.addIQProvider("query", "jabber:iq:private",
				new PrivateDataManager.PrivateDataIQProvider());

		// Roster Exchange
		pm.addExtensionProvider("x", "jabber:x:roster",
				new RosterExchangeProvider());

		// Message Events
		pm.addExtensionProvider("x", "jabber:x:event",
				new MessageEventProvider());

		// Chat State
		pm.addExtensionProvider("active",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());
		pm.addExtensionProvider("composing",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());
		pm.addExtensionProvider("paused",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());
		pm.addExtensionProvider("inactive",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());
		pm.addExtensionProvider("gone",
				"http://jabber.org/protocol/chatstates",
				new ChatStateExtension.Provider());

		// XHTML
		pm.addExtensionProvider("html", "http://jabber.org/protocol/xhtml-im",
				new XHTMLExtensionProvider());

		// Group Chat Invitations
		pm.addExtensionProvider("x", "jabber:x:conference",
				new GroupChatInvitation.Provider());

		// Service Discovery # Items
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#items",
				new DiscoverItemsProvider());

		// Service Discovery # Info
		pm.addIQProvider("query", "http://jabber.org/protocol/disco#info",
				new DiscoverInfoProvider());

		// Data Forms
		pm.addExtensionProvider("x", "jabber:x:data", new DataFormProvider());

		// MUC User
		pm.addExtensionProvider("x", "http://jabber.org/protocol/muc#user",
				new MUCUserProvider());

		// MUC Admin
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#admin",
				new MUCAdminProvider());

		// MUC Owner
		pm.addIQProvider("query", "http://jabber.org/protocol/muc#owner",
				new MUCOwnerProvider());

		// Delayed Delivery
		pm.addExtensionProvider("x", "jabber:x:delay",
				new DelayInformationProvider());

		// Version
		try {
			pm.addIQProvider("query", "jabber:iq:version",
					Class.forName("org.jivesoftware.smackx.packet.Version"));
		} catch (ClassNotFoundException e) {
			// Not sure what's happening here.
		}

		// VCard
		pm.addIQProvider("vCard", "vcard-temp", new VCardProvider());

		// Offline Message Requests
		pm.addIQProvider("offline", "http://jabber.org/protocol/offline",
				new OfflineMessageRequest.Provider());

		// Offline Message Indicator
		pm.addExtensionProvider("offline",
				"http://jabber.org/protocol/offline",
				new OfflineMessageInfo.Provider());

		// Last Activity
		pm.addIQProvider("query", "jabber:iq:last", new LastActivity.Provider());

		// User Search
		pm.addIQProvider("query", "jabber:iq:search", new UserSearch.Provider());

		// SharedGroupsInfo
		pm.addIQProvider("sharedgroup",
				"http://www.jivesoftware.org/protocol/sharedgroup",
				new SharedGroupsInfo.Provider());

		// JEP-33: Extended Stanza Addressing
		pm.addExtensionProvider("addresses",
				"http://jabber.org/protocol/address",
				new MultipleAddressesProvider());

		// FileTransfer
		pm.addIQProvider("si", "http://jabber.org/protocol/si",
				new StreamInitiationProvider());
		pm.addIQProvider("query", "http://jabber.org/protocol/bytestreams",
				new BytestreamsProvider());

		// Privacy
		pm.addIQProvider("query", "jabber:iq:privacy", new PrivacyProvider());

		pm.addIQProvider("command", "http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider());

		pm.addExtensionProvider("malformed-action",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.MalformedActionError());

		pm.addExtensionProvider("bad-locale",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.BadLocaleError());

		pm.addExtensionProvider("bad-payload",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.BadPayloadError());

		pm.addExtensionProvider("bad-sessionid",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.BadSessionIDError());

		pm.addExtensionProvider("session-expired",
				"http://jabber.org/protocol/commands",
				new AdHocCommandDataProvider.SessionExpiredError());
	}

}
