package com.binish.sample.photoeditorx.fragments

import android.content.ContentValues
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.binish.sample.photoeditorx.R
import kotlinx.android.synthetic.main.fragment_display_saved_image.*
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class ImagePreviewFragment : Fragment() {
    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.apply {
            bitmap = getParcelable(BITMAP)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_display_saved_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        imageViewEditPreviewBack.setOnClickListener {
            fragmentManager?.popBackStack()
        }

        imageViewSavedImage.setImageBitmap(bitmap)
        bitmap?.let { saveMediaToStorage(it) }
    }


    companion object {
        private const val BITMAP = "BITMAP"
        fun newInstance(bitmap: Bitmap?) = ImagePreviewFragment().apply {
            arguments = Bundle().apply {
                putParcelable(BITMAP, bitmap)
            }
        }
    }


    private fun saveMediaToStorage(bitmap: Bitmap) {
        //Generating a file name
        val filename = "${System.currentTimeMillis()}.jpg"

        //Output stream
        var fos: OutputStream? = null

        //For devices running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //getting the contentResolver
            context?.contentResolver?.also { resolver ->

                //Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {

                    //putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                //Inserting the contentValues to contentResolver and getting the Uri
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                //Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            //These for devices running on android < Q
            //So I don't think an explanation is needed here
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            //Finally writing the bitmap to the output stream that we opened
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
//            context?.toast("Saved to Photos")
            Toast.makeText(context, "Saved to Photos", Toast.LENGTH_SHORT)
                .show()
        }
    }
}