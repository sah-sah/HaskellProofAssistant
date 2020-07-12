package hpa;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class HPASession {
    private final List<JSONObject> commands;
    private int nextCommandId;

    public HPASession() {
        commands = new ArrayList<>();
        nextCommandId = 0;
    }

    public void recordCommand(JSONObject cmd) {
        commands.add(cmd);
        nextCommandId++;
    }

    public void removeCommandWithId(int cmdId) {

    }

    public JSONArray toJSONArray() {
        // write to file as a JSONObject
        return new JSONArray(commands);
    }

    public void loadFromFile(File saveFile) {

    }

    public int getNextCommandId() {
        return nextCommandId;
    }

    public List<JSONObject> getCommands() {
        return commands;
    }
}