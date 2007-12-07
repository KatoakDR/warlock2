/*
 * Created on Sep 17, 2004
 */
package cc.warlock.rcp.views;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import cc.warlock.core.client.IWarlockClient;
import cc.warlock.core.client.IWarlockClientViewer;
import cc.warlock.rcp.configuration.GameViewConfiguration;
import cc.warlock.rcp.plugin.Warlock2Plugin;
import cc.warlock.rcp.ui.WarlockCompass;
import cc.warlock.rcp.ui.WarlockEntry;
import cc.warlock.rcp.ui.WarlockText;
import cc.warlock.rcp.ui.client.SWTWarlockClientViewer;
import cc.warlock.rcp.ui.style.CompassThemes;

/**
 * @author marshall
 */
public abstract class GameView extends StreamView implements IWarlockClientViewer {

	protected static GameView firstInstance;
	protected static boolean firstInstanceIsUsed = false;
	protected static ArrayList<GameView> openViews = new ArrayList<GameView>();
	protected static ArrayList<IGameViewFocusListener> focusListeners = new ArrayList<IGameViewFocusListener>();
	protected static GameView viewInFocus;
	
	protected WarlockText text;
	protected WarlockEntry entry;
	protected WarlockCompass compass;
	protected SWTWarlockClientViewer wrapper;
	protected Composite entryComposite;
	
	public GameView () {
		if (firstInstance == null) {
			firstInstance = this;
			viewInFocus = this;
		}
		
		// currentCommand = "";
		openViews.add(this);
		wrapper = new SWTWarlockClientViewer(this);
		
		setStreamName(IWarlockClient.DEFAULT_STREAM_NAME);
	}
	
	public static void addGameViewFocusListener (IGameViewFocusListener listener)
	{
		focusListeners.add(listener);
	}
	
	public static void removeGameViewFocusListener (IGameViewFocusListener listener)
	{
		if (focusListeners.contains(listener))
			focusListeners.remove(listener);
	}
	
	public static Collection<GameView> getOpenGameViews ()
	{
		return openViews;
	}
	
	public static GameView getViewInFocus ()
	{
		return viewInFocus;
	}
	
	public static void initializeGameView (GameView gameView)
	{
		viewInFocus = gameView;
		
		if (ConnectionView.closeAfterConnect)
		{
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			IViewPart part = page.findView(ConnectionView.VIEW_ID);
			if (part != null)
				page.hideView(part);
		}
	}
	
	public static GameView createNext (String viewId, String secondId) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			IViewPart part = page.showView(viewId, secondId, IWorkbenchPage.VIEW_ACTIVATE);
			// if there's an error in creating the view, we want to know about it.. don't cast unless we know it's not an errorviewpart
			if (part instanceof GameView)
			{
				GameView nextInstance = (GameView) part;
				initializeGameView(nextInstance);
				return nextInstance;
			}
			
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
		
		this.client = Warlock2Plugin.getDefault().getCurrentClient();
		this.text = getTextForClient(this.client);
		book.showPage(this.text.getTextWidget());
		
		text.setLineLimit(GameViewConfiguration.instance().getBufferLines());
		text.setScrollDirection(SWT.DOWN);
		
		entryComposite = new Composite(mainComposite, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		entryComposite.setLayout(layout);
		entryComposite.setLayoutData(new GridData(GridData.FILL, GridData.VERTICAL_ALIGN_END, true, false));
		
		entry = new WarlockEntry(entryComposite, wrapper);
		
		compass = new WarlockCompass(text, CompassThemes.getCompassTheme("small"));
		text.setBackgroundMode(SWT.INHERIT_DEFAULT);
		
		text.getTextWidget().addKeyListener(entry);
		
	}
	
	
	public void setFocus() {
		viewInFocus = this;
		for (IGameViewFocusListener listener : focusListeners)
		{
			listener.gameViewFocused(this);
		}
	}
	
	protected Image createCaretImage (int width, Color foreground)
	{
		PaletteData caretPalette = new PaletteData(new RGB[] {
				new RGB(0, 0, 0), new RGB(255, 255, 255) });
		
		int widthOffset = width - 1;
		ImageData imageData = new ImageData(4 + widthOffset,
				entry.getWidget().getLineHeight(), 1, caretPalette);
		Display display = entry.getWidget().getDisplay();
		Image bracketImage = new Image(display, imageData);
		GC gc = new GC(bracketImage);
		gc.setForeground(foreground);
		gc.setLineWidth(1);
		// gap between two bars of one third of the height
		// draw boxes using lines as drawing a line of a certain width produces
		// rounded corners.
		for (int i = 0; i < width; i++) {
			gc.drawLine(i, 0, i, imageData.height - 1);
		}

		gc.dispose();

		return bracketImage;
	}

	protected Caret createCaret (int width, Color foreground) {
		Caret caret = new Caret(entry.getWidget(), SWT.NULL);
		Image image = createCaretImage(width, foreground);
		
		if (image != null)
			caret.setImage(image);
		else
			caret.setSize(width, entry.getWidget().getLineHeight());

		caret.setFont(entry.getWidget().getFont());

		return caret;
	}
	
	public String getCurrentCommand ()
	{
		return entry.getText();
	}
	
	public void setCurrentCommand(String command) {
		if(command == null) {
			command = "";
		}
		GameView.this.entry.setText(command);
		GameView.this.entry.setSelection(command.length());
	}
	
	public void append(char c) {
		entry.append(c);
	}
	
	public void nextCommand() {
		entry.nextCommand();
	}
	
	public void prevCommand() {
		entry.prevCommand();
	}
	
	public void searchHistory() {
		entry.searchHistory();
	}
	
	public void submit() {
		entry.submit();
	}
	
	public void repeatLastCommand() {
		entry.repeatLastCommand();
	}
	
	public void repeatSecondToLastCommand() {
		entry.repeatSecondToLastCommand();
	}
	
	public void copy() {
		text.copy();
	}
	
	public void paste() {
		entry.getWidget().paste();
	}
	
	public void setClient(IWarlockClient client) {
		this.client = client;
		book.showPage(getTextForClient(client).getTextWidget());
		
		setMainStream(client.getDefaultStream());
		client.addViewer(wrapper);
		
		for (StreamView streamView : StreamView.openViews)
		{
			if (!(streamView instanceof GameView))
			{
				// initialize pre-opened stream views
				streamView.setAppendNewlines(false);
				streamView.setClient(client);
			}
		}
	}
	
	public IWarlockClient getWarlockClient() {
		return client;
	}
	
	@Override
	public void dispose() {
		if (client != null && client.getConnection() != null && client.getConnection().isConnected())
		{
			try {
				client.getConnection().disconnect();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		super.dispose();
	}
}
