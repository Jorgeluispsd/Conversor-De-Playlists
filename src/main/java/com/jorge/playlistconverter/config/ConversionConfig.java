package com.jorge.playlistconverter.config;

import com.jorge.playlistconverter.converter.PlaylistConverter;
import com.jorge.playlistconverter.matcher.TrackMatcher;
import com.jorge.playlistconverter.spotify.SpotifyMusicService;
import com.jorge.playlistconverter.youtube.YoutubeMusicService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConversionConfig {

    @Bean
    public TrackMatcher trackMatcher() {
        return new TrackMatcher();
    }

    @Bean
    public PlaylistConverter spotifyToYoutubeConverter(SpotifyMusicService spotifyMusicService,
                                                       YoutubeMusicService youtubeMusicService,
                                                       TrackMatcher trackMatcher) {
        return new PlaylistConverter(spotifyMusicService, youtubeMusicService, trackMatcher);
    }
}
