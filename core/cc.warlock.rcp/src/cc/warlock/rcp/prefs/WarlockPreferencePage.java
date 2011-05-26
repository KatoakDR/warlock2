/**
 * Warlock, the open-source cross-platform game client
 *  
 * Copyright 2008, Warlock LLC, and individual contributors as indicated
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package cc.warlock.rcp.prefs;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PropertyPage;

import cc.warlock.core.client.IWarlockClient;
import cc.warlock.core.script.configuration.ScriptConfiguration;
import cc.warlock.rcp.configuration.GameViewConfiguration;
import cc.warlock.rcp.views.GameView;

public class WarlockPreferencePage extends PropertyPage implements IWorkbenchPropertyPage {
	protected Button promptButton, suppressScriptExceptionsButton;
	
	protected IWarlockClient client;
	
	protected Control createContents(Composite parent) {
		Composite main = new Composite (parent, SWT.NONE);
		main.setLayout(new GridLayout(1, false));
		
		GameViewConfiguration viewConfig = (GameViewConfiguration)client.getViewer().getSettings();
		boolean suppressPrompt = viewConfig.getSuppressPrompt();
		promptButton = new Button(main, SWT.CHECK);
		promptButton.setText("Supress prompts");
		promptButton.setSelection(suppressPrompt);
		
		boolean suppressScriptExceptions = ScriptConfiguration.instance().getSupressExceptions().get();
		suppressScriptExceptionsButton = new Button(main, SWT.CHECK);
		suppressScriptExceptionsButton.setText("Suppress Script Exceptions");
		suppressScriptExceptionsButton.setSelection(suppressScriptExceptions);
		
		return main;
	}
	
	@Override
	public void setElement(IAdaptable element) {
		client = (IWarlockClient)element.getAdapter(IWarlockClient.class);
	}
	
	@Override
	public void performDefaults() {
		//suppressPrompt = false;
		promptButton.setSelection(false);
	}
	
	@Override
	public boolean performOk() {
		GameView gameView = (GameView)client.getViewer();
		gameView.getSettings().setSuppressPrompt(promptButton.getSelection());
		ScriptConfiguration.instance().getSupressExceptions().set(suppressScriptExceptionsButton.getSelection());
		return true;
	}
}
