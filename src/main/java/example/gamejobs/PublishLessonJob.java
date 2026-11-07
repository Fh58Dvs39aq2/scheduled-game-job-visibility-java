package example.gamejobs;

public final class PublishLessonJob {
    public enum Decision { PUBLISHED, CAPTURE_REQUIRED }

    public record PlayerAsset(String assetId, String creatorId, String title) {}
    public record LiveEvent(String eventId, String lessonTitle) {}
    public record ModerationItem(String queueId, String state) {}
    public record JobInput(PlayerAsset asset, LiveEvent event, ModerationItem moderation) {}
    public record JobResult(Decision decision, String message) {}

    public JobResult run(JobInput input) {
        if (!"APPROVED".equals(input.moderation().state())) {
            return new JobResult(Decision.CAPTURE_REQUIRED,
                "moderation is " + input.moderation().state() + " for " + input.asset().assetId());
        }
        return new JobResult(Decision.PUBLISHED,
            "published " + input.asset().assetId() + " into " + input.event().eventId());
    }
}
