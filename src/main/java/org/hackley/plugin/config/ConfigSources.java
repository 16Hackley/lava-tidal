package org.hackley.plugin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@ConfigurationProperties(prefix = "plugins.lavatidal.sources")
@Component
public class ConfigSources {

    private boolean tidal = false;

    public boolean isTidal() {
        return this.tidal;
    }

    public void setTidal(boolean tidal) {
        this.tidal = tidal;
    }

}