package hpa;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import org.json.JSONObject;

import java.util.ArrayList;

/*
* TODO: update PredicateServer so assume and similar return info about the new result
*
 */

public class ProofHtmlDoc {

    private final String css =
            "<style>\n" +
                    ".affix { bottom: 0; width: 100%; z-index: 9999 !important; }\n" +
                    "section.proof .focus { height: 100px; }\n" +
                    "section.proof .focus-dummy { height: 130px; }\n" +
                    "section.proof .proof-item { text-align: left; margin-top: 5px; margin-bottom: 5px; height: 100px; }\n" +
                    "section.proof .display-math { text-align: left; }\n" +
                    "</style>\n";

    private final String htmlHeader =
            "<!DOCTYPE html>\n" +
                    "<html lang=\"en\">\n" +
                    "<head>\n" +
                    "    <meta charset=\"UTF-8\">\n" +
                    "    <title>Axioms</title>\n" +
                    "\n" +
                    "    <script src=\"https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js\"></script>\n" +
                    "    <script src=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js\"></script>\n" +
                    "    <script type=\"text/javascript\" async=\"async\" src=\"https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.7/MathJax.js?config=TeX-MML-AM_CHTML\"></script>\n" +
                    "\n" +
                    "    <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css\">\n" +
                    "    <link rel=\"stylesheet\" href=\"https://maxcdn.bootstrapcdn.com/font-awesome/4.6.3/css/font-awesome.min.css\">\n" +
                    css +
                    "</head>";

    private final String bodyStart =
            "<body>\n" +
                    "\n" +
                    "<div class=\"container-fluid\">\n" +
                    "    <section class=\"proof\">";

    private String bodyMiddle;

    private final String bodyEnd =
            "<div class=\"row\" data-spy=\"affix\">\n" +
                    "    <div class=\"col-md-12\">\n" +
                    "        <div class=\"focus panel panel-info\">\n" +
                    "        Focus\n" +
                    "        </div>\n" +
                    "     </div>\n" +
                    "</div>\n" +
                    "<div class=\"row\">\n" +
                    "    <div class=\"col-md-12\">\n" +
                    "        <div class=\"focus-dummy\">\n" +
                    "        </div>\n" +
                    "    </div>\n" +
                    "</div>\n" +
                    "</section>\n" +
                    "</div>\n" +
                    "\n" +
                    "</body>\n" +
                    "</html>";

    private final HPAController owner;
    private final ArrayList<ProofHtmlDoc.ProofItem> resultList;

    private class ProofItem {
        // this needs more stuff
        public String name, latex;
        public String assumptions;

        public ProofItem(String name, String latex) {
            this.name = name;
            this.latex = latex;
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
        // load
        load();
    }

    private void load() {
        // update the middle
        updateMiddle();
        // load the webpage
        Platform.runLater(() -> engine.loadContent(htmlHeader + bodyStart + bodyMiddle + bodyEnd));
    }

    private void updateMiddle() {
        StringBuilder middle = new StringBuilder();
        for (ProofHtmlDoc.ProofItem ax : resultList) {
            if(ax.latex != null) {
                // this should be in the ProofItem class
                middle.append("<div class=\"row\">\n");
                middle.append("    <div class=\"col-md-12\">\n");
                middle.append("        <div class=\"proof-item well\">\n");
                middle.append("            <p>");
                middle.append("                <h4>").append(ax.name).append("</h4>\n");
                middle.append("                <div class=\"display-math\">\n");
                middle.append("                    \\(").append(ax.latex).append("\\)\n");
                middle.append("                </div>\n");
                middle.append("            </p>\n");
                middle.append("        </div>\n");
                middle.append("    </div>\n");
                middle.append("</div>\n");
            }
        }
        bodyMiddle = middle.toString();
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

    public void processAssume(JSONObject jo) {
        // add the new result
        /*
        try {
            int ix = Integer.valueOf((String)jo.get("index"));
        } catch (Exception e) {
            System.out.println("Error(ProofHtmlDoc.processAssume): missing or unrecognised index field");
            System.out.println(e);
            return;
        }
        */
        String name = (String)jo.get("name");
        if(name == null || name.length() == 0) {
            System.out.println("Error(ProofHtmlDoc.processAssume): missing or empty name field");
            return;
        }
        // add the new result
        resultList.add(new ProofItem(name, null));
        // send command to get latex of name
        owner.sendCommand(HPACommand.printDetails(name));
    }

    public void processIS(JSONObject jo) {
        String name = (String)jo.get("name");
        if(name == null || name.length() == 0) {
            System.out.println("Error(ProofHtmlDoc.processIS): missing or empty name field");
            return;
        }
        // add the new result
        resultList.add(new ProofItem(name, null));
        // send command to get latex of name
        owner.sendCommand(HPACommand.printDetails(name));
    }

    public void processDetails(JSONObject jo) {
        //System.out.println(jo);
        // get name
        String name = (String)jo.get("name");
        if(name == null || name.length() == 0) {
            System.out.println("Error(ProofHtmlDoc.processDetails): missing name field");
            System.out.println(jo);
            return;
        }
        // get latex string
        String latex = (String)jo.get("result");
        if(latex == null || latex.length() == 0) {
            System.out.println("Error(ProofHtmlDoc.processDetails): missing result field");
            System.out.println(jo);
            return;
        }
        // update
        for(ProofItem pi : resultList) {
            if(pi.name.equals(name)) {
                pi.latex = latex;
                // update display
                load();
                owner.displayMessage("Updated predicates...");
                return;
            }
        }
        System.out.println("Error(ProofHtmlDoc.updateResult): result with name " + name + " not found.");
    }

    public void updateResult(String name, String latex) {
        for(ProofItem pi : resultList) {
            if(pi.name.equals(name)) {
                pi.latex = latex;
                // update display
                load();
                owner.displayMessage("Assumed predicate..."); // this won't always be from an assumption
                return;
            }
        }
        System.out.println("Error(ProofHtmlDoc.updateResult): result with name " + name + " not found.");
    }

    public ArrayList<String> getResultNames() {
        // is there an easier way to do this? using map?
        ArrayList<String> names = new ArrayList<>();
        for(ProofItem pi : resultList) {
            names.add(pi.name);
        }
        return names;
    }
}
