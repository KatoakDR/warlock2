/*
 * Created on Jan 16, 2005
 */
package cc.warlock.client.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

import cc.warlock.client.IProperty;
import cc.warlock.client.IStream;
import cc.warlock.client.IStreamListener;
import cc.warlock.client.IStyledString;
import cc.warlock.client.IWarlockStyle;


/**
 * @author Marshall
 * 
 * The internal implementation of a StormFront stream.
 */
public class Stream implements IStream {
	
	private static Hashtable<String, Stream> streams = new Hashtable<String, Stream>();
	
	protected IProperty<String> streamName, streamTitle;
	protected ArrayList<IStreamListener> listeners;
	protected boolean isPrompting = false;
	protected StyledString buffer = null;
	
	private Stream (String streamName) {
		this.streamName = new Property<String>("streamName", null);
		this.streamName.set(streamName);
		this.streamTitle = new Property<String>("streamTitle", null);
		
		listeners = new ArrayList<IStreamListener>();
		
		streams.put(streamName, this);
	}

	public void addStreamListener(IStreamListener listener) {
		listeners.add(listener);
	}
	
	public void removeStreamListener(IStreamListener listener) {
		listeners.remove(listener);
	}

	public void clear() {
		for(IStreamListener listener : listeners) {
			listener.streamCleared(this);
		}
	}
	
	public void send(String text) {
		send (text, WarlockStyle.EMPTY_STYLE);
	}
	
	public void send(String data, IWarlockStyle style) {
		
		if (buffer == null)
		{
			buffer = new StyledString();
		}
		int currentOffset = buffer.getBuffer().length();
		
		buffer.getBuffer().append(data);
		if (!buffer.getStyles().contains(style))
		{
			// allows styles to be relative to the string they're using
			style.setStart(style.getStart() + currentOffset);
			buffer.addStyle(style);
		}
		
		sendBuffer();
	}

	public void send (IStyledString text)
	{
		if (buffer == null)
			buffer = new StyledString();
		
		buffer.append(text);
		sendBuffer();
	}
	
	private void sendBuffer() {
		if (buffer.readyToFlush())
		{
			StyledString newline = new StyledString();
			newline.getBuffer().append("\n");
			for(IStreamListener listener : listeners) {
				try {
					if(isPrompting) {
						listener.streamReceivedText(this, newline);
					}
					listener.streamReceivedText(this, buffer);
				} catch (Throwable t) {
					// TODO Auto-generated catch block
					t.printStackTrace();
				}
			}
			
			if (buffer.getBuffer().length() > 0)
			{
				isPrompting = false;
			}
			buffer = null;
		}
	}
	
	public void prompt(String prompt) {
		if (!isPrompting)
		{
			for (IStreamListener listener : listeners)
			{
				listener.streamPrompted(this, prompt);
			}

			isPrompting = true;
		}
	}
	
	public void donePrompting() {
		isPrompting = false;
	}
	
	public void echo(String text) {
		for (IStreamListener listener : listeners)
		{
			listener.streamEchoed(this, text);
		}
	}
	
	public IProperty<String> getName() {
		return streamName;
	}
	
	protected static Stream fromName (String name)
	{
		if (streams.containsKey(name))
			return streams.get(name);
		
		else return new Stream(name);
	}
	
	public static Collection<Stream> getStreams ()
	{
		return streams.values();
	}

	public IProperty<String> getTitle() {
		return streamTitle;
	}
}
