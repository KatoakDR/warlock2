package cc.warlock.rcp.stormfront.ui.views;

import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import cc.warlock.core.client.IProperty;
import cc.warlock.core.client.IWarlockClient;
import cc.warlock.core.client.PropertyListener;
import cc.warlock.core.client.WarlockColor;
import cc.warlock.core.configuration.Profile;
import cc.warlock.core.network.IConnection;
import cc.warlock.core.network.IConnectionListener;
import cc.warlock.core.stormfront.ProfileConfiguration;
import cc.warlock.core.stormfront.client.IStormFrontClient;
import cc.warlock.core.stormfront.client.IStormFrontClientViewer;
import cc.warlock.core.stormfront.client.StormFrontColor;
import cc.warlock.core.stormfront.serversettings.server.ServerSettings;
import cc.warlock.rcp.stormfront.StormFrontGameViewConfiguration;
import cc.warlock.rcp.stormfront.adapters.SWTStormFrontClientViewer;
import cc.warlock.rcp.stormfront.ui.StormFrontMacros;
import cc.warlock.rcp.stormfront.ui.StormFrontSharedImages;
import cc.warlock.rcp.stormfront.ui.StormFrontStatus;
import cc.warlock.rcp.stormfront.ui.actions.ProfileConnectAction;
import cc.warlock.rcp.stormfront.ui.style.StormFrontStyleProvider;
import cc.warlock.rcp.stormfront.ui.wizards.SGEConnectWizard;
import cc.warlock.rcp.ui.WarlockPopupAction;
import cc.warlock.rcp.ui.WarlockSharedImages;
import cc.warlock.rcp.ui.WarlockWizardDialog;
import cc.warlock.rcp.ui.style.StyleProviders;
import cc.warlock.rcp.util.ColorUtil;
import cc.warlock.rcp.util.RCPUtil;
import cc.warlock.rcp.views.ConnectionView;
import cc.warlock.rcp.views.GameView;
import cc.warlock.rcp.views.StreamView;

public class StormFrontGameView extends GameView implements IStormFrontClientViewer {
	
	public static final String VIEW_ID = "cc.warlock.rcp.stormfront.ui.views.StormFrontGameView";
	
	protected IStormFrontClient sfClient;
	protected StormFrontStatus status;
	protected WarlockPopupAction reconnectPopup;
	
	public StormFrontGameView ()
	{
		super();

		wrapper = new SWTStormFrontClientViewer(this);
	}
	
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		text.addCompass();
		
		((GridLayout)entryComposite.getLayout()).numColumns = 2;
		status = new StormFrontStatus(entryComposite);
		
		String fullId = getViewSite().getId() + ":" + getViewSite().getSecondaryId();
		String characterName = StormFrontGameViewConfiguration.instance().getProfileId(fullId);
		
		final Profile profile = ProfileConfiguration.instance().getProfileByCharacterName(characterName);
		createReconnectPopup();
		
		if (profile != null) {
			setReconnectProfile(profile);
		}
		else {			
			setNoReconnectProfile(characterName);
		}
		
	}
	
	protected Label reconnectLabel;
	
	protected void createReconnectPopup()
	{
		reconnectPopup = new WarlockPopupAction(text.getTextWidget(), SWT.NONE);
		reconnectPopup.setLayout(new GridLayout(2, false));
		reconnectLabel = new Label(reconnectPopup, SWT.WRAP);
		
		final Composite buttons = new Composite(reconnectPopup, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		buttons.setLayout(layout);
		
		reconnect = new Button(buttons, SWT.PUSH);
		
		final Button connections = new Button(buttons, SWT.TOGGLE);
		connections.setText("Connections...");
		connections.setImage(WarlockSharedImages.getImage(WarlockSharedImages.IMG_CONNECT));

		final Menu profileMenu = createProfileMenu(connections);
		
		connections.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Rectangle rect = connections.getBounds();
				Point pt = new Point(rect.x, rect.y + rect.height);
				pt = buttons.toDisplay(pt);
				profileMenu.setLocation(pt.x, pt.y);
				profileMenu.setVisible(true);
			}
		});
	}
	
	protected SelectionListener currentListener;
	protected void setReconnectProfile (final Profile profile)
	{
		String characterName = profile.getName();
		reconnectLabel.setText("The character \"" + characterName + "\" is currently disconnected.");

		reconnect.setText("Login as \"" + characterName + "\"");
		reconnect.setImage(WarlockSharedImages.getImage(WarlockSharedImages.IMG_RECONNECT));
		if (currentListener != null)
			reconnect.removeSelectionListener(currentListener);
		currentListener = new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				ProfileConnectAction action = new ProfileConnectAction(profile);
				action.run();
				reconnectPopup.setVisible(false);
			}
		};
		
		reconnect.addSelectionListener(currentListener);
	}
	
	protected void setNoReconnectProfile (String characterName)
	{
		reconnectLabel.setText("The character \"" + characterName +
			"\" is not a saved profile, you will need to re-login:");
		reconnect.setText("Login");
		reconnect.setImage(StormFrontSharedImages.getImage(StormFrontSharedImages.IMG_GAME));
		if (currentListener != null)
			reconnect.removeSelectionListener(currentListener);
		currentListener = new SelectionAdapter () {
			public void widgetSelected(SelectionEvent e) {
				WarlockWizardDialog dialog = new WarlockWizardDialog(Display.getDefault().getActiveShell(),
						new SGEConnectWizard());
			
				int response = dialog.open();
				reconnectPopup.setVisible(false);
			}
		};
		
		reconnect.addSelectionListener(currentListener);
	}
	
	protected Menu createProfileMenu (final Button connections)
	{
		Menu menu = new Menu(connections);
		menu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				connections.setSelection(true);
			}
			public void menuHidden(MenuEvent e) {
				connections.setSelection(false);
			}
		});
		
		for (final Profile profile : ProfileConfiguration.instance().getAllProfiles())
		{
			MenuItem item = new MenuItem (menu, SWT.PUSH);
			item.setText(profile.getName());
			item.setImage(WarlockSharedImages.getImage(WarlockSharedImages.IMG_CHARACTER));
			item.addSelectionListener(new SelectionAdapter () {
				public void widgetSelected(SelectionEvent e) {
					ProfileConnectAction action = new ProfileConnectAction(profile);
					action.run();
					reconnectPopup.setVisible(false);
				}
			});
		}
		
		MenuItem item = new MenuItem(menu, SWT.SEPARATOR);
		item = new MenuItem(menu, SWT.PUSH);
		item.setText("All Connections...");
		item.setImage(WarlockSharedImages.getImage(WarlockSharedImages.IMG_CONNECT));
		item.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				try {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(ConnectionView.VIEW_ID);
				} catch (PartInitException e1) {
					e1.printStackTrace();
				}
			}
		});
		return menu;
	}
	
	private ProgressMonitorDialog settingsProgressDialog;
	public void startDownloadingServerSettings() {
		settingsProgressDialog = new ProgressMonitorDialog(getSite().getShell());
		settingsProgressDialog.setBlockOnOpen(false);
		settingsProgressDialog.open();
		
		IProgressMonitor monitor = settingsProgressDialog.getProgressMonitor();
		monitor.beginTask("Downloading server settings...", SettingType.values().length);
	}
	
	public void receivedServerSetting(SettingType settingType)
	{
		IProgressMonitor monitor = settingsProgressDialog.getProgressMonitor();
		monitor.subTask("Downloading " + settingType.toString() + "...");
		
		monitor.worked(1);
	}
	
	public void finishedDownloadingServerSettings() {
		IProgressMonitor monitor = settingsProgressDialog.getProgressMonitor();
		monitor.done();
		settingsProgressDialog.close();
	}
	
	@Override
	public void setClient(IWarlockClient client) {
		super.setClient(client);
		
		reconnectPopup.setVisible(false);
		if (client instanceof IStormFrontClient)
		{
			addStream(client.getStream(IStormFrontClient.DEATH_STREAM_NAME));
			sfClient = (IStormFrontClient) client;

			StyleProviders.setStyleProvider(client, new StormFrontStyleProvider(sfClient.getServerSettings()));
			
			text.getCompass().setCompass(sfClient.getCompass());
			
			if (status != null)
				status.setActiveClient(sfClient);
		}
	}
	
	protected void disconnected ()
	{
		super.disconnected();
		
		reconnectPopup.setVisible(true);
		removeStream(client.getStream(IStormFrontClient.DEATH_STREAM_NAME));
	}
	
	protected Font normalFont;

	private Button reconnect;
	
	public void loadServerSettings (ServerSettings settings)
	{
		sfClient.getConnection().addConnectionListener(new IConnectionListener () {
			public void connected(IConnection connection) {};
			public void dataReady(IConnection connection, String line) {};
			public void disconnected(IConnection connection) {
				Display.getDefault().asyncExec(new Runnable() {
					public void run () { 
						StormFrontGameView.this.disconnected();
					}
				});
			}
		});
		
		sfClient.getCharacterName().addListener(new PropertyListener<String>() {
			public void propertyChanged(IProperty<String> property, String oldValue) {
				String viewId = getViewSite().getId() + ":" + getViewSite().getSecondaryId();
				StormFrontGameViewConfiguration.instance().addProfileMapping(viewId, sfClient.getCharacterName().get());
				
				Display.getDefault().asyncExec(new Runnable() {
					public void run() {
						Profile profile = ProfileConfiguration.instance().getProfileByCharacterName(sfClient.getCharacterName().get());
						if (profile != null) {
							setReconnectProfile(profile);
						} else {
							setNoReconnectProfile(sfClient.getCharacterName().get());
						}	
					}
				});
			}
		});
		
		WarlockColor bg = settings.getMainWindowSettings().getBackgroundColor();
		WarlockColor fg = settings.getMainWindowSettings().getForegroundColor();
		
		String fontFace = settings.getMainWindowSettings().getFontFace();
		int fontSize = settings.getMainWindowSettings().getFontSizeInPoints();
		
		boolean fontFaceEmpty = (fontFace == null || fontFace.length() == 0);
		normalFont = fontFaceEmpty ? JFaceResources.getDefaultFont() : new Font(getSite().getShell().getDisplay(), fontFace, fontSize, SWT.NONE);
		text.setFont(normalFont);
		
		WarlockColor entryBG = settings.getCommandLineSettings().getBackgroundColor();
		WarlockColor entryFG = settings.getCommandLineSettings().getForegroundColor();
		WarlockColor entryBarColor = settings.getCommandLineSettings().getBarColor();
		
		entry.getWidget().setForeground(ColorUtil.warlockColorToColor(entryFG.equals(StormFrontColor.DEFAULT_COLOR) ? fg  : entryFG));
		entry.getWidget().setBackground(ColorUtil.warlockColorToColor(entryBG.equals(StormFrontColor.DEFAULT_COLOR) ? bg : entryBG));
		entry.getWidget().redraw();
		
		Caret newCaret = createCaret(1, ColorUtil.warlockColorToColor(entryBarColor));
		entry.getWidget().setCaret(newCaret);
		
		text.setBackground(ColorUtil.warlockColorToColor(bg));
		text.setForeground(ColorUtil.warlockColorToColor(fg));
		text.getTextWidget().redraw();
		
		StormFrontMacros.addMacrosFromServerSettings(settings);
		
		if (HandsView.getDefault() != null)
		{
			HandsView.getDefault().loadServerSettings(settings);
		}
		
		for (StreamView streamView : StreamView.getOpenViews())
		{
			if (!(streamView instanceof GameView))
			{
				streamView.setBackground(client, ColorUtil.warlockColorToColor(bg));
				streamView.setForeground(client, ColorUtil.warlockColorToColor(fg));
			}
		}
		
		if (status != null)
			status.loadServerSettings(settings);
	}
	
	public void launchURL(final URL url) {
		Display.getDefault().asyncExec(new Runnable () {
			public void run () {
				RCPUtil.openURL(url.toString());
			}
		});
	}
	
	public void appendImage(final URL imageURL) {
//		Display.getDefault().asyncExec(new Runnable () {
//			public void run () {
//				text.append(""+WarlockText.OBJECT_HOLDER);
//				
//				try {
//					InputStream imageStream = imageURL.openStream();
//					
//					Image image = new Image(text.getDisplay(), imageStream);
//					imageStream.close();
//					
//					text.addImage(image);
//					
//				} catch (IOException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
//				}
//			}
//		});
	}
	
	@Override
	public Object getAdapter(Class adapter) {
		if (IStormFrontClient.class.equals(adapter))
		{
			return client;
		}
		return super.getAdapter(adapter);
	}
	
	public IStormFrontClient getStormFrontClient() {
		return sfClient;
	}
}
