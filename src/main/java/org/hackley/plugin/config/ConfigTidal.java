package org.hackley.plugin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "plugins.lavatidal.tidal")
@Component
public class ConfigTidal {

    private String tidalToken;
    private String countryCode;
    private int tracksSearchLimit;
    private int playlistTracksLoadLimit;
    private int artistTopTracksLoadLimit;

    public String getTidalToken() {
        return this.tidalToken;
    }

    public void setTidalToken(String tidalToken) {
        this.tidalToken = tidalToken;
    }

    public String getCountryCode() {
        return this.countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public int getTracksSearchLimit() {
        return this.tracksSearchLimit;
    }

    public void setTracksSearchLimit(int tracksSearchLimit) {
        this.tracksSearchLimit = tracksSearchLimit;
    }

    public int getPlaylistTracksLoadLimit() {
        return this.playlistTracksLoadLimit;
    }

    public void setPlaylistTracksLoadLimit(int playlistTracksLoadLimit) {
        this.playlistTracksLoadLimit = playlistTracksLoadLimit;
    }

    public int getArtistTopTracksLoadLimit() {
        return this.artistTopTracksLoadLimit;
    }

    public void setArtistTopTracksLoadLimit(int artistTopTracksLoadLimit) {
        this.artistTopTracksLoadLimit = artistTopTracksLoadLimit;
    }

}
