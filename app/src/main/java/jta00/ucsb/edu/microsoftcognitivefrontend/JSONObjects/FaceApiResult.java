package jta00.ucsb.edu.microsoftcognitivefrontend.JSONObjects;

import java.util.Hashtable;

/**
 * Created by john on 6/9/16.
 */
public class FaceApiResult {
    String faceId;
    Hashtable<String, Integer> faceRectangle;
    public FaceAttributes faceAttributes;
}
