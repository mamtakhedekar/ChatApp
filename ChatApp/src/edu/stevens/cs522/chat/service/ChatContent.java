/*********************************************************************

    Content provider for peer information for the chat program.
    This collects the URIs, MIME types and column names for the content provider.

    Copyright (c) 2012 Stevens Institute of Technology

**********************************************************************/

package edu.stevens.cs522.chat.service;

import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

/*
 * @author dduggan
 * 
 * Convenience class for content providers
 */
public class ChatContent {
	
	public static final String AUTHORITY_PREFIX = "edu.stevens.cs522.chat.service";

	/*
	 * Content with information about messages:
	 * content://edu.stevens.cs522.chat/messages
	 */
	public static final class Messages implements BaseColumns {
		
		/*
		 * Content URI for message data
		 */
		public static final String AUTHORITY = AUTHORITY_PREFIX + ".messages";
		public static final Uri CONTENT_URI = Uri.parse("content://"+ AUTHORITY);
		
		/*
         * The MIME type of {@link #CONTENT_URI} providing a directory of messages.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.edu.stevens.cs.cs522.chat.messages";

        /*
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single message.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.edu.stevens.cs.cs522.chat.messages";

	
		public static final String DEFAULT_SORT_ORDER = "_id ASC";
	
		/*
		 * Sender name.
		 */
		public static final String SENDER = "sender";
	
		/*
		 * Content of a essage.
		 */
		public static final String MESSAGE = "message";
	
	}


	/*
	 * Content with information about peers:
	 * content://edu.stevens.cs522.chat/peers
	 */
	public static final class Peers implements BaseColumns {
		
		/*
		 * Content URI for peer data
		 */
		public static final String AUTHORITY = AUTHORITY_PREFIX + ".peers";
		public static final Uri CONTENT_URI = Uri.parse("content://"+ AUTHORITY);
		
		/*
         * The MIME type of {@link #CONTENT_URI} providing a directory of peers.
         */
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.edu.stevens.cs.cs522.chat.peers";

        /*
         * The MIME type of a {@link #CONTENT_URI} sub-directory of a single peer.
         */
        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.edu.stevens.cs.cs522.chat.peers";

	
		public static final String DEFAULT_SORT_ORDER = "_id ASC";
	
		/*
		 * Peer user name
		 */
		public static final String NAME = "name";
	
		/*
		 * Host (IP address) for that peer's chat program (obtained from messages sent)
		 */
		public static final String HOST = "host";
	
		/*
		 * UDP port number for that peer's chat program (obtained from messages sent)
		 */
		public static final String PORT = "port";
	
		/*
		 * latitude coordinate
		 */
		public static final String LATITUDE = "latitude";
	
		/*
		 * longitude coordinate
		 */
		public static final String LONGITUDE = "longitude";
		
		public static final String getName(Cursor c) {
			return c.getString(c.getColumnIndexOrThrow(NAME));
		}

		public static final String getHost(Cursor c) {
			return c.getString(c.getColumnIndexOrThrow(HOST));
		}

		public static final int getPort(Cursor c) {
			return c.getInt(c.getColumnIndexOrThrow(PORT));
		}

		public static final double getLatitude(Cursor c) {
			return c.getDouble(c.getColumnIndexOrThrow(LATITUDE));
		}

		public static final double getLongitude(Cursor c) {
			return c.getDouble(c.getColumnIndexOrThrow(LONGITUDE));
		}
	}
}
