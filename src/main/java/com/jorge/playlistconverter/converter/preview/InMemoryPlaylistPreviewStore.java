package com.jorge.playlistconverter.converter.preview;

import com.jorge.playlistconverter.enums.PlaylistVisibility;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryPlaylistPreviewStore {

    private final ConcurrentMap<UUID, StoredPlaylistPreview> previews = new ConcurrentHashMap<>();

    private final Clock clock;
    private final Duration lifetime;

    public InMemoryPlaylistPreviewStore(Clock clock, Duration lifetime) {
        this.clock = Objects.requireNonNull(clock, "O relógio deve ser informado.");
        this.lifetime = Objects.requireNonNull(lifetime, "O prazo de validade deve ser informado.");

        if (lifetime.isZero() || lifetime.isNegative()){
            throw new IllegalArgumentException("O prazo de validade deve ser positivo.");
        }
    }

    public StoredPlaylistPreview save(PlaylistPreview preview, String destinationName,
                                      PlaylistVisibility visibility){
        Objects.requireNonNull(preview, "A prévia deve ser informada.");

        Instant now = clock.instant();

        removeExpired(now);

        StoredPlaylistPreview stored = new StoredPlaylistPreview(
                UUID.randomUUID(), preview, destinationName, visibility, now.plus(lifetime));

        previews.put(stored.id(), stored);

        return stored;
    }

    public Optional<StoredPlaylistPreview> find(UUID id){
        Objects.requireNonNull(id, "O identificador da prévia deve ser informado.");

        StoredPlaylistPreview stored = previews.get(id);

        if (stored == null){
            return Optional.empty();
        }

        if (stored.isExpired(clock.instant())){
            previews.remove(id, stored);
            return Optional.empty();
        }

        return Optional.of(stored);
    }

    public Optional<StoredPlaylistPreview> consume(UUID id){
        Objects.requireNonNull(id, "O identificador da prévia deve ser informado.");

        StoredPlaylistPreview stored = previews.remove(id);

        return Optional.ofNullable(stored).filter(preview -> !preview.isExpired(clock.instant()));
    }

    private void removeExpired(Instant now){
        previews.entrySet().removeIf(
                entry -> entry.getValue().isExpired(now));
    }
}
