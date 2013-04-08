/*********************************************************************

    Content provider for peer information for the chat program.
	We record host and port number for their chat programs
	as well as their locations.

    Copyright (c) 2012 Stevens Institute of Technology

**********************************************************************/

package edu.stevens.cs522.chat.service;

import java.util.HashMap;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

/*
 * @author dduggan
 * 
 * Derived from the NotesList sample application
 *
 */
public class PeerInfoProvider extends ContentProvider {
	
	private static final String DATABASE_NAME = "chat.db";
	private static final int DATABASE_VERSION = 1;
	private static final String PEER_TABLE_NAME = "peers";
	
	private static HashMap<String, String> peerProjectionMap;
	
	private static final int PEERS = 1;
	private static final int PEER_ID = 2;
	
	private static final UriMatcher uriMatcher;
	
	/*
	 * helper class to open, create, and upgrade the database file
	 */
	private static class DatabaseHelper extends SQLiteOpenHelper {
		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		
		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE " + PEER_TABLE_NAME + " ("
					+ ChatContent.Peers._ID + " INTEGER PRIMARY KEY,"
					+ ChatContent.Peers.NAME + " TEXT,"
					+ ChatContent.Peers.HOST + " TEXT,"
					+ ChatContent.Peers.PORT + " INTEGER,"
					+ ChatContent.Peers.LATITUDE + " FLOAT,"
					+ ChatContent.Peers.LONGITUDE + " FLOAT"
					+ ");");
		}
		
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(ChatApp.TAG, "Upgrading database from version " + oldVersion
					+ " to " + newVersion + ", which will destroy all old data");
			db.execSQL("DROP TABLE IF EXISTS " + PEER_TABLE_NAME);
			onCreate(db);
		}
	}
	
	private DatabaseHelper openHelper;

	@Override
	public boolean onCreate() {
		openHelper = new DatabaseHelper(getContext());
		return true;
	}
	
	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		
		switch (uriMatcher.match(uri)) {
		case PEERS:
			qb.setTables(PEER_TABLE_NAME);
			qb.setProjectionMap(peerProjectionMap);
			break;
			
		case PEER_ID:
			qb.setTables(PEER_TABLE_NAME);
			qb.setProjectionMap(peerProjectionMap);
			qb.appendWhere(ChatContent.Peers._ID + "=" + uri.getPathSegments().get(1));
			break;
			
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		/*
		 * Use default sort order if not specified
		 */
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = ChatContent.Peers.DEFAULT_SORT_ORDER;
		} else {
			orderBy = sortOrder;
		}
	
		/*
		 * Query the database
		 */
		SQLiteDatabase db = openHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
		
		/*
		 * Tell the cursor what uri to watch (for data changes)
		 */
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public String getType(Uri uri) {
		switch (uriMatcher.match(uri)) {
		case PEERS:
			return ChatContent.Peers.CONTENT_TYPE;
			
		case PEER_ID:
			return ChatContent.Peers.CONTENT_ITEM_TYPE;
			
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}
	
	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		if (uriMatcher.match(uri) != PEERS) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	
		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}
		
		// Make sure fields are set
		if (values.containsKey(ChatContent.Peers.NAME) == false) {
			values.put(ChatContent.Peers.NAME, "Unknown");
		}
		
		if (values.containsKey(ChatContent.Peers.HOST) == false) {
			values.put(ChatContent.Peers.HOST, 0);
		}
		
		if (values.containsKey(ChatContent.Peers.PORT) == false) {
			values.put(ChatContent.Peers.PORT, 0);
		}

		if (values.containsKey(ChatContent.Peers.LATITUDE) == false) {
			values.put(ChatContent.Peers.LATITUDE, 0.0);
		}
		
		if (values.containsKey(ChatContent.Peers.LONGITUDE) == false) {
			values.put(ChatContent.Peers.LONGITUDE, 0.0);
		}
		
		SQLiteDatabase db = openHelper.getWritableDatabase();
		long rowId = db.insert(PEER_TABLE_NAME, ChatContent.Peers.NAME, values);
		if (rowId > 0) {
			Uri peerUri = ContentUris.withAppendedId(ChatContent.Peers.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(peerUri, null);
			return peerUri;
		}
	
		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
		int count;
		switch (uriMatcher.match(uri)) {
		case PEERS:
			count = db.delete(PEER_TABLE_NAME, selection, selectionArgs);
			break;
			
		case PEER_ID: 
			String id = uri.getPathSegments().get(1);
			count = db.delete(PEER_TABLE_NAME, ChatContent.Peers._ID
					+ "=" + id + (!TextUtils.isEmpty(selection) ? " AND (" 
							+ selection + ")" : ""), selectionArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		SQLiteDatabase db = openHelper.getWritableDatabase();
	
		int count;
		switch (uriMatcher.match(uri)) {
		case PEERS:
			count = db.update(PEER_TABLE_NAME, values, selection, selectionArgs);
			break;
			
		case PEER_ID:
			String id = uri.getPathSegments().get(1);
			count = db.update(PEER_TABLE_NAME, values, ChatContent.Peers._ID
					+ "=" + id + (!TextUtils.isEmpty(selection) ? " AND (" 
							+ selection + ")" : ""), selectionArgs);
			break;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
		
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}
	
	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(ChatContent.Peers.AUTHORITY, null, PEERS);
		uriMatcher.addURI(ChatContent.Peers.AUTHORITY, "#", PEER_ID);
		
		peerProjectionMap = new HashMap<String, String>();
		peerProjectionMap.put(ChatContent.Peers._ID, ChatContent.Peers._ID);
		peerProjectionMap.put(ChatContent.Peers.NAME, ChatContent.Peers.NAME);
		peerProjectionMap.put(ChatContent.Peers.HOST, ChatContent.Peers.HOST);
		peerProjectionMap.put(ChatContent.Peers.PORT, ChatContent.Peers.PORT);
		peerProjectionMap.put(ChatContent.Peers.LATITUDE, ChatContent.Peers.LATITUDE);
		peerProjectionMap.put(ChatContent.Peers.LONGITUDE, ChatContent.Peers.LONGITUDE);
	}

}
