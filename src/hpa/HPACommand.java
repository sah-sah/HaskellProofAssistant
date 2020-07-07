package hpa;

import org.json.simple.JSONObject;

public abstract class HPACommand {
    public static String listAxioms() {
        final JSONObject cmdObj = new JSONObject();
        // put values into object
        cmdObj.put("cmd", "axioms");
        //cmdObj.put("other", "stuff");
        //System.out.println(cmdObj.toString());
        return cmdObj.toString();
    }

    private static String printPredicate(String name, boolean isAxiom) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd", "print");
        cmdObj.put("name", name);
        cmdObj.put("type", isAxiom ? "axiom" : "proofstep");
        return cmdObj.toString();
    }

    public static String printAxiom(String name) {
        return printPredicate(name, true);
    }

    public static String printResult(String name) {
        return printPredicate(name, false);
    }

    public static String readPredicate(String predicate, String source) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","read");
        cmdObj.put("predicate",predicate);
        cmdObj.put("source", source);
        return cmdObj.toString();
    }

    // TODO: commands for proof steps should return the completed proof step for display
    // TODO: should return all the proofstep information
}
