/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package example.udt.echo.messages;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.UdtChannel;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioUdtProvider;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.logging.InternalLoggerFactory;
import io.netty.logging.Slf4JLoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yammer.metrics.reporting.ConsoleReporter;

import example.udt.util.ThreadFactoryUDT;

/**
 * UDT STREAM client
 * <p>
 * Sends one message when a connection is open and echoes back any received data
 * to the server. Simply put, the echo client initiates the ping-pong traffic
 * between the echo client and server by sending the first message to the
 * server.
 */
public class MsgEchoClient {

    static Logger log = LoggerFactory.getLogger(MsgEchoClient.class);

    /**
     * use slf4j provider for io.netty.logging.InternalLogger
     */
    static {

        final InternalLoggerFactory defaultFactory = new Slf4JLoggerFactory();

        InternalLoggerFactory.setDefaultFactory(defaultFactory);

        log.info("InternalLoggerFactory={}", InternalLoggerFactory
                .getDefaultFactory().getClass().getName());

    }

    private final String host;
    private final int port;
    private final int messageSize;

    public MsgEchoClient(final String host, final int port,
            final int messageSize) {
        this.host = host;
        this.port = port;
        this.messageSize = messageSize;
    }

    public void run() throws Exception {

        // Configure the client.

        final Bootstrap boot = new Bootstrap();

        final ThreadFactory connectFactory = new ThreadFactoryUDT("connect");

        final NioEventLoopGroup connectGroup = new NioEventLoopGroup(//
                1, connectFactory, NioUdtProvider.MESSAGE_PROVIDER);

        try {

            boot.group(connectGroup)
                    .channelFactory(NioUdtProvider.MESSAGE_CONNECTOR)
                    .localAddress("localhost", 0)
                    .remoteAddress(new InetSocketAddress(host, port))
                    .handler(new ChannelInitializer<UdtChannel>() {
                        @Override
                        public void initChannel(final UdtChannel ch)
                                throws Exception {
                            ch.pipeline().addLast(
                                    new LoggingHandler(LogLevel.INFO),
                                    new MsgEchoClientHandler(messageSize));
                        }
                    });

            // Start the client.
            final ChannelFuture f = boot.connect().sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();

        } finally {

            // Shut down the event loop to terminate all threads.
            boot.shutdown();

        }

    }

    public static void main(final String[] args) throws Exception {

        log.info("init");

        ConsoleReporter.enable(3, TimeUnit.SECONDS);

        final String host = "localhost";
        final int port = 1234;
        final int messageSize = 64 * 1024;

        new MsgEchoClient(host, port, messageSize).run();

        log.info("done");

    }

}