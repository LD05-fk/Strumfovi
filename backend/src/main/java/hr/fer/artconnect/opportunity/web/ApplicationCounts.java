package hr.fer.artconnect.opportunity.web;

public record ApplicationCounts(
        long total,
        long submitted,
        long underReview,
        long needsInfo,
        long accepted,
        long rejected
) {
}
