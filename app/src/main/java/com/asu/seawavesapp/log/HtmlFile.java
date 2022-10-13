package com.asu.seawavesapp.log;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;

import com.asu.seawavesapp.R;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class HtmlFile extends Logger {
    private int display;

    public HtmlFile(File logFile, AppCompatActivity context) {
        super(logFile, context);
        this.display = display;
    }

    private String getHtmlHeader() {
        return "<html>" +
                "<body>";
    }

    private String getHtmlFooter() {
        return "</body>" +
                "</html>";
    }

    private String makeDefinitionList(String description, HashMap<String, String> map) {
        String content = "<h3>" + description + "</h3>";
        content += "<dl>";
        for (Map.Entry<String, String> entry : map.entrySet()) {
            content += "<dt>" + entry.getKey() + "</dt>" +
                    "<dd>" + entry.getValue() + "</dd>";
        }
        content += "</dl>";
        return content;
    }

    /**
     * Generates the HTML with the info in it and returns the URL of the HTML file
     * @return URL of the HTML file
     */
    public String generateInfoContent() {
        String content = getHtmlHeader();

        // retrieve boat information
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        Resources res = getContext().getResources();
        HashMap<String, String> boatInfo = new HashMap<>();
        boatInfo.put("Boat ID", pref.getString(res.getString(R.string.id_key), "0"));
        boatInfo.put("Boat Name", pref.getString(res.getString(R.string.boat_key), "Boat"));
        boatInfo.put("Boat Owner", pref.getString(res.getString(R.string.owner_key), ""));
        boatInfo.put("Owner Contact", pref.getString(res.getString(R.string.contact_key), "096900338822"));
        boatInfo.put("Boat Length",  pref.getString(res.getString(R.string.length_key), "0"));
        boatInfo.put("Boat Width",  pref.getString(res.getString(R.string.width_key), "0"));
        boatInfo.put("Boat Height", pref.getString(res.getString(R.string.height_key), "0"));
        HashMap<String, String> alertInfo = new HashMap<>();
        alertInfo.put("Pitch Angle Alert",  pref.getString(res.getString(R.string.pitch_key), "0"));
        alertInfo.put("Roll Angle Alert", pref.getString(res.getString(R.string.roll_key), "0"));
        boatInfo.put("Reading Interval", pref.getString(res.getString(R.string.reading_key), "0"));
        HashMap<String, String> timerInfo = new HashMap<>();
        timerInfo.put("SMS Interval", pref.getString(res.getString(R.string.sms_key), "0"));
        timerInfo.put("Saving Interval", pref.getString(res.getString(R.string.saving_key), "0"));
        timerInfo.put("Server Posting Interval", pref.getString(res.getString(R.string.post_key), "0"));

        content += makeDefinitionList("Boat Information", boatInfo);
        content += makeDefinitionList("Alert Information", alertInfo);
        content += makeDefinitionList("Timer Information", timerInfo);

        content += getHtmlFooter();
        write(content);

        return getFileUrl();
    }

    /**
     * Generates an HTML file with About the app and returns the HTML file's URL
     * @return HTML file's URL
     */
    public String generateAboutContent() {
        String content = getHtmlHeader();

        // body here
        content += "The SEAWAVeS Mobile Application is part of the project “Design and Development of a Low-Cost Mobile Data Acquisition System for Small Crafts” of the Aklan State University - College of Industrial Technology, DOST VI, and DOST-PCIEERD. It was developed by the following:\n\n" +
                "Julie Ann A. Salido<br>Rowen R. Gelonga<br>Abraham A. Porcal<br>Ma. Fe P. Popes<br>Miquel Von Oquendo<br>Mary Ann Martirez<br>Kirk M. Hilario";

        content += getHtmlFooter();
        write(content);

        return getFileUrl();
    }
}
