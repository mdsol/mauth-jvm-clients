package com.mdsol.mauth.proxy;

import com.mdsol.mauth.apache.HttpClientRequestSigner;
import com.mdsol.mauth.util.CurrentEpochTimeProvider;

import com.typesafe.config.ConfigFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.littleshoot.proxy.HttpProxyServer;
import org.littleshoot.proxy.extras.SelfSignedMitmManager;
import org.littleshoot.proxy.impl.DefaultHttpProxyServer;

import java.net.InetSocketAddress;

public class ProxyServer {
    private final ProxyConfig proxyConfig;

    private final HttpClientRequestSigner httpClientRequestSigner;
    private HttpProxyServer httpProxyServer;

    ProxyServer(ProxyConfig proxyConfig) {
        this.proxyConfig = proxyConfig;

        httpClientRequestSigner = new HttpClientRequestSigner(
                proxyConfig.getAppUuid(),
                proxyConfig.getPrivateKey(),
                new CurrentEpochTimeProvider(),
                proxyConfig.isV2OnlySignRequests()
        );
    }

    void serve() {
        httpProxyServer =
                DefaultHttpProxyServer.bootstrap()
                        .withMaxInitialLineLength(16384)
                        .withMaxHeaderSize(65536)
                        .withManInTheMiddle(new SelfSignedMitmManager())
                        .withPort(proxyConfig.getProxyPort())
                        .withAddress(new InetSocketAddress("0.0.0.0", proxyConfig.getProxyPort()))
                        .withFiltersSource(new HttpFiltersSourceAdapter() {
                            @Override
                            public int getMaximumRequestBufferSizeInBytes() {
                                return proxyConfig.getBufferSizeInByes();
                            }

                            public HttpFilters filterRequest(HttpRequest originalRequest, ChannelHandlerContext clientCtx) {
                                return new MAuthForwardRequestFilter(originalRequest, httpClientRequestSigner);
                            }
                        })
                        .start();
    }

    void stop() {
        if (httpProxyServer != null) {
            httpProxyServer.stop();
        }
    }

    int getPort() {
        return httpProxyServer.getListenAddress().getPort();
    }

    public static void main(String[] args) {
        new ProxyServer(new ProxyConfig(ConfigFactory.load().resolve())).serve();
    }
}
