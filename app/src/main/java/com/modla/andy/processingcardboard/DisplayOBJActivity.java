/*
  Example Processing VR sketch using Google Android Cardboard SDK

  Copyright (c) 2015 Andy Modla

  Modifications to Processing Android library to use Google Cardboard library.

  This source code is free software; you can redistribute it and/or
  modify it under the terms of the GNU Lesser General Public
  License version 2.1 as published by the Free Software Foundation.

  This library is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General
  Public License along with this library; if not, write to the
  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
  Boston, MA  02111-1307  USA
*/

package com.modla.andy.processingcardboard;

/**
 * Description
 * This Android app is an example Cardboard VR program coded using the Processing for Android
 * library and the Google Cardboard Android SDK. The app displays an OBJ model rocket and allows
 * keyboard input to rotate, zoom with head tilt and pan left or right.
 *
 * <p/>
 * The Processing library has an abstraction layer for OPENGL making it possible
 * to write an Android Cardboard app without needing direct Android OPENGL calls.
 * Using Processing with Cardboard SDK is an alternative for writing Android VR applications.
 *
 * <p/>
 * Built with Android Studio (1.3.1)
 * <p/>
 * Cardboard SDK for Android 0.5.5
 * <p/>
 * Minimum Android API 4.1 (16)
 * <p/>
 * Tested with Sony Z1S phone, 1920x1080 pixel display, running Android version 5.0.2, and
 * hardware accelerated GPU
 *
 * <p/>
 * Issues:
 * Distortion correction is disabled because the Cardboard correction feature does not work well
 * The display is not distorted enough to matter with my Unofficial cardboard viewer lens and
 * home made viewer with stereoscopic quality lens.
 * <p/>
 * Out of memory can occur when using large images and restarting the app
 * <p/>
 * Processing-Cardboard Library build for Processing 2.2.1 IDE not implemented. Here the library
 * is included with the app as Processing source code.
 * <p/>
 *
 * notes:
 * The magnet trigger does not work well with my phone so I use new convert tap to trigger feature
 * available in Cardboard V2.
 * <p/>
 * Changes made to Processing-Anddroid core library:
 * <p/>
 * PApplet extends CardboardActivity
 * <p/>
 * SketchSurfaceView extends CardboardView
 * <p/>
 * SketchSurfaceViewGL extends CardboardView
 * <p/>
 * CardboardView rendering uses CardboardView.Renderer
 * <p/>
 * CardboardView.StereoRenderer code is also available
 * <p/>
 * PStereo class added to Processing core for stereo view control
 * <p/>
 *
 *
 *
 * Cardboard is a trademark of Google Inc.
 */

import android.content.Context;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.HeadTransform;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;


public class DisplayOBJActivity extends PApplet {
    private static String TAG = "DisplayOBJActivity";

    CardboardView cardboardView;
    private Vibrator vibrator;

    static final float STARTX = 0f;
    static final float STARTY = 0f;
    static final float STARTZ = 10f;
    static final int XBOUND = 9;
    static final int YBOUND = 10;
    static final int ZBOUND_IN = 4;
    static final int ZBOUND_OUT = 36;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        cardboardView = (CardboardView) surfaceView;
        //cardboardView.setAlignmentMarkerEnabled(false);
        //cardboardView.setSettingsButtonEnabled(false);
        setCardboardView(cardboardView);
        cardboardView.setDistortionCorrectionEnabled(false);
        //cardboardView.setDistortionCorrectionEnabled(true);
        cardboardView.setChromaticAberrationCorrectionEnabled(false);
        //cardboardView.setChromaticAberrationCorrectionEnabled(true);
        //cardboardView.setVRModeEnabled(false); // sets Monocular mode
        //Log.d(TAG, "getVRMode=" + cardboardView.getVRMode());
        setConvertTapIntoTrigger(true);
        //Log.d(TAG, "getConvertTapIntoTrigger=" + getConvertTapIntoTrigger());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //    @Override
    public void onCardboardTrigger() {
        // user feedback
        vibrator.vibrate(50);
        resetTracker();
    }

    @Override
    public void onPause() {
        super.onPause();
        // The following call pauses the rendering thread.
        // If your OpenGL application is memory intensive,
        // you should consider de-allocating objects that
        // consume significant memory here.
        cardboardView.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    public void onResume() {
        super.onResume();
        // The following call resumes a paused rendering thread.
        // If you de-allocated graphic objects for onPause()
        // this is a good place to re-allocate them.
        cardboardView.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        // TODO release image resources
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Processing sketch for the Android app starts here.
    // Display OBJ file and zoom with head tilt and rotate with keyboard.

    float rotx = 0; //PI / 4;
    float roty = 0; //PI / 4;
    PShape rocket;
    PShape textImage;
    PShape gridImage;
    float nearPlane = .1f;
    float farPlane = 1000f;
    float convPlane = 20.0f;
    float eyeSeparation = convPlane / 60.f;
    float fieldOfViewY = 45f;
    float cameraPositionX = STARTX;
    float cameraPositionY = STARTY;
    float cameraPositionZ = STARTZ;
    float[] headView = new float[16];

    @Override
    public void settings() {
        // set size to full screen dimensions
        //size(displayWidth, displayHeight, P3D);  // equivalent to OPENGL
        // Processing variables displayWidth and displayHeight are your phone screen dimensions
        size(displayWidth, displayHeight, OPENGL);
        println("settings()");
    }

    /**
     * One time initial call to set up your Processing sketch variables, etc.
     */
    @Override
    public void setup() {
        background(0);
        rocket = loadShape("obj/rocket.obj");
        textSize(32);
        textImage = createTextGraphics("ROCKET");
        gridImage = createGridShape();
        /* second constructor, custom eye separation, custom convergence */
        stereoView(
                width, height, eyeSeparation, fieldOfViewY,
                nearPlane,
                farPlane,
                convPlane);

        //println("Screen Width="+ width + " Height="+height);
        // stereoPosition only needs to be called repeatedly if you are
        // changing camera position, which we are doing
        stereoPosition(
                cameraPositionX, cameraPositionY, cameraPositionZ,
                0f, 0f, -1f,  // directionX, directionY, directionZ
                0f, 1f, 0f);  // upX, upY, upZ
        cardboardView.resetHeadTracker();

    }

    PShape createTextGraphics(String s) {
        PGraphics buffer = createGraphics(width/2, height);
        buffer.beginDraw();
        buffer.textSize(128);
        buffer.text(s, width / 8, height / 2 + height / 4);
        buffer.endDraw();

        PShape face = createShape();
        face.beginShape(QUAD);
        face.noStroke();
        face.textureMode(NORMAL);
        face.texture(buffer);
        face.vertex(-1, -1, 0, 0, 0);
        face.vertex(1, -1, 0, 1, 0);
        face.vertex(1, 1, 0, 1, 1);
        face.vertex(-1, 1, 0, 0, 1);
        face.endShape();
        return face;
    }

    void drawTextGraphics(PShape s) {
        pushMatrix();
        scale(8);
        translate(0, 0, -.25f);
        shape(s);
        popMatrix();
    }

    void drawShape(PShape s) {
        pushMatrix();
        scale(.01f);
        rotateX(rotx);
        rotateY(roty);
        rotateZ(PI);
        shape(s);
        popMatrix();
    }

    // this test draw only shows up in right viewport
    public void drawText(String s, float x) {
        stroke(255);
        fill(255);
        pushMatrix();
        scale(.05f);
        text(s, x, 0, 5);
        popMatrix();
    }

    public void drawGridShape(PShape grid) {
        pushMatrix();
        scale(1f);
        shape(grid);
        popMatrix();
    }

    public PShape createGridShape() {
        int gridSize = 1;
        stroke(128);
        fill(64);
        PGraphics buffer = createGraphics(width/2, height, P3D);
        buffer.beginDraw();

        for(int i = -20; i <20; i+=gridSize) {
            for (int j = -20; j < 20; j +=gridSize) {
                int y = 1;
                int z = 0;
                buffer.line(i, y, j, i + gridSize, y, j);
                buffer.line(i + gridSize, y, j, i + gridSize, y, j + gridSize);
                buffer.line(i + gridSize, y, j + gridSize, i, y, j + gridSize);
                buffer.line(i, y, j, i, y, j + gridSize);
            }
        }
        buffer.endDraw();

        PShape face = createShape();
        face.beginShape(QUAD);
        face.noStroke();
        face.textureMode(NORMAL);
        face.texture(buffer);
        face.vertex(-1, -1, 0, 0, 0);
        face.vertex(1, -1, 0, 1, 0);
        face.vertex(1, 1, 0, 1, 1);
        face.vertex(-1, 1, 0, 0, 1);
        face.endShape();
        return face;
    }

    public void drawGrid() {
        int gridSize = 1;
        stroke(128);
        fill(64);
        pushMatrix();
        for(int i = -20; i <20; i+=gridSize) {
            for(int j = -20; j < 20; j+=gridSize) {
                int y = 1;
                int z = 0;
                line(i,          y, j,           i+gridSize, y, j          );
                line(i+gridSize, y, j,           i+gridSize, y, j+gridSize );
                line(i+gridSize, y, j+gridSize,  i,          y, j+gridSize );
                line(i,          y, j,           i,          y, j+gridSize );
            }
        }
        popMatrix();
    }

    public void mouseDragged() {
        float rate = 0.01f;
        rotx += (pmouseY - mouseY) * rate;
        roty += (mouseX - pmouseX) * rate;
    }

    void resetTracker() {
        cameraPositionX = STARTX;
        cameraPositionY = STARTY;
        cameraPositionZ = STARTZ;
        cardboardView.resetHeadTracker();
    }

    @Override
    public void headTransform(HeadTransform headTransform) {
        float[] quat = new float[4];
        headTransform.getQuaternion(quat, 0);
        // normalize quaternion
        float length = (float) Math.sqrt(quat[0] * quat[0] + quat[1] * quat[1] + quat[2] * quat[2] + quat[3] * quat[3]);
        int DIV = 10;
        float lowSpeed = .01f;  //.005f;
        float mediumSpeed = .02f;  //.01f;
        float highSpeed = .04f;  //.02f;
        float pitchSpeed = 0;
        float yawSpeed = 0;
        float rollSpeed = 0;
        if (length != 0) {
            int pitch = (int) ((quat[0] / length) * DIV);  // pitch up/down
            int yaw = (int) ((quat[1] / length) * DIV);  // yaw left/ right
            int roll = (int) ((quat[2] / length) * DIV);  // roll left/right
            //int w = (int) ((quat[3] / length) * DIV);  //
            //Log.d(TAG, "normalized quaternion " + pitch + " " + yaw + " " + roll );

            if (pitch >= 3)
                pitchSpeed = -highSpeed;
            else if (pitch <= -3)
                pitchSpeed = highSpeed;
            else if (pitch == 2)
                pitchSpeed = -mediumSpeed;
            else if (pitch == -2)
                pitchSpeed = mediumSpeed;
            else if (pitch == 1)
                pitchSpeed = -lowSpeed;
            else if (pitch == -1)
                pitchSpeed = lowSpeed;
            else
                pitchSpeed = 0;

            if (yaw >= 3)
                yawSpeed = -highSpeed;
            else if (yaw <= -3)
                yawSpeed = highSpeed;
            else if (yaw == 2)
                yawSpeed = -mediumSpeed;
            else if (yaw == -2)
                yawSpeed = mediumSpeed;
            else if (yaw == 1)
                yawSpeed = -lowSpeed;
            else if (yaw == -1)
                yawSpeed = lowSpeed;
            else
                yawSpeed = 0;

            if (roll >= 3)
                rollSpeed = -highSpeed;
            else if (roll <= -3)
                rollSpeed = highSpeed;
            else if (roll == 2)
                rollSpeed = -mediumSpeed;
            else if (roll == -2)
                rollSpeed = mediumSpeed;
            else if (roll == 1)
                rollSpeed = -lowSpeed;
            else if (roll == -1)
                rollSpeed = lowSpeed;
            else
                rollSpeed = 0;

            if ((cameraPositionX > XBOUND && yawSpeed < 0) ||
                    (cameraPositionX < -XBOUND && yawSpeed > 0) ||
                    (cameraPositionX <= XBOUND && cameraPositionX >= -XBOUND))
                cameraPositionX += yawSpeed;


            if ((cameraPositionY > YBOUND && pitchSpeed < 0) ||
                    (cameraPositionY < -YBOUND && pitchSpeed > 0) ||
                    (cameraPositionY <= YBOUND && cameraPositionY >= -YBOUND))
                cameraPositionY += pitchSpeed;

            if ((cameraPositionZ > ZBOUND_IN && rollSpeed < 0) ||
                    (cameraPositionZ < ZBOUND_OUT && rollSpeed > 0) ||
                    (cameraPositionZ <= ZBOUND_OUT && cameraPositionZ >= ZBOUND_IN))
                cameraPositionZ += rollSpeed;

//            Log.d(TAG, "Normalized quaternion " + pitch + " " + yaw + " " + roll + " Camera position "+ cameraPositionX + " " + cameraPositionY + " " + cameraPositionZ);
        } else {
            Log.d(TAG, "Quaternion 0");
        }

//        headTransform.getHeadView(headView, 0);
//
//        if (!Float.isNaN(headView[0])) {
//            Log.d(TAG, "headView"  + " "+ headView[0] + " " + headView[1] + " " + headView[2] + " " + headView[3] + " " );
//            Log.d(TAG, "        "  +  " "+ headView[4] + " " + headView[5] + " " + headView[6] + " " + headView[7] + " " );
//            Log.d(TAG, "        "  +  " "+ headView[8] + " " + headView[9] + " " + headView[10] + " " + headView[11] + " " );
//            Log.d(TAG, "        "  +  " "+ headView[12] + " " + headView[13] + " " + headView[14] + " " + headView[15] + " " );
//        }

    }

    /**
     * Draw left eye. Called when VRMode enabled.
     */
    @Override
    public void drawLeft() {
        drawShape(rocket);
        //drawGrid();
        drawGridShape(gridImage);
        drawTextGraphics(textImage);
        //drawText("ROCKET", 30.0f);
    }

    /**
     * Draw right eye. Called when VRMode enabled.
     */
    @Override
    public void drawRight() {
        drawShape(rocket);
        //drawGrid();
        drawGridShape(gridImage);
        drawTextGraphics(textImage);
        //drawText("ROCKET", 30.0f);
    }

    /**
     * Processing draw function. Called before drawLeft and drawRight.
     */
    @Override
    public void draw() {
        background(0);
        stereoPosition(
                cameraPositionX, cameraPositionY, cameraPositionZ,
                0f, 0f, -1f,  // directionX, directionY, directionZ
                0f, 1f, 0f);  // upX, upY, upZ
    }

    public void keyPressed() {
        println("keyCode=" + keyCode);
        Log.d(TAG, "keyCode=" + keyCode);

        if (keyCode == LEFT || keyCode == MEDIA_PREVIOUS) {
            rotx += PI / 4;
        } else if (keyCode == RIGHT || keyCode == MEDIA_NEXT) {
            rotx -= PI / 4;
        } else if (keyCode == UP || keyCode == MEDIA_FAST_FORWARD) {
            roty += PI / 4;
        } else if (keyCode == DOWN || keyCode == MEDIA_REWIND) {
            roty -= PI / 4;
        } else if (keyCode == MEDIA_ENTER) {
            resetTracker();
        }
    }

}
