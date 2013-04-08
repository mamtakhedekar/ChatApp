/*********************************************************************

    Content provider for messages received.  We record both the 
    message and the identity of the sender.

    Copyright (c) 2012 Stevens Institute of Technology

 **********************************************************************/

package edu.stevens.cs522.chat.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import edu.stevens.cs522.chat.service.ChatContent;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.net.Uri;
import android.util.Log;

/*
 * @author dduggan
 * 
 * Derived from the NotesList sample application
 *
 */

public class MessageProvider extends ContentProvider {

	private static final String DATABASE_NAME = "messages.txt";

	@Override
	public boolean onCreate() {
		/*
		 * Can't create the messages file here, don't have access to the context
		 * for openFileOutput (to provide write access to local file space).
		 */
		return true;
	}

	private static final int MESSAGES = 1;
	private static final int MESSAGE_ID = 2;

	private static final UriMatcher uriMatcher;

	/*
	 * Note: We should use a cursor that implements a circular buffer and drops all but the last N
	 * messages received.  As it is, this buffer grows unboundedly, a potential DoS attack.
	 */
	MatrixCursor messages = null;

	/*
	 * Utility operations for reading messages from a file and saving messages
	 * back to that file.
	 */
	private MatrixCursor loadMessages() {

		if (messages == null) {
			
			String[] columns = { ChatContent.Messages._ID, 
								 ChatContent.Messages.SENDER, 
								 ChatContent.Messages.MESSAGE };

			messages = new MatrixCursor(columns);

			try {
				InputStream is = getContext().openFileInput(DATABASE_NAME);
				BufferedReader messageInputFile = new BufferedReader(new InputStreamReader(is));
				String messageLine = messageInputFile.readLine();
				while (messageLine != null) {
					/*
					 * Each line contains sender and message.
					 */
					String[] message = messageLine.split(":");
					messages.addRow(message);
					messageLine = messageInputFile.readLine();
					/*
					 * TODO: Finish this loop.
					 */
				}
				messageInputFile.close();
			} catch (FileNotFoundException e) {
				Log.i(ChatApp.TAG, "Messages file has not been created yet.");
			} catch (IOException e) {
				Log.e(ChatApp.TAG, "IO error while reading message file");
			}
		}

		return messages;
	}

	private void saveMessages(Cursor messages) {

		messages.moveToFirst();
		
		try {
			OutputStream os = getContext().openFileOutput(DATABASE_NAME, Context.MODE_PRIVATE);
			BufferedWriter messageOutputFile = new BufferedWriter(new OutputStreamWriter(os));
			for (int row=1; row<=messages.getCount(); row++) {
				String sender = messages.getString(messages.getColumnIndex(ChatContent.Messages.SENDER));
				String message = messages.getString(messages.getColumnIndex(ChatContent.Messages.MESSAGE));
				messageOutputFile.write(row + ":" + sender + ":" + message);
				messageOutputFile.newLine();
				messages.moveToNext();
			}
			messageOutputFile.close();
		} catch (IOException e) {
			Log.e(ChatApp.TAG, "IO error while writing message file");
		}

	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		/*
		 * We don't support message deletion.
		 */
		return 0;
	}

	@Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues initialValues) {
		if (uriMatcher.match(uri) != MESSAGES) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		ContentValues values;
		if (initialValues != null) {
			values = new ContentValues(initialValues);
		} else {
			values = new ContentValues();
		}

		/*
		 * Make sure fields are set
		 */
		if (values.containsKey(ChatContent.Messages.SENDER) == false) {
			values.put(ChatContent.Messages.SENDER, "Unknown");
		}

		if (values.containsKey(ChatContent.Messages.MESSAGE) == false) {
			values.put(ChatContent.Messages.MESSAGE, " ");
		}
		
		/*
		 * TODO: Load messages from file, add new message, and save file.
		 */
		MatrixCursor cursor = loadMessages();
		
		String[] columns = { ChatContent.Messages._ID, 
				 ChatContent.Messages.SENDER, 
				 ChatContent.Messages.MESSAGE };
		
		String[] message = new String[columns.length];
		message[0] = values.getAsString(ChatContent.Messages._ID);
		message[1] = values.getAsString(ChatContent.Messages.SENDER);
		message[2] = values.getAsString(ChatContent.Messages.MESSAGE);
		
		cursor.addRow(message);
		
		saveMessages(cursor);
		
		/*
		 * End Todo.
		 */
		
		int rowId = messages.getCount();
		if (rowId > 0) {
			Uri peerUri = ContentUris.withAppendedId(
					ChatContent.Messages.CONTENT_URI, rowId);
			getContext().getContentResolver().notifyChange(peerUri, null);
			return peerUri;
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		/*
		 * We just support returning all messages.
		 */
		if (uriMatcher.match(uri) != MESSAGES) {
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		Cursor messages = loadMessages();
		return messages;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		/*
		 * We don't support message editing.
		 */
		return 0;
	}

	static {
		uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		uriMatcher.addURI(ChatContent.Messages.AUTHORITY, null, MESSAGES);
		uriMatcher.addURI(ChatContent.Messages.AUTHORITY, "#", MESSAGE_ID);

	}

}
