package jta00.ucsb.edu.microsoftcognitivefrontend;

import java.net.URL;

/**
 * Created by john on 3/14/16.
 */
public class StringUrlPair {
    public String str;
    public byte[] data;
    public URL url;
    public String password;
    public String method;

    public StringUrlPair(String s, URL u, String m){
        str = s;
        url = u;
        method = m;
        password = null;
    }

    public StringUrlPair(String s, URL u, String m, String pwd){
        str = s;
        url = u;
        method = m;
        password = pwd;
    }

    public StringUrlPair(byte[] d, URL u, String m){
        data = d;
        url = u;
        method = m;
    }
}
