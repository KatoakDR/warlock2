/*
 * Created on Dec 31, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package cc.warlock.rcp.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import cc.warlock.configuration.Account;
import cc.warlock.configuration.SavedProfiles;
import cc.warlock.network.SGEConnection;
import cc.warlock.network.SGEConnectionListener;
import cc.warlock.rcp.ui.ComboField;
import cc.warlock.rcp.ui.TextField;
import cc.warlock.rcp.ui.WarlockSharedImages;
import cc.warlock.rcp.ui.network.SWTSGEConnectionListenerAdapter;

/**
 * @author Marshall
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AccountWizardPage extends WizardPageWithNotification {

	private SGEConnection connection;
	private ComboField account;
	private TextField password;
	private Listener listener;
	private Account savedAccount; 
	
	public AccountWizardPage (SGEConnection connection)
	{
		super ("Account Information", "Please enter your account information.", WarlockSharedImages.getImageDescriptor(WarlockSharedImages.IMG_WIZBAN_WARLOCK));
		
		this.connection = connection;
		listener = new Listener();
		connection.addSGEConnectionListener(new SWTSGEConnectionListenerAdapter(listener));
		connection.connect();
	}
	
	public void createControl(Composite parent) {
	
		Composite controls = new Composite(parent, SWT.NONE);
		controls.setLayout(new GridLayout(1, false));
		
		new Label(controls, SWT.NONE).setText("Account Name");
		account = new ComboField(controls, SWT.BORDER | SWT.DROP_DOWN);
		account.getCombo().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		new Label(controls, SWT.NONE).setText("Password");
		password = new TextField(controls, SWT.BORDER);
		password.getTextControl().setEchoChar('*');
		password.getTextControl().setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		setControl(controls);
		
		final Collection<Account> accounts = SavedProfiles.getAccounts();
		for (Account account : accounts) {
			this.account.getCombo().add(account.getAccountName());
		}
		if (accounts.size() > 0)
		{
			account.getCombo().select(0);
			password.getTextControl().setText(SavedProfiles.getAccount(account.getCombo().getText()).getPassword());
			
			account.getCombo().addSelectionListener(new SelectionListener() {
				public void widgetDefaultSelected(SelectionEvent e) {
					widgetSelected(e);
				}
				public void widgetSelected(SelectionEvent e) {
					String accountName = account.getCombo().getText();
					password.getTextControl().setText(SavedProfiles.getAccount(accountName).getPassword());
				}
			});
		}
	}
	
	@Override
	public void pageExited(int button) {
		if (button == WizardWithNotification.NEXT)
		{
			savedAccount = SavedProfiles.getAccount(account.getText());
			if (savedAccount == null)
			{
				boolean save = MessageDialog.openQuestion(Display.getDefault().getActiveShell(),
					"Save Account?", "Would you like to save this account information?");
				
				if (save)
				{
					savedAccount = SavedProfiles.addAccount(account.getText(), password.getText());
				}
			}
			
			try {
				getContainer().run(true, true, new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException
					{
						listener.setProgressMonitor(monitor);
						
						monitor.beginTask("Authorizing account \"" + account.getText() + "\"...", 3);
						connection.login(account.getText(), password.getText());
						monitor.worked(1);
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private class Listener extends SGEConnectionListener
	{
		private IProgressMonitor monitor;
		public void setProgressMonitor(IProgressMonitor monitor)
		{
			this.monitor = monitor;
		}
		
		public void loginFinished(SGEConnection connection, int status) {
			if (monitor != null)
			{
				monitor.worked(1);
			}
		}
		
		public void gamesReady(SGEConnection connection, Map games) {
			if (monitor != null)
			{
				monitor.worked(1);
				monitor.done();
			}
		}
	}
	
	protected Text createTextWithLabel (Composite parent, String label)
	{
		new Label(parent, SWT.NONE).setText(label);
		return  new Text(parent, SWT.BORDER);
	}
	
	public Account getSavedAccount ()
	{
		return savedAccount;
	}
}
