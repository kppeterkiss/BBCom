package httpCom;

import lib.Com;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@WebServlet("/")
public class HTTPComServlet /*extends HttpServletimplements Com */ {
    //public String getId() {
    //    return id;
    //}

    public void setId(String id) {
        this.id = id;
    }

    String id;

    public List<String> getAddresses() {
        return addresses;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }

    List<String> addresses;
    List<String> ids;
    Map<String,List<String>> messages = new HashMap<>();

   // @Override
    protected synchronized void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Part toPart =request.getPart("To");
        Reader in = new BufferedReader(new InputStreamReader(toPart.getInputStream()));
        StringBuilder sb = new StringBuilder();
        for (int c; (c = in.read()) >= 0;)
            sb.append((char)c);
        String to = sb.toString();
        Reader r  = request.getReader();
        sb = new StringBuilder();
        for (int c; (c = r.read()) >= 0;)
            sb.append((char)c);
        String msg = sb.toString();
        if(!this.messages.containsKey(to))
            this.messages.put(to, new LinkedList<String>());
        this.messages.get(to).add(msg);
        System.out.println(msg);
        response.getWriter().write("OK");
        response.getWriter().flush();
    }

    //@Override
    public boolean send(String msg,String id) {
        int i = 0;
        for(String url :addresses){//make a http post with the msg
            try {
                id = ids.get(i++);
                byte[] postDataBytes = msg.getBytes("UTF-8");
                URL obj = new URL(url);
                HttpURLConnection conn = (HttpURLConnection) obj.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                conn.setRequestProperty("To", id);
                conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
                conn.setDoOutput(true);
                conn.getOutputStream().write(postDataBytes);
                Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                for (int c; (c = in.read()) >= 0;)
                    sb.append((char)c);
                String response = sb.toString();
                System.out.println(response);

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return false;
    }


    //@Override
    public synchronized List<String> receive(String id) {
        List<String> ret = new LinkedList<>();
        if(messages.containsKey(id))
            ret = messages.get(id);
        messages.put(id,new LinkedList<>());
        return ret;
    }
}
