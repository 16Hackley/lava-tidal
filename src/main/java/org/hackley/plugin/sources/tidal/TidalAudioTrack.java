package org.hackley.plugin.sources.tidal;

import org.hackley.plugin.mirror.MirroringAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

public class TidalAudioTrack extends MirroringAudioTrack {

    public TidalAudioTrack(AudioTrackInfo trackInfo, TidalSourceManager sourceManager) {
        super(trackInfo, sourceManager);
    }

    @Override
    protected AudioTrack makeShallowClone() {
        return new TidalAudioTrack(this.trackInfo, (TidalSourceManager) this.sourceManager);
    }

}