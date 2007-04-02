package com.arcaner.warlock.stormfront.internal;

import org.xml.sax.Attributes;

import com.arcaner.warlock.client.IWarlockClient;
import com.arcaner.warlock.client.stormfront.internal.StormFrontStyle;
import com.arcaner.warlock.stormfront.IStormFrontProtocolHandler;

public class OutputTagHandler extends DefaultTagHandler {

	private String currentClass;
	private StormFrontStyle currentStyle;
	
	public OutputTagHandler (IStormFrontProtocolHandler handler) {
		super (handler);
	}
	
	public String getName() {
		return "output";
	}
	
	@Override
	public void handleStart(Attributes atts) {
		String clazz = atts.getValue("class");
		
		if (clazz == null || clazz.length() == 0)
		{
			StringBuffer buffer = DocumentTagHandler.getBuffer(currentClass);
			handler.getClient().append(IWarlockClient.DEFAULT_VIEW, buffer.toString(), currentStyle);
			
			DocumentTagHandler.stopCollecting(currentClass);	
		}
		else
		{
			DocumentTagHandler.startCollecting(currentClass);
			currentStyle = StormFrontStyle.createCustomStyle(currentClass);
		}
		
		currentClass = clazz;
	}

}
