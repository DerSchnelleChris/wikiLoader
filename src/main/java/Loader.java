import http.requests.GetRequest;
import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.data.StringList;

import java.util.ArrayList;







public class Loader extends PApplet {

    static String search = "https://de.wikipedia.org/wiki/Hongkong";
    static String imagelist = "https://de.wikipedia.org/w/api.php?action=query&prop=images&format=json&formatversion=2&titles=";
    static String imagedetails = "https://de.wikipedia.org/w/api.php?action=query&prop=imageinfo&iiprop=url&format=json&formatversion=2&titles=Image:";

    static int limit = 15;

    //===========================================================================
// GLOBAL VARIABLES:
    StringList imgsURL;
    int nImages;
    ArrayList<PImage> imgsBin;

    boolean picked=false;
    int cIdx=-1;




//===========================================================================
// PROCESSING DEFAULT FUNCTIONS:
public static void main(String[] args) {

    for (int i = 0; i < 4; i++) {
        search = search.substring(search.indexOf("/") + 1);
    }

    PApplet.main("Loader");
}

    public void settings() {
        size(1280, 720);
    }

    public void setup() {

        noLoop();
        textAlign(CENTER, CENTER);
        rectMode(CENTER);
        imageMode(CENTER);

        fill(255);
        strokeWeight(2);

        imgsURL=new StringList();
        imgsBin=new ArrayList<PImage>();

        background(90);
        textSize(32);
        text("Loading data....\n Wait", width/2, height/2);

        registerMethod("pre", this);
    }

    public void pre() {
        unregisterMethod("pre", this);
        textSize(10);

        GetRequest get;
        JSONObject values;
        get = new GetRequest(imagelist + search);
        get.send();

        values=parseJSONObject(get.getContent());
        JSONArray imgs=values.getJSONObject("query").getJSONArray("pages").getJSONObject(0).getJSONArray("images");


        for (int i = 0; i < imgs.size(); i++) {
            if (imgs.getJSONObject(i).getString("title").endsWith("ogv") || imgs.getJSONObject(i).getString("title").endsWith("svg"))  // keine ogv Dateien (= Videos)
                imgs.remove(i);
        }

        if (imgs!=null) {

            for (int i=0; i < imgs.size() && i<limit; i++) {
                String imageFileName = imgs.getJSONObject(i).getString("title");
                imageFileName = fixFilename(imageFileName);           // Entferne alles nach "Datei: / File:" + Ersetze Leerzeichen mit "_"

                String urlStr=null;
                PImage loadedImg=null;

                if (imageFileName.endsWith("jpg") || imageFileName.endsWith("png") || imageFileName.endsWith("tif") || imageFileName.endsWith("JPG") || imageFileName.endsWith("jpeg")) {
                    get = new GetRequest(imagedetails +imageFileName);
                    get.send();

                    JSONObject res = parseJSONObject(get.getContent());
                    urlStr=res.getJSONObject("query").getJSONArray("pages").getJSONObject(0).getJSONArray("imageinfo").getJSONObject(0).getString("url");
                    loadedImg=loadImage(urlStr);
                }

                imgsURL.append(urlStr);
                imgsBin.add(loadedImg);
                println("........... ", urlStr);
            }
        } else {
            println("Retrieving images from query failed!");
        }
    }

    public void draw() {
        background(200, 20, 220);
        showAllImages();

        if (picked==true  ) {
            if (cIdx>=0) {
                background(200, 20, 220);
                showImage(cIdx, width/2, height/2, width, height);
            } else
                picked=false;  //It gets reset by mouse clicked unless if no image was clicked
        }

        if (nImages==0) {
            textSize(32);
            text("Loading Resources\nFailure", width/2, height/2);
        }
    }

    public void keyReleased() {
        exit();
    }

    public void mouseReleased() {
        picked=!picked;
        redraw();
    }

//===========================================================================
// OTHER FUNCTIONS:

    void showAllImages() {

        nImages=imgsURL.size();
        if (nImages>0) {
            int ncol=2;
            int nrow=nImages%ncol==0?nImages/ncol:nImages/ncol+1;

            float dw=width/(ncol+2);
            float dh=height/(nrow*2);

            float posx=dw;
            float posy=dh;
            int ctr=0;

            cIdx=-1;
            noFill();
            stroke(255, 20, 20);
            strokeWeight(3);
            for (int x=0; x<ncol; x++) {

                for (int y=0; y<nrow && ctr<nImages; y++) {

                    rect(posx, posy, dw, dh);
                    if (picked && isOverImage(posx, posy, dw, dh))
                        cIdx=ctr;
                    showImage(ctr++, posx, posy, dw, dh);

                    posy+=2*dh;
                }
                posx+=2*dw;
                posy=dh;
            }
        }
    }

    boolean isOverImage(float px, float py, float dw, float dh) {
        return mouseX>(px-dw/2) && mouseX<(px+dw/2) && mouseY>(py-dh/2) && mouseY<(py+dh/2);
    }

    void showImage(int ii, float px, float py, float dw, float dh) {
        String fn=imgsURL.get(ii);
        if (fn!=null) {
            image(imgsBin.get(ii), px, py, dw, dh);
        } else {
            text("Not valid format", px, py, dw, dh);
        }
    }


    String fixFilename(String in) {
        String ret = in.substring(in.indexOf(":")+1);
        ret = ret.replaceAll("\s", "_");

        println("Requesting image: "+ ret);
        return  ret;

    }
}
