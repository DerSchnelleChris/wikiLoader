import http.requests.GetRequest;
import processing.core.PApplet;
import processing.core.PImage;
import processing.data.JSONArray;
import processing.data.JSONObject;
import processing.data.StringList;

import java.net.URLEncoder;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;







public class Loader extends PApplet {


//         *-- Base on the follwing url: https:// en.wikipedia.org/wiki/Albert_Einstein
//         *-- This program reads all the images in the requested link and not more
//         *-- set by the limit MAX_IMGS.
//         *-- Click on any image to zoom in. Second click to go back to tiles.


    static String WIKI_PAGE="Cat";  //"Art";
    static String WIKIPEDIA_IMAGES_QUERY_LIST="https://en.wikipedia.org/w/api.php?action=query&prop=images&format=json&formatversion=2&titles=";
    static String WIKIPEDIA_IMAGE_QUERY_URL="https://en.wikipedia.org/w/api.php?action=query&prop=imageinfo&iiprop=url&format=json&formatversion=2&titles=Image:";

    static int limit=30;

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

    System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.client.protocol.ResponseProcessCookies", "fatal");

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
        get = new GetRequest(WIKIPEDIA_IMAGES_QUERY_LIST+htmlCustomFormatter(WIKI_PAGE));
        get.send();

        values=parseJSONObject(get.getContent());
        JSONArray imgs=values.getJSONObject("query").getJSONArray("pages").getJSONObject(0).getJSONArray("images");

        if (imgs!=null) {
            int n=values.getJSONObject("query").getJSONArray("pages").getJSONObject(0).getJSONArray("images").size();



            for (int i=0; i<n && i<limit; i++) {
                String imageFileName=imgs.getJSONObject(i).getString("title");
                imageFileName=getFileName(imageFileName);           //Remove "File:"
                imageFileName=htmlCustomFormatter(imageFileName);   //Remove white spaces from URL
                println("Image "+i+"=> "+imgs.getJSONObject(i).getString("title"));

                String urlStr=null;
                PImage loadedImg=null;
                if (imageFileName.endsWith("jpg") || imageFileName.endsWith("png") || imageFileName.endsWith("tif")) {
                    get = new GetRequest(WIKIPEDIA_IMAGE_QUERY_URL+imageFileName);
                    get.send();

                    JSONObject res=parseJSONObject(get.getContent());
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

                for (int y=0; y<nrow && ctr<limit; y++) {

                    rect(posx, posy, dw, dh);
                    if (picked==true && isOverImage(posx, posy, dw, dh))
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


    String getFileName(String in) {
        boolean goodValue=in.startsWith("File:");
        String ret=null;

        if (goodValue) ret=in.substring(in.indexOf(":")+1);

        println("Requisting image: "+ret);
        return  ret;
    }

    //REFERENCE: https:// stackoverflow.com/questions/30998288/php-how-to-replace-special-characters-for-url
    String htmlCustomFormatter(String in) {
        boolean goodValue=in!=null;
        String ret=null;

        //  $specChars = array(
        //    '!' => '%21',    '"' => '%22',
        //    '#' => '%23',    '$' => '%24',    '%' => '%25',
        //    '&' => '%26',    '\'' => '%27',   '(' => '%28',
        //    ')' => '%29',    '*' => '%2A',    '+' => '%2B',
        //    ',' => '%2C',    '-' => '%2D',    '.' => '%2E',
        //    '/' => '%2F',    ':' => '%3A',    ';' => '%3B',
        //    '<' => '%3C',    '=' => '%3D',    '>' => '%3E',
        //    '?' => '%3F',    '@' => '%40',    '[' => '%5B',
        //    '\\' => '%5C',   ']' => '%5D',    '^' => '%5E',
        //    '_' => '%5F',    '`' => '%60',    '{' => '%7B',
        //    '|' => '%7C',    '}' => '%7D',    '~' => '%7E',
        //    ',' => '%E2%80%9A',  ' ' => '%20'
        //);



        if (goodValue) {

            //ret=in.replaceAll(" ", "%20");

            try {
                ret=URLEncoder.encode(in, "UTF-8");  //Also PApplet.encoder()
            }
            catch(UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        //println("Requisting image(formatted): "+ret);
        return  ret;
    }
}
