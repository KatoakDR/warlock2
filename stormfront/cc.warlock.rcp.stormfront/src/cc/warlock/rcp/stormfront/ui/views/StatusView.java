package cc.warlock.rcp.stormfront.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import cc.warlock.core.client.ICharacterStatus;
import cc.warlock.core.client.IProperty;
import cc.warlock.core.client.IPropertyListener;
import cc.warlock.core.stormfront.client.IStormFrontClient;
import cc.warlock.core.stormfront.serversettings.server.ServerSettings;
import cc.warlock.rcp.plugin.Warlock2Plugin;
import cc.warlock.rcp.stormfront.ui.StormFrontSharedImages;
import cc.warlock.rcp.ui.client.SWTPropertyListener;
import cc.warlock.rcp.util.ColorUtil;

public class StatusView extends ViewPart implements IPropertyListener<String>
{
	public static final String VIEW_ID = "cc.warlock.rcp.stormfront.ui.views.StatusView";
	protected static StatusView _instance;
	protected Label[] statusLabels = new Label[5];
	protected IStormFrontClient client;
	protected SWTPropertyListener<String> wrapper = new SWTPropertyListener<String>(this);
	
	public StatusView() {
		_instance = this;
	}
	
	public static StatusView getDefault() {
		return _instance;
	}

	@Override
	public void createPartControl(Composite parent) {
		parent.setLayout(new FillLayout());
		parent.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(5, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		main.setLayout(layout);
		main.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true));
		
		for (int i = 0; i < 5; i++)
		{
			statusLabels[i] = new Label(parent, SWT.NONE);
			statusLabels[i].setImage(StormFrontSharedImages.getImage(StormFrontSharedImages.IMG_STATUS_BLANK));
			GridData data = new GridData(GridData.FILL, GridData.FILL, true, true);
			data.minimumHeight = 32;
			data.minimumWidth = 32;
			statusLabels[i].setLayoutData(data);
		}
		
		client = (IStormFrontClient) Warlock2Plugin.getDefault().getCurrentClient();
		client.getCharacterStatus().addListener(wrapper);
		
		setColors(new Color(main.getDisplay(), 240, 240, 255), new Color(main.getDisplay(), 25, 25, 50));
	}

	public void propertyActivated(IProperty<String> property) {}
	
	public void propertyChanged(IProperty<String> property, String oldValue) {
		if (property == null || property.getName() == null) return;
		
		if ("characterStatus".equals(property.getName()))
		{
			ICharacterStatus status = client.getCharacterStatus();
			
			if (status.getStatus().get(ICharacterStatus.StatusType.Standing))
			{
				statusLabels[4].setImage(StormFrontSharedImages.getImage(StormFrontSharedImages.IMG_STATUS_STAND));
			}
			else if (status.getStatus().get(ICharacterStatus.StatusType.Sitting))
			{
				statusLabels[4].setImage(StormFrontSharedImages.getImage(StormFrontSharedImages.IMG_STATUS_SIT));
			}
			else if (status.getStatus().get(ICharacterStatus.StatusType.Kneeling))
			{
				statusLabels[4].setImage(StormFrontSharedImages.getImage(StormFrontSharedImages.IMG_STATUS_KNEEL));
			}
			else if (status.getStatus().get(ICharacterStatus.StatusType.Prone))
			{
				statusLabels[4].setImage(StormFrontSharedImages.getImage(StormFrontSharedImages.IMG_STATUS_LIE));
			}
			
			// need more images to implement the rest...
		}
	}
	
	public void propertyCleared(IProperty<String> property, String oldValue) {}
	
	@Override
	public void setFocus() {
//		this.client = Warlock2Plugin.getDefault().getCurrentClient();
	}
	
	protected void setColors (Color fg, Color bg)
	{
		for (int i = 0; i < statusLabels.length; i++)
		{
			statusLabels[i].setForeground(fg);
			statusLabels[i].setBackground(bg);
		}
	}
	
	public void loadServerSettings (ServerSettings settings)
	{
		Color bg = ColorUtil.warlockColorToColor(settings.getMainWindowSettings().getBackgroundColor());
		Color fg = ColorUtil.warlockColorToColor(settings.getMainWindowSettings().getForegroundColor());
		
		setColors(fg, bg);
	}

}
