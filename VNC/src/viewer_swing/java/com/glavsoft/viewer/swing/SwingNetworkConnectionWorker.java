// Copyright (C) 2010, 2011, 2012, 2013 GlavSoft LLC.
// All rights reserved.
//
//-------------------------------------------------------------------------
// This file is part of the TightVNC software.  Please visit our Web site:
//
//                       http://www.tightvnc.com/
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//-------------------------------------------------------------------------
//

package com.glavsoft.viewer.swing;

import com.glavsoft.viewer.CancelConnectionException;
import com.glavsoft.viewer.ConnectionErrorException;
import com.glavsoft.viewer.ConnectionPresenter;
import com.glavsoft.viewer.NetworkConnectionWorker;

import javax.net.ssl.SSLSocketFactory;
import javax.swing.*;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.AccessControlException;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;


public class SwingNetworkConnectionWorker extends SwingWorker<Socket, String> implements NetworkConnectionWorker {
    public static final int MAX_HOSTNAME_LENGTH_FOR_MESSAGES = 40;
    private final JFrame parentWindow;
    private ConnectionParams connectionParams;
    private ConnectionPresenter presenter;


    public SwingNetworkConnectionWorker(JFrame parentWindow) {
        this.parentWindow = parentWindow;
    }

    @Override
    public Socket doInBackground() throws Exception {
        String s = "<b>" +connectionParams.hostName + "</b>:" + connectionParams.getPortNumber();

        String message = "<html>Trying to connect to " + s + "</html>";
        publish(message);

        int port = connectionParams.getPortNumber();
        String host = connectionParams.hostName;
		boolean useSSL = connectionParams.getUseSSL();

        message = "Connecting to host " + host + ":" + port + (useSSL ? " (SSL)" : "");
        publish(message);

        if(useSSL)
        	return SSLSocketFactory.getDefault().createSocket(host, port);
		else
			return new Socket(host, port);
    }

    private String formatHostString(String hostName) {
        if (hostName.length() <= MAX_HOSTNAME_LENGTH_FOR_MESSAGES) {
            return  hostName;
        } else {
            return hostName.substring(0, MAX_HOSTNAME_LENGTH_FOR_MESSAGES) + "...";
        }
    }

    @Override
    protected void process(List<String> strings) { // EDT
        String message = strings.get(strings.size() - 1); // get last
        presenter.showMessage(message);
    }

    @Override
    protected void done() { // EDT
        try {
            final Socket socket = get();
            presenter.successfulNetworkConnection(socket);
        } catch (CancellationException e) {
            presenter.showMessage("Cancelled");
            presenter.connectionFailed();
        } catch (InterruptedException e) {
            presenter.showMessage("Interrupted");
            presenter.connectionFailed();
        } catch (ExecutionException e) {
            String errorMessage = null;
            try {
                throw e.getCause();
            } catch (UnknownHostException uhe) {
                errorMessage = "Unknown host: '" + formatHostString(connectionParams.hostName) + "'";
            } catch (IOException ioe) {
                errorMessage = "Couldn't connect to '" + formatHostString(connectionParams.hostName) +
                        ":" + connectionParams.getPortNumber() + "':\n" + ioe.getMessage();
            } catch (CancelConnectionException cce) {

            } catch (AccessControlException ace) {
                errorMessage = "Access control error";
            } catch (ConnectionErrorException cee) {
                errorMessage = cee.getMessage() + "\nHost: " +
                    formatHostString(connectionParams.hostName) + ":" + connectionParams.getPortNumber();
            } catch (Throwable throwable) {
                errorMessage = "Couldn't connect to '" + formatHostString(connectionParams.hostName) +
                        ":" + connectionParams.getPortNumber() + "':\n" + throwable.getMessage();
            }
            presenter.showConnectionErrorDialog(errorMessage);
            presenter.clearMessage();
            presenter.connectionFailed();
        }
    }

    @Override
    public void setConnectionParams(ConnectionParams connectionParams) {
        this.connectionParams = connectionParams;
    }

    @Override
    public void setPresenter(ConnectionPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public boolean cancel() {
        return super.cancel(true);
    }
}
