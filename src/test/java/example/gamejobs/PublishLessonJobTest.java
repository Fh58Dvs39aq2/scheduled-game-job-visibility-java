package example.gamejobs;

public final class PublishLessonJobTest {
    public static void main(String[] args) {
        PublishLessonJob.JobInput input = new PublishLessonJob.JobInput(
            new PublishLessonJob.PlayerAsset("asset-1", "learner-1", "Quiz Garden"),
            new PublishLessonJob.LiveEvent("event-1", "Algebra lab"),
            new PublishLessonJob.ModerationItem("queue-1", "PENDING"));
        PublishLessonJob.JobResult result = new PublishLessonJob().run(input);
        if (result.decision() != PublishLessonJob.Decision.CAPTURE_REQUIRED) {
            throw new AssertionError("pending moderation must block publication");
        }
        System.out.println("CAPTURE_REQUIRED verified");
    }
}
