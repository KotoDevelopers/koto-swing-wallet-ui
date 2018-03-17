// Code was originally written by developer - https://github.com/zlatinb
// Taken from repository https://github.com/zlatinb/zcash-swing-wallet-ui under an MIT license
package com.vaklinov.zcashui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Locale;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import cash.koto.daemon.UsersMessageConsole;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.vaklinov.zcashui.ZCashClientCaller.WalletCallException;


public class StartupProgressDialog extends JFrame implements UsersMessageConsole{
    

    private static final int POLL_PERIOD = 1500;
    private static final int STARTUP_ERROR_CODE = -28;
    
    private BorderLayout borderLayout1 = new BorderLayout();
    private JLabel imageLabel = new JLabel();
    private JLabel progressLabel = new JLabel();
    private JPanel southPanel = new JPanel();
    private BorderLayout southPanelLayout = new BorderLayout();
    private JProgressBar progressBar = new JProgressBar();
    private ImageIcon imageIcon;
    
    private ResourceBundleUTF8 rb = ResourceBundleUTF8.getResourceBundle();

    private final ZCashClientCaller clientCaller;
    
    public StartupProgressDialog(ZCashClientCaller clientCaller, String text)
    {
        this.clientCaller = clientCaller;
        
        URL iconUrl = this.getClass().getClassLoader().getResource("images/koto-logo-color-large.png");
        imageIcon = new ImageIcon(iconUrl);
        imageLabel.setIcon(imageIcon);
        imageLabel.setBorder(BorderFactory.createEmptyBorder(16, 16, 0, 16));
        Container contentPane = getContentPane();
        contentPane.setLayout(borderLayout1);
        southPanel.setLayout(southPanelLayout);
        southPanel.setBorder(BorderFactory.createEmptyBorder(0, 16, 12, 16));
        contentPane.add(imageLabel, BorderLayout.NORTH);
		JLabel zcashWalletLabel = new JLabel(
			"<html><span style=\"font-weight:bold;font-family : 'Helvetica';font-size:3.4em\">" + 
            rb.S("Koto Wallet</span></html>"));
		zcashWalletLabel.setBorder(BorderFactory.createEmptyBorder(16, 16, 2, 16));
		// todo - place in a panel with flow center
		JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 1));
		tempPanel.add(zcashWalletLabel);
		contentPane.add(tempPanel, BorderLayout.CENTER);
        contentPane.add(southPanel, BorderLayout.SOUTH);
        progressBar.setIndeterminate(true);
        southPanel.add(progressBar, BorderLayout.NORTH);

        progressLabel.setText(rb.S("Starting..."));

        southPanel.add(progressLabel, BorderLayout.SOUTH);
        
        pack();
        setLocationRelativeTo(null);
        
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    public void waitFor(final Object flag) {
        try {
            synchronized (flag) {
                flag.wait();
            }
        } catch (InterruptedException e) {
            Log.warning(e.getMessage());
        }
    }
    
    public void waitForStartup() throws IOException,
        InterruptedException,WalletCallException,InvocationTargetException {
        
        // special handling of OSX app bundle
//        if (OSUtil.getOSType() == OS_TYPE.MAC_OS) {
//            ProvingKeyFetcher keyFetcher = new ProvingKeyFetcher();
//            keyFetcher.fetchIfMissing(this);
//            if ("true".equalsIgnoreCase(System.getProperty("launching.from.appbundle")))
//                performOSXBundleLaunch();
//        }
        if(clientCaller==null) {
            throw new IllegalStateException("called waitForStartup while caller not set");
        }

        Log.info("Splash: checking if kotod is already running...");
        boolean shouldStartZCashd = false;
        try {
            clientCaller.getDaemonRawRuntimeInfo();
        } catch (IOException e) { 
        	// Relying on a general exception may be unreliable
        	// may be thrown for an unexpected reason!!! - so message is checked
        	if (e.getMessage() != null && 
        		e.getMessage().toLowerCase(Locale.ROOT).contains("error: couldn't connect to server"))
        	{
        		shouldStartZCashd = true;
        	}
        }
        
        if (!shouldStartZCashd) {
        	Log.info("Splash: kotod already running...");
            // What if started by hand but taking long to initialize???
//            doDispose();
//            return;
        } else {
        	Log.info("Splash: kotod will be started...");
        }
        
        final Process daemonProcess = shouldStartZCashd ? clientCaller.startDaemon() : null;
        
        Thread.sleep(POLL_PERIOD); // just a little extra

        if(!isAlive(daemonProcess)) {
            int exitCode = daemonProcess.exitValue();
            Log.warning("exit code for daemon present: "+exitCode);
            OSUtil.printStreamToLog(daemonProcess.getErrorStream());
            OSUtil.printStreamToLog(daemonProcess.getInputStream());
            return;
        }
        
        int iteration = 0;
        while(true) {
        	iteration++;
            Thread.sleep(POLL_PERIOD);
            
            JsonObject info = null;
            
            try
            {
            	info = clientCaller.getDaemonRawRuntimeInfo();
            } catch (IOException e)
            {
            	if (iteration > 4)
            	{
            		throw e;
            	} else
            	{
            		continue;
            	}
            }
            
            JsonValue code = info.get("code");
            if (code == null || (code.asInt() != STARTUP_ERROR_CODE)) {
                break;
            }
            final String message = info.getString("message", "???");
            setProgressText(message);
            
        }

        // doDispose(); - will be called later by the main GUI
        
        if (daemonProcess != null) // Shutdown only if we started it
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
            	Log.info("Stopping kotod because we started it - now it is alive: " + 
                		           StartupProgressDialog.this.isAlive(daemonProcess));
                try 
                {
                    clientCaller.stopDaemon();
	                long start = System.currentTimeMillis();
	                
	                while (!StartupProgressDialog.this.waitFor(daemonProcess, 3000))
	                {
	                	long end = System.currentTimeMillis();
	                	Log.info("Waiting for " + ((end - start) / 1000) + " seconds for kotod to exit...");
	                	
	                	if (end - start > 10 * 1000)
	                	{
	                		clientCaller.stopDaemon();
	                		daemonProcess.destroy();
	                	}
	                	
	                	if (end - start > 1 * 60 * 1000)
	                	{
	                		break;
	                	}
	                }
	            
	                if (StartupProgressDialog.this.isAlive(daemonProcess)) {
	                	Log.info("kotod is still alive although we tried to stop it. " +
	                                           "Hopefully it will stop later!");
	                        //System.out.println("zcashd is still alive, killing forcefully");
	                        //daemonProcess.destroyForcibly();
                    } else {
                        Log.info("kotod shut down successfully");
                    }
                } catch (Exception bad) {
                	Log.error("Couldn't stop kotod!", bad);
                }
            }
        });
        
    }
    
    public void doDispose() {
        SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				setVisible(false);
				dispose();
			}
		});
    }
    
    public void setProgressText(final String text) {
        SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				progressLabel.setText(text);
			}
	         });
    }

    @Override
    public void showMessage(String text) {
        setProgressText(text);
    }

    // TODO: Unused for now
    private void performOSXBundleLaunch() throws IOException, InterruptedException {
    	Log.info("performing OSX Bundle-specific launch");
        File bundlePath = new File(System.getProperty("zcash.location.dir"));
        bundlePath = bundlePath.getCanonicalFile();
        
        // run "first-run.sh"
        File firstRun = new File(bundlePath,"first-run.sh");
        Process firstRunProcess = Runtime.getRuntime().exec(firstRun.getCanonicalPath());
        firstRunProcess.waitFor();
    }
    
    
    // Custom code - to allow JDK7 compilation.
    public boolean isAlive(Process p) 
    {
    	if (p == null)
    	{
    		return false;
    	}
    	
        try 
        {
            int val = p.exitValue();
            
            return false;
        } catch (IllegalThreadStateException itse) 
        {
            return true;
        }
    }
    
    
    // Custom code - to allow JDK7 compilation.
    public boolean waitFor(Process p, long interval)
    {
		synchronized (this) 
		{
			long startWait = System.currentTimeMillis();
			long endWait = startWait;
			do
			{
				boolean ended = !isAlive(p);
				
				if (ended)
				{
					return true; // End here
				}
				
				try
				{
					this.wait(100);
				} catch (InterruptedException ie)
				{
					// One of the rare cases where we do nothing
					Log.error("Unexpected error: ", ie);
				}
				
				endWait = System.currentTimeMillis();
			} while ((endWait - startWait) <= interval);
		}
		
		return false;
    }
}
