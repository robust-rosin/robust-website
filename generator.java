import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class generator {

    Map<String, String> bugMap;
    String title;

    public generator(String pathIn, String pathOut)
    {
        

        String dataPath = pathIn.replace("\\", "/");
        dataPath = dataPath.endsWith("/") ? dataPath : dataPath+"/";
        String outPath = pathOut.replace("\\", "/");
        outPath = outPath.endsWith("/") ? outPath : outPath+"/";

        Map<String, Path> systems = new HashMap<String, Path>();
        systems.put("care-o-bot", Paths.get(dataPath+"care-o-bot"));
        systems.put("confidential", Paths.get(dataPath+"confidential"));
        systems.put("geometry2", Paths.get(dataPath+"geometry2"));
        systems.put("kobuki", Paths.get(dataPath+"kobuki"));
        systems.put("mavros", Paths.get(dataPath+"mavros"));
        systems.put("motoman", Paths.get(dataPath+"motoman"));
        systems.put("other", Paths.get(dataPath+"other"));
        systems.put("ros_comm", Paths.get(dataPath+"ros_comm"));
        systems.put("turtlebot", Paths.get(dataPath+"turtlebot"));
        systems.put("universal_robot", Paths.get(dataPath+"universal_robot"));

        new File(outPath).mkdir();
        
        File template = new File("./bugtemplate.html");
        File dbPage = new File(outPath+"database.html");
        File page;
        File bug;

        String bugList = "";
        String currentSys = "";

        try {
            Files.copy(Paths.get("./styles.css"), Paths.get(outPath+"styles.css"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Paths.get("./home.html"), Paths.get(outPath+"index.html"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (Map.Entry<String, Path> entry : systems.entrySet()) {
            if(!entry.getKey().equals(currentSys))
            {
                bugList += "<h3>"+entry.getKey().toUpperCase()+"</h3>";
                currentSys = entry.getKey();
            }
            new File(outPath+entry.getKey()).mkdir();
            List<String> result = getFolders(entry.getValue());

            for (String folder : result)
            {   
                InitMap();
                title = "";
                bug = new File(entry.getValue().toString()+"/"+folder+"/"+folder+".bug");
                try
                {
                    page = new File(outPath+entry.getKey()+"/"+bug.getName().split("\\.")[0]+".html");
                    page.createNewFile();
                    
                    if(Files.notExists(bug.toPath())) continue;

                    parseBug(Files.lines(bug.toPath()).collect(Collectors.toList()));
                
                    List<String> pageList = fillHtml(Files.lines(template.toPath()).collect(Collectors.toList()), 
                                                    bugMap);

                    Files.write(page.toPath(), pageList);

                    bugList +="<a href='"+entry.getKey()+"/"+bug.getName().split("\\.")[0]+".html'>["+bug.getName().split("\\.")[0]+"] "+title+"</a>"+"<br><br>";
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            String db = new String(Files.readAllBytes(Paths.get("./databasetemplate.html")));
            db = db.replace("$links", bugList);
            dbPage.createNewFile();
            Files.write(dbPage.toPath(), db.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void InitMap() {
        bugMap = new HashMap<String, String>();
        bugMap.put("id", "");
        bugMap.put("title", "");
        bugMap.put("description", "");
        bugMap.put("classification", "");
        bugMap.put("keywords", "");
        bugMap.put("system", "");
        bugMap.put("severity", "");
        bugMap.put("links", "");

        bugMap.put("bugphase", "");
        bugMap.put("bugspecificity", "");
        bugMap.put("bugarchitectural-location", "");
        bugMap.put("bugapplication", "");
        bugMap.put("bugtask", "");
        bugMap.put("bugsubsystem", "");
        bugMap.put("bugpackage", "");
        bugMap.put("buglanguages", "");
        bugMap.put("bugdetected-by", "");
        bugMap.put("bugreported-by", "");
        bugMap.put("bugissue", "");
        bugMap.put("bugtime-reported", "");
        bugMap.put("bugreproducibility", "");
        bugMap.put("bugtrace", "");
        bugMap.put("bugreproduction", "");

        bugMap.put("fixrepo", "");
        bugMap.put("fixhash", "");
        bugMap.put("fixpull-request", "");
        bugMap.put("fixlicense", "");
        bugMap.put("fixfix-in", "");
        bugMap.put("fixlanguages", "");
        bugMap.put("fixtime", "");
    }

    private void debug() {
        System.out.println("-----------------------Debug-----------------------------");
        for (Map.Entry e : bugMap.entrySet()) {
            System.out.println(e.getKey()+": "+e.getValue());
            System.out.println("------");
        }
        System.out.println("---------------------------------------------------------\n\n");
    }

    private List<String> getFolders(Path path) {
        try (Stream<Path> walk = Files.list(path)) {
            return walk.filter(Files::isDirectory).map(x -> x.getFileName().toString()).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void parseBug(List<String> bug)
    {
        String[] split;

        String line, section = "", field = "", content = "";
        

        for (int i = 0; i < bug.size(); i++) {
        
            line = bug.get(i);
            split = line.split(":");

            if(line.contains(":") && !field.equals(split[0].trim().toLowerCase())){
                if(!field.equals("")) {
                    if(field.equals("hash")) bugMap.put((!field.equals(section)?section:"")+field, content);
                    else if(field.equals("repo")) bugMap.put((!field.equals(section)?section:"")+field, content);
                    else {
                        if(isUrl(content)) content = toLink(content, content);
                        bugMap.put((!field.equals(section)?section:"")+field, toTr(field, content));
                    }
                }
                field = split[0].trim().toLowerCase();
                if(!Character.isWhitespace(line.charAt(0))){
                    section = field;
                }
                content =  "";
                for (int j = 1; j < split.length; j++) {
                    content += split[j];
                    if(j!=split.length-1) content += ":";
                }
                if(content.trim().matches("\\[.*\\]")) {
                    content = content.replaceAll("\\[|\\'|\\]", "");
                }
                if(field.equals("title")) title = content;
                continue;
            }
            if(!field.equals("")) content += line+"\n";
        }
        if(isUrl(content)) content = toLink(content, content);
        bugMap.put((!field.equals(section)?section:"")+field, toTr(field, content));
        if(bugMap.containsKey("fixrepo")) {
            if(bugMap.containsKey("fixhash")) bugMap.replace("fixhash", toTr("hash", toLink(bugMap.get("fixrepo")+"/commit/"+bugMap.get("fixhash").trim(), bugMap.get("fixhash"))));
            bugMap.replace("fixrepo", toTr("repo", toLink(bugMap.get("fixrepo"), bugMap.get("fixrepo"))));
        }
    }

    private List<String> fillHtml(List<String> page, Map<String, String> map)
    {
        for (int i = 0; i < page.size(); i++) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                page.set(i, page.get(i).replace("$"+entry.getKey()+"^", entry.getValue()));
            }
        }
        return page;
    }

    private String toTr(String name, String text)
    {
        return "<tr><td width='20%'><b>"+name+"</b></td><td>"+text+"</td></tr>";
    }

    private String toLink(String link, String text)
    {
        return "<a href='"+link+"'>"+text+"</a>";
    }

    private boolean isUrl(String url) 
    { 
        try { 
            new URL(url).toURI(); 
            return true; 
        } 

        catch (Exception e) { 
            return false; 
        } 
    } 

    public static void main(String[] args)
    {
        String in = (args.length > 0) ? args[0] : "../robust";
        String out = (args.length > 1) ? args[1] : "./out";
        new generator(in, out);    
    }
}