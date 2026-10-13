package com.jorge.playlistconverter.youtube;

import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import com.google.api.services.youtube.YouTube;
import com.jorge.playlistconverter.errors.youtube.YoutubeQuotaExceededException;
import com.jorge.playlistconverter.errors.youtube.YoutubeSearchException;
import com.jorge.playlistconverter.model.Song;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class YoutubeMusicServiceTest {

    private YouTube youtube;
    private YoutubeMusicService service;

    private final Song sourceSong = new Song("test-song", "Test Song", "Test Artist", 200000);

    @BeforeEach
    void setUp(){
        youtube = mock(YouTube.class);
        service = new YoutubeMusicService(youtube, "test-api-key");
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
    void shouldHandleNullErrorEntryAndNullReason() throws IOException{
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
}
