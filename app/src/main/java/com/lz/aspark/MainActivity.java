package com.lz.aspark;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.lz.aspark.spark.SmackHelper;
import com.lz.aspark.spark.SparkResponseHandler;

@SuppressLint("NewApi")
public class MainActivity extends Activity implements OnClickListener {

	private Bitmap mIcon;
	private Button userChatTitleBtn;
	private EditText chatContentEdt;
	private Button chatContentBtn;
	private Button chatSendFileBtn;
	private ListView chatContentLv;
	private List<Message> mMessages;
	private ChatAdapter mChatAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		initView();
		Login();
		Semaphore semaphore;
		Condition condition;
	}

	private void Login() {
		SmackHelper.getInstance().connect("", "", new SparkResponseHandler() {

			@Override
			public void onSuccess() {

				SmackHelper.getInstance().mSmackHandler.post(new Runnable() {
					@Override
					public void run() {
						SmackHelper.getInstance().login("akalz", "akalz");
						SmackHelper.getInstance().addUser(
								"lizhao@ety.akalz.com", "lizhao");

						SmackHelper.getInstance().updateStateMessage(
								"new state");
						SmackHelper.getInstance().getRosterInfo();

						SmackHelper.getInstance().mMainHandler
								.post(new Runnable() {
									@Override
									public void run() {
										mIcon = SmackHelper.getInstance()
												.getUserImage(
														"lizhao@ety.akalz.com");
										userChatTitleBtn
												.setBackground(new BitmapDrawable(
														mIcon));
									}
								});

					}
				});

			}

			@Override
			public void onFailure() {
				// ((TextView) findViewById(R.id.state)).setText("fail");
			}
		});
		SmackHelper.getInstance().setMessageListener(new MessageListener() {

			@Override
			public void processMessage(Chat chat, final Message message) {
				if (message.getBody() != null) {
					Log.d(chat.getParticipant(), message.getBody());
					SmackHelper.getInstance().mMainHandler.post(new Runnable() {

						@Override
						public void run() {
							mMessages.add(message);
							mChatAdapter.notifyDataSetChanged();
						}
					});
				}
			}
		});
	}

	private void initView() {
		userChatTitleBtn = $(R.id.user_chat_title_btn);
		chatContentEdt = $(R.id.user_chat_content_edt);
		chatContentBtn = $(R.id.user_chat_send_btn);
		chatSendFileBtn = $(R.id.user_char_sendfile_btn);
		chatSendFileBtn.setOnClickListener(this);
		chatContentBtn.setOnClickListener(this);
		mMessages = new ArrayList<>();
		chatContentLv = $(R.id.user_chat_content_lv);
		mChatAdapter = new ChatAdapter();
		chatContentLv.setAdapter(mChatAdapter);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
			case R.id.user_chat_send_btn:
				String chatTmp = chatContentEdt.getText().toString();
				Message msg = new Message();
				msg.setBody(chatTmp);
				msg.setTo("lizhao@ety.akalz.com");
				mMessages.add(msg);
				mChatAdapter.notifyDataSetChanged();
				SmackHelper.getInstance().chat("lizhao@ety.akalz.com", chatTmp);
				break;

			case R.id.user_char_sendfile_btn:

				try {
					SmackHelper.getInstance().sendFile(getSDPath() + "/test.txt");
				} catch (XMPPException e) {
					e.printStackTrace();
				}
				break;

			default:
				break;
		}

	}

	private <T extends View> T $(int Rid) {
		return (T) findViewById(Rid);
	}

	class ChatAdapter extends BaseAdapter {

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(MainActivity.this).inflate(
						android.R.layout.simple_list_item_1, null);
			}
			((TextView) convertView.findViewById(android.R.id.text1))
					.setText(mMessages.get(position).getBody() + "\nto "
							+ mMessages.get(position).getTo());
			return convertView;
		}

		@Override
		public long getItemId(int position) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public Object getItem(int position) {
			return mMessages.get(position);
		}

		@Override
		public int getCount() {
			return mMessages.size();
		}
	}

	public String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED); // 判断sd卡是否存在
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();// 获取跟目录
		}
		return sdDir.getPath().toString();
	}
}
