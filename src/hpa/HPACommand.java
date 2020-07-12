package hpa;

import org.json.JSONObject;

public abstract class HPACommand {
    public static final int MoveUp = 0;
    public static final int MoveRight = 1;
    public static final int MoveDown = 2;
    public static final int MoveLeft = 3;

    public static JSONObject listAxioms(String handler) {
        final JSONObject cmdObj = new JSONObject();
        // put values into object
        cmdObj.put("cmd", "axioms");
        cmdObj.put("handler", handler);
        return cmdObj;
    }

    public static JSONObject printPredicate(String handler, String name, boolean isAxiom) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd", "print");
        cmdObj.put("handler", handler);
        cmdObj.put("name", name);
        cmdObj.put("type", isAxiom ? "axiom" : "proofstep");
        return cmdObj;
    }

    public static JSONObject printAxiom(String handler, String name) {
        return printPredicate(handler, name, true);
    }

    public static JSONObject printDetails(String handler, String name) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","details");
        cmdObj.put("name", name);
        cmdObj.put("handler", handler);
        return cmdObj;
    }

    public static JSONObject readPredicate(String handler, String predicate) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","read");
        cmdObj.put("predicate",predicate);
        cmdObj.put("handler", handler);
        return cmdObj;
    }

    public static JSONObject assume(String handler, String name, String predicate) {
        final JSONObject cmdObj =  new JSONObject();
        cmdObj.put("cmd","assume");
        cmdObj.put("name", name);
        cmdObj.put("predicate", predicate);
        cmdObj.put("handler", handler);
        return cmdObj;
    }

    public static JSONObject instantiateSchema(String handler, String name, String schemaName, String[] patvars, String[] predicates) {
        // build JSON object
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd", "instantiateSchema");
        cmdObj.put("name", name);
        cmdObj.put("schema", schemaName);
        cmdObj.put("handler", handler);
        // add matching
        if(patvars.length == predicates.length && patvars.length > 0) {
            for (int i = 0; i < patvars.length; i++) {
                cmdObj.put(patvars[i], predicates[i]);
            }
        } else {
            System.out.println("Warning(HPACommand.instantiateSchema): pattern variables and predicates do not have same size, not added to command");
            System.out.println(cmdObj.toString());
        }
        // return JSON Object as String
        return cmdObj;
    }

    public static JSONObject modusPonens(String handler, String name, String pimpq, String p) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd", "modusPonens");
        cmdObj.put("handler", handler);
        cmdObj.put("name", name);
        cmdObj.put("pimpqn", pimpq);
        cmdObj.put("pn", p);
        return cmdObj;
    }

    public static JSONObject generalise(String handler, String name, String resultName, String variable) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd", "generalise");
        cmdObj.put("handler", handler);
        cmdObj.put("name", name);
        cmdObj.put("result", resultName);
        cmdObj.put("var", variable);
        return cmdObj;
    }

    public static JSONObject instantiateAt(String handler, String name, String fan, String xvar) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","instantiateAt");
        cmdObj.put("handler", handler);
        cmdObj.put("name", name);
        cmdObj.put("fan", fan);
        cmdObj.put("xvarp", xvar);
        return cmdObj;
    }

    public static JSONObject splitAnd(String handler, String pname, String qname, String pandq) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","splitAnd");
        cmdObj.put("handler", handler);
        cmdObj.put("pname", pname);
        cmdObj.put("qname", qname);
        cmdObj.put("pandq", pandq);
        return cmdObj;
    }

    public static JSONObject setFocus(String handler, String name) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","setFocus");
        cmdObj.put("handler", handler);
        cmdObj.put("name", name);
        return cmdObj;
    }

    public static JSONObject transformFocus(String handler, String logiclaw) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","transformFocus");
        cmdObj.put("handler", handler);
        cmdObj.put("name", logiclaw);
        return cmdObj;
    }

    public static JSONObject recordFocus(String handler, String name) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","recordFocus");
        cmdObj.put("handler", handler);
        cmdObj.put("name", name);
        return cmdObj;
    }

    public static JSONObject clearFocus(String handler) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","clearFocus");
        cmdObj.put("handler", handler);
        return cmdObj;
    }

    public static JSONObject moveFocus(String handler, int direction) {
        return HPACommand.moveFocus(handler, direction, false);
    }

    public static JSONObject moveFocus(String handler, int direction, boolean isBranch) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd", "moveFocus");
        cmdObj.put("handler", handler);
        if(isBranch) {
            cmdObj.put("direction", "branch");
            cmdObj.put("branch", String.valueOf(direction));
        }
        else {
            switch(direction) {
                case MoveUp -> cmdObj.put("direction", "up");
                case MoveRight -> cmdObj.put("direction", "right");
                case MoveDown -> cmdObj.put("direction", "down");
                case MoveLeft -> cmdObj.put("direction", "left");
            }
        }
        return cmdObj;
    }

    public static JSONObject liftResult(String handler, String name, String result, String assumption) {
        final JSONObject cmdObj = new JSONObject();
        cmdObj.put("cmd","liftResult");
        cmdObj.put("handler", handler);
        cmdObj.put("name", name);
        cmdObj.put("result", result);
        cmdObj.put("assumption", assumption);
        return cmdObj;
    }
}
