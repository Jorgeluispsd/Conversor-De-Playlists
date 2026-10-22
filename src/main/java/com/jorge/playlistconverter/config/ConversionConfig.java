package com.jorge.playlistconverter.config;

import com.jorge.playlistconverter.converter.*;
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
                                                       YoutubeMusicService youtubeMusicService,
                                                       NewPlaylistExecutor youtubeNewPlaylistExecutor,
                                                       ExistingPlaylistExecutor youtubeExistingPlaylistExecutor) {
        return new PlaylistConverter(spotifyToYoutubeAnalyzer, youtubeMusicService,
                youtubeNewPlaylistExecutor, youtubeExistingPlaylistExecutor);
    }

    @Bean
    public PlaylistConversionValidator playlistConversionValidator() {
        return new PlaylistConversionValidator();
    }

    @Bean
    public NewPlaylistExecutor youtubeNewPlaylistExecutor(
            YoutubeMusicService youtubeMusicService, PlaylistConversionValidator playlistConversionValidator){
        return new NewPlaylistExecutor(youtubeMusicService, playlistConversionValidator);
    }

    @Bean
    public ExistingPlaylistExecutor youtubeExistingPlaylistExecutor(
            YoutubeMusicService youtubeMusicService, PlaylistConversionValidator playlistConversionValidator){
        return new ExistingPlaylistExecutor(youtubeMusicService, playlistConversionValidator);
    }
}
