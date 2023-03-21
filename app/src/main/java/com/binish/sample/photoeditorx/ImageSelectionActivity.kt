package com.binish.sample.photoeditorx

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_image_selection.*
import java.io.*


class ImageSelectionActivity : AppCompatActivity() {
     var putImg : Bitmap ? =null
    var selectImg : String? =null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_selection)
        btnTakePhoto.setOnClickListener {
            val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            filePhoto = getPhotoFile(FILE_NAME)


            val providerFile =FileProvider.getUriForFile(this,"com.example.androidcamera.fileprovider", filePhoto)
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, providerFile)
            if (takePhotoIntent.resolveActivity(this.packageManager) != null){
                startActivityForResult(takePhotoIntent, REQUEST_CODE)
            }else {
                Toast.makeText(this,"Camera could not open", Toast.LENGTH_SHORT).show()
            }
        }

        btnChoosePhoto.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_DENIED){
                    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                    requestPermissions(permissions, PERMISSION_CODE)
                } else{
                    chooseImageGallery();

                }
            }else{
                chooseImageGallery();

            }

        }

        btnEditPhoto.setOnClickListener{
            val stream = ByteArrayOutputStream()
            putImg?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val byteArray: ByteArray = stream.toByteArray()

            val in1 = Intent(this, MainActivity::class.java)
            in1.putExtra("imgUri", byteArray)
            in1.putExtra("selectImg", selectImg)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(in1)
        }
    }

    private fun chooseImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_CHOOSE)
    }

    companion object {
        private val IMAGE_CHOOSE = 1000;
        private val PERMISSION_CODE = 1001;
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    chooseImageGallery()
                }else{
                    Toast.makeText(this,"Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }




    private fun getPhotoFile(fileName: String): File {
        val directoryStorage = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(fileName, ".jpg", directoryStorage)
    }



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK){
            btnEditPhoto.visibility =View.VISIBLE

            val takenPhoto = BitmapFactory.decodeFile(filePhoto.absolutePath)

            selectImg= filePhoto.absolutePath
            viewImage.setImageBitmap(takenPhoto)
        }
        else {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
                try {
                    btnEditPhoto.visibility =View.VISIBLE

                    val imageUri: Uri? = data.data
                    val imageStream: InputStream? = imageUri?.let { contentResolver.openInputStream(it) }
                    val selectedImage = BitmapFactory.decodeStream(imageStream)
                    viewImage.setImageBitmap(selectedImage)

                    putImg= compressImage(selectedImage)

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
        if(requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK){
            viewImage.setImageURI(data?.data)
        }

    }

}

private fun compressImage(image: Bitmap): Bitmap? {
    val baos = ByteArrayOutputStream()
    image.compress(
        Bitmap.CompressFormat.JPEG,
        100,
        baos
    ) //Compression quality, here 100 means no compression, the storage of compressed data to baos
    var options = 90
    while (baos.toByteArray().size / 1024 > 400) {  //Loop if compressed picture is greater than 400kb, than to compression
        baos.reset() //Reset baos is empty baos
        image.compress(
            Bitmap.CompressFormat.JPEG,
            options,
            baos
        ) //The compression options%, storing the compressed data to the baos
        options -= 10 //Every time reduced by 10
    }
    val isBm =
        ByteArrayInputStream(baos.toByteArray()) //The storage of compressed data in the baos to ByteArrayInputStream
    return BitmapFactory.decodeStream(isBm, null, null)
}

private const val REQUEST_CODE = 13
private const val PICK_IMAGE_REQUEST = 1000
private lateinit var filePhoto: File
private const val FILE_NAME = "photo.jpg"