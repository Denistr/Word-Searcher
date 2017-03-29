package com.istratenko.searcher;


import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * Created by denis on 28.03.17.
 */
public class HTMLController {

    public void createHTMLFile(Map<String, List<String>> phrases) {
        File f = new File("result.html");
        String currDocument=null;
        Writer bw = null;

        try {
            bw = new OutputStreamWriter(new FileOutputStream(f), "cp1251");
            bw.write("<html>");
            bw.write("<body>");
            bw.write("<h2>Result:</h2>");

            for (Map.Entry document : phrases.entrySet()) {
                if (!document.getKey().equals(currDocument)){
                    currDocument=(String)document.getKey();
                    String doc = new File((String)document.getKey()).getName();
                    StringBuilder fileName = new StringBuilder()
                            .append("<h3><a href=")
                            .append(document.getKey()).append(">")
                            .append(doc).append("</a>")
                            .append("</h3>");
                    bw.write(fileName.toString());
                }

                for (String phrase : (List<String>)document.getValue()) {
                    StringBuilder body = new StringBuilder().append(phrase).append("<br>");
                    bw.write(body.toString());
                }
            }
            bw.write("</body>");
            bw.write("</html>");
            bw.flush();

            Desktop.getDesktop().browse(f.toURI());

        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (bw!=null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
