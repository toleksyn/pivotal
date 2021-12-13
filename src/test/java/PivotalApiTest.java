import onespot.pivotal.PivotalApi;
import onespot.pivotal.api.resources.Story;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.System.getProperty;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class PivotalApiTest {

    @Test
    public void getMetricsData() throws FileNotFoundException {
        var storiesDone = new ArrayList<String[]>();
        storiesDone.add(new String[]{"name", "stories done"});

        int digitalProjectId = parseInt(getProperty("digitalProjectId"));
        var digitalTeam = splitIntoList(getProperty("digital_team"));
        var storiesDoneDigital = getStoriesDone(digitalProjectId, digitalTeam, false);
        storiesDone.addAll(storiesDoneDigital);

        var notDevLabels = splitIntoList(getProperty("not_dev_labels"));
        var notDevStoriesTotalDigital = valueOf(getNotDevStoriesCount(digitalProjectId, notDevLabels, false));
        storiesDone.add(new String[]{"Digital not dev total", notDevStoriesTotalDigital});

        var monitoringTeam = splitIntoList(getProperty("monitoring_team"));
        int monitoringProjectId = parseInt(getProperty("monitoringProjectId"));
        var storiesDoneMonitoring = getStoriesDone(monitoringProjectId, monitoringTeam, true);

        storiesDone.addAll(storiesDoneMonitoring);

        var csvOutputFile = new File(format("metrics_%s.csv", getCurrentWeekLabel()));
        try (var printWriter = new PrintWriter(csvOutputFile)) {
            storiesDone
                    .stream()
                    .map(this::convertToCsv)
                    .forEach(printWriter::println);
        }
    }

    private List<String> splitIntoList(String string) {
        return Arrays.stream(string
                        .split(","))
                .collect(toList());
    }

    private int getNotDevStoriesCount(int projectId, List<String> notDevLabels, boolean collectOnlyAccepted) {
        var pivotalApi = new PivotalApi(projectId, getCurrentWeekLabel(), collectOnlyAccepted);

        return pivotalApi
                .getStoriesDone()
                .stream()
                .mapToInt(story -> {
                    var labels = story.getLabels();

                    long notDevCount = labels
                            .stream()
                            .filter(label -> notDevLabels.contains(label.name))
                            .count();

                    if (story.getName().contains("regression")) {
                        notDevCount++;
                    }
                    ;

                    return (int) notDevCount;
                })
                .sum();
    }

    private List<String[]> getStoriesDone(int projectId, List<String> persons, boolean collectOnlyAccepted) {
        var digitalPivotalApi = new PivotalApi(projectId, getCurrentWeekLabel(), collectOnlyAccepted);
        List<String[]> storiesAndStoryPointsPerPerson = new ArrayList<>();

        int totalStoriesDone = persons
                .stream()
                .mapToInt(member -> {
                    var doneStoriesPerPerson = digitalPivotalApi.getDoneStoriesPerPerson(member);
                    int storiesDone = doneStoriesPerPerson.size();
                    var storyPoints = doneStoriesPerPerson
                            .stream()
                            .mapToDouble(Story::getEstimate)
                            .mapToInt(estimate -> (int) estimate)
                            .sum();
                    storiesAndStoryPointsPerPerson.add(new String[]{member, valueOf(storiesDone), valueOf(storyPoints)});

                    return storiesDone;
                })
                .sum();

        int totalStoryPoints = storiesAndStoryPointsPerPerson
                .stream()
                .mapToInt(dataArray -> parseInt(dataArray[2]))
                .sum();

        storiesAndStoryPointsPerPerson.add(new String[]{"Total stories", valueOf(totalStoriesDone)});
        storiesAndStoryPointsPerPerson.add(new String[]{"Total story points", valueOf(totalStoryPoints)});

        return storiesAndStoryPointsPerPerson;
    }

    private String convertToCsv(String[] data) {
        return Stream.of(data)
                .map(this::escapeSpecialCharacters)
                .collect(joining(","));
    }

    private String getLastWeekNumber() {
        return valueOf(LocalDate
                .now()
                .minus(1, WEEKS)
                .get(WeekFields.ISO.weekOfYear()));
    }

    private String getCurrentWeekNumber() {
        return valueOf(LocalDate
                .now()
                .get(WeekFields.ISO.weekOfYear()));
    }

    private String escapeSpecialCharacters(String data) {
        var escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    private String getLastWeekLabel() {
        return getProperty("week_label", "").isEmpty()
                ? "21ww" + getLastWeekNumber()
                : getProperty("week_label");
    }

    private String getCurrentWeekLabel() {
        return getProperty("week_label", "").isEmpty()
                ? "21ww" + getCurrentWeekNumber()
                : getProperty("week_label");
    }
}
