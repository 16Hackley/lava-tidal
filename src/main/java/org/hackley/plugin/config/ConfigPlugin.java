package org.hackley.plugin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.hackley.plugin.mirror.MirroringAudioSourceManager.ISRC_PATTERN;
import static org.hackley.plugin.mirror.MirroringAudioSourceManager.QUERY_PATTERN;

@ConfigurationProperties(prefix = "plugins.lavatidal")
@Component
public class ConfigPlugin {

    private String[] providers = {
            "ytsearch:\"" + ISRC_PATTERN + "\"",
            "ytsearch:" + QUERY_PATTERN
    };

    public String[] getProviders() {
        return this.providers;
    }

    public void setProviders(String[] providers) {
        this.providers = providers;
    }

}