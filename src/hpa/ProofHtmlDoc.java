package hpa;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProofHtmlDoc {

    private final String css =
            "<style>\n" +
                    "section.proof .proof-item { border-bottom: 3px solid; height: 103px; background-color:#EBF5FC; }\n" +
                    "section.proof .proof-result { position: relative; height: 100px; }\n" +
                    "section.proof .display-math { margin: 0; position: absolute; top: 50%; -ms-transform: translateY(-50%); transform: translateY(-50%); font-size:20px; }\n" +
                    "section.proof a { color: gray; background-color: transparent; }\n" +
                    "section.proof a:hover a:active { text-decoration: underline; }\n" +
                    "section.proof .deductions { margin: 0; position: absolute; top: 80%; left: 0%; -ms-transform: translateY(-50%); transform: translateY(-50%); font-size:12px; }\n" +
                    "section.proof .proof-name { position: relative; height: 100px; border-right: 1px dashed; }\n" +
                    "section.proof .name { margin: 0; position: absolute; top: 50%; left: 50%; -ms-transform: translate(-50%, -50%); transform: translate(-50%, -50%); font-size: 25px; }\n" +
                    "section.proof .assumptions { margin: 0; position: absolute; top: 15%; left: 20%; -ms-transform: translate(-50%, -50%); transform: translate(-50%, -50%); font-size:12px; white-space: nowrap; }\n" +
                    "section.proof .focus { height: 100px; }\n" +
                    "</style>\n";

    private final String popoverScript =
            "<script>\n" +
                    "$(document).ready(function(){\n" +
                    "  $('[data-toggle=\"popover\"]').popover({ html: true });\n" +
                    "});\n" +
                    "</script>";

    private final String htmlHeader =
            "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>Proof</title>\n" +
                    "    <script type=\"text/javascript\" async=\"async\" src=\"https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.7/MathJax.js?config=TeX-MML-AM_CHTML\"></script>\n" +
                    "    <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.5.0/css/bootstrap.min.css\">\n" +
                    "    <script src=\"https://ajax.googleapis.com/ajax/libs/jquery/3.5.1/jquery.min.js\"></script>\n" +
                    "    <script src=\"https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.16.0/umd/popper.min.js\"></script>\n" +
                    "    <script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/4.5.0/js/bootstrap.min.js\"></script>\n" +
                    "    <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css\">\n" +
                    css +
                    popoverScript +
                    "</head>";

    private final String bodyStart =
            "<body>\n" +
                    "<div class=\"container-fluid\">\n" +
                    "    <section class=\"proof\">";

    private String bodyMiddle;
    private String bodyFocus;

    private final String bodyEnd =
                    "</section>\n" +
                    "</div>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html>";

    private final HPAController owner;
    private final List<ProofHtmlDoc.ProofItem> resultList;
    private ProofItem focus;

    private class ProofItem {
        // this needs more stuff
        public String name, latex, type;
        public List<String> assumptions;
        public List<Deduction> deductions;

        public ProofItem(String name) {
            this(name, null);
        }

        public ProofItem(String name, String latex) {
            this.name = name;
            this.latex = latex;
            this.type = null;
            this.assumptions = new ArrayList<>();
            this.deductions = new ArrayList<>();
        }

        public String getHtml() {
            String asText, allAsText = assumptions.toString();
            switch(assumptions.size()) {
                case 0 -> { asText = "[]=>"; allAsText = "None"; }
                case 1 -> asText = assumptions.get(0) + "=>";
                default -> asText = assumptions.get(0) + ",...=>";
            }

            StringBuilder html = new StringBuilder();
            html.append("    <div class=\"col-2\">\n");
            html.append("        <div class=\"proof-name\">\n");
            html.append("            <div class=\"name\">").append(this.name).append("</div>\n");
            html.append("                <a href=\"#\" class=\"assumptions\" data-placement=\"bottom\" data-toggle=\"popover\" title=\"<em>Assumptions</em>\" data-content=\"").append(allAsText).append("\">").append(asText).append("</a>\n");
            html.append("        </div>\n");
            html.append("    </div>\n");
            html.append("    <div class=\"col-10\">\n");
            html.append("        <div class=\"proof-result\">\n");
            html.append("            <div class=\"display-math\">\n");
            html.append("\\(").append(this.latex).append("\\)\n");
            html.append("            </div>\n");
            html.append("            <a href=\"#\" class=\"deductions\" data-toggle=\"popover\" title=\"<em>Deductions</em>\" data-content=\"<ul class='deductions-list'>");
            for (Deduction d : deductions) html.append(d.getHtml());
            html.append("</ul>\">Deductions</a>\n");
            html.append("        </div>\n");
            html.append("    </div>\n");
            return html.toString();
        }
    }

    private class Deduction {
        public List<String> from;
        public String description;

        public Deduction(List<String> from, String description) {
            this.from = from;
            this.description = description;
        }

        public String getHtml() {
            // return some html code for this deduction
            // <li class='deduction-item'>R1, A2 lift result</li>
            StringBuilder html = new StringBuilder();
            html.append("<li class='deduction-item'>");
            for(String r : from) {
                html.append(r + ", ");
            }
            html.append(description.replaceAll("\"",""));
            html.append("</li>");
            return html.toString();
        }
    }

    // the web view
    private final WebEngine engine;

    public ProofHtmlDoc(HPAController owner, WebEngine engine) {
        // set owner
        this.owner = owner;
        // set engine
        this.engine = engine;
        // initialise axiom list
        resultList = new ArrayList<>();
        focus = null;
        // load
        load();
    }

    private void load() {
        // update the middle
        updateMiddle();
        // update the focus
        updateFocus();
        // load the webpage
        Platform.runLater(() -> engine.loadContent(htmlHeader + bodyStart + bodyMiddle + bodyFocus + bodyEnd));
        //System.out.println(htmlHeader + bodyStart + bodyMiddle + bodyFocus + bodyEnd);
    }

    private void updateMiddle() {
        StringBuilder middle = new StringBuilder();
        for (ProofHtmlDoc.ProofItem ax : resultList) {
            if(ax.latex != null) {
                System.out.println("Adding result " + ax.name);
                // this should be in the ProofItem class
                middle.append("<div class=\"row proof-item\">\n");
                middle.append(ax.getHtml());
                middle.append("</div>\n");
            }
        }
        bodyMiddle = middle.toString();
    }

    public void processResponse(String cmd, JSONObject jo) {
        switch(cmd) {
            case "assume" -> addResult(jo);
            case "details" -> processDetails(jo);
            case "instantiateSchema" -> addResult(jo);
            case "modusPonens" -> addResult(jo);
            case "instantiateAt" -> addResult(jo);
            case "splitAnd" -> { addResult(jo, "pname"); addResult(jo, "qname"); }
            case "setFocus" -> {
                try {
                    this.focus = new ProofItem("focus", (String)jo.get("focus"));
                    load();
                    owner.displayMessage("Updated focus...");
                } catch (JSONException | ClassCastException je) {
                    System.out.println("Error(ProofHtmlDoc.processResponse): missing or empty focus field when setting focus");
                    System.out.println(jo);
                }
            }
            case "moveFocus", "transformFocus" -> {
                try {
                    this.focus.latex = (String)jo.get("focus");
                    load();
                    owner.displayMessage("Updated focus...");
                } catch (JSONException | ClassCastException je) {
                    System.out.println("Error(ProofHtmlDoc.processResponse): missing or empty focus field when moving or transforming focus");
                    System.out.println(jo);
                }

            }
            case "recordFocus" -> addResult(jo);
            case "clearFocus" -> {
                this.focus = null;
                load();
                owner.displayMessage("Cleared focus...");
            }
            case "generalise" -> addResult(jo);
            case "liftResult" -> addResult(jo);
            default -> {
                System.out.println("Error(ProofHtmlDoc.processResponse): unrecognised command");
                System.out.println(jo);
            }
        }
    }

    private void addResult(JSONObject jo, String nameKey) {
        try {
            String name = (String)jo.get(nameKey);
            resultList.add(new ProofItem(name));
            owner.sendCommand(HPACommand.printDetails("proofDoc", name));
        } catch (JSONException | ClassCastException je) {
            System.out.println("Error(ProofHtmlDoc.processResponse): invalid JSON object");
            System.out.println(jo);
        }
    }

    private void addResult(JSONObject jo) { addResult(jo, "name"); }

    public void updateResult(String name, String latex) {
        for(ProofItem pi : resultList) {
            if(pi.name.equals(name)) {
                pi.latex = latex;
                // update display
                load();
                owner.displayMessage("Updated proof..."); // this won't always be from an assumption
                return;
            }
        }
        System.out.println("Error(ProofHtmlDoc.updateResult): result with name " + name + " not found.");
    }

    private void updateFocus() {
        StringBuilder focusHtml = new StringBuilder();
        focusHtml.append("<div class=\"row\">\n");
        focusHtml.append("    <div class=\"col-12\">\n");
        focusHtml.append("        <div class=\"focus border\">\n");
        if(focus != null) {
            focusHtml.append("\\(").append(focus.latex).append("\\)");
        } else {
            focusHtml.append("Focus\n");
        }
        focusHtml.append("        </div>\n");
        focusHtml.append("    </div>\n");
        focusHtml.append("</div>\n");
        bodyFocus = focusHtml.toString();
    }

    public String getNextResultName(String prefix) {
        boolean valid = false;
        int ix = getNextResultNum() - 1;
        String name = prefix + ix;
        // keep trying until we find a unique name
        while(!valid) {
            ix++;
            name = prefix + ix;
            valid = true;
            for(ProofItem pi : resultList) {
                if(pi.name.equals(name)) {
                    valid = false;
                    break;
                }
            }
        }
        return name;
    }

    public int getNextResultNum() {
        return resultList.size() + 1;
    }

    public void processDetails(JSONObject jo) {
        //System.out.println(jo);
        // parse JSON object
        try {
            // get name
            String name = (String)jo.get("name");
            // get latex string
            String latex = (String)jo.get("result");
            // get type
            String type = (String)jo.get("type");
            // get assumptions
            JSONArray assumptionsJSON = jo.getJSONArray("assumptions");
            List<String> assumptions = new ArrayList<>();
            for(Object o : assumptionsJSON) assumptions.add((String)o);
            // get deductions e.g. [ [["A1"],"LogicLaw [Transform \"commutativeAnd\"]"] ]
            JSONArray dsJSON = jo.getJSONArray("deductions");
            List<Deduction> deductions = new ArrayList<>();
            for(Object o : dsJSON) {
                // each deduction is an array
                JSONArray d = (JSONArray) o;
                // each deduction has two entries
                // the first is an array of strings
                JSONArray rs = (JSONArray)d.get(0);
                ArrayList<String> from = new ArrayList<>();
                for(Object s : rs) from.add((String)s);
                // the second is a string
                String desc = (String)d.get(1);
                // create the Deduction object
                deductions.add(new Deduction(from,desc));
            }

            // update the proof item
            for(ProofItem pi : resultList) {
                if(pi.name.equals(name)) {
                    pi.latex = latex;
                    pi.type = type;
                    pi.assumptions = assumptions;
                    pi.deductions = deductions;
                    // update display
                    load();
                    owner.displayMessage("Updated predicates...");
                    return;
                }
            }
            // we failed to find the name
            System.out.println("Error(ProofHtmlDoc.processDetails): result with name " + name + " not found.");
        } catch (JSONException | ClassCastException je) {
            System.out.println("Error(ProofHtmlDoc.processDetails): invalid JSON data");
            System.out.println(jo);
        }
    }

    public ArrayList<String> getResultNames() {
        // is there an easier way to do this? using map?
        ArrayList<String> names = new ArrayList<>();
        for(ProofItem pi : resultList) {
            names.add(pi.name);
        }
        return names;
    }

    public List<String> getAssumptionsOfResult(String name) {
        for(ProofItem pi : resultList) {
            if(pi.name.equals(name)) {
                return pi.assumptions;
            }
        }
        // else we didn't find the result
        return new ArrayList<>();
    }
}
