package org.hackley.plugin.config;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import dev.arbjerg.lavalink.api.AudioPlayerManagerConfiguration;
import org.hackley.plugin.sources.tidal.TidalSourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class Plugin implements AudioPlayerManagerConfiguration {

    private static final Logger log = LoggerFactory.getLogger(Plugin.class);

    private final ConfigPlugin pluginConfig;
    private final ConfigSources sourcesConfig;
    private final ConfigTidal tidalConfig;

    public Plugin(ConfigPlugin pluginConfig, ConfigSources sourcesConfig, ConfigTidal tidalConfig) {
        log.info("Loading LavaTidal plugin...");
        this.pluginConfig = pluginConfig;
        this.sourcesConfig = sourcesConfig;
        this.tidalConfig = tidalConfig;
    }

    @Override
    public AudioPlayerManager configure(AudioPlayerManager manager) {
        if (this.sourcesConfig.isTidal()) {
            log.info("Registering Tidal audio source manager");
            var tidalSourceManager = new TidalSourceManager(this.pluginConfig.getProviders(), this.tidalConfig.getTidalToken(), this.tidalConfig.getCountryCode(), this.tidalConfig.getTracksSearchLimit(), this.tidalConfig.getPlaylistTracksLoadLimit(), this.tidalConfig.getArtistTopTracksLoadLimit(), manager);
            manager.registerSourceManager(tidalSourceManager);
        }
        return manager;
    }

}