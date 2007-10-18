/**
 * 
 */
package cc.warlock.rcp.userstreams;

import cc.warlock.rcp.ui.client.SWTWarlockClientListener;
import cc.warlock.rcp.views.StreamView;
import cc.warlock.core.client.IStream;
import cc.warlock.core.client.IWarlockClient;
import cc.warlock.core.client.IWarlockClientListener;
import cc.warlock.core.client.WarlockClientRegistry;
//import cc.warlock.core.client.IStreamListener;

/**
 * @author Will Robertson
 *
 */
public class UserStream extends StreamView implements IWarlockClientListener {
	private SWTWarlockClientListener clientListenerWrapper;
	
	public void clientActivated(IWarlockClient client) {
		// TODO Auto-generated method stub
		
	}
	
	public void streamReceivedText (IStream stream, String string) {
		super.streamReceivedText(stream, string);
	}

	public void streamPrompted(IStream stream, String prompt) {
		// super.streamPrompted(stream, prompt);
	}
	
	public void streamEchoed(IStream stream, String text) {
		// Discard
	}
	
	public void clientConnected(IWarlockClient client) {
		//mainStream = client.getDefaultStream();
		//client.getDefaultStream().addStreamListener(this);
		this.addStream(client.getDefaultStream());
	}
	
	public void clientDisconnected(IWarlockClient client) {
		//mainStream = null;
		 client.getDefaultStream().removeStreamListener(streamListenerWrapper);
	}

	
	public void clientRemoved(IWarlockClient client) {
		// TODO Auto-generated method stub
		
	}

	public UserStream() {
		// Constructor
		super();
		clientListenerWrapper = new SWTWarlockClientListener(this);
		WarlockClientRegistry.addWarlockClientListener(clientListenerWrapper); // new SWTWarlockClientListener(this));
	}
}
