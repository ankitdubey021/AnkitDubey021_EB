package fragments;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.eb.ankitdubey021.R;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

import static android.app.Activity.RESULT_OK;

public class FirstFragment extends Fragment {

    private FirebaseFirestore firebaseFirestore;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;


    Uri selectedImageUri;

    public static final Pattern VALID_EMAIL_ADDRESS_REGEX = 	Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);
    public static final String VALID_MOB="^(0|[+91]{3})?[7-9][0-9]{9}$";
    private static final int PICK_IMAGE=101;

    @BindView(R.id.et_name) TextInputEditText etName;
    @BindView(R.id.et_mob) TextInputEditText etPhone;
    @BindView(R.id.et_mail) TextInputEditText etMail;
    @BindView(R.id.fab_edit) FloatingActionButton fabEdit;
    @BindView(R.id.imageView) CircleImageView img;
    @BindView(R.id.btn_save) MaterialButton saveBtn;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view=inflater.inflate(R.layout.fragment_first, container, false);
        ButterKnife.bind(this,view);

        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseStorage= FirebaseStorage.getInstance();
        storageReference=firebaseStorage.getReference().child("user_photos");

        saveBtn.setOnClickListener(v-> validate(v));
        fabEdit.setOnClickListener(v->{
            Log.e("inside ","called");
            //first check is permission has been given
                    if(ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 102);
                        Log.e("inside ","if");
                    }
                    else {
                        pickImage();
                        Log.e("inside ","pic image");
                    }
        });

        return view;
    }

    private void pickImage(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK) {

            try {
                selectedImageUri= data.getData();

                Bitmap bitmapImage = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), selectedImageUri);
                img.setImageBitmap(bitmapImage);
            }catch (Exception e){
                e.printStackTrace();
            }


        }
    }

    private void validate(View v) {
        Snackbar snackbar=Snackbar.make(v,"",Snackbar.LENGTH_LONG);
        String name=etName.getText().toString().trim();
        String mob=etPhone.getText().toString().trim();
        String mail=etMail.getText().toString().trim();

        if(TextUtils.isEmpty(name)){
            snackbar.setText("Name is mandatory!");
            snackbar.show();
            return;
        }
        if(TextUtils.isEmpty(mob) || !mob.matches(VALID_MOB)){
            snackbar.setText("Please enter valid Mobile no.");
            snackbar.show();
            return;
        }
        if(TextUtils.isEmpty(mail) || !VALID_EMAIL_ADDRESS_REGEX.matcher(mail).matches()){
            snackbar.setText("Please enter valid Email Address!");
            snackbar.show();
            return;
        }




        StorageReference photoRef= storageReference.child(selectedImageUri.getLastPathSegment());
        UploadTask uploadTask= photoRef.putFile(selectedImageUri);

        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return photoRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    Log.e("got downloaded path",downloadUri.toString());

                    Map<String, Object> user = new HashMap<>();
                    user.put("name", name);
                    user.put("phone", mob);
                    user.put("mail", mail);
                    user.put("image_path",downloadUri.toString());

// Add a new document with a generated ID
                    firebaseFirestore.collection("users")
                            .add(user)
                            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                @Override
                                public void onSuccess(DocumentReference documentReference) {
                                    etName.setText("");
                                    etMail.setText("");
                                    etPhone.setText("");
                                    selectedImageUri=null;
                                    img.setImageDrawable(ContextCompat.getDrawable(getContext(),R.drawable.ic_boy));
                                    Snackbar snackbar=Snackbar.make(v,"Data uploaded Successfully!",Snackbar.LENGTH_LONG);
                                    snackbar.setBackgroundTint(ContextCompat.getColor(getContext(),R.color.colorAccent));
                                    snackbar.show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Snackbar snackbar=Snackbar.make(v,"Oops! something went wrong!",Snackbar.LENGTH_LONG);
                                    snackbar.show();
                                }
                            });

                } else {
                    // Handle failures
                    // ...
                }
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
            pickImage();
        }
        else{
            Toast.makeText(getContext(),"App need permission to upload image!",Toast.LENGTH_LONG).show();
        }
    }
}
