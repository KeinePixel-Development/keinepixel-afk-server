package eu.koboo.minestom.server;

import eu.koboo.minestom.api.server.Server;
import eu.koboo.minestom.api.config.ServerConfig;
import eu.koboo.minestom.commands.CommandStop;
import eu.koboo.minestom.commands.CommandVersion;
import eu.koboo.minestom.config.ConfigLoader;
import eu.koboo.minestom.console.Console;

import java.nio.file.Paths;
import java.util.List;
import lombok.Getter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.AsyncPlayerPreLoginEvent;
import net.minestom.server.extras.MojangAuth;
import net.minestom.server.extras.bungee.BungeeCordProxy;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.instance.*;
import net.minestom.server.instance.batch.ChunkBatch;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.tinylog.Logger;

public class ServerImpl extends Server {

    @Getter
    private static ServerImpl instance;

    private final ServerConfig serverConfig;

    @Getter
    private final Console console;

    public ServerImpl() {
        long startTime = System.nanoTime();

        instance = this;

        Logger.info("Loading settings..");
        serverConfig = ConfigLoader.loadConfig();

        Logger.info("Initializing console..");
        console = new Console();

        Logger.info("Initializing server..");
        MinecraftServer minecraftServer = MinecraftServer.init();

        MinecraftServer.getExceptionManager()
                .setExceptionHandler(exc -> Logger.error("An unexpected error occurred! ", exc));

        Logger.info("Registering commands..");
        MinecraftServer.getCommandManager().register(new CommandStop());
        MinecraftServer.getCommandManager().register(new CommandVersion());

        Logger.info("Creating instance..");
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();
        AnvilLoader anvilLoader = new AnvilLoader(Paths.get("afk-lobby"));
        anvilLoader.saveInstance(instanceContainer);

        GlobalEventHandler eventHandler = MinecraftServer.getGlobalEventHandler();
        eventHandler.addListener(AsyncPlayerPreLoginEvent.class, event -> {
            Logger.info("Player " + event.getUsername() + " is trying to join..");
            event.getPlayer().setInstance(instanceContainer, new Pos(0, 90, 0));
        });

        String host = serverConfig.host();
        int port = serverConfig.port();

        MinecraftServer.setBrandName(this.getName());
        MinecraftServer.setDifficulty(serverConfig.difficulty());

        MinecraftServer.setCompressionThreshold(serverConfig.compressionThreshold());

        setViewDistance("minestom.chunk-view-distance", serverConfig.chunkViewDistance());
        setViewDistance("minestom.entity-view-distance", serverConfig.entityViewDistance());

        switch (serverConfig.proxyMode()) {
            case NONE -> {
                if (serverConfig.onlineMode()) {
                    MojangAuth.init();
                    Logger.info("ProxyMode 'NONE', enabled MojangAuth.");
                } else {
                    Logger.info("ProxyMode 'NONE', without MojangAuth.");
                }
            }
            case VELOCITY -> {
                if (serverConfig.velocitySecret() == null || serverConfig.velocitySecret().equalsIgnoreCase("")) {
                    Logger.warn("ProxyMode 'VELOCITY' selected, but no proxy-secret set! Abort!");
                    System.exit(0);
                    break;
                }
                VelocityProxy.enable(serverConfig.velocitySecret());
                Logger.info("ProxyMode 'VELOCITY', enabled VelocityProxy.");
            }
            case BUNGEECORD -> {
                BungeeCordProxy.enable();
                Logger.info("ProxyMode 'BUNGEECORD', enabled BungeeCordProxy.");
            }
        }
        Logger.info("Starting @ " + host + ":" + port);

        minecraftServer.start(host, port);
        Logger.info("Listening on " + host + ":" + port);

        console.start();

        double timeToStartInMillis = (double) (System.nanoTime() - startTime) / 1000000000;
        timeToStartInMillis *= 100;
        timeToStartInMillis = Math.round(timeToStartInMillis);
        timeToStartInMillis /= 100;
        Logger.info("Started in " + timeToStartInMillis + "s!");
    }

    @Override
    public ServerConfig getServerConfig() {
        return serverConfig;
    }

    @Override
    public String getName() {
        return ProjectVariables.NAME;
    }

    @Override
    public String getVersion() {
        return ProjectVariables.VERSION;
    }

    @Override
    public String getMinestomVersion() {
        return ProjectVariables.MINESTOM_VERSION;
    }

    private void setViewDistance(String key, int value) {
        if(System.getProperty(key) != null) {
            return;
        }
        System.setProperty(key, Integer.valueOf(value).toString());
    }
}
