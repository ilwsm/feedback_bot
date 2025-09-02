package org.example.admin;

import ch.qos.logback.core.util.StringUtil;
import io.javalin.Javalin;
import io.javalin.http.Context;
import org.example.dto.FeedbackDto;
import org.example.repository.FeedbackRepository;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class AdminServer {

    private final FeedbackRepository feedbackRepo;

    public AdminServer(FeedbackRepository feedbackRepo) {
        this.feedbackRepo = feedbackRepo;
    }

    private static Javalin startServer() {
        return Javalin.create(config -> {
            config.showJavalinBanner = false;
        }).start(7070);
    }

    public void start() {
        Javalin app = startServer();

        app.get("/", ctx -> {
            ctx.contentType("text/html; charset=UTF-8");
            ctx.result(HTML.WELCOME);
        });

        app.get("/feedbacks", this::feedbacks_handle);

        app.get("/feedbacks/export", this::export_csv_handle);
    }

    private void feedbacks_handle(@NotNull Context ctx) {
        String branch = ctx.queryParam("branch");
        String role = ctx.queryParam("role");
        String criticality = ctx.queryParam("criticality");

        List<String> branches = feedbackRepo.findDistinctBranches();
        List<String> roles = feedbackRepo.findDistinctRoles();

        String branchOptions = branches.stream()
                .map(b -> String.format("<option value=\"%s\" %s>%s</option>",
                        b,
                        b.equals(branch) ? "selected" : "",
                        b))
                .reduce("", (a, b) -> a + b);

        String roleOptions = roles.stream()
                .map(r -> String.format("<option value=\"%s\" %s>%s</option>",
                        r,
                        r.equals(role) ? "selected" : "",
                        r))
                .reduce("", (a, b) -> a + b);

        StringBuilder crsb = new StringBuilder();
        int criticalityInt = !StringUtil.isNullOrEmpty(criticality) ? Integer.parseInt(criticality) : 0;
        // <option value="1">1</option>
        for (int i = 1; i <= 5; i++) {
            crsb.append(String.format("<option value=\"%d\" %s>%d</option>", i, i == criticalityInt ? " selected" : "", i));
        }
        String crOptions = crsb.toString();

        List<FeedbackDto> feedbacks = feedbackRepo.findFiltered(branch, role, criticality)
                .stream()
                .map(FeedbackDto::fromEntity)
                .toList();

        StringBuilder html = new StringBuilder(String.format(HTML.FEEDBACKS[0],
                branchOptions, roleOptions, crOptions,
                branch != null ? branch : "",
                role != null ? role : "",
                criticality != null ? criticality : ""));


        for (FeedbackDto f : feedbacks) {
            html.append("<tr>")
                    .append("<td>").append(f.id()).append("</td>")
                    .append("<td>").append(f.message()).append("</td>")
                    .append("<td>").append(f.sentiment()).append("</td>")
                    .append("<td>").append(f.criticality()).append("</td>")
                    .append("<td>").append(f.recommendation()).append("</td>")
                    .append("<td>").append(f.chatId()).append("</td>")
                    .append("<td>").append(f.userRole()).append("</td>")
                    .append("<td>").append(f.userBranch()).append("</td>")
                    .append("<td>").append(f.createdAt()).append("</td>")
                    .append("</tr>");
        }
        html.append(HTML.FEEDBACKS[1]);

        ctx.contentType("text/html; charset=UTF-8");
        ctx.result(html.toString());
    }

    private void export_csv_handle(@NotNull Context ctx) {
        String branch = ctx.queryParam("branch");
        String role = ctx.queryParam("role");
        String criticality = ctx.queryParam("criticality");

        List<FeedbackDto> feedbacks = feedbackRepo.findFiltered(branch, role, criticality)
                .stream()
                .map(FeedbackDto::fromEntity)
                .toList();

        byte[] csvBytes = generateCsvForExcel(feedbacks);

        ctx.contentType("text/csv; charset=UTF-8");
        ctx.header("Content-Disposition", "attachment; filename=feedbacks.csv");
        ctx.result(csvBytes);
    }

    private byte[] generateCsvForExcel(List<FeedbackDto> feedbacks) {
        StringBuilder csv = new StringBuilder();

        // BOM
        csv.append("\uFEFF");

        csv.append("ID,Message,Sentiment,Criticality,Recommendation,ChatId,User Role,User Branch,Created At\n");

        for (FeedbackDto f : feedbacks) {
            csv.append(String.format("%s,\"%s\",%s,%s,\"%s\",%s,\"%s\",\"%s\",%s\n",
                    f.id(),
                    escapeCsv(f.message()),
                    escapeCsv(f.sentiment()),
                    f.criticality(),
                    escapeCsv(f.recommendation()),
                    f.chatId(),
                    escapeCsv(f.userRole()),
                    escapeCsv(f.userBranch()),
                    f.createdAt() != null ? f.createdAt() : ""));
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
