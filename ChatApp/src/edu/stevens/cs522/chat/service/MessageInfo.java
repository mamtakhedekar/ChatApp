/*********************************************************************

    Information about a message that has been received.
    
    Copyright (c) 2012 Stevens Institute of Technology

 **********************************************************************/

package edu.stevens.cs522.chat.service;

import java.net.InetAddress;

public class MessageInfo {
	
	private String sender;
	private InetAddress srcAddr;
	private int srcPort;
	private double latitude;
	private double longitude;
	private String message;
	
	public String getSender() { return sender; }
	public String getSrcAddr() { return srcAddr.getCanonicalHostName(); }
	public int getSrcPort() { return srcPort; }
	public double getLatitude() { return latitude; }
	public double getLongitude() { return longitude; }
	public String getMessage() { return message; }
	
	public MessageInfo (String s, InetAddress a, int p, double lat, double lng, String m) {
		sender = s;
		srcAddr = a;
		srcPort = p;
		latitude = lat;
		longitude = lng;
		message = m;
	}

}
