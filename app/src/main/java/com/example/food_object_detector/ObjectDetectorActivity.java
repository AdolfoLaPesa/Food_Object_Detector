package com.example.food_object_detector;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.food_object_detector.ml.DetectObject;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ObjectDetectorActivity extends AppCompatActivity {

    private Button buttonPhoto;
    private Button buttonSearch;
    private ImageView imageView;
    private TextView textView;
    private int imageSize = 224;
    private Bitmap image;
    private String[] labels;
    private int count = 0;

    private String[] languages;
    private AutoCompleteTextView autoComlete;
    private ArrayAdapter arrayAdapter;

    private String selectedLenguage = "Inglese";

    private final Context context = this;
    private boolean flagBtnSearch = false;
    private String searchString = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_detector);

        buttonPhoto = findViewById(R.id.button_take_object_photo);
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.text_view_object_classified);

        buttonSearch = findViewById(R.id.search_button);

        languages =  getResources().getStringArray(R.array.language);

        autoComlete = findViewById(R.id.auto_complete_txt);
        arrayAdapter = new ArrayAdapter<String>(this, R.layout.list_language, languages);
        autoComlete.setAdapter(arrayAdapter);


        buttonPhoto.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                if(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 3);

                }else{
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });


        autoComlete.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                selectedLenguage = autoComlete.getText().toString();
               System.out.println("hai scelto: " + selectedLenguage);
            }
        });
        if(flagBtnSearch == false){
            buttonSearch.setVisibility(View.GONE);
        }

        searchWikipedia();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if(resultCode == RESULT_OK){
            if(requestCode == 3){
                image = (Bitmap) data.getExtras().get("data");
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                imageView.setImageBitmap(image);

                labels = new String[1001];
                switch (selectedLenguage){
                    case "Inglese":
                        objectDetectorControl("object_list.txt");
                        break;
                    case "Italiano":
                        objectDetectorControl("object_list_ita.txt");
                        break;
                    case "Russo":
                        objectDetectorControl("object_list_ru.txt");
                        break;
                    case "Francese":
                        objectDetectorControl("object_list_fr.txt");
                        break;
                    case "Tedesco":
                        objectDetectorControl("object_list_de.txt");
                        break;
                    case "Spagnolo":
                        objectDetectorControl("object_list_es.txt");
                        break;

                    default:
                        selectedLenguage = "object_list.txt";
                }
                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage();

            }

            /* Funzinoe per integrare l'assunzione di un immagine dalla galleria
            else{
                Uri dat = data.getData();
                Biitmap image = null;
                try {
                    Bitmap image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageView.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }

             */


        }


        super.onActivityResult(requestCode, resultCode, data);

    }

    public void classifyImage(){

        try {
            DetectObject model = DetectObject.newInstance(ObjectDetectorActivity.this);
            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.UINT8);
            inputFeature0.loadBuffer(TensorImage.fromBitmap(image).getBuffer());

            // Runs model inference and gets result.
            DetectObject.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            textView.setText(labels[getMax(outputFeature0.getFloatArray())] +  " ");

            switch (selectedLenguage){
                case "Inglese":
                    searchString = "http://en.wikipedia.org/wiki/" + textView.getText();
                    break;
                case "Italiano":
                    searchString = "http://it.wikipedia.org/wiki/" + textView.getText();
                    break;
                case "Russo":
                    searchString = "http://ru.wikipedia.org/wiki/" + textView.getText();
                    break;
                case "Francese":
                    searchString = "http://fr.wikipedia.org/wiki/" + textView.getText();
                    break;
                case "Tedesco":
                    searchString = "http://de.wikipedia.org/wiki/" + textView.getText();
                    break;
                case "Spagnolo":
                    searchString = "http://es.wikipedia.org/wiki/" + textView.getText();
                    break;

            }

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }


    }

    public int getMax(float[] array ){
        int max = 0;
        for(int i = 0; i < array.length; i++){
            if(array[i] > array[max]) max = i;
        }
        return max;
    }

    public void objectDetectorControl(String string){
        try {
            count=0;
            labels = new String[1001];
            System.out.println("nome della stringa: " + string);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(getAssets().open(string)));
            String line = bufferedReader.readLine();
            while(line != null){
                labels[count] = line;
                count++;
                line = bufferedReader.readLine();
                flagBtnSearch = true;
                buttonSearch.setVisibility(View.VISIBLE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void searchWikipedia(){

        buttonSearch.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                Intent intent = new Intent (Intent.ACTION_VIEW, Uri.parse(searchString));
                startActivity(intent);

            }
        });


    }



}