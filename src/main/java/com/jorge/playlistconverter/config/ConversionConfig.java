package com.jorge.playlistconverter.config;

import com.jorge.playlistconverter.converter.PlaylistAnalyzer;
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
    public PlaylistAnalyzer spotifyToYoutubeAnalyzer(SpotifyMusicService spotifyMusicService,
                                                     YoutubeMusicService youtubeMusicService,
                                                     TrackMatcher trackMatcher){
        return new PlaylistAnalyzer(spotifyMusicService, youtubeMusicService, trackMatcher);
    }

    @Bean
    public PlaylistConverter spotifyToYoutubeConverter(PlaylistAnalyzer spotifyToYoutubeAnalyzer,
                                                       YoutubeMusicService youtubeMusicService) {
        return new PlaylistConverter(spotifyToYoutubeAnalyzer, youtubeMusicService);
    }
}
