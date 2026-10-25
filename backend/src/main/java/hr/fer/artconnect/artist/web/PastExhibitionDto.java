package hr.fer.artconnect.artist.web;

import java.util.UUID;

/** Shema PastExhibition. */
public record PastExhibitionDto(UUID id, String title, int year, String place) {
}
