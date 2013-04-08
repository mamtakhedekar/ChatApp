/*********************************************************************

    Chat app: exchange messages with other instances of the app.
    
    Copyright (c) 2012 Stevens Institute of Technology

 **********************************************************************/

package edu.stevens.cs522.chat.service;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import edu.stevens.cs522.chat.service.ChatContent;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class ChatApp extends FragmentActivity 
implements LoaderManager.LoaderCallbacks<Cursor> {
	
	final static public String TAG = ChatApp.class.getCanonicalName();
	Receiver updater;	
	/*
	 * Adapter for displaying received messages.
	 */
	//CursorAdapter messageAdapter;
	SimpleCursorAdapter messageAdapter = null;
	private DatagramPacket sendPacket = null;
	
	private static final int URL_LOADER = 0;
	private static final int LOADER_ID = 0;	
	private ListView msgList;
	
	// The callbacks through which we will interact with the LoaderManager.
	private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;	

	/*
	 * Widgets for dest address, message text, send button.
	 */
	EditText destHost;
	EditText destPort;
	EditText msg;
	Button send;
	
	/*
	 * Service binder.
	 */
	private IChatService serviceBinder;
	
	/*
	 * TODO: Handle the connection with the service.
	 */
	
	private ServiceConnection mConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className,
                IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
        	serviceBinder = ((ChatService.ChatBinder) service).getService();
        }

        public void onServiceDisconnected(ComponentName arg0) {
        	serviceBinder = null;
        }
    };	

	/*
	 * End Todo
	 */

	/*
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		destHost = (EditText) findViewById(R.id.dest_text);

		destPort = (EditText) findViewById(R.id.port_text);

		msg = (EditText) findViewById(R.id.message_text);

		/*
		 * TODO: Messages content provider should be linked to the listview
		 * named "msgList" in the UI:
		 * 1. Build a cursor that projects Messages content.  See makeMessageCursor().
		 * 2. Use a SimpleCursorAdapter to adapt this cursor for msgList listview.
		 * 3. Use messages_row layout for the list of messages
		 */
		
		String[] to = new String[] { ChatContent.Messages.MESSAGE };
        int[] from = new int[] { R.id.messages_message };		
				
        mCallbacks = this;     
        LoaderManager lm = getSupportLoaderManager();
        lm.initLoader(LOADER_ID, null, mCallbacks);
        
        // Now create a list adaptor that encapsulates the result of a DB query
        this.messageAdapter = new SimpleCursorAdapter(
        		this,       // Context.
                R.layout.messages_row,  // Specify the row template to use 
                null,          // Cursor encapsulates the DB query result.
                to, 		// Array of cursor columns to bind to.
                from, 0);
        
        // Bind to our new adapter.
        msgList = (ListView)findViewById(R.id.msgList);
        msgList.setAdapter(this.messageAdapter);		

		/*
		 * End Todo
		 */

		send = (Button) findViewById(R.id.send_button);
		send.setOnClickListener(sendListener);

		/*
		 * TODO: Start the background service that will receive messages from peers.
		 */
		
		Intent intent = new Intent(this, ChatService.class);
		bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		startService(intent);

		/*
		 * End Todo
		 */
	}
	
	/*
	 * TODO: Since the content provider for messages received is now updated on a background
	 * thread, it sends a broadcast to the UI to tell it to update the cursor.  The UI
	 * should register a broadcast receiver that will change the cursor for the messages adapter.
	 */
	
	@Override
	protected void onPause() {
	   unregisterReceiver(updater);
	   super.onPause();
	}
	 
	@Override
	protected void onResume() {
	   this.updater = new Receiver();
	   registerReceiver(
	         this.updater, 
	         new IntentFilter(
	        		 ChatService.NEW_MESSAGE_BROADCAST));
	               //ConnectivityManager.CONNECTIVITY_ACTION));
	   super.onResume();
	}
	
	/*
	 * End Todo
	 */

	
	protected Cursor makeMessageCursor () {
		/*
		 * TODO: managedQuery is deprecated, use CursorLoader instead!
		 */
		String[] projection = 
				new String[] { ChatContent.Messages._ID,
							   ChatContent.Messages.SENDER, 
							   ChatContent.Messages.MESSAGE };
/*		Cursor c = managedQuery(ChatContent.Messages.CONTENT_URI, 
				projection,
				null, null, null);*/
		ContentResolver cr = getContentResolver();
		Cursor c = cr.query(ChatContent.Messages.CONTENT_URI,
		        projection, null, null, null);		
		return c;
	}

	/*
	 * On click listener for the send button
	 */
	private OnClickListener sendListener = new OnClickListener() {
		public void onClick(View v) {
			postMessage();
		}
	};

	/*
	 * Send the message in the msg EditText
	 */
	private void postMessage() {
		try {
			/*
			 * On the emulator, which does not support WIFI stack, we'll send to
			 * (an AVD alias for) the host loopback interface, with the server
			 * port on the host redirected to the server port on the server AVD.
			 */
			InetAddress targetAddr = InetAddress.getByName(destHost.getText().toString());

			int targetPort = Integer.parseInt(destPort.getText().toString());
			
			String theNewMessage = msg.getText().toString();
			
			this.sendMessage(targetAddr, targetPort, theNewMessage);
		} catch (UnknownHostException e) {
			Log.e(TAG, "Unknown host exception: " + e.getMessage());
		} catch (IOException e) {
			Log.e(TAG, "IO exception: " + e.getMessage());
		}

		msg.setText("");
	}	

	/*
	 * Send a UDP packet
	 */
	public void sendMessage(InetAddress destAddr, int destPort, String msg)
			throws IOException {

		/*
		 * Append client info to the front of the message.
		 */
		String sender = getString(R.string.user_name);
		String latitude = getString(R.string.latitude);
		String longitude = getString(R.string.longitude);
		String line = sender + ":" + latitude + ":" + longitude + ":" + msg;
		byte[] sendData = line.getBytes();

		this.sendPacket = new DatagramPacket(sendData,
				sendData.length, destAddr, destPort);

		Thread thread = new Thread(null, doBackgroundThreadProcessing, "Background");
		thread.start();
		
		
		//serviceBinder.send(sendPacket);
		try {
			thread.join();
		} catch (InterruptedException e) {
			Log.i(TAG, "Background thread interrupted");
		}

		Log.i(TAG, "Sent packet: " + msg);
		

	}
	
	/*
	 * To handle NetworkOnMainThreadException
	 */
	
	//Initialize a handler on main thread.
	private Handler handler = new Handler();
	private Runnable doBackgroundThreadProcessing = new Runnable()
	{
		public void run()
		{
			backgroundThreadProcessing();
		}
	};
	// Do processing in the background.
	private void backgroundThreadProcessing()
	{
		serviceBinder.send(sendPacket);
		handler.post(doUpdateGUI);
	}
	// Runnable that executes GUI update 
	private Runnable doUpdateGUI = new Runnable()
	{
		public void run()
		{
			updateGUI();
		}
	};
	private void updateGUI()
	{
		((EditText) findViewById(R.id.message_text)).setText(null);
	}	

	@Override
	public void onDestroy() {
		super.onDestroy();
		stopService(new Intent(this, ChatService.class));
	}

	/*
	 * Options menu includes an option to list all peers from whom we have
	 * received communication.
	 */

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		this.getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		super.onOptionsItemSelected(item);
		Intent i;

		switch (item.getItemId()) {
		case (R.id.show_peers):
			i = new Intent(this, ShowPeers.class);
			startActivity(i);
			return true;
		}
		return false;
	}

	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		// TODO Auto-generated method stub
		String[] projection = 
				new String[] { ChatContent.Messages._ID,
							   ChatContent.Messages.SENDER, 
							   ChatContent.Messages.MESSAGE };
		switch
		(LOADER_ID)
		{
		case URL_LOADER:
			return new CursorLoader(this, ChatContent.Messages.CONTENT_URI, projection, null, null, null);
		default:
			return null;		//An invalid id was passed in
		}
	}

	public void onLoadFinished(Loader<Cursor> arg0, Cursor arg1) {
		messageAdapter.changeCursor(arg1);
	}

	public void onLoaderReset(Loader<Cursor> arg0) {
		messageAdapter.changeCursor(null);
	}
	
	public class Receiver extends BroadcastReceiver {
	 
	@Override 
	public void onReceive(Context context, Intent intent) {
	      // react to the event
		String action = intent.getAction();
	       if(action.equalsIgnoreCase(ChatService.NEW_MESSAGE_BROADCAST)){  
	   		messageAdapter.changeCursor(makeMessageCursor());
			//LoaderManager lm = getSupportLoaderManager();
		     //lm.restartLoader(LOADER_ID, null, mCallbacks);	    	   
	       }
	   }
	}

}
