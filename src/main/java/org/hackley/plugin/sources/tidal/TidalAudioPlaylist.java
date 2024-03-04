package org.hackley.plugin.sources.tidal;

import org.hackley.plugin.ExtendedAudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.List;

public class TidalAudioPlaylist extends ExtendedAudioPlaylist {

    public TidalAudioPlaylist(String name, List<AudioTrack> tracks, String type, String identifier, String artworkURL, String author) {
        super(name, tracks, type, identifier, artworkURL, author);
    }

}