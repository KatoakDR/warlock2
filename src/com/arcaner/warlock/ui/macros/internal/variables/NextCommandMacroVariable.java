/*
 * Created on Mar 27, 2005
 */
package com.arcaner.warlock.ui.macros.internal.variables;

import com.arcaner.warlock.client.ICommand;
import com.arcaner.warlock.client.IWarlockClient;
import com.arcaner.warlock.client.IWarlockClientViewer;
import com.arcaner.warlock.ui.macros.IMacroVariable;

/**
 * @author Marshall
 */
public class NextCommandMacroVariable implements IMacroVariable {

	public String getIdentifier() {
		return "$nextCommand";
	}

	public String getValue(IWarlockClientViewer context) {
		IWarlockClient client = context.getWarlockClient();
		ICommand command = client.getCommandHistory().next();
		
		if(command != null) {
			return command.getCommand();
		}
		
		return null;
	}

}
