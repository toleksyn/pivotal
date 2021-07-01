import onespot.pivotal.PivotalApi;
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

import static java.lang.String.format;
import static java.lang.System.getProperty;
import static java.time.temporal.ChronoUnit.WEEKS;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class PivotalApiTest {

    @Test
    public void getMetricsData() throws FileNotFoundException {
        var digitalTeam = splitIntoList(getProperty("digital_team"));
        int digitalProjectId = Integer.parseInt(getProperty("digitalProjectId"));
        var storiesDone = new ArrayList<String[]>();
        storiesDone.add(new String[] {"name", "stories done"});
        storiesDone.addAll(getStoriesDone(digitalProjectId, digitalTeam));

        var monitoringTeam = splitIntoList(getProperty("monitoring_team"));
        int monitoringProjectId = Integer.parseInt(getProperty("monitoringProjectId"));
        var storiesDoneMonitoring = getStoriesDone(monitoringProjectId, monitoringTeam);

        var notDevLabels = splitIntoList(getProperty("not_dev_labels"));
        int notDevStoriesTotal = getNotDevStoriesCount(digitalProjectId, notDevLabels);

        storiesDoneMonitoring.add(new String[] {"Not dev total", String.valueOf(notDevStoriesTotal)});
        storiesDone.addAll(storiesDoneMonitoring);

        var csvOutputFile = new File(format("metrics_%s.csv", getLastWeekLabel()));
        try (var printWriter = new PrintWriter(csvOutputFile)) {
            storiesDone
                    .stream()
                    .map(this::convertToCSV)
                    .forEach(printWriter::println);
        }
    }

    private List<String> splitIntoList(String string) {
        return Arrays.stream(string
                .split(","))
                .collect(toList());
    }

    private int getNotDevStoriesCount(int projectId, List<String> notDevLabels) {
        var pivotalApi = new PivotalApi(projectId, getLastWeekLabel());

        return pivotalApi
                .getStoriesDone()
                .stream()
                .mapToInt(story -> {
                    var labels = story.getLabels();

                    long notDevCount = labels
                            .stream()
                            .filter(label -> notDevLabels.contains(label.name))
                            .count();

                    if(story.getName().contains("regression")) {
                        notDevCount++;
                    };

                    return (int) notDevCount;
                })
                .sum();
    }

    private List<String[]> getStoriesDone(int projectId, List<String> persons) {
        var digitalPivotalApi = new PivotalApi(projectId, getLastWeekLabel());
        List<String[]> dataLines = new ArrayList<>();

        int totalStoriesDone = persons
                .stream()
                .mapToInt(member -> {
                    int storiesDone = digitalPivotalApi.getDoneStoriesPerPerson(member).size();
                    dataLines.add(new String[] {member, String.valueOf(storiesDone)});
                    return storiesDone;
                })
                .sum();

        dataLines.add(new String[] {"Total", String.valueOf(totalStoriesDone)});

        return dataLines;
    }

    public String convertToCSV(String[] data) {
        return Stream.of(data)
                .map(this::escapeSpecialCharacters)
                .collect(joining(","));
    }

    public String getLastWeekNumber() {
        return String.valueOf(LocalDate
                .now()
                .minus(1, WEEKS)
                .get(WeekFields.ISO.weekOfYear()));
    }
    public String escapeSpecialCharacters(String data) {
        var escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }

    public String getLastWeekLabel() {
        return getProperty("week_label", "").isEmpty()
                ? "21ww" + getLastWeekNumber()
                : getProperty("week_label");
    }
}
