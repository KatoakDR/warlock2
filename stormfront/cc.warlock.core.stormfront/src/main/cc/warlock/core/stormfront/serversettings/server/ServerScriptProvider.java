package cc.warlock.core.stormfront.serversettings.server;

import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import cc.warlock.core.client.IWarlockClient;
import cc.warlock.core.script.IScript;
import cc.warlock.core.script.IScriptEngine;
import cc.warlock.core.script.IScriptInfo;
import cc.warlock.core.script.ScriptEngineRegistry;
import cc.warlock.core.stormfront.client.IStormFrontClient;

public class ServerScriptProvider implements IServerScriptProvider
{
	protected HashMap<String, ServerScriptInfo> scripts = new HashMap<String, ServerScriptInfo>();
	protected IStormFrontClient client;
	
	protected class ServerScriptInfo implements IServerScriptInfo
	{
		public ServerScript script;
		
		public String getContents() {
			return script.getScriptContents();
		}
		
		public String getScriptName() {
			return script.getName();
		}
		
		public String getComment () {
			return script.getComment();
		}
		
		public Reader openReader() {
			return new StringReader(script.getScriptContents());
		}
		
		public IStormFrontClient getClient() {
			return ServerScriptProvider.this.client;
		}
	}
	
	public ServerScriptProvider (IStormFrontClient client)
	{
		this.client = client;
	}
	
	public IStormFrontClient getClient() {
		return client;
	}
	
	public List<IScriptInfo> getScriptInfos() {
		return Arrays.asList(scripts.values().toArray(new IScriptInfo[scripts.values().size()]));
	}
	
	public IScript startScript (IScriptInfo scriptInfo, IWarlockClient client, String[] arguments)
	{
		for (IScriptEngine engine : ScriptEngineRegistry.getScriptEngines())
		{
			if (engine.supports(scriptInfo))
			{
				return engine.startScript(scriptInfo, client, arguments);
			}
		}
		return null;
	}
	
	public void scriptContentsUpdated (ServerScript script)
	{
		if (!scripts.containsKey(script.getName()))
		{
			ServerScriptInfo info = new ServerScriptInfo();
			info.script = script;
			
			scripts.put(script.getName(), info);
		}
	}
}
