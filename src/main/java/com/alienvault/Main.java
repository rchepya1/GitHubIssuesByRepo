package com.alienvault;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GitHub Issues -------------
 * <p>
 * Create a program that generates a report about the the Issues belonging to a
 * list of github repositories ordered by creation time, and information about
 * the day when most Issues were created.
 * <p>
 * Input: ----- List of 1 to n Strings with Github repositories references with
 * the format "owner/repository"
 * <p>
 * <p>
 * Output: ------ String representation of a Json dictionary with the following
 * content:
 * <p>
 * - "issues": List containing all the Issues related to all the repositories
 * provided. The list should be ordered by the Issue "created_at" field (From
 * oldest to newest) Each entry of the list will be a dictionary with basic
 * Issue information: "id", "state", "title", "repository" and "created_at"
 * fields. Issue entry example: { "id": 1, "state": "open", "title": "Found a
 * bug", "repository": "owner1/repository1", "created_at":
 * "2011-04-22T13:33:48Z" }
 * <p>
 * - "top_day": Dictionary with the information of the day when most Issues were
 * created. It will contain the day and the number of Issues that were created
 * on each repository this day If there are more than one "top_day", the latest
 * one should be used. example: { "day": "2011-04-22", "occurrences": {
 * "owner1/repository1": 8, "owner2/repository2": 0, "owner3/repository3": 2 } }
 * <p>
 * <p>
 * Output example: --------------
 * <p>
 * {
 * "issues": [ { "id": 38, "state": "open", "title": "Found a bug",
 * "repository": "owner1/repository1", "created_at": "2011-04-22T13:33:48Z" }, {
 * "id": 23, "state": "open", "title": "Found a bug 2", "repository":
 * "owner1/repository1", "created_at": "2011-04-22T18:24:32Z" }, { "id": 24,
 * "state": "closed", "title": "Feature request", "repository":
 * "owner2/repository2", "created_at": "2011-05-08T09:15:20Z" } ], "top_day": {
 * "day": "2011-04-22", "occurrences": { "owner1/repository1": 2,
 * "owner2/repository2": 0 } } }
 * <p>
 * --------------------------------------------------------
 * <p>
 * You can create the classes and methods you consider. You can use any library
 * you need. Good modularization, error control and code style will be taken
 * into account. Memory usage and execution time will be taken into account.
 * <p>
 * Good Luck!
 */


/**
 * Class "Issue" to represent the required fields to be displayed
 */
class Issue {
    int id;
    String state;
    String title;
    String repository;
    Instant created_at;

    public void setId(int id) {
        this.id = id;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public Instant getCreated_at() {
        return created_at;
    }

    public void setCreated_at(Instant created_at) {
        this.created_at = created_at;
    }

    @Override
    public String toString() {
        return "Issue{" +
                "id: \"" + id + '\"' +
                ", state: \"" + state + '\"' +
                ", title: \"" + title + '\"' +
                ", repository: \"" + repository + '\"' +
                ", created_at: \"" + created_at + '\"' +
                '}';
    }
}

public class Main {

    /**
     * @param args String array with Github repositories with the format
     *             "owner/repository"
     */
    public static void main(String[] args) {

        List<JSONArray> jsonArrays = new ArrayList<>();
        List<Issue> issues = new ArrayList<>();
        String gitHubApiUrl = "https://api.github.com/repos/";
        String urlAppender = "/issues?per_page=100";
        for (String repo : args) {
            System.out.println("Repository: owner/repositoryName: " + repo);
            String url = gitHubApiUrl + repo + urlAppender;
            try {
                //Building a simple HTTP connection and sending the request (<a href="http://www.baeldung.com/java-http-request"> Java-HTTP-REQUEST</a>
                URL obj = new URL(url);
                System.out.println("Getting issues from the repo: " + url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();
                con.setRequestMethod("GET");
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                jsonArrays.add(new JSONArray(in.readLine()));
                con.disconnect();
                in.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Read the jsonArray populated by the response from the issues from all repositories
         * Convert them into JSONObjects so that they can be mapped to the "Issue" class and the required fields
         * Add to a List of all issues
         */
        jsonArrays.forEach(jsonArray -> {
            jsonArray.forEach(json -> {
                JSONObject obj = (JSONObject) json;
                Issue issue = new Issue();
                issue.setId(obj.getInt("id"));
                issue.setTitle(obj.get("title").toString());
                issue.setRepository(obj.get("repository_url").toString());
                issue.setState(obj.getString("state"));
                issue.setCreated_at(Instant.parse(obj.getString("created_at")));
                issues.add(issue);
            });
        });

        //Sort the issues list based on created_at (Date).
        issues.sort(Comparator.comparing(Issue::getCreated_at));

        // First Output as required by the problem statement
        System.out.println("Issues: ");
        issues.forEach(issue -> System.out.println(issue.toString()));

        //Write the data into a map of {Key - Date, Value: count of issues on that day} - All repos combined.
        //TreeMap, so that it's sorted which helps when highest count is present for more than one date and we need the latest date as per problem statement
        //Trimming out to YYYY-MM-DD so that count is obtained for a date instead of specific time of the day
        Map map = new TreeMap(issues.stream().collect(Collectors.groupingBy(issue -> {
            LocalDateTime localDateTime = LocalDateTime.ofInstant(issue.getCreated_at(), ZoneId.of(ZoneOffset.UTC.getId()));
            return localDateTime.toLocalDate();
        }, Collectors.counting())));

        //Getting the maximum occurrence date. Unlike regular comparator which returns 0 when values are same, returning 1 so that latest date is returned finally
        LocalDate maxOccurrenceDate = (LocalDate) Collections.max(map.entrySet(), Map.Entry.comparingByValue(
                (Comparator<Long>) (o1, o2) -> o1 >= o2 ? 1 : -1)).getKey();

        //List of the issues on the maxIssuesDate
        List<Issue> maxIssuesDateList = issues.stream().filter(issue -> maxOccurrenceDate.equals(LocalDateTime.ofInstant(issue.getCreated_at(),
                ZoneId.of(ZoneOffset.UTC.getId())).toLocalDate())).collect(Collectors.toList());

        //Separate the repositories and count for the issues on MaxIssuesDate
        Map maxIssuesDateRepoWiseMap = new HashMap();
        maxIssuesDateList.forEach(issue -> {
            if (null == maxIssuesDateRepoWiseMap || !maxIssuesDateRepoWiseMap.keySet().contains(issue.getRepository())) {
                maxIssuesDateRepoWiseMap.put(issue.getRepository(), 1);
            } else {
                maxIssuesDateRepoWiseMap.put(issue.getRepository(), (Integer) maxIssuesDateRepoWiseMap.get(issue.getRepository()) + 1);
            }
        });

        //Printing the output for top_day fields - Date, List of occurrences(repo name appended by its count for the MaxIssues day)
        List<String> occurrences = new ArrayList<>();
        maxIssuesDateRepoWiseMap.forEach((k, v) -> {
            String repoName = k.toString().substring(gitHubApiUrl.length());
            occurrences.add(repoName + ": " + v);
        });
        System.out.println("\ntop_day: {" + "day: " + maxOccurrenceDate + "," + " occurrences :" + occurrences + "}");
    }
}