package com.jorge.playlistconverter.youtube;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.ResourceId;
import com.jorge.playlistconverter.errors.youtube.YoutubePlaylistReadException;
import com.jorge.playlistconverter.errors.youtube.YoutubeQuotaExceededException;
import com.jorge.playlistconverter.model.Song;
import com.jorge.playlistconverter.youtube.authorization.YoutubeAuthenticatedClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YoutubeExistingPlaylistReadTest {

    private YouTube youtube;
    private YouTube authenticatedYoutube;
    private YoutubeAuthenticatedClientFactory clientFactory;
    private YoutubeMusicService service;

    @BeforeEach
    void setUp() {
        youtube = mock(YouTube.class);
        authenticatedYoutube = mock(YouTube.class);
        clientFactory = mock(YoutubeAuthenticatedClientFactory.class);

        service = new YoutubeMusicService(youtube, "test-api-key", clientFactory);
    }

    @Test
    void shouldReadAllPagesUsingAuthenticatedClient() throws IOException {
        YouTube.PlaylistItems.List request = readRequest();

        PlaylistItemListResponse firstPage =
                new PlaylistItemListResponse().setItems(List.of(
                                playlistItem("video-A", "Song A", "Artist A")))
                        .setNextPageToken("next-page");

        PlaylistItemListResponse secondPage = new PlaylistItemListResponse()
                        .setItems(List.of(
                                playlistItem("video-B", "Song B", "Artist B")));

        when(request.execute()).thenReturn(firstPage, secondPage);

        List<Song> result = service.getPlaylistTracksAlreadyAdded("destination-playlist");

        assertEquals(List.of(
                        new Song("video-A", "Song A", "Artist A", 0),
                        new Song("video-B", "Song B", "Artist B", 0)),
                result);

        verify(clientFactory, times(1)).create();
        verify(request, times(2)).setPlaylistId("destination-playlist");
        verify(request, times(2)).setMaxResults(50L);
        verify(request, times(2)).setKey((String) null);

        var order = inOrder(request);
        order.verify(request).execute();
        order.verify(request).setPageToken("next-page");
        order.verify(request).execute();

        verifyNoInteractions(youtube);
    }

    @Test
    void shouldReturnEmptyListForEmptyDestination() throws IOException {
        YouTube.PlaylistItems.List request = readRequest();

        when(request.execute()).thenReturn(
                new PlaylistItemListResponse().setItems(List.of()));

        List<Song> result = service.getPlaylistTracksAlreadyAdded("destination-playlist");

        assertTrue(result.isEmpty());
        verify(request, times(1)).execute();
        verifyNoInteractions(youtube);
    }

    @Test
    void shouldRejectInvalidPlaylistIdBeforeAuthentication() {
        for (String playlistId : new String[]{null, "", "   "}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.getPlaylistTracksAlreadyAdded(playlistId));
        }

        verifyNoInteractions(clientFactory, authenticatedYoutube, youtube);
    }

    @Test
    void shouldFailInsteadOfReturningPartialRead() throws IOException {
        IOException original = new IOException("Falha na segunda página");

        YouTube.PlaylistItems.List request = readRequest();

        PlaylistItemListResponse firstPage =
                new PlaylistItemListResponse()
                        .setItems(List.of(
                                playlistItem("video-A", "Song A", "Artist A")))
                        .setNextPageToken("next-page");

        when(request.execute())
                .thenReturn(firstPage)
                .thenThrow(original);

        YoutubePlaylistReadException result = assertThrows(
                YoutubePlaylistReadException.class,
                () -> service.getPlaylistTracksAlreadyAdded("destination-playlist"));

        assertSame(original, result.getCause());
        verify(request, times(2)).execute();
    }

    @Test
    void shouldIdentifyQuotaExceededDuringAuthenticatedRead() throws IOException {
        GoogleJsonResponseException original = apiErrorWithReason("quotaExceeded");

        YouTube.PlaylistItems.List request = readRequest();
        when(request.execute()).thenThrow(original);

        YoutubeQuotaExceededException result = assertThrows(
                YoutubeQuotaExceededException.class,
                () -> service.getPlaylistTracksAlreadyAdded("destination-playlist"));

        assertSame(original, result.getCause());
        verify(request, times(1)).execute();
    }

    @Test
    void shouldPreserveApiRefusalDuringAuthenticatedRead() throws IOException {
        GoogleJsonResponseException original = apiErrorWithReason("playlistItemsNotAccessible");

        YouTube.PlaylistItems.List request = readRequest();
        when(request.execute()).thenThrow(original);

        YoutubePlaylistReadException result = assertThrows(
                YoutubePlaylistReadException.class,
                () -> service.getPlaylistTracksAlreadyAdded("destination-playlist"));

        assertSame(original, result.getCause());
        verify(request, times(1)).execute();
    }

    private YouTube.PlaylistItems.List readRequest() throws IOException {
        YouTube.PlaylistItems resource = mock(YouTube.PlaylistItems.class);

        YouTube.PlaylistItems.List request = mock(YouTube.PlaylistItems.List.class, RETURNS_SELF);

        when(clientFactory.create()).thenReturn(authenticatedYoutube);
        when(authenticatedYoutube.playlistItems()).thenReturn(resource);
        when(resource.list(List.of("snippet"))).thenReturn(request);

        return request;
    }

    private PlaylistItem playlistItem(String videoId, String title, String artist) {
        return new PlaylistItem()
                .setSnippet(new PlaylistItemSnippet()
                        .setTitle(title)
                        .setVideoOwnerChannelTitle(artist)
                        .setResourceId(new ResourceId()
                                .setKind("youtube#video")
                                .setVideoId(videoId)));
    }

    private GoogleJsonResponseException apiErrorWithReason(String reason) {
        GoogleJsonError.ErrorInfo error = new GoogleJsonError.ErrorInfo();
        error.setReason(reason);

        GoogleJsonError details = new GoogleJsonError();
        details.setCode(403);
        details.setErrors(List.of(error));

        HttpResponseException.Builder response =
                new HttpResponseException.Builder(
                        403, "Forbidden",
                        new HttpHeaders());

        return new GoogleJsonResponseException(response, details);
    }
}
