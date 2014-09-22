package com.dy.photo;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ImageEditActivity extends Activity implements OnClickListener {
	private AssetManager asset;
	private String[] images = null;
	private Button selectImageBtn;
	private Button cutImageBtn;
	private Button resetBtn;
	private RedPointView redPointView;
	// private ImageView imageView;
	private static final int IMAGE_SELECT = 1;
	private static final int IMAGE_SAVE = 2;
	private static final String TAG = "ImageEditActivity";
	private static final String ALBUM_PATH = Environment
			.getExternalStorageDirectory() + "/TagAdder_Image/";
private TextView savePathTextView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		asset = getAssets();
		try {
			images = asset.list("");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setContentView(R.layout.activity_image_edit);
		selectImageBtn = (Button) findViewById(R.id.selectImageBtn);
		cutImageBtn = (Button) findViewById(R.id.saveImageBtn);
		// imageView=(ImageView)findViewById(R.id.imageView);
		redPointView = (RedPointView) findViewById(R.id.imageView);
		resetBtn = (Button) findViewById(R.id.resetBtn);
		cutImageBtn.setOnClickListener(this);
		selectImageBtn.setOnClickListener(this);
		resetBtn.setOnClickListener(this);
		savePathTextView= (TextView) findViewById(R.id.savePathTextView);
		// this.redPointView=new RedPointView(this);
		// setContentView(redPointView);
		savePathTextView.setText("图片保存路径为："+ALBUM_PATH);
	}

	@Override
	public void onClick(View v) {
		if (v == selectImageBtn) {
			Intent intent = new Intent(
					Intent.ACTION_PICK,
					android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(intent, IMAGE_SELECT);
		}// 一般用于头像等需要设置指定大小
		
		
		else if(v ==resetBtn){
			redPointView.reset();
		}
		else if (v == cutImageBtn) {
			// TODO 增加保存到媒体库的方法。
			String url = null;
			String finlename = null;
			try {
				Date date = new Date();
				SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");

				finlename = "test" + df.format(date) + ".jpg";
				url = saveFile(redPointView.editedBitmap, finlename);
				if (url != null) {
					Toast.makeText(this, "已成功保存至" + url, Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(this, "保存失败", Toast.LENGTH_LONG).show();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (url != null) {
				Log.e(TAG, "保存成功");
				// 4.4之后无法发送扫描sd卡的广播
				// sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
				// Uri.parse(url)));
				MediaScannerConnection.scanFile(this, new String[] { ALBUM_PATH
						+ "/" + finlename }, null, null);
			} else {
				Log.e(TAG, "保存失败");
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		// TODO Auto-generated method stub
		if (resultCode == RESULT_OK) {
			if (requestCode == IMAGE_SELECT) {
				Uri imageFileUri = intent.getData();
				try {
					BitmapFactory.Options factory = new BitmapFactory.Options();
					factory.inJustDecodeBounds = false; // 当为true时 允许查询图片不为
														// 图片像素分配内存
					Bitmap bmp = BitmapFactory.decodeStream(
							getContentResolver().openInputStream(imageFileUri),
							null, factory);
					// TODO 可设计成可选择图案的
					InputStream assetFile = null;
					try {
						assetFile = asset.open("red1.png");
					} catch (IOException e) {
						e.printStackTrace();
					}
					BitmapFactory.Options options = new BitmapFactory.Options();
					Bitmap redPoint = BitmapFactory.decodeStream(assetFile,
							null, options);
					redPointView.setRedPoint(redPoint);
					redPointView.setImageBitmap(bmp);
				} catch (Exception ex) {

				}
			} else if (requestCode == IMAGE_SAVE) {

			}
		}
	}

	public String saveFile(Bitmap bm, String fileName) throws IOException {
		File dirFile = new File(ALBUM_PATH);
		if (!dirFile.exists()) {
			dirFile.mkdir();
		}
		File myCaptureFile = new File(ALBUM_PATH, fileName);
		BufferedOutputStream bos = new BufferedOutputStream(
				new FileOutputStream(myCaptureFile));
		bm.compress(Bitmap.CompressFormat.JPEG, 100, bos);
		bos.flush();
		bos.close();

		String url = MediaStore.Images.Media.insertImage(getContentResolver(),
				myCaptureFile.getAbsolutePath(), myCaptureFile.getName(),
				myCaptureFile.getName());

		return url;
	}
}
