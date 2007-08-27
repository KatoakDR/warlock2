/*
 * Created on Jan 15, 2005
 */
package cc.warlock.stormfront.internal;

import java.util.HashMap;

import cc.warlock.client.stormfront.IStormFrontClient;
import cc.warlock.stormfront.IStormFrontProtocolHandler;


/**
 * @author Marshall
 *
 * An XPath Listener that handles the health, mana, fatigue, and spirit bars.
 */
public class BarTagHandler extends DefaultTagHandler {
	
	public BarTagHandler (IStormFrontProtocolHandler handler) {
		super(handler);
	}
	
	public String[] getTagNames() {
		return new String[] { "progressBar" };
	}

	public void handleStart(HashMap<String,String> attributes) {
    	handleProgressBar(attributes.get("id"), Integer.parseInt(attributes.get("value")), attributes.get("text"));	
	}
	
	private void handleProgressBar (String which, int percentage, String label)
	{
		IStormFrontClient client = handler.getClient();
		
		if (which.equals("health"))
		{
			client.getHealth().set(percentage);
		}
		else if (which.equals("mana"))
		{
			client.getMana().set(percentage);
		}
		else if (which.equals("stamina"))
		{
			client.getFatigue().set(percentage);
		}
		else if (which.equals("spirit"))
		{
			client.getSpirit().set(percentage);
		}
	}

}
