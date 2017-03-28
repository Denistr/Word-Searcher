package com.istratenko.searcher;


import com.istratenko.searcher.entity.Word;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.Map;

/**
 * Created by denis on 28.03.17.
 */
public class HTMLController {

    public void createHTMLFile(Map<String, List<String>> phrases) {
        //BufferedReader br = new BufferedReader(new FileReader("/Users/denis/Documents/java/searcher/WordSearcher/src/main/java/com/istratenko/searcher/HTMLController.java"));
        File f = new File("result.html");
        BufferedWriter bw=null;
        try {
             //= new BufferedWriter(new FileWriter(f));
            bw = new BufferedWriter
                    (new OutputStreamWriter(new FileOutputStream(f),"cp1251"));
            bw.write("<html>");
            bw.write("<body>");
            bw.write("<h2>Result:</h2>");
            //bw.write("<textarea cols=50 rows=30>");

            for (Map.Entry document : phrases.entrySet()) {
                String doc = new File((String)document.getKey()).getName();
                for (String phrase : (List<String>)document.getValue()) {
                    StringBuilder name = new StringBuilder()
                            .append("<h3><a href=")
                            .append(document.getKey()).append(">")
                            .append(doc).append("</a>")
                            .append("</h3>");
                    bw.write(name.toString());
                    StringBuilder body = new StringBuilder().append(phrase).append("<br>");
                    bw.write(body.toString());
                    bw.newLine();
                }
            }

            //bw.write("</text" + "area>");
            bw.write("</body>");
            bw.write("</html>");

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
