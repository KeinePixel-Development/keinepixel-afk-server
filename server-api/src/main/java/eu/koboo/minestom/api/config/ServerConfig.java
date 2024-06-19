package eu.koboo.minestom.api.config;


/**
 * Config object to represent the `server_config.yml` file
 */
public record ServerConfig(String host, int port, boolean onlineMode, Difficulty difficulty,
                           ProxyMode proxyMode, String velocitySecret,
                           int packetRateLimit, int maxPacketSize, int compressionThreshold,
                           int chunkViewDistance, int entityViewDistance) {

}
