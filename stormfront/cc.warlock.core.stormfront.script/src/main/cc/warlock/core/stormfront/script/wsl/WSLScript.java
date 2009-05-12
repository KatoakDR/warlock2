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
package cc.warlock.core.stormfront.script.wsl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import cc.warlock.core.client.internal.WarlockStyle;
import cc.warlock.core.client.settings.IVariable;
import cc.warlock.core.client.settings.internal.ClientSettings;
import cc.warlock.core.script.AbstractScript;
import cc.warlock.core.script.IMatch;
import cc.warlock.core.script.IScriptCommands;
import cc.warlock.core.script.IScriptEngine;
import cc.warlock.core.script.IScriptInfo;
import cc.warlock.core.script.configuration.ScriptConfiguration;
import cc.warlock.core.script.internal.RegexMatch;
import cc.warlock.core.script.internal.TextMatch;
import cc.warlock.core.stormfront.client.IStormFrontClient;
import cc.warlock.core.stormfront.script.IStormFrontScriptCommands;
import cc.warlock.core.stormfront.script.internal.StormFrontScriptCommands;

public class WSLScript extends AbstractScript {
	
	private boolean debugging = false;
	private int debugLevel = 1;
	protected double delay = 0.0;
	private HashMap<String, Integer> labels = new HashMap<String, Integer>();
	private int nextLine = 0;
	private WSLAbstractCommand curCommand;
	private String curLine;
	private HashMap<String, IWSLValue> specialVariables = new HashMap<String, IWSLValue>();
	private HashMap<String, IWSLValue> localVariables = new HashMap<String, IWSLValue>();
	private Stack<WSLFrame> callstack = new Stack<WSLFrame>();
	private HashMap<String, IWSLCommandDefinition> wslCommands = new HashMap<String, IWSLCommandDefinition>();
	private Thread scriptThread;
	private Pattern commandPattern = Pattern.compile("^([\\w]+)(\\s+(.*))?");

	private boolean lastCondition = false;
	private ArrayList<WSLAbstractCommand> commands = new ArrayList<WSLAbstractCommand>();
	private ArrayList<IMatch> matches = new ArrayList<IMatch>();
	private HashMap<IMatch, Runnable> matchData = new HashMap<IMatch, Runnable>();
	private BlockingQueue<String> matchQueue;
	
	protected WSLEngine engine;
	protected IStormFrontScriptCommands scriptCommands;
	protected IStormFrontClient sfClient;
	
	private static final String argSeparator = "\\s+";
	
	public WSLScript (WSLEngine engine, IScriptInfo info, IStormFrontClient client)
	{
		super(info, client);
		this.engine = engine;
		this.sfClient = client;
		
		this.debugging = !ScriptConfiguration.instance().getSupressExceptions().get();
		
		scriptCommands = new StormFrontScriptCommands(client, this);
		
		// add command handlers
		addCommandDefinition("counter", new WSLCounter());
		addCommandDefinition("deletevariable", new WSLDeleteVariable());
		addCommandDefinition("deletelocalvariable", new WSLDeleteLocalVariable());
		addCommandDefinition("debug", new WSLDebug());
		addCommandDefinition("debuglevel", new WSLDebugLevel());
		addCommandDefinition("delay", new WSLDelay());
		addCommandDefinition("echo", new WSLEcho());
		addCommandDefinition("else", new WSLElse());
		addCommandDefinition("exit", new WSLExit());
		addCommandDefinition("gosub", new WSLGosub());
		addCommandDefinition("goto", new WSLGoto());
		for(int i = 0; i <= 9; i++) {
			addCommandDefinition("if_" + i, new WSLIf_(String.valueOf(i)));
		}
		addCommandDefinition("match", new WSLMatch());
		addCommandDefinition("matchre", new WSLMatchRe());
		addCommandDefinition("matchwait", new WSLMatchWait());
		addCommandDefinition("math", new WSLMath());
		addCommandDefinition("move", new WSLMove());
		addCommandDefinition("nextroom", new WSLNextRoom());
		addCommandDefinition("pause", new WSLPause());
		addCommandDefinition("put", new WSLPut());
		addCommandDefinition("playsound", new WSLPlaySound());
		addCommandDefinition("random", new WSLRandom());
		addCommandDefinition("return", new WSLReturn());
		addCommandDefinition("run", new WSLRun());
		addCommandDefinition("save", new WSLSave());
		addCommandDefinition("setlocalvariable", new WSLSetLocalVariable());
		addCommandDefinition("setvariable", new WSLSetVariable());
		addCommandDefinition("shift", new WSLShift());
		addCommandDefinition("timer", new WSLTimer());
		addCommandDefinition("wait", new WSLWait());
		addCommandDefinition("waitfor", new WSLWaitFor());
		addCommandDefinition("waitforre", new WSLWaitForRe());
		
		for (IWSLCommandDefinitionProvider provider : engine.getCommandProviders())
		{
			for (Map.Entry<String, IWSLCommandDefinition> entry : provider.getCommands().entrySet()) {
				addCommandDefinition(entry.getKey(), entry.getValue());
			}
		}
		
		setSpecialVariable("mana", new WSLMana());
		setSpecialVariable("health", new WSLHealth());
		setSpecialVariable("fatigue", new WSLFatigue());
		setSpecialVariable("spirit", new WSLSpirit());
		setSpecialVariable("rt", new WSLRoundTime());
		setSpecialVariable("monstercount", new WSLMonsterCount());
		setSpecialVariable("lhand", new WSLLeftHand());
		setSpecialVariable("rhand", new WSLRightHand());
		setSpecialVariable("spell", new WSLSpell());
		setSpecialVariable("roomdesc", new WSLComponent(IStormFrontClient.COMPONENT_ROOM_DESCRIPTION));
		setSpecialVariable("roomexits", new WSLComponent(IStormFrontClient.COMPONENT_ROOM_EXITS));
		setSpecialVariable("roomplayers", new WSLComponent(IStormFrontClient.COMPONENT_ROOM_PLAYERS));
		setSpecialVariable("roomobjects", new WSLComponent(IStormFrontClient.COMPONENT_ROOM_OBJECTS));
		setSpecialVariable("roomtitle", new WSLRoomTitle());
		setSpecialVariable("lastcommand", new WSLLastCommand());
	}

	public IWSLValue getVariable(String name) {
		// these values are maintained by the script
		IWSLValue val = specialVariables.get(name);
		if (val != null)
			return val;
		
		// return value from settings. All user global variables are stored here
		IVariable var = sfClient.getClientSettings().getVariable(name);
		if (var != null)
			return new WSLString(var.getValue());
		
		return null;
	}
	
	public boolean variableExists(String name) {
		return specialVariables.containsKey(name) || sfClient.getClientSettings().getVariable(name) != null;
	}
	
	public boolean localVariableExists(String name) {
		return localVariables.containsKey(name);
	}
	
	public IWSLValue getLocalVariable(String name) {
		return localVariables.get(name);
	}
	
	private class WSLFrame {
		private int line;
		private HashMap<String, IWSLValue> localVariables;
		
		public WSLFrame(int line, HashMap<String, IWSLValue> variables) {
			this.line = line;
			this.localVariables = variables;
		}

		public void restore() {
			WSLScript.this.localVariables = localVariables;
			curCommand = commands.get(line);
			nextLine = line;
			while(curCommand == null) {
				nextLine++;
				if(nextLine >= commands.size())
					break;
				curCommand = commands.get(nextLine);
			}
		}
	}
	
	private class WSLMana extends WSLAbstractNumber {
		public double toDouble() {
			return sfClient.getMana().get().getValue();
		}
	}
	
	private class WSLHealth extends WSLAbstractNumber {
		public double toDouble() {
			return sfClient.getHealth().get().getValue();
		}
	}
	
	private class WSLFatigue extends WSLAbstractNumber {
		public double toDouble() {
			return sfClient.getFatigue().get().getValue();
		}
	}
	
	private class WSLSpirit extends WSLAbstractNumber {
		public double toDouble() {
			return sfClient.getSpirit().get().getValue();
		}
	}
	
	private class WSLRoundTime extends WSLAbstractNumber {
		public double toDouble() {
			return sfClient.getRoundtime().get();
		}
	}
	
	private class WSLMonsterCount extends WSLAbstractNumber {
		public double toDouble() {
			return sfClient.getMonsterCount().get();
		}
	}
	
	private class WSLLeftHand extends WSLAbstractString {
		public String toString() {
			return sfClient.getLeftHand().get();
		}
	}
	
	private class WSLRightHand extends WSLAbstractString {
		public String toString() {
			return sfClient.getRightHand().get();
		}
	}
	
	private class WSLSpell extends WSLAbstractString {
		public String toString() {
			return sfClient.getCurrentSpell().get();
		}
	}
	
	private class WSLRoomTitle extends WSLAbstractString {
		public String toString() {
			return sfClient.getStream(IStormFrontClient.ROOM_STREAM_NAME).getTitle().get();
		}
	}
	
	private class WSLComponent extends WSLAbstractString {
		protected String componentName;
		public WSLComponent(String componentName) {
			this.componentName = componentName;
		}
		
		public String toString () {
			return sfClient.getComponent(componentName).get();
		}
	}
	
	private class WSLLastCommand extends WSLAbstractString {
		public String toString() {
			return scriptCommands.getLastCommand();
		}
	}
	
	private class ScriptRunner  implements Runnable {
		public void run() {
			scriptCommands.addThread(Thread.currentThread());
			try {
				Reader scriptReader = info.openReader();
				
				CharStream input = new ANTLRNoCaseReaderStream(scriptReader);
				WSLLexer lex = new WSLLexer(input);
				CommonTokenStream tokens = new CommonTokenStream(lex);
				WSLParser parser = new WSLParser(tokens);

				parser.setScript(WSLScript.this);

				parser.script();
			} catch(IOException e) {
				e.printStackTrace();
				return;
			} catch (RecognitionException e) {
				e.printStackTrace();
				return;
			}
			
			curCommand = commands.get(0);
			while(curCommand == null) {
				nextLine++;
				if(nextLine >= commands.size())
					break;
				curCommand = commands.get(nextLine);
			}
			
			while(isRunning()) {
				if(curCommand == null)
					break;
				// find the next non-null command
				do {
					nextLine++;
					if(nextLine >= commands.size())
						break;
				} while (commands.get(nextLine) == null);
				
				// crazy dance to make sure we're not suspended and not in a roundtime
				try {
					if(!curCommand.isInstant())
						scriptCommands.waitForRoundtime(delay);
					while(scriptCommands.isSuspended()) {
						scriptCommands.waitForResume();
						if(!curCommand.isInstant())
							scriptCommands.waitForRoundtime(delay);
					}
				} catch(InterruptedException e) {
					
				} finally {
					if(!isRunning())
						break;
				}
				
				try {
					curCommand.execute();
				} catch(InterruptedException e) {
					if(!isRunning())
						break;
				}
				
				if(nextLine >= commands.size())
					break;
				
				curCommand = commands.get(nextLine);
			}
			
			if(isRunning())
				stop();
		}
	}
	
	public void start (Collection<String> arguments)
	{
		super.start();
		
		StringBuffer totalArgs = new StringBuffer();
		int i = 1;
		for (String argument : arguments) {
			setSpecialVariable(Integer.toString(i), argument);
			if (i > 1)
				totalArgs.append(" ");
			totalArgs.append(argument);
			i++;
		}
		// populate the rest of the argument variable
		for(; i <= 9; i++) {
			setSpecialVariable(Integer.toString(i), "");
		}
		// set 0 to the entire list
		setSpecialVariable("0", totalArgs.toString());
		
		scriptThread = new Thread(new ScriptRunner());
		scriptThread.setName("Wizard Script: " + getName());
		scriptThread.start();
	}
	
	public void addLabel(String label, Integer line) {
		labels.put(label.toLowerCase(), line);
	}
	
	public int labelLineNumber(String label) {
		Integer line = labels.get(label.toLowerCase());
		if(line != null)
			return line;
		else
			return -1;
	}
	
	protected void addCommand(WSLAbstractCommand command) {
		commands.add(command);
	}
	
	protected void execute(String line) throws InterruptedException {
		curLine = line;
		Matcher m = commandPattern.matcher(line.trim());
		
		if (!m.find()) {
			return;
		}
		
		String commandName = m.group(1).toLowerCase();
		String arguments = m.group(3);
		if(arguments == null) arguments = "";
		
		IWSLCommandDefinition command = wslCommands.get(commandName);
		if(command != null) {
			scriptDebug(2, "Debug: " + line);
			command.execute(arguments);
		} else {
			//TODO output the line number here
			scriptCommands.echo("Invalid command \"" + line + "\"");
		}
	}
	
	private void addCommandDefinition (String name, IWSLCommandDefinition command) {
		wslCommands.put(name, command);
	}
	
	protected void scriptError(String message) {
		echo("Script error on line " + curCommand.getLineNumber() + " (" + curLine + "): " + message);
		stop();
	}
	
	private void scriptWarning(String message) {
		echo("Script warning on line " + curCommand.getLineNumber() + " (" + curLine + "): " + message);
	}
	
	protected void scriptDebug (int level, String message)
	{
		if (level <= debugLevel && debugging) {
			echo(message);
		}
	}
	
	private class ScriptTimer extends WSLAbstractNumber {
		private long timerStart = -1L;
		private long timePast = 0L;
		
		public double toDouble() {
			if(timerStart < 0) return timePast / 1000;
			return (System.currentTimeMillis() - timerStart) / 1000;
		}
		
		public void start() {
			if(timerStart < 0)
				timerStart = System.currentTimeMillis() - timePast;
		}
		
		public void stop() {
			if(timerStart >= 0) {
				timePast = timerStart - System.currentTimeMillis();
				timerStart = -1L;
			}
		}
		
		public void clear() {
			timerStart = -1L;
			timePast = 0L;
		}
	}
	
	protected class WSLSave implements IWSLCommandDefinition {
		
		public void execute(String arguments) {
			setSpecialVariable("s", arguments);
		}
	}

	protected class WSLDebug implements IWSLCommandDefinition {
		public void execute(String arguments) {
			if (arguments == null || arguments.length() == 0)
			{
				debugging = true;
			}
			else {
				String onoff = arguments.split(argSeparator)[0];
				
				debugging = (onoff.equalsIgnoreCase("on") || onoff.equalsIgnoreCase("true") || onoff.equalsIgnoreCase("yes"));
			}
		}
	}
	
	protected class WSLDebugLevel implements IWSLCommandDefinition {
		private Pattern format = Pattern.compile("^(\\d+)$");
		
		public void execute(String arguments) {
			Matcher m = format.matcher(arguments);
			if (m.find()) {
				debugLevel = Integer.parseInt(m.group(1));
			}
		}
	}
	
	protected class WSLDelay implements IWSLCommandDefinition {
		
		public void execute(String arguments) {
			try {
				delay = Double.parseDouble(arguments);
			} catch(NumberFormatException e) {
				scriptWarning("Invalid arguments to delay");
			}
		}
	}
	
	protected class WSLShift implements IWSLCommandDefinition {
		
		public void execute (String arguments) {
			boolean local = arguments.equalsIgnoreCase("local");
			
			StringBuffer allArgs = new StringBuffer();
			for (int i = 1; ; i++) {
				String nextVar = Integer.toString(i + 1);
				boolean exists = local ? localVariableExists(nextVar) : variableExists(nextVar);
				if (!exists)
				{
					if (local) {
						setLocalVariable("0", allArgs.toString());
						deleteLocalVariable(Integer.toString(i));
					} else {
						setSpecialVariable("0", allArgs.toString());
						setSpecialVariable(Integer.toString(i), "");
					}
					break;
				}
				else
				{
					String arg = local ? getLocalVariable(nextVar).toString() : getVariable(nextVar).toString();
					if (arg == null)
						scriptError("String error in arguments.");
					if(allArgs.length() > 0)
						allArgs.append(" ");
					allArgs.append(arg);
					if (local) {
						setLocalVariable(Integer.toString(i), arg);
					} else {
						setSpecialVariable(Integer.toString(i), arg);
					}
				}
			}
		}
	}

	protected class WSLDeleteVariable implements IWSLCommandDefinition {
		
		public void execute (String arguments) {
			String name = arguments.split(argSeparator)[0];
			deleteVariable(name);
		}
	}
	
	protected class WSLDeleteLocalVariable implements IWSLCommandDefinition {
		
		public void execute (String arguments) {
			String name = arguments.split(argSeparator)[0];
			deleteLocalVariable(name);
		}
	}

	private void setGlobalVariable(String name, IWSLValue value) {
		setGlobalVariable(name, value.toString());
	}
	
	private void setGlobalVariable(String name, String value) {
		if(specialVariables.containsValue(name))
			scriptError("Cannot overwrite special variable \"" + name + "\"");
		((ClientSettings)sfClient.getClientSettings()).getVariableConfigurationProvider().addVariable(name, value);
	}
	
	private void setSpecialVariable(String name, String value) {
		setSpecialVariable(name, new WSLString(value));
	}
	
	private void setSpecialVariable(String name, IWSLValue value) {
		deleteVariable(name);
		specialVariables.put(name, value);
	}
	
	private void deleteVariable(String name) {
		((ClientSettings)sfClient.getClientSettings()).getVariableConfigurationProvider().removeVariable(name);
	}
	
	private void deleteLocalVariable(String name) {
		localVariables.remove(name);
	}
	
	public void setLocalVariable(String name, String value) {
		setLocalVariable(name, new WSLString(value));
	}
	
	public void setLocalVariable(String name, IWSLValue value) {
		localVariables.put(name, value);
	}
	
	protected class WSLSetVariable implements IWSLCommandDefinition {
		
		private Pattern format = Pattern.compile("^([^\\s]+)(\\s+(.+)?)?$");
		
		public void execute (String arguments) {
			Matcher m = format.matcher(arguments);
			if (m.find())
			{
				String name = m.group(1);
				String value = m.group(3);
				if(value == null)
					value = " ";
				
				scriptDebug(1, "setVariable: " + name + "=" + value);
				setGlobalVariable(name, value);
			} else {
				scriptWarning("Invalid arguments to setvariable");
			}
		}
	}
	
	protected class WSLSetLocalVariable implements IWSLCommandDefinition {
		
		private Pattern format = Pattern.compile("^([^\\s]+)(\\s+(.+)?)?$");
		
		public void execute (String arguments) {
			Matcher m = format.matcher(arguments);
			if (m.find())
			{
				String name = m.group(1);
				String value = m.group(3);
				if(value == null)
					value = " ";
				
				scriptDebug(1, "setLocalVariable: " + name + "=" + value);
				setLocalVariable(name, value);
			} else {
				scriptError("Invalid arguments to setLocalVariable");
			}
		}
	}
	
	protected void gotoCommand(int line) {
		curCommand = commands.get(line);
		nextLine = line;
		while(curCommand == null) {
			nextLine++;
			if(nextLine >= commands.size())
				break;
			curCommand = commands.get(nextLine);
		}
		
		// if we're in an action, interrupt execution on the main thread
		if(Thread.currentThread() != scriptThread) {
			scriptCommands.interrupt();
		}
	}
	
	protected void gotoLabel (String label)
	{
		// remove ":" from labels
		int pos = label.indexOf(':');
		if(pos >= 0)
			label = label.substring(0, pos);
		
		Integer command = labels.get(label.toLowerCase());
		
		if (command != null) {
			gotoCommand(command);
		} else {
			command = labels.get("labelerror");
			if (command != null)
			{
				scriptDebug(1, "Label \"" + label + "\" does not exist, going to \"labelerror\"");
				gotoCommand(command);
			}
			else
			{
				scriptError("Label \"" + label + "\" does not exist");
			}
		}
	}
	
	protected class WSLGoto implements IWSLCommandDefinition {
		
		public void execute (String arguments) {
			if(arguments.length() > 0) {
				String[] args = arguments.split(argSeparator);
				String label = args[0];
				gotoLabel(label);
			} else {
				scriptError("Invalid arguments to goto");
			}
		}
	}
	
	protected void gosub (String label, String arguments)
	{
		String[] args = arguments.split(argSeparator);
		
		WSLFrame frame = new WSLFrame(nextLine, localVariables);
		callstack.push(frame);
		
		// TODO perhaps abstract this
		localVariables = (HashMap<String, IWSLValue>)localVariables.clone();
		setLocalVariable("0", arguments);
		for(int i = 0; i < args.length; i++) {
			setLocalVariable(String.valueOf(i + 1), args[i]);
		}
		
		Integer command = labels.get(label.toLowerCase());
		
		if (command != null)
		{
			gotoCommand(command);
		} else {
			scriptError("Invalid gosub statement, label \"" + label + "\" does not exist");
		}
	}
	
	protected class WSLGosub implements IWSLCommandDefinition {
		
		private Pattern format = Pattern.compile("^([^\\s]+)\\s*(.*)?$");
		
		public void execute (String arguments) {
			Matcher m = format.matcher(arguments);
			
			if (m.find()) {
				gosub(m.group(1), m.group(2));
			} else {
				scriptError("Invalid arguments to gosub");
			}
		}
	}
	
	protected void gosubReturn () {
		if (callstack.empty()) {
			scriptError("Invalid use of return, not in a subroutine");
		} else {
			WSLFrame frame = callstack.pop();
			frame.restore();
		}
	}
	
	protected class WSLReturn implements IWSLCommandDefinition {
		
		public void execute (String arguments) {
			gosubReturn();
		}
	}
	
	protected class WSLMatchWait implements IWSLCommandDefinition {
		
		public void execute (String arguments) throws InterruptedException {
			double time;
			
			if(arguments.length() > 0) {
				String[] args = arguments.split(argSeparator);
			
				try {
					time = Double.parseDouble(args[0]);
				} catch(NumberFormatException e) {
					scriptError("Non-numeral \"" + args[0] + "\" passed to matchwait");
					return;
				}
			} else {
				time = 0;
			}
			
			try {
				// Remove matchQueue before going into the wait so we don't end up trashing another wait's queue.
				BlockingQueue<String> myQueue = matchQueue;
				matchQueue = null;
				IMatch match = scriptCommands.matchWait(matches, myQueue, time);

				if (match != null)
				{
					matchData.get(match).run();
				}
			} finally {
				matches.clear();
				matchData.clear();
			}
		}
	}

	private class WSLTextMatchData implements Runnable {
		
		private String label;
		
		public WSLTextMatchData(String label) {
			this.label = label;
		}
		
		public void run() {
			gotoLabel(label);
		}
	}
	
	private class WSLRegexMatchData implements Runnable {
		
		private String label;
		private RegexMatch match;
		
		public WSLRegexMatchData(String label, RegexMatch match) {
			this.label = label;
			this.match = match;
		}
		
		public void run() {
			setVariablesFromMatch(match);
			gotoLabel(label);
		}
	}
	
	protected class WSLMatchRe implements IWSLCommandDefinition {
		
		private Pattern format = Pattern.compile("^([^\\s]+)\\s+/(.*)/(\\w*)");
		
		public void execute (String arguments) {
			Matcher m = format.matcher(arguments);
			
			if (m.find())
			{
				String regex = m.group(2);
				boolean caseInsensitive = m.group(3).contains("i");
				RegexMatch match = new RegexMatch(regex, caseInsensitive);
				
				matches.add(match);
				matchData.put(match, new WSLRegexMatchData(m.group(1), match));
				if(matchQueue == null) {
					matchQueue = scriptCommands.createLineQueue();
				}
			} else {
				scriptError("Invalid arguments to matchre");
			}
		}

	}
	
	public void setVariablesFromMatch(RegexMatch match) {
		int i = 0;
		for(String var : match.groups()) {
			setLocalVariable(String.valueOf(i), var);
			i++;
		}
	}

	protected class WSLMatch implements IWSLCommandDefinition {
		
		private Pattern format = Pattern.compile("^([^\\s]+)\\s+(.*)$");
		
		public void execute (String arguments) {
			Matcher m = format.matcher(arguments);
			
			if (m.find())
			{
				IMatch match = new TextMatch(m.group(2));
				matches.add(match);
				matchData.put(match, new WSLTextMatchData(m.group(1)));
				if(matchQueue == null) {
					matchQueue = scriptCommands.createLineQueue();
				}
			} else {
				scriptError("Invalid arguments to match");
			}
		}
	}
	
	protected class WSLCounter implements IWSLCommandDefinition {
		
		public void execute (String arguments) {
			if (arguments.length() == 0) {
				scriptError("You must provide an argument to counter");
				return;
			}
			
			doMath("c", arguments);
			
		}
	}
	
	protected class WSLMath implements IWSLCommandDefinition {

		public void execute (String arguments) {
			String[] args = arguments.split(argSeparator, 2);
			if (args.length < 2) {
				scriptError("Not enough arguments to math");
				return;
			}

			doMath(args[0], args[1]);

		}
	}
	
	private void doMath(String targetVar, String arguments) {
		String[] args = arguments.split(argSeparator);
		if (args.length < 1) {
			scriptError("No operator for math");
			return;
		}

		String operator = args[0].trim().toLowerCase();
		
		double operand;
		if (args.length > 1) {
			try {
				operand = Double.parseDouble(args[1].trim());
			} catch (NumberFormatException e) {
				scriptError("Operand must be a number");
				return;
			}
		} else
				operand = 1;

		if ("set".equalsIgnoreCase(operator))
		{
			setGlobalVariable(targetVar, new WSLNumber(operand));
			return;
		}
		
		double value;
		if(variableExists(targetVar)) {
			try {
				value = getVariable(targetVar).toDouble();
			} catch(NumberFormatException e) {
				scriptError("The variable \"" + targetVar + "\" must be a number to do math with it");
				return;
			}
		} else
				value = 0;


		if ("add".equalsIgnoreCase(operator))
		{	
			double newValue = value + operand;
			setGlobalVariable(targetVar, new WSLNumber(newValue));
		}
		else if ("subtract".equalsIgnoreCase(operator))
		{
			double newValue = value - operand;
			setGlobalVariable(targetVar, new WSLNumber(newValue));
		}
		else if ("multiply".equalsIgnoreCase(operator))
		{
			double newValue = value * operand;
			setGlobalVariable(targetVar, new WSLNumber(newValue));
		}
		else if ("divide".equalsIgnoreCase(operator))
		{
			if (operand == 0) {
				scriptError("Cannot divide by zero");
				return;
			}
			double newValue = value / operand;
			setGlobalVariable(targetVar, new WSLNumber(newValue));
		}
		else if ("modulus".equalsIgnoreCase(operator))
		{
			double newValue = value % operand;
			setGlobalVariable(targetVar, new WSLNumber(newValue));
		}
		else
		{
			scriptError("Unrecognized math command \"" + operator + "\"");
		}
	}

	protected class WSLWaitForRe implements IWSLCommandDefinition {

		private Pattern format = Pattern.compile("^/([^/]*)/(\\w*)");

		public void execute (String arguments) throws InterruptedException {
			Matcher m = format.matcher(arguments);
			
			if (m.find())
			{
				String flags = m.group(2);
				boolean ignoreCase = false;
				
				if (flags != null && flags.contains("i"))
				{
					ignoreCase = true;
				}
				
				RegexMatch match = new RegexMatch(m.group(1), ignoreCase);
				
				scriptCommands.waitFor(match);
				setVariablesFromMatch(match);
			} else {
				scriptError("Invalid arguments to waitforre");
			}
		}
	}
	
	protected class WSLWaitFor implements IWSLCommandDefinition {
		
		public void execute (String arguments) throws InterruptedException {
			if (arguments.length() >= 1)
			{
				IMatch match = new TextMatch(arguments);
				scriptCommands.waitFor(match);
				
			} else {
				scriptError("Invalid arguments to waitfor");
			}
		}
	}

	protected class WSLWait implements IWSLCommandDefinition {
		
		public void execute (String arguments) throws InterruptedException {
			scriptCommands.waitForPrompt();
		}
	}
	
	protected class WSLPut implements IWSLCommandDefinition {
		
		public void execute(String arguments) throws InterruptedException {
			scriptCommands.put(arguments);
			// Quit this script if we're starting another one
			if(arguments.startsWith(ScriptConfiguration.instance().getScriptPrefix())) {
				stop();
			}
		}
	}
	
	protected class WSLPlaySound implements IWSLCommandDefinition {
		public void execute(String arguments) throws InterruptedException {
			
			File file = new File(arguments);
			if (file.exists()) {
				try {
					scriptCommands.playSound(new FileInputStream(file));
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				scriptError("Sound file \"" + file + "\" not found.");
				return;
			}
		}
	}
	
	protected class WSLRun implements IWSLCommandDefinition {
		
		public void execute(String arguments) throws InterruptedException {
			sfClient.runScript(arguments);
		}
	}
	
	protected class WSLEcho implements IWSLCommandDefinition {
		
		public void execute (String arguments)
		{
			scriptCommands.echo(arguments);
		}
	}
	
	protected class WSLPause implements IWSLCommandDefinition {
		
		public void execute (String arguments) throws InterruptedException
		{
			double time;
			
			if(arguments.length() > 0) {
				String[] args = arguments.split(argSeparator);
			
				try {
					time = Double.parseDouble(args[0]);
				} catch(NumberFormatException e) {
					scriptError("Non-numeral \"" + args[0] + "\" passed to pause");
					return;
				}
			} else {
				time = 1;
			}
			scriptCommands.pause(time);
		}
	}
	
	protected class WSLMove implements IWSLCommandDefinition {
		
		public void execute (String arguments) throws InterruptedException
		{
			scriptCommands.move(arguments);
		}
	}
	
	protected class WSLNextRoom implements IWSLCommandDefinition {
		
		public void execute (String arguments) throws InterruptedException
		{
			scriptCommands.waitNextRoom();
		}
	}
	
	protected class WSLExit implements IWSLCommandDefinition {
		
		public void execute (String arguments) {
			// TODO figure out if we should make this call here or elsewhere
			stop();
		}
	}
	
	protected class WSLIf_ implements IWSLCommandDefinition {
		private String variableName;
		public WSLIf_ (String variableName)
		{
			this.variableName = variableName;
		}
		
		public void execute (String arguments) throws InterruptedException {
			if (variableExists(variableName) && getVariable(variableName).toString().length() > 0)
			{
				WSLScript.this.execute(arguments);
			}
		}
	}
	
	private class WSLRandom implements IWSLCommandDefinition {
		
		private Pattern format = Pattern.compile("^(\\d+)\\s+(\\d+)");
		
		public void execute(String arguments) {
			Matcher m = format.matcher(arguments);
			
			if(m.find()) {
				int min = Integer.parseInt(m.group(1));
				int max = Integer.parseInt(m.group(2));
				int r = min + new Random().nextInt(max - min + 1);
				
				setSpecialVariable("r", Integer.toString(r));
			} else {
				scriptError("Invalid arguments to random");
			}
		}
	}
	
	private class WSLTimer implements IWSLCommandDefinition {
		
		private Pattern format = Pattern.compile("^(\\w+)(\\s+([^\\s]+))?");
		
		public void execute(String arguments) {
			Matcher m = format.matcher(arguments);
			
			if(m.find()) {
				String command = m.group(1);
				String timerName = m.group(3);
				if(timerName == null)
					timerName = "t";
				IWSLValue var = getVariable(timerName);
				if(var instanceof ScriptTimer || var == null) {
					ScriptTimer timer = (ScriptTimer)var;
					if(command.equals("start")) {
						if(timer == null) {
							timer = new ScriptTimer();
							setSpecialVariable(timerName, timer);
						}
						timer.start();
					} else if(command.equals("stop")) {
						if(timer == null) {
							scriptWarning("Timer \"" + timerName + "\" undefined.");
						} else {
							timer.stop();
						}
					} else if(command.equals("clear")) {
						if(timer == null) {
							scriptWarning("Timer \"" + timerName + "\" undefined.");
						} else {
							timer.clear();
						}
					} else {
						scriptError("Invalid command \"" + command + "\" given to timer");
					}
				} else {
					scriptError("Variable \"" + timerName + "\" is not a timer.");
				}
			} else {
				scriptError("Invalid arguments to timer");
			}
		}
	}
	
	protected class WSLAddHighlightString implements IWSLCommandDefinition {
		
		private Pattern format = Pattern.compile("^\"([^\"])\"(\\s*(.*))?");
		private Pattern optionFormat = Pattern.compile("(\\w+)=(.*)");
		
		public void execute (String arguments)
		{
			Matcher m = format.matcher(arguments);
			if(m.find()) {
				String text = m.group(1);
				String optionString = m.group(3);
				String[] options = optionString.split(argSeparator);
				
				WarlockStyle style = new WarlockStyle();
				for(String option : options) {
					Matcher optionMatcher = optionFormat.matcher(option);
					if(optionMatcher.find()) {
						String key = optionMatcher.group(1);
						String value = optionMatcher.group(2);
						
						if(key.equalsIgnoreCase("forecolor")) {
							
						}
					}
				}
			}
		}
	}
	
	public void setLastCondition(boolean condition) {
		this.lastCondition = condition;
	}
	
	private class WSLElse implements IWSLCommandDefinition {
		
		public void execute (String arguments) throws InterruptedException {
			if (!lastCondition)
			{
				WSLScript.this.execute(arguments);
			}
		}
	}
	
	public IScriptEngine getScriptEngine() {
		return engine;
	}
	
	public IScriptCommands getCommands() {
		return scriptCommands;
	}
	
	protected void echo(String message) {
		super.echo(message);
	}
}
