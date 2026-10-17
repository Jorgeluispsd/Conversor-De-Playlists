package com.jorge.playlistconverter.youtube;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.PlaylistItem;
import com.jorge.playlistconverter.errors.youtube.YoutubeQuotaExceededException;
import com.jorge.playlistconverter.errors.youtube.YoutubeTrackInsertionException;
import com.jorge.playlistconverter.youtube.authorization.YoutubeAuthenticatedClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class YoutubeTrackInsertionTest {

    private YouTube youtube;
    private YouTube authenticatedYoutube;
    private YouTube.PlaylistItems resource;
    private YoutubeAuthenticatedClientFactory clientFactory;
    private YoutubeMusicService service;

    @BeforeEach
    void setUp(){
        youtube = mock(YouTube.class);
        authenticatedYoutube = mock(YouTube.class);
        resource = mock(YouTube.PlaylistItems.class);
        clientFactory = mock(YoutubeAuthenticatedClientFactory.class);

        service = new YoutubeMusicService(
                youtube, "test-api-key", clientFactory);
    }

    @Test
    void shouldInsertVideosInReceivedOrder() throws IOException{
        YouTube.PlaylistItems.Insert firstRequest = mock(YouTube.PlaylistItems.Insert.class);
        YouTube.PlaylistItems.Insert secondRequest = mock(YouTube.PlaylistItems.Insert.class);

        when(clientFactory.create()).thenReturn(authenticatedYoutube);
        when(authenticatedYoutube.playlistItems()).thenReturn(resource);

        when(resource.insert(eq(List.of("snippet")), any(PlaylistItem.class)))
                .thenReturn( firstRequest, secondRequest);

        when(firstRequest.execute()).thenReturn(new PlaylistItem().setId("entry-A"));
        when(secondRequest.execute()).thenReturn(new PlaylistItem().setId("entry-B"));

        service.addTracks("test-playlist", List.of("video-A", "video-B"));

        var captor = ArgumentCaptor.forClass(PlaylistItem.class);

        verify(resource, times(2))
                .insert(eq(List.of("snippet")), captor.capture());

        List<PlaylistItem> sentItems = captor.getAllValues();

        assertEquals(List.of("video-A", "video-B"), sentItems.stream()
                .map(item -> item.getSnippet().getResourceId().getVideoId()).toList());

        for (PlaylistItem item : sentItems) {
            assertEquals("test-playlist", item.getSnippet().getPlaylistId());

            assertEquals("youtube#video", item.getSnippet().getResourceId().getKind());
        }

        var order = inOrder(resource, firstRequest, secondRequest);

        order.verify(resource).insert(eq(List.of("snippet")), same(sentItems.get(0)));
        order.verify(firstRequest).execute();

        order.verify(resource).insert(eq(List.of("snippet")), same(sentItems.get(1)));
        order.verify(secondRequest).execute();

        verify(clientFactory, times(1)).create();
        verifyNoInteractions(youtube);
    }

    @Test
    void shouldIgnoreEmptyListWithoutAuthentication(){
        service.addTracks("test-playlist", List.of());

        verifyNoInteractions(clientFactory, youtube, authenticatedYoutube, resource);
    }

    @Test
    void shouldRejectInvalidPlaylistIdBeforeAuthentication(){
        for (String playlistId : new String[]{null, "", "  "}){
            assertThrows(IllegalArgumentException.class, () -> {
                service.addTracks(playlistId, List.of("video-A"));});
        }

        verifyNoInteractions(clientFactory, youtube, authenticatedYoutube);
    }

    @Test
    void shouldRejectNullTrackListBeforeAuthentication(){
        assertThrows(IllegalArgumentException.class, () -> {
            service.addTracks("test-playlist", null);
        });

        verifyNoInteractions(clientFactory, youtube, authenticatedYoutube);
    }

    @Test
    void shouldValidateEveryVideoBeforeStartingInsertion(){
        for (String invalidId : new String[]{null, "", "  "}){
            List<String> trackIds = Arrays.asList("video-A", invalidId);

            assertThrows(IllegalArgumentException.class, () -> {
                service.addTracks("test-playlist", trackIds);
            });
        }

        verifyNoInteractions(clientFactory, youtube, authenticatedYoutube);
    }

    @Test
    void shouldStopAfterCommunicationFailureOnSecondVideo() throws IOException{
        IOException original = new IOException("Falha simulada");

        YouTube.PlaylistItems.Insert request = insertionRequest();

        when(request.execute()).thenReturn(new PlaylistItem().setId("entry-A"))
                .thenThrow(original);

        YoutubeTrackInsertionException result = assertThrows(YoutubeTrackInsertionException.class, () ->
            service.addTracks("test-playlist", List.of("video-A", "video-B", "video-C")));

        assertSame(original, result.getCause());

        assertTrue(result.getMessage().contains("Inserções anteriores confirmadas: 1"));

        verify(request, times(2)).execute();
        verify(clientFactory, times(1)).create();

        var captor = ArgumentCaptor.forClass(PlaylistItem.class);

        verify(resource, times(2)).insert(
                eq(List.of("snippet")), captor.capture());

        assertEquals(List.of("video-A", "video-B"), captor.getAllValues().stream()
                .map(item -> item.getSnippet().getResourceId().getVideoId()).toList());
    }

    @Test
    void shouldRejectInsertionResponseWithoutId() throws IOException{
        YouTube.PlaylistItems.Insert request = insertionRequest();

        when(request.execute()).thenReturn(new PlaylistItem());

        YoutubeTrackInsertionException result = assertThrows(YoutubeTrackInsertionException.class, () ->
            service.addTracks("test-playlist", List.of("video-A", "video-B")));

        assertTrue(result.getMessage().contains("Inserções anteriores confirmadas: 0"));

        verify(request, times(1)).execute();
    }

    @Test
    void shouldIdentifyQuotaExceededDuringInsertion() throws IOException{
        GoogleJsonResponseException original = apiErrorWithReason("quotaExceeded");

        YouTube.PlaylistItems.Insert request = insertionRequest();

        when(request.execute()).thenThrow(original);

        YoutubeQuotaExceededException result = assertThrows(YoutubeQuotaExceededException.class,
                () -> service.addTracks("test-playlist", List.of("video-A", "video-B")));

        assertSame(original, result.getCause());
        verify(request, times(1)).execute();
    }

    @Test
    void shouldPreserveApiRefusalDuringInsertion() throws IOException{
        GoogleJsonResponseException original = apiErrorWithReason("playlistItemsNotAccessible");

        YouTube.PlaylistItems.Insert request = insertionRequest();
        when(request.execute()).thenThrow(original);

        YoutubeTrackInsertionException result = assertThrows(YoutubeTrackInsertionException.class,
                () -> service.addTracks("test-playlist", List.of("video-A")));

        assertSame(original, result.getCause());
        verify(request, times(1)).execute();
    }

    private YouTube.PlaylistItems.Insert insertionRequest() throws IOException{
        YouTube.PlaylistItems.Insert request = mock(YouTube.PlaylistItems.Insert.class);

        when(clientFactory.create()).thenReturn(authenticatedYoutube);
        when(authenticatedYoutube.playlistItems()).thenReturn(resource);

        when(resource.insert(eq(List.of("snippet")),
                any(PlaylistItem.class))).thenReturn(request);

        return request;
    }

    private GoogleJsonResponseException apiErrorWithReason(String reason){
        GoogleJsonError.ErrorInfo error = new GoogleJsonError.ErrorInfo();
        error.setReason(reason);

        GoogleJsonError details = new GoogleJsonError();
        details.setCode(403);
        details.setErrors(List.of(error));

        HttpResponseException.Builder response =
                new HttpResponseException.Builder(
                        403, "Forbidden", new HttpHeaders());

        return new GoogleJsonResponseException(response, details);
    }
}
