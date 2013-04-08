/*********************************************************************

    Chat server: list peers with whom we've exchanged messages.
    
    Display information about people we're communicating with.

    Copyright (c) 2012 Stevens Institute of Technology

 **********************************************************************/

package edu.stevens.cs522.chat.service;

import android.app.Activity;
import android.app.ListActivity;
import android.os.Bundle;

public class ShowPeers extends Activity {
	
	/*
	 * Called when the activity is first created. 
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.show_peers);        
	}

}
