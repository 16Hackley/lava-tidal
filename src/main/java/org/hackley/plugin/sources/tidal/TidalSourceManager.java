package org.hackley.plugin.sources.tidal;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;
import com.sedmelluq.discord.lavaplayer.track.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hackley.plugin.mirror.DefaultMirroringAudioTrackResolver;
import org.hackley.plugin.mirror.MirroringAudioSourceManager;
import org.hackley.plugin.mirror.MirroringAudioTrackResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Pattern;

public class TidalSourceManager extends MirroringAudioSourceManager implements HttpConfigurable {

    public static final Pattern URL_PATTERN = Pattern.compile("^(https?://)?(www\\.)?(listen\\.)?(embed\\.)?tidal\\.com/((browse/)?(?<type>track|album|playlist|mix|artist)|(tracks|albums|playlists|artists))/(?<identifier>[a-zA-Z0-9-_]+)");
    public static final String SEARCH_PREFIX = "tdsearch:";
    public static final String API_BASE = "https://api.tidal.com/v1/";
    private static final Logger log = LoggerFactory.getLogger(TidalSourceManager.class);
    private final HttpInterfaceManager httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager();
    private final String tidalToken;
    private final String countryCode;
    private final int tracksSearchLimit;
    private final int playlistTracksLoadLimit;
    private final int artistTopTracksLoadLimit;


    public TidalSourceManager(String[] providers, String tidalToken, String countryCode, int tracksSearchLimit, int playlistTracksLoadLimit, int artistTopTracksLoadLimit, AudioPlayerManager audioPlayerManager) {
        this(tidalToken, countryCode, tracksSearchLimit, playlistTracksLoadLimit, artistTopTracksLoadLimit, audioPlayerManager, new DefaultMirroringAudioTrackResolver(providers));
    }

    public TidalSourceManager(String tidalToken, String countryCode, int tracksSearchLimit, int playlistTracksLoadLimit, int artistTopTracksLoadLimit, AudioPlayerManager audioPlayerManager, MirroringAudioTrackResolver mirroringAudioTrackResolver) {
        super(audioPlayerManager, mirroringAudioTrackResolver);

        if (tidalToken == null || tidalToken.isEmpty()) {
            throw new IllegalArgumentException("Tidal Token must be set");
        }
        this.tidalToken = tidalToken;

        if (countryCode == null || countryCode.isEmpty()) {
            countryCode = "US";
        }

        if (tracksSearchLimit == 0) {
            tracksSearchLimit = 25;
        }

        if (playlistTracksLoadLimit == 0) {
            playlistTracksLoadLimit = 100;
        }

        if (artistTopTracksLoadLimit == 0) {
            artistTopTracksLoadLimit = 100;
        }

        this.countryCode = countryCode;
        this.tracksSearchLimit = tracksSearchLimit;
        this.playlistTracksLoadLimit = playlistTracksLoadLimit;
        this.artistTopTracksLoadLimit = artistTopTracksLoadLimit;

    }

    @Override
    public String getSourceName() {
        return "tidal";
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) {
        return new TidalAudioTrack(trackInfo, this);
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return true;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) {
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        try {
            if (reference.identifier.startsWith(SEARCH_PREFIX)) {
                return this.getSearch(reference.identifier.substring(SEARCH_PREFIX.length()).trim());
            }

            var matcher = URL_PATTERN.matcher(reference.identifier);
            if (!matcher.find()) {
                return null;
            }

            var id = matcher.group("identifier");
            switch (matcher.group("type")) {
                case "album":
                    return this.getAlbum(id);

                case "track":
                    return this.getTrack(id);

                case "playlist":
                    return this.getPlaylist(id);

                case "artist":
                    return this.getArtist(id);

                case "mix":
                    return this.getMix(id);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public JsonBrowser getJson(String uri) throws IOException {
        var request = new HttpGet(uri);
        request.addHeader("x-tidal-token", this.tidalToken);
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");
        return HttpClientTools.fetchResponseAsJson(this.httpInterfaceManager.getInterface(), request);
    }

    public AudioItem getSearch(String query) throws IOException {
        var json = this.getJson(API_BASE + "search/tracks?query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&limit=" + this.tracksSearchLimit + "&offset=0" + "&countryCode=" + this.countryCode);
        if (json == null) {
            return AudioReference.NO_TRACK;
        }

        var tracks = this.parseTracks(json);
        return new BasicAudioPlaylist("Tidal search results for: " + query, tracks, null, true);
    }

    public AudioItem getAlbum(String id) throws IOException {
        var json = this.getJson(API_BASE + "albums/" + id + "/tracks?" + "countryCode=" + this.countryCode);
        if (json == null || json.get("items").values().isEmpty()) {
            return AudioReference.NO_TRACK;
        }

        return new TidalAudioPlaylist(json.get("items").index(0).get("album").get("title").text(), this.parseTracks(json), "album", "https://tidal.com/browse/album/" + id, null, null);
    }

    public AudioItem getPlaylist(String id) throws IOException {

        var playlistInfoJson = this.getJson(API_BASE + "playlists/" + id  + "?&countryCode=" + this.countryCode);
        if (playlistInfoJson == null) {
            return AudioReference.NO_TRACK;
        }

        var tracksJson = this.getJson(API_BASE + "playlists/" + id + "/tracks?" + "&countryCode=" + this.countryCode + "&limit=" + this.playlistTracksLoadLimit + "&offset=0");

        if (tracksJson == null || tracksJson.get("items").values().isEmpty()) {
            return AudioReference.NO_TRACK;
        }

        return new TidalAudioPlaylist(playlistInfoJson.get("title").text(), this.parseTracks(tracksJson), "playlist", "https://tidal.com/browse/playlist/" + id, null, null);
    }

    public AudioItem getMix(String id) throws IOException {
        var json = this.getJson(API_BASE + "mixes/" + id + "/items" + "?countryCode=" + this.countryCode);
        if (json == null || json.get("items").values().isEmpty()) {
            return AudioReference.NO_TRACK;
        }

        return new TidalAudioPlaylist("Mix", this.parseMixTracks(json), "playlist", "https://tidal.com/browse/mix/" + id, null, null);
    }

    public AudioItem getArtist(String id) throws IOException {
        var json = this.getJson(API_BASE + "artists/" + id + "/toptracks" + "?limit=" + this.artistTopTracksLoadLimit + "&offset=0" + "&countryCode=" + this.countryCode);
        if (json == null || json.get("items").values().isEmpty()) {
            return AudioReference.NO_TRACK;
        }

        var author = json.get("items").index(0).get("artist").get("name").text();
        return new TidalAudioPlaylist(author + " - Top tracks", this.parseTracks(json), "artist", "https://tidal.com/browse/artist/" + id, null, null);
    }

    public AudioItem getTrack(String id) throws IOException {
        var json = this.getJson(API_BASE + "tracks/" + id + "?countryCode=" + this.countryCode);
        if (json == null) {
            return AudioReference.NO_TRACK;
        }
        return this.parseTrack(json);
    }

    private List<AudioTrack> parseTracks(JsonBrowser json) {
        var tracks = new ArrayList<AudioTrack>();
        for (var value : json.get("items").values()) {
            tracks.add(this.parseTrack(value));
        }
        return tracks;
    }

    private List<AudioTrack> parseMixTracks(JsonBrowser json) {
        var tracks = new ArrayList<AudioTrack>();
        for (var item : json.get("items").values()) {
            var trackItem = item.get("item");
            tracks.add(this.parseTrack(trackItem));
        }
        return tracks;
    }

    private String parseArtworkUrl(JsonBrowser json) {
        var text = json.get("album").get("cover").text();
        if (text == null) {
            return null;
        }
        return text.replace("-", "/");
    }

    private AudioTrack parseTrack(JsonBrowser json) {

        return new TidalAudioTrack(
                new AudioTrackInfo(
                        json.get("title").text(),
                        json.get("artist").get("name").text(),
                        json.get("duration").as(Long.class) * 1000,
                        json.get("id").text(),
                        false,
                        json.get("url").text(),
                        "https://resources.tidal.com/images/" + parseArtworkUrl(json) + "/1080x1080.jpg",
                        json.get("isrc").text()
                ),
                this
        );
    }

    @Override
    public void shutdown() {
        try {
            this.httpInterfaceManager.close();
        } catch (IOException e) {
            log.error("Failed to close HTTP interface manager", e);
        }
    }

    @Override
    public void configureRequests(Function<RequestConfig, RequestConfig> configurator) {
        this.httpInterfaceManager.configureRequests(configurator);
    }

    @Override
    public void configureBuilder(Consumer<HttpClientBuilder> configurator) {
        this.httpInterfaceManager.configureBuilder(configurator);
    }

}