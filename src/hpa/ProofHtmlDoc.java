package hpa;

import javafx.application.Platform;
import javafx.scene.web.WebEngine;
import org.json.simple.JSONObject;

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

    private HPAController owner;
    private ArrayList<ProofHtmlDoc.ProofItem> resultList;
    private int nextResultNum;
    private String nextResultName;

    private class ProofItem {
        // this needs more stuff
        public String name, latex;

        public ProofItem(String name, String latex) {
            this.name = name;
            this.latex = latex;
        }
    }

    // the web view
    private WebEngine engine;

    public ProofHtmlDoc(HPAController owner, WebEngine engine) {
        // set owner
        this.owner = owner;
        // set engine
        this.engine = engine;
        // initialise axiom list
        resultList = new ArrayList<ProofHtmlDoc.ProofItem>();
        // initialise next result data
        nextResultNum = 1;
        nextResultName = "R"+nextResultNum;
        // load
        load();
    }

    private void load() {
        // update the middle
        updateMiddle();
        // load the webpage
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                engine.loadContent(htmlHeader + bodyStart + bodyMiddle + bodyEnd);
            }
        });
    }

    private void updateMiddle() {
        StringBuilder middle = new StringBuilder();
        for (ProofHtmlDoc.ProofItem ax : resultList) {
            if(ax.latex != null) {
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

    public String getNextResultName() {
        return nextResultName;
    }

    public int getNextResultNum() {
        return nextResultNum;
    }

    public void processAssume(JSONObject jo) {
        // add the new result
        try {
            int ix = Integer.valueOf((String)jo.get("index"));
        } catch (Exception e) {
            System.out.println("Error(ProofHtmlDoc.processAssume): missing or unrecognised index field");
            System.out.println(e);
            return;
        }
        String name = (String)jo.get("name");
        if(name == null || name.length() == 0) {
            System.out.println("Error(ProofHtmlDoc.processAssume): missing or empty name field");
            return;
        }
        // add the new result
        resultList.add(new ProofItem(name, null));
        // send command to get latex of name
        owner.sendCommand(HPACommand.printResult(name));
    }

    public void updateResult(String name, String latex) {
        for(ProofItem pi : resultList) {
            if(pi.name.equals(name)) {
                pi.latex = latex;
                // update display
                load();
                owner.displayMessage("Assumed predicate...");
                return;
            }
        }
        System.out.println("Error(ProofHtmlDoc.updateResult): result with name " + name + " not found.");
    }
}
