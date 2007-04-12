package com.arcaner.warlock.configuration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Hashtable;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import com.arcaner.warlock.client.IWarlockClientViewer;
import com.arcaner.warlock.client.stormfront.IStormFrontClient;
import com.arcaner.warlock.client.stormfront.IStormFrontClientViewer;
import com.arcaner.warlock.client.stormfront.WarlockColor;

public class ServerSettings {

	private IStormFrontClient client;
	private String playerId;
	private Document document;
	private Element mainWindowElement, mainWindowFontElement, mainWindowColumnFontElement, commandLineElement;
	private Hashtable<String, Hashtable<String, String>>presets = new Hashtable<String, Hashtable<String, String>>();
	private Hashtable<String, Hashtable<String, String>> highlightStrings = new Hashtable<String, Hashtable<String, String>>();
	
	public static enum StringType {
		MainWindow_FontFace, MainWindow_MonoFontFace, CommandLine_FontFace
	};
	
	public static enum ColorType {
		MainWindow_Background, MainWindow_Foreground, CommandLine_Background, CommandLine_Foreground
	};
	
	public static enum IntType {
		MainWindow_FontSize, MainWindow_MonoFontSize, CommandLine_FontSize
	}
	
	public static int getPixelSizeInPoints (int pixelSize)
	{
		// we'll assume 96 dpi for now
		double points = pixelSize * (72.0/96.0);
		return (int) Math.round(points);
	}
	
	public ServerSettings (IStormFrontClient client)
	{
		this.client = client;
	}
	
	public ServerSettings (IStormFrontClient client, String playerId)
	{
		this.client = client;
		
		load(playerId);
	}
	
	public void load (String playerId)
	{
		this.playerId = playerId;
		
		try {
			FileInputStream stream = new FileInputStream(WarlockConfiguration.getConfigurationFile("serverSettings_" + playerId + ".xml"));
			SAXReader reader = new SAXReader();
			document = reader.read(stream);
			
			mainWindowElement = (Element) document.selectSingleNode("/settings/stream/w[@id=\"smain\"]");
			mainWindowFontElement = (Element) mainWindowElement.selectSingleNode("font");
			mainWindowColumnFontElement = (Element) mainWindowElement.selectSingleNode("columnFont");
			commandLineElement = (Element) document.selectSingleNode("/settings/cmdline");
			
			loadPresets();
			loadHighlightStrings();
			
			for (IWarlockClientViewer v : client.getViewers())
			{
				IStormFrontClientViewer viewer = (IStormFrontClientViewer) v;
				viewer.loadServerSettings(this);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void saveAttributes (Element element, Hashtable<String, String> table)
	{
		for (Object o2 : element.attributes())
		{
			Attribute attr = (Attribute) o2;
			if (!attr.getName().equals("id"))
			{
				table.put(attr.getName(), attr.getValue());
			}
		}
	}
	
	private void loadPresets ()
	{
		Element presetsElement = (Element) document.selectSingleNode("/settings/presets");
		for (Object o : presetsElement.elements())
		{
			Element pElement = (Element) o;
			String presetId = pElement.attributeValue("id");
			
			presets.put(presetId, new Hashtable<String, String>());
			saveAttributes(pElement, presets.get(presetId));
		}
	}
	
	private void loadHighlightStrings()
	{
		Element stringsElement = (Element) document.selectSingleNode("/settings/strings");
		for (Object o : stringsElement.elements())
		{
			Element hElement = (Element) o;
			String text = hElement.attributeValue("text");
			
			highlightStrings.put(text, new Hashtable<String, String>());
			saveAttributes(hElement, highlightStrings.get(text));
		}
		
		Element namesElement = (Element) document.selectSingleNode("/settings/names");
		for (Object o : namesElement.elements())
		{
			Element hElement = (Element) o;
			String text = hElement.attributeValue("text");
			
			highlightStrings.put(text, new Hashtable<String, String>());
			saveAttributes(hElement, highlightStrings.get(text));
		}
	}
	
	public String getStringSetting (StringType settingType)
	{
		Element fontElement = null;
		
		switch (settingType)
		{
			case MainWindow_FontFace: fontElement = mainWindowFontElement; break;
			case MainWindow_MonoFontFace: fontElement = mainWindowColumnFontElement; break;
			case CommandLine_FontFace: fontElement = commandLineElement; break;
		}
		
		if (fontElement != null)
		{
			return fontElement.attributeValue("face");
		}
		
		return null;
	}
	
	public WarlockColor getColorSetting (ColorType settingType)
	{
		String color = null;
		
		switch (settingType)
		{
			case MainWindow_Background: color = mainWindowElement.attributeValue("bgcolor"); break;
			case MainWindow_Foreground: color = mainWindowElement.attributeValue("fgcolor"); break;
			case CommandLine_Background: color = commandLineElement.attributeValue("bgcolor"); break;
			case CommandLine_Foreground: color = commandLineElement.attributeValue("fgcolor"); break;
		}
		
		if (color != null) 
		{
			if (color.charAt(0) == '@')
			{
				return getPaletteColor(Integer.parseInt(color.substring(1)));
			}
			else if ("skin".equals(color))
			{
				if (settingType == ColorType.MainWindow_Background)
					return getSkinBackgroundColor("main");
				else if (settingType == ColorType.MainWindow_Foreground)
					return getSkinForegroundColor("main");
				else if (settingType == ColorType.CommandLine_Background)
					return getSkinBackgroundColor("cmdline");
				else if (settingType == ColorType.CommandLine_Foreground)
					return getSkinForegroundColor("cmdline");
			}
			else return new WarlockColor(color);
		}
		
		return WarlockColor.DEFAULT_COLOR;
	}
	
	public int getIntSetting (IntType settingType)
	{
		Element fontElement = null;
		
		switch (settingType)
		{
			case MainWindow_FontSize: fontElement = mainWindowFontElement;
			case MainWindow_MonoFontSize: fontElement = mainWindowColumnFontElement;
		}
		
		if (fontElement != null)
		{
			return getPixelSizeInPoints(Integer.parseInt(fontElement.attributeValue("size")));
		}
		
		return -1;
	}
	
	public WarlockColor getPaletteColor (int id)
	{
		Element paletteElement = (Element) document.selectSingleNode("/settings/palette/i[@id=\"" + id + "\"]");
		
		if (paletteElement != null)
		{
			return new WarlockColor(paletteElement.attributeValue("color"));
		}
		
		return null;
	}
	
	private WarlockColor getPresetColor (String presetId, String colorKey)
	{
		if (presets.containsKey(presetId))
		{
			String color = presets.get(presetId).get(colorKey);
			if (color != null)
			{
				if (color.equals("skin"))
				{
					if (colorKey.equals("color"))
						return getSkinForegroundColor(presetId);
					else if (colorKey.equals("bgcolor"))
						return getSkinBackgroundColor(presetId);
				}
				else if (color.charAt(0) == '@')
				{
					return getPaletteColor(Integer.parseInt(color.substring(1)));
				}
				else return new WarlockColor(color);
			}
		}
		
		return WarlockColor.DEFAULT_COLOR;
	}
	
	public WarlockColor getPresetForegroundColor (String presetId)
	{
		return getPresetColor(presetId, "color");
	}
	
	public WarlockColor getPresetBackgroundColor (String presetId)
	{
		return getPresetColor(presetId, "bgcolor");
	}
	
	public boolean getPresetFillEntireLine (String presetId)
	{
		if (presets.containsKey(presetId))
		{
			if (presets.get(presetId).containsKey("line"))
			{
				String line = presets.get(presetId).get("line");
				return "y".equalsIgnoreCase(line);
			}
		}
		return false;
	}
	
	public Collection<String> getHighlightStrings ()
	{
		return highlightStrings.keySet();
	}
	
	private WarlockColor getHighlightColor (String highlightString, String colorKey)
	{
		if (highlightStrings.containsKey(highlightString))
		{
			String color = highlightStrings.get(highlightString).get(colorKey);
			if (color != null)
			{
				if (color.equals("skin") || color.equals(""))
				{
					if (colorKey.equals("color"))
						return getColorSetting(ColorType.MainWindow_Foreground);
					else if (colorKey.equals("bgcolor"))
						return getColorSetting(ColorType.MainWindow_Background);
				}
				else if (color.charAt(0) == '@')
				{
					return getPaletteColor(Integer.parseInt(color.substring(1)));
				}
				else return new WarlockColor(color);
			}
		}
		
		return WarlockColor.DEFAULT_COLOR;
	}
	
	public WarlockColor getHighlightForegroundColor (String highlightString)
	{
		return getHighlightColor(highlightString, "color");
	}
	
	public WarlockColor getHighlightBackgroundColor (String highlightString)
	{
		return getHighlightColor(highlightString, "bgcolor");
	}
	
	public boolean getHighlightFillEntireLine (String highlightString)
	{
		if (highlightStrings.containsKey(highlightString))
		{
			if (highlightStrings.get(highlightString).containsKey("line"))
			{
				String line = highlightStrings.get(highlightString).get("line");
				return "y".equalsIgnoreCase(line);
			}
		}
		return false;
	}

	// These are hard coded for now, we should either have our own "skin" defined in a configuration somewhere,
	// or try to pull from stormfront's binary "skn" file somehow?
	// At any rate -- these look to be the right "default" settings for stormfront..
	public WarlockColor getSkinForegroundColor (String presetId)
	{
		if ("bold".equals(presetId))
		{
			return new WarlockColor("#FFFF00");
		}
		else if ("roomName".equals(presetId))
		{
			return new WarlockColor("#FFFFFF");
		}
		else if ("speech".equals(presetId))
		{
			return new WarlockColor("#80FF80");
		}
		else if ("thought".equals(presetId))
		{
			return new WarlockColor("#FF8000");
		}
		else if ("cmdline".equals(presetId))
		{
			return new WarlockColor("#FFFFFF");
		}
		else if ("whisper".equals(presetId))
		{
			return new WarlockColor("#80FFFF");
		}
		else if ("watching".equals(presetId))
		{
			return new WarlockColor("#FFFF00");
		}
		else if ("link".equals(presetId))
		{
			return new WarlockColor("#62B0FF");
		}
		else if ("selectedLink".equals(presetId))
		{
			return new WarlockColor("#000000");
		}
		else if ("command".equals(presetId))
		{
			return new WarlockColor("#FFFFFF");
		}
		else if ("main".equals(presetId))
		{
			return new WarlockColor("#D4D4D4");
		}
		
		return WarlockColor.DEFAULT_COLOR;
	}
	
	public WarlockColor getSkinBackgroundColor (String presetId)
	{
		if ("roomName".equals(presetId))
		{
			return new WarlockColor("#0000FF");
		}
		else if ("bold".equals(presetId) || "speech".equals(presetId) || "whisper".equals(presetId) || "thought".equals(presetId)
			|| "watching".equals(presetId) || "link".equals(presetId))
		{
			return getColorSetting(ColorType.MainWindow_Background);
		}
		else if ("main".equals(presetId))
		{
			return new WarlockColor("#000000");
		}
		else if ("cmdline".equals(presetId))
		{
			return new WarlockColor("#000000");
		}
		else if ("selectedLink".equals(presetId))
		{
			return new WarlockColor("#62B0FF");
		}
		else if ("command".equals(presetId))
		{
			return new WarlockColor("#404040");
		}
		return WarlockColor.DEFAULT_COLOR;
	}
}
