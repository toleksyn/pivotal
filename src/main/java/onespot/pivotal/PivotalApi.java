package onespot.pivotal;

import onespot.pivotal.api.PivotalTracker;
import onespot.pivotal.api.resources.Person;
import onespot.pivotal.api.resources.Story;

import java.util.List;

import static java.lang.System.getProperty;
import static java.util.stream.Collectors.toList;
import static onespot.pivotal.api.resources.Story.StoryFieldNames.*;

public class PivotalApi {
    private PivotalTracker pivotalTracker = new PivotalTracker(getProperty("pivotal_token"));
    private List<Story> storiesDone;

    public PivotalApi(int projectId, String label, boolean collectOnlyAccepted) {
        this.storiesDone = getDoneStoriesForLabel(projectId, label, collectOnlyAccepted);
    }

    public List<Story> getDoneStoriesForLabel(int projectId, String label, boolean collectOnlyAccepted) {
        var project = pivotalTracker
                .projects()
                .id(projectId);

        var stories = project
                .stories()
                .withFields(owners, name, estimate, labels)
                .withLabel(label);

        var acceptedStories = stories
                .withState(Story.StoryState.accepted)
                .getAll();

        if (!collectOnlyAccepted) {
            var deliveredStories = stories
                    .withState(Story.StoryState.delivered)
                    .getAll();

            acceptedStories.addAll(deliveredStories);
        }

        return acceptedStories;
    }

    public List<Story> getDoneStoriesPerPerson(String personName) {
        return storiesDone
                .stream()
                .filter(story -> story
                        .getOwners()
                        .stream()
                        .map(Person::getName)
                        .anyMatch(name -> name.contains(personName)))
                .collect(toList());
    }

    public List<Story> getStoriesDone() {
        return storiesDone;
    }
}
