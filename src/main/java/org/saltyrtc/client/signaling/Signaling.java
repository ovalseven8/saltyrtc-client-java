/*
 * Copyright (c) 2016 Threema GmbH / SaltyRTC Contributors
 *
 * Licensed under the Apache License, Version 2.0, <see LICENSE-APACHE file>
 * or the MIT license <see LICENSE-MIT file>, at your option. This file may not be
 * copied, modified, or distributed except according to those terms.
 */

package org.saltyrtc.client.signaling;

import org.java_websocket.WebSocketImpl;
import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
import org.saltyrtc.client.SaltyRTC;
import org.saltyrtc.client.cookie.CookiePair;
import org.saltyrtc.client.exceptions.ConnectionException;
import org.saltyrtc.client.keystore.AuthToken;
import org.saltyrtc.client.keystore.KeyStore;
import org.saltyrtc.client.nonce.CombinedSequence;
import org.saltyrtc.client.signaling.state.SignalingState;
import org.slf4j.Logger;
import org.webrtc.DataChannel;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import javax.net.ssl.SSLContext;

public abstract class Signaling {

    protected static String SALTYRTC_WS_SUBPROTOCOL = "saltyrtc-1.0";
    protected static int SALTYRTC_WS_CONNECT_TIMEOUT = 2000;
    protected static int SALTYRTC_ADDR_UNKNOWN = 0x00;
    protected static int SALTYRTC_ADDR_SERVER = 0x00;
    protected static int SALTYRTC_ADDR_INITIATOR = 0x01;

    // Logger
    protected abstract Logger getLogger();

    // WebSocket
    protected String host;
    protected int port;
    protected String protocol = "wss";
    protected WebSocketClient ws;
    protected SSLContext sslContext;

    // WebRTC / ORTC
    protected DataChannel dc;

    // Connection state
    public SignalingState state = SignalingState.NEW;
    public SignalingChannel channel = SignalingChannel.WEBSOCKET;

    // Reference to main class
    protected SaltyRTC saltyRTC;

    // Keys
    protected byte[] serverKey;
    protected KeyStore permanentKey;
    protected KeyStore sessionKey;
    protected AuthToken authToken;

    // Signaling
    protected int address = SALTYRTC_ADDR_UNKNOWN;
    protected CookiePair cookiePair;
    protected CombinedSequence serverCsn = new CombinedSequence();

    public Signaling(SaltyRTC saltyRTC, String host, int port,
                     KeyStore permanentKey, SSLContext sslContext) {
        this.saltyRTC = saltyRTC;
        this.host = host;
        this.port = port;
        this.permanentKey = permanentKey;
        this.sslContext = sslContext;
    }

    public byte[] getPublicPermanentKey() {
        return this.permanentKey.getPublicKey();
    }

    public byte[] getAuthToken() {
        return this.authToken.getAuthToken();
    }

    /**
     * Connect to the SaltyRTC server.
     */
    public FutureTask<Void> connect() {
        return new FutureTask<>(
            new Callable<Void>() {
                @Override
                public Void call() throws InterruptedException, ConnectionException {
                    resetConnection();
                    initWebsocket();
                    if (!connectWebsocket()) {
                        throw new ConnectionException("Connecting to server failed");
                    }
                    return null;
                }
            }
        );
    }

    /**
     * Reset / close the connection.
     *
     * - Close WebSocket if still open.
     * - Set `ws` attribute to null.
     * - Set `state` attribute to `NEW`
     * - Reset server CSN
     */
    protected void resetConnection() throws InterruptedException {
        this.state = SignalingState.NEW;
        this.serverCsn = new CombinedSequence();

        // Close websocket instance
        if (this.ws != null) {
            getLogger().debug("Disconnecting WebSocket");
            this.ws.closeBlocking();
            this.ws = null;
        }
    }

    /**
     * Return the WebSocket path.
     */
    protected abstract String getWebsocketPath();

    /**
     * Initialize the WebSocket including TLS configuration.
     */
    private void initWebsocket() {
        // Build connection URL
        final String baseUrl = this.protocol + "://" + this.host + ":" + this.port + "/";
        final URI uri = URI.create(baseUrl + this.getWebsocketPath());

        // Set debug mode
        WebSocketImpl.DEBUG = this.saltyRTC.getDebug();

        // Create WebSocket client instance
        final Map<String, String> headers = new HashMap<>();
        headers.put("Sec-WebSocket-Protocol", SALTYRTC_WS_SUBPROTOCOL);
        this.ws = new WebSocketClient(uri, new Draft_17(), headers, SALTYRTC_WS_CONNECT_TIMEOUT) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                getLogger().debug("Connection opened");
            }

            @Override
            public void onMessage(String message) {
                getLogger().debug("New string message: " + message);
            }

            @Override
            public void onMessage(ByteBuffer bytes) {
                getLogger().debug("New bytes message (" + bytes.array().length + " bytes)");
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                getLogger().debug("Connection closed with code " + code + ": " + reason);
                state = SignalingState.CLOSED; // TODO don't set this on handover
            }

            @Override
            public void onError(Exception ex) {
                getLogger().error("A WebSocket error occured: " + ex.getMessage());
                ex.printStackTrace();
            }
        };

        // Set up TLS
        this.ws.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(this.sslContext));

        getLogger().debug("Initialize WebSocket connection to " + uri);
    }

    /**
     * Connect to WebSocket.
     *
     * @return boolean indicating whether connecting succeeded or not.
     */
    private boolean connectWebsocket() throws InterruptedException {
        Signaling.this.state = SignalingState.WS_CONNECTING;
        final boolean connected = Signaling.this.ws.connectBlocking();
        if (connected) {
            Signaling.this.state = SignalingState.SERVER_HANDSHAKE;
        } else {
            Signaling.this.state = SignalingState.ERROR;
            Signaling.this.getLogger().error("Connecting to server failed");
        }
        return connected;
    }
}