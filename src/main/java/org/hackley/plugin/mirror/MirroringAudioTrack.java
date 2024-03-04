package org.hackley.plugin.mirror;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public abstract class MirroringAudioTrack extends DelegatedAudioTrack {

    private static final Logger log = LoggerFactory.getLogger(MirroringAudioTrack.class);

    protected final MirroringAudioSourceManager sourceManager;

    public MirroringAudioTrack(AudioTrackInfo trackInfo, MirroringAudioSourceManager sourceManager) {
        super(trackInfo);
        this.sourceManager = sourceManager;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        AudioItem track = this.sourceManager.getResolver().apply(this);

        if (track instanceof AudioPlaylist) {
            track = ((AudioPlaylist) track).getTracks().get(0);
        }
        if (track instanceof InternalAudioTrack) {
            processDelegate((InternalAudioTrack) track, executor);
            return;
        }
        throw new FriendlyException("No matching track found", FriendlyException.Severity.COMMON, new TrackNotFoundException());
    }

    @Override
    public AudioSourceManager getSourceManager() {
        return this.sourceManager;
    }

    public AudioItem loadItem(String query) {
        var cf = new CompletableFuture<AudioItem>();
        this.sourceManager.getAudioPlayerManager().loadItem(query, new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(AudioTrack track) {
                log.debug("Track loaded: " + track.getIdentifier());
                cf.complete(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                log.debug("Playlist loaded: " + playlist.getName());
                cf.complete(playlist);
            }

            @Override
            public void noMatches() {
                log.debug("No matches found for: " + query);
                cf.complete(AudioReference.NO_TRACK);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                log.debug("Failed to load: " + query);
                cf.completeExceptionally(exception);
            }
        });
        return cf.join();
    }

}