package objects;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;


public class Artist implements Serializable 
{
    private static final long serialVersionUID = 1L;
    private String id;
    private String name;
    private String spotifyURL;
    private long followers;
    private int popularity;

    private HashMap <String, MyImage> images = new HashMap <String, MyImage>();


    public String getID() {
        return id;
    }

    public void addImages(ArrayList<MyImage> imgs) {
        for (MyImage myImage : imgs) {
            images.put(myImage.getSize(), myImage);
        }
    }

    public MyImage getImage(MyImage.ImageSize size) {
        return images.get(size.getImgSize());
    }

    public String getName() {
        return name;
    }

    public String getSpotifyURL() {
        return spotifyURL;
    }

    public long getFollowers() {
        return followers;
    }

    public int getPopularity() {
        return popularity;
    }
}
