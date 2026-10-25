package com.jorge.playlistconverter.config;

import com.jorge.playlistconverter.converter.*;
import com.jorge.playlistconverter.enums.ConversionDirection;
import com.jorge.playlistconverter.matcher.TrackMatcher;
import com.jorge.playlistconverter.spotify.SpotifyMusicService;
import com.jorge.playlistconverter.state.SyncStateStore;
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
                                                       ExistingPlaylistExecutor youtubeExistingPlaylistExecutor,
                                                       PlaylistConversionValidator playlistConversionValidator) {
        return new PlaylistConverter(spotifyToYoutubeAnalyzer, youtubeMusicService,
                youtubeNewPlaylistExecutor, youtubeExistingPlaylistExecutor, playlistConversionValidator);
    }

    @Bean
    public PlaylistConversionValidator playlistConversionValidator() {
        return new PlaylistConversionValidator();
    }

    @Bean
    public NewPlaylistExecutor youtubeNewPlaylistExecutor(
            YoutubeMusicService youtubeMusicService, PlaylistConversionValidator playlistConversionValidator,
            PlaylistDestinationRegistry spotifyToYoutubeDestinationRegistry, SyncRunTracker syncRunTracker){
        return new NewPlaylistExecutor(youtubeMusicService, playlistConversionValidator,
                spotifyToYoutubeDestinationRegistry, syncRunTracker);
    }

    @Bean
    public ExistingPlaylistExecutor youtubeExistingPlaylistExecutor(
            YoutubeMusicService youtubeMusicService, PlaylistConversionValidator playlistConversionValidator,
            PlaylistDestinationRegistry spotifyToYoutubeDestinationRegistry, SyncRunTracker runTracker){
        return new ExistingPlaylistExecutor(youtubeMusicService, playlistConversionValidator,
                spotifyToYoutubeDestinationRegistry, runTracker);
    }

    @Bean
    public PlaylistDestinationRegistry spotifyToYoutubeDestinationRegistry(SyncStateStore store,
                                                                           PlaylistConversionValidator playlistConversionValidator){
        return new PlaylistDestinationRegistry(store, ConversionDirection.SPOTIFY_TO_YOUTUBE,
                playlistConversionValidator);
    }

    @Bean
    public SyncRunTracker syncRunTracker(SyncStateStore store){
        return new SyncRunTracker(store);
    }
}
