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
package cc.warlock.core.client.settings.internal;

import java.util.Collection;
import java.util.HashMap;

import org.osgi.service.prefs.Preferences;

import cc.warlock.core.client.settings.IVariable;
import cc.warlock.core.client.settings.IVariableProvider;
import cc.warlock.core.configuration.WarlockSetting;

public class VariableConfigurationProvider extends WarlockSetting implements IVariableProvider {

	protected HashMap<String, IVariable> variables = new HashMap<String, IVariable>();
	
	public VariableConfigurationProvider (Preferences parentNode)
	{
		super(parentNode, "variables");
		
		try {
			for(String id : getNode().childrenNames()) {
				variables.put(id, new Variable(getNode(), id));
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public IVariable addVariable(IVariable variable) {
		return variables.put(variable.getIdentifier(), variable);
	}
	
	public IVariable addVariable(String identifier, String value) {
		return addVariable(new Variable(getNode(), identifier, value));
	}
	
	public void setVariable(String id, IVariable variable) {
		variables.put(id, variable);
	}

	public IVariable getVariable(String identifier) {
		return variables.get(identifier);
	}

	public Collection<IVariable> getVariables() {
		return variables.values();
	}

	public IVariable removeVariable(String identifier) {
		return variables.remove(identifier);
	}
}
