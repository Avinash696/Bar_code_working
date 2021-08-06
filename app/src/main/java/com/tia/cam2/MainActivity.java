package com.tia.cam2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import cn.simonlee.xcodescanner.core.CameraScanner;
import cn.simonlee.xcodescanner.core.GraphicDecoder;
import cn.simonlee.xcodescanner.core.NewCameraScanner;
import cn.simonlee.xcodescanner.core.OldCameraScanner;
import cn.simonlee.xcodescanner.view.AdjustTextureView;

public class MainActivity extends AppCompatActivity implements CameraScanner.CameraListener,
        TextureView.SurfaceTextureListener, GraphicDecoder.DecodeListener {
    private AdjustTextureView mTextureView;
    private static final int MY_PERMISSION_REQUEST_CAMERA = 0;
    private View mScannerFrameView;
    private TextView tv;
    private CameraScanner mCameraScanner;
    protected GraphicDecoder mGraphicDecoder;

    protected String TAG = "XCodeScanner";
    private Button mButton_Flash;
    private int[] mCodeType;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
        requestCameraPermission();

        if ( Build. VERSION . SDK_INT  >=  Build . VERSION_CODES . LOLLIPOP ) {
            mCameraScanner =  new NewCameraScanner( this );
        } else {
            mCameraScanner =  new OldCameraScanner( this );
        }
    }

    @Override
    protected void onRestart() {
        if (mTextureView.isAvailable()) {
            mCameraScanner.setPreviewTexture(mTextureView.getSurfaceTexture());
            mCameraScanner.setPreviewSize(mTextureView.getWidth(), mTextureView.getHeight());
            mCameraScanner.openCamera(this.getApplicationContext());
        }
        super.onRestart();
    }
    @Override
    protected void onPause() {
        mCameraScanner.closeCamera();
        super.onPause();
    }
    @Override
    public void onDestroy() {
        mCameraScanner.setGraphicDecoder(null);
        if (mGraphicDecoder != null) {
            mGraphicDecoder.setDecodeListener(null);
            mGraphicDecoder.detach();
        }
        mCameraScanner.detach();
        super.onDestroy();
    }
    void init(){
        tv=findViewById(R.id.textview);
        mScannerFrameView=findViewById(R.id.scannerframe);
        mTextureView = findViewById (R.id.textureview);
        mTextureView.setSurfaceTextureListener(this);
    }

    @Override
    public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
    //cam open if sureface availabe
        mCameraScanner.setPreviewTexture(surface);
        mCameraScanner.setPreviewSize(width, height);
        mCameraScanner.openCamera(this);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        //someday
    }

    @Override
    public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
        Toast.makeText(MainActivity.this, "onSureface Texture Destroyed", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

    }

    @Override
    public void openCameraSuccess(int surfaceWidth, int surfaceHeight, int surfaceDegree) {
        mTextureView.setImageFrameMatrix(surfaceWidth, surfaceHeight, surfaceDegree);
        if (mGraphicDecoder == null) {
//            mGraphicDecoder = new Decoder((GraphicDecoder.DecodeListener) this, mCodeType);
            mGraphicDecoder=new DebugZBarDecoder( this,mCodeType);
        }
        //scanner frameView
        mCameraScanner.setFrameRect(mScannerFrameView.getLeft(), mScannerFrameView.getTop(), mScannerFrameView.getRight(), mScannerFrameView.getBottom());
        mCameraScanner.setGraphicDecoder(mGraphicDecoder);
    }

    @Override
    public void openCameraError() {
        Toast.makeText(this, "Camera open error", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void noCameraPermission() {
        Toast.makeText(this, "Allow Camera Permission", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void cameraDisconnected() {
        Toast.makeText(this, "Camera got disconnected", Toast.LENGTH_SHORT).show(); }

        int mBrightnessCount=0;
    @Override
    public void cameraBrightnessChanged(int brightness) {
        if (brightness <= 50) {
            if (mBrightnessCount < 0) {
                mBrightnessCount = 1;
            } else {
                mBrightnessCount++;
            }
        } else {
            if (mBrightnessCount > 0) {
                mBrightnessCount = -1;
            } else {
                mBrightnessCount--;
            }
        }
        if (mBrightnessCount > 4) {
            mButton_Flash.setVisibility(View.VISIBLE);
        } else if (mBrightnessCount < -4 && !mCameraScanner.isFlashOpened()) {
            mButton_Flash.setVisibility(View.GONE);
        }
    }


    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) ;
        {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
//                initQRCodeReaderView();
            } else {
                requestCameraPermission();
            }
        }
    }
    private void requestCameraPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
            Toast.makeText(getApplicationContext(), "Camera access is required to display the camera preview.", Toast.LENGTH_SHORT).show();
            new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                            Manifest.permission.CAMERA
                    }, MY_PERMISSION_REQUEST_CAMERA);
                }
            };
        } else {
            Toast.makeText(getApplicationContext(), "Permission is not available. Requesting camera permission.",
                    Toast.LENGTH_LONG).show();
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.CAMERA
            }, MY_PERMISSION_REQUEST_CAMERA);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != MY_PERMISSION_REQUEST_CAMERA) {
            return;
        }

        if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this
                    , "Camera permission granted", Toast.LENGTH_SHORT).show();
//            initQRCodeReaderView();
        } else {
            Toast.makeText(this, "Camera permission request was denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void decodeComplete(String result, int type, int quality, int requestCode) {
        tv.setText(result);
    }
}