package example.gamejobs;

public final class GameJobFailureDemo {
    public static void main(String[] args) throws Exception {
        PublishLessonJob.JobInput input = new PublishLessonJob.JobInput(
            new PublishLessonJob.PlayerAsset("asset-lesson-7", "learner-18", "Fraction Quest"),
            new PublishLessonJob.LiveEvent("event-saturday", "Co-op fractions"),
            new PublishLessonJob.ModerationItem("moderation-7", "PENDING"));
        PublishLessonJob.JobResult result = new PublishLessonJob().run(input);
        System.out.println("job decision=" + result.decision());
        if (result.decision() == PublishLessonJob.Decision.CAPTURE_REQUIRED) {
            String key = System.getenv("INFRAI_API_KEY");
            if (key == null || key.isBlank()) throw new IllegalStateException("set INFRAI_API_KEY");
            new InfraiErrorsClient(java.net.http.HttpClient.newHttpClient(), key).capture(input, result.message());
            System.out.println("capture=accepted");
        }
    }
}
