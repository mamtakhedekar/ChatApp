/*********************************************************************

    Chat service: accept chat messages from other peers.
    
    Sender name and GPS coordinates are encoded
    in the messages, and stripped off upon receipt.

    Copyright (c) 2012 Stevens Institute of Technology

 **********************************************************************/

package edu.stevens.cs522.chat.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import edu.stevens.cs522.chat.service.ChatContent;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.SQLException;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class ChatService extends Service implements IChatService {

	/*
	 * The chat service uses a background thread to receive messages sent by
	 * other devices, so the main UI thread does not block while waiting for a
	 * message. The content providers for messages and peer info are updated. A
	 * notification is placed in the UI, and may be used to bring the chat app
	 * to the foreground to see the messages that have been received.
	 */

	public static final String NEW_MESSAGE_BROADCAST = "edu.stevens.cs522.chat.NewMessageBroadcast";

	private Notification newMessageNotification;
	public static final int NOTIFICATION_ID = 1;

	/*
	 * Socket for communication with other instances.
	 */
	private DatagramSocket appSocket;

	@Override
	public void onCreate() {
		int icon = R.drawable.ic_launcher;
		String tickerText = "New message received";
		long when = System.currentTimeMillis();

		newMessageNotification = new Notification(icon, tickerText, when);
		
		try {
			appSocket = new DatagramSocket(Integer.parseInt(getString(R.string.app_port)));
		} catch (IOException e) {
			Log.e(ChatApp.TAG, "Cannot create socket."+e);
		}
	}
	
	public void send(DatagramPacket p) {
		try {
			Log.i(ChatApp.TAG, "Sending a message.");
			appSocket.send(p);
		} catch (IOException e) {
			Log.e(ChatApp.TAG, "Cannot send packet."+e);
		}
	}

	@Override
	public void onDestroy() {
		appSocket.close();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i(ChatApp.TAG, "Started Chat service, running task for receiving messages.");
		ReceiveMessageTask recvTask = new ReceiveMessageTask();
		recvTask.execute((Void[]) null);
		return START_STICKY;
	}
	
	/*
	 * Provide a binder, since all socket-related operations are on the service.
	 */
	
	private final IBinder binder = new ChatBinder();
	
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}
	
	public class ChatBinder extends Binder {
		IChatService getService() {
			return ChatService.this;
		}
	}

	/*
	 * The description of the logic that is performed on a background thread.
	 */
	private class ReceiveMessageTask extends AsyncTask<Void, MessageInfo, Void> {
		@Override
		protected Void doInBackground(Void... params) {

			/*
			 * Main background loop: receiving and saving messages.
			 * "publishProgress" calls back to the UI loop to notify the user
			 * when a message is received.
			 */

			try {
				while (true) {
					MessageInfo msg = nextMessage();
					addReceivedMessage(msg);
					addSender(msg);
					publishProgress(msg);
				}
			} catch (IOException e) {
				Log.e(ChatApp.TAG, "Problem receiving a message: " + e);
			}
			return ((Void) null);
		}

		@Override
		protected void onProgressUpdate(MessageInfo... values) {
			/*
			 * Progress update for UI thread: The notification is given a
			 * "pending intent," so that if the user selects the notification,
			 * that pending intent is used to launch the main UI (ChatApp).
			 */
			String svcName = Context.NOTIFICATION_SERVICE;
			NotificationManager notificationManager;
			notificationManager = (NotificationManager) getSystemService(svcName);

			Context context = getApplicationContext();
			String expandedText = values[0].getMessage();
			String expandedTitle = "M:" + values[0].getSender();
			Intent startActivityIntent = new Intent(ChatService.this,
					ChatApp.class);
			PendingIntent launchIntent = PendingIntent.getActivity(context, 0,
					startActivityIntent, 0);

			newMessageNotification.setLatestEventInfo(context, expandedTitle,
					expandedText, launchIntent);
			newMessageNotification.when = java.lang.System.currentTimeMillis();

			notificationManager.notify(NOTIFICATION_ID, newMessageNotification);

			Toast.makeText(context, expandedTitle, Toast.LENGTH_SHORT).show();
		}

		@Override
		protected void onPostExecute(Void result) {
			/*
			 * If an exception is raised during message receipt, then stop the
			 * background thread.
			 */
			stopSelf();
		}
	}

	private MessageInfo nextMessage() throws IOException {
		byte[] receiveData = new byte[1024];
		
		Log.i(ChatApp.TAG, "Waiting for a message.");

		DatagramPacket receivePacket = new DatagramPacket(receiveData,
				receiveData.length);

		appSocket.receive(receivePacket);
		Log.i(ChatApp.TAG, "Received a packet.");

		InetAddress sourceIPAddress = receivePacket.getAddress();
		Log.d(ChatApp.TAG, "Source IP Address: " + sourceIPAddress);

		String msgContents[] = new String(receivePacket.getData(), 0,
				receivePacket.getLength()).split(":");
		String name = msgContents[0];
		InetAddress host = receivePacket.getAddress();
		int port = receivePacket.getPort();
		double latitude = Double.parseDouble(msgContents[1]);
		double longitude = Double.parseDouble(msgContents[2]);
		String message = msgContents[3];

		Log.i(ChatApp.TAG, "Received from " + name + ": " + message);
		return new MessageInfo(name, host, port, latitude, longitude, message);

	}
	
	private Intent msgUpdateBroadcast = new Intent(NEW_MESSAGE_BROADCAST);

	public void addReceivedMessage(MessageInfo msg) {
		/*
		 * Add sender and message to the content provider for received messages.
		 */
		ContentValues values = new ContentValues();
		values.put(ChatContent.Messages.SENDER, msg.getSender());
		values.put(ChatContent.Messages.MESSAGE, msg.getMessage());
		ContentResolver cr = getContentResolver();
		cr.insert(ChatContent.Messages.CONTENT_URI, values);
		
		/*
		 * Logic for updating the Messages cursor is done on the UI thread.
		 */
		sendBroadcast(msgUpdateBroadcast);
	}

	public void addSender(MessageInfo msg) {

		/*
		 * TODO: Add sender information to content provider for peers
		 * information, if we have not already heard from them. If repeat
		 * message, update location information.
		 */
		ContentResolver cr = getContentResolver();
		try
		{
			String tmpQry = ChatContent.Peers.HOST + " = ?" + " AND " + ChatContent.Peers.PORT  + " = ?" ;	//+"\" AND "+ChatContent.Peers.PORT+"=\""+msg.getSrcPort()+"\"";			
			
			String[] projection = 
					new String[] { ChatContent.Peers._ID,
								   ChatContent.Peers.NAME,
								   ChatContent.Peers.HOST,
								   ChatContent.Peers.PORT,
								   ChatContent.Peers.LATITUDE,
								   ChatContent.Peers.LONGITUDE};
			String[] selectArg = { "", "" };
			selectArg[0] = msg.getSrcAddr();
			selectArg[1] = Integer.toString(msg.getSrcPort());
			Cursor msgCursor = cr.query(ChatContent.Peers.CONTENT_URI,
			        projection, tmpQry, selectArg, null);
			
			if (msgCursor.getCount() == 0)
			{
				ContentValues values = new ContentValues();
				values.put(ChatContent.Peers.NAME, msg.getSender());
				values.put(ChatContent.Peers.HOST, msg.getSrcAddr());
				values.put(ChatContent.Peers.PORT, msg.getSrcPort());
				values.put(ChatContent.Peers.LATITUDE, msg.getLatitude());
				values.put(ChatContent.Peers.LONGITUDE, msg.getLongitude());		
				cr.insert(ChatContent.Peers.CONTENT_URI, values);
			}
			else 
			{
				if (msgCursor.moveToFirst())
				{
					int ID = msgCursor.getInt(0);
					ContentValues values1 = new ContentValues();			
					values1.put(ChatContent.Peers.LATITUDE, msg.getLatitude());
					values1.put(ChatContent.Peers.LONGITUDE, msg.getLongitude());		
					values1.put(ChatContent.Peers.HOST, msg.getSrcAddr());
					values1.put(ChatContent.Peers.PORT, msg.getSrcPort());			
					cr.update(ChatContent.Peers.CONTENT_URI, values1, ChatContent.Peers._ID+"="+ID, null);
				}
			}
		}
		catch(SQLException ex)
		{
			throw ex;
		}
		catch(Exception ex)
		{
			Log.e("ChatServer", ex.getMessage());
		}
		/*
		 * End Todo
		 */
	}

}
