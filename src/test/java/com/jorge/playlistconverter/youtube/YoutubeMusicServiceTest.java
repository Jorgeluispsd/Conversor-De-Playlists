package com.jorge.playlistconverter.youtube;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Playlist;
import com.jorge.playlistconverter.enums.YoutubePlaylistVisibility;
import com.jorge.playlistconverter.errors.youtube.YoutubePlaylistCreationException;
import com.jorge.playlistconverter.errors.youtube.YoutubeQuotaExceededException;
import com.jorge.playlistconverter.errors.youtube.YoutubeSearchException;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.youtube.authorization.YoutubeAuthenticatedClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class YoutubeMusicServiceTest {

    private YouTube youtube;
    private YoutubeMusicService service;
    private YoutubeAuthenticatedClientFactory authenticatedClientFactory;

    private final Song sourceSong = new Song("test-song", "Test Song", "Test Artist", 200000);

    @BeforeEach
    void setUp(){
        youtube = mock(YouTube.class);
        authenticatedClientFactory = mock(YoutubeAuthenticatedClientFactory.class);

        service = new YoutubeMusicService(youtube, "test-api-key", authenticatedClientFactory);
    }

    @Test
    void shouldIdentifyQuotaExceededDuringSearch() throws IOException{
        GoogleJsonResponseException original = apiErrorWithReason("quotaExceeded");

        YouTube.Search.List request = searchRequestFailingWith(original);

        YoutubeQuotaExceededException result = assertThrows(
                YoutubeQuotaExceededException.class,
                () -> service.searchCandidates(sourceSong));

        assertSame(original, result.getCause());
        verify(request, times(1)).execute();
    }

    @Test
    void shouldIdentifyQuotaExceededDuringPlaylistRead() throws IOException{
        GoogleJsonResponseException original = apiErrorWithReason("quotaExceeded");

        YouTube.PlaylistItems resource = mock(YouTube.PlaylistItems.class);

        YouTube.PlaylistItems.List request = mock(YouTube.PlaylistItems.List.class, RETURNS_SELF);

        when(youtube.playlistItems()).thenReturn(resource);
        when(resource.list(List.of("snippet"))).thenReturn(request);
        when(request.execute()).thenThrow(original);



        YoutubeQuotaExceededException result = assertThrows(
                YoutubeQuotaExceededException.class,
                () -> service.getPlaylistTracks("test-playlist"));

        assertSame(original, result.getCause());
        verify(request, times(1)).execute();
    }

    @Test
    void shouldNotTreatAnotherForbiddenErrorAsQuota() throws IOException{
        assertSearchErrorIsPreserved(apiErrorWithReason("forbidden"));
    }

    @Test
    void shouldHandleMissingErrorDetails() throws IOException{
        assertSearchErrorIsPreserved(apiError(null));
    }

    @Test
    void shouldHandleMissingErrorList() throws IOException{
        GoogleJsonError details = new GoogleJsonError();
        details.setCode(403);

        assertSearchErrorIsPreserved(apiError(details));
    }

    @Test
    void shouldHandleNullErrorReason() throws IOException{
        GoogleJsonError.ErrorInfo error = new GoogleJsonError.ErrorInfo();

        GoogleJsonError details = new GoogleJsonError();
        details.setCode(403);
        details.setErrors(List.of(error));

        assertSearchErrorIsPreserved(apiError(details));
    }

    private void assertSearchErrorIsPreserved(
            GoogleJsonResponseException original
    ) throws IOException {

        YouTube.Search.List request = searchRequestFailingWith(original);

        YoutubeSearchException result = assertThrows(
                YoutubeSearchException.class,
                () -> service.searchCandidates(sourceSong));

        assertSame(original, result.getCause());
        verify(request, times(1)).execute();
    }

    private YouTube.Search.List searchRequestFailingWith(
            GoogleJsonResponseException original
    ) throws IOException {

        YouTube.Search resource = mock(YouTube.Search.class);

        YouTube.Search.List request =
                mock(YouTube.Search.List.class, RETURNS_SELF);

        when(youtube.search()).thenReturn(resource);
        when(resource.list(List.of("snippet"))).thenReturn(request);
        when(request.execute()).thenThrow(original);

        return request;
    }

    private GoogleJsonResponseException apiErrorWithReason(String reason) {
        GoogleJsonError.ErrorInfo error =
                new GoogleJsonError.ErrorInfo();

        error.setReason(reason);

        GoogleJsonError details = new GoogleJsonError();
        details.setCode(403);
        details.setErrors(List.of(error));

        return apiError(details);
    }

    private GoogleJsonResponseException apiError(GoogleJsonError details) {
        HttpResponseException.Builder response =
                new HttpResponseException.Builder(
                        403, "Forbidden", new HttpHeaders());

        return new GoogleJsonResponseException(response, details);
    }

    @Test
    void shouldCreatePrivatePlaylistAndReturnId() throws IOException{
        YouTube authenticatedYoutube = mock(YouTube.class);
        YouTube.Playlists resource = mock(YouTube.Playlists.class);
        YouTube.Playlists.Insert request = mock(YouTube.Playlists.Insert.class);

        when(authenticatedClientFactory.create()).thenReturn(authenticatedYoutube);
        when(authenticatedYoutube.playlists()).thenReturn(resource);

        when(resource.insert(eq(List.of("snippet", "status")),
                any(Playlist.class))).thenReturn(request);

        when(request.execute()).thenReturn(new Playlist().setId("created-playlist"));

        String result = service.createPlaylist(" Minha playlist ");

        assertEquals("created-playlist", result);

        var playlistCaptor = ArgumentCaptor.forClass(Playlist.class);

        verify(resource).insert(eq(List.of("snippet", "status")),
                playlistCaptor.capture());

        Playlist sent = playlistCaptor.getValue();

        assertEquals("Minha playlist", sent.getSnippet().getTitle());
        assertEquals("private", sent.getStatus().getPrivacyStatus());

        verify(request, times(1)).execute();
        verifyNoInteractions(youtube);
    }

    @Test
    void shouldRejectMissingPlaylistNameBeforeAuthentication(){
        for (String name : new String[]{null, "", "  "}){
            assertThrows(IllegalArgumentException.class,
                    () -> service.createPlaylist(name));
        }

        verifyNoInteractions(authenticatedClientFactory, youtube);
    }

    @Test
    void shouldIdentifyQuotaExceededDuringPlaylistCreation() throws IOException{
        GoogleJsonResponseException original = apiErrorWithReason("quotaExceeded");

        YouTube.Playlists.Insert request = playlistCreationRequest();
        when(request.execute()).thenThrow(original);

        YoutubeQuotaExceededException result = assertThrows(
                YoutubeQuotaExceededException.class, () -> service.createPlaylist("Minha playlist"));

        assertSame(original, result.getCause());
        verify(request, times(1)).execute();
    }

    @Test
    void shouldPreservePlaylistCreationCommunicationFailure() throws IOException{
        IOException original = new IOException("Falha simulada");

        YouTube.Playlists.Insert request = playlistCreationRequest();
        when(request.execute()).thenThrow(original);

        YoutubePlaylistCreationException result = assertThrows(
                YoutubePlaylistCreationException.class,
                () -> service.createPlaylist("Minha playlist"));

        assertSame(original, result.getCause());
        verify(request, times(1)).execute();
    }

    @Test
    void shouldRejectPlaylistCreationResponseWithoutId() throws IOException{
        YouTube.Playlists.Insert request = playlistCreationRequest();
        when(request.execute()).thenReturn(new Playlist());

        assertThrows(YoutubePlaylistCreationException.class,
                () -> service.createPlaylist("Minha playlist"));

        verify(request, times(1)).execute();
    }

    @Test
    void shouldPreservePlaylistCreationApiRefusal() throws IOException{
        GoogleJsonResponseException original = apiErrorWithReason("playlistForbidden");

        YouTube.Playlists.Insert request = playlistCreationRequest();
        when(request.execute()).thenThrow(original);

        YoutubePlaylistCreationException result = assertThrows(
                YoutubePlaylistCreationException.class,
                () -> service.createPlaylist("Minha playlist"));

        assertSame(original, result.getCause());
        verify(request, times(1)).execute();
    }

    private YouTube.Playlists.Insert playlistCreationRequest() throws IOException{
        YouTube authenticatedYoutube = mock(YouTube.class);
        YouTube.Playlists resource = mock(YouTube.Playlists.class);
        YouTube.Playlists.Insert request = mock(YouTube.Playlists.Insert.class);

        when(authenticatedClientFactory.create()).thenReturn(authenticatedYoutube);
        when(authenticatedYoutube.playlists()).thenReturn(resource);

        when(resource.insert(eq(List.of("snippet", "status")),
                any(Playlist.class))).thenReturn(request);

        return request;
    }

    @ParameterizedTest
    @EnumSource(YoutubePlaylistVisibility.class)
    void shouldCreatePlaylistWithSelectedVisibility(YoutubePlaylistVisibility visibility) throws IOException{
        YouTube authenticatedYoutube = mock(YouTube.class);
        YouTube.Playlists resource = mock(YouTube.Playlists.class);
        YouTube.Playlists.Insert request = mock(YouTube.Playlists.Insert.class);

        when(authenticatedClientFactory.create()).thenReturn(authenticatedYoutube);
        when(authenticatedYoutube.playlists()).thenReturn(resource);

        when(resource.insert(eq(List.of("snippet", "status")),
                any(Playlist.class))).thenReturn(request);

        when(request.execute()).thenReturn(new Playlist().setId("created-playlist"));

        String result = service.createPlaylist("Minha playlist", visibility);

        var captor = ArgumentCaptor.forClass(Playlist.class);
        verify(resource).insert(eq(List.of("snippet", "status")), captor.capture());

        assertEquals(visibility.getApiValue(),
                captor.getValue().getStatus().getPrivacyStatus());

        assertEquals("created-playlist", result);
        verify(request, times(1)).execute();
    }

    @Test
    void shouldRejectNullVisibilityBeforeAuthentication(){
        assertThrows(NullPointerException.class,
                () -> service.createPlaylist("Minha playlist", null));

        verifyNoInteractions(authenticatedClientFactory, youtube);
    }
}
