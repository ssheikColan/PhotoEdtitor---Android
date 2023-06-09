package com.binish.sample.photoeditorx

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.binish.photoeditorx.models.StrokeProperties
import com.binish.photoeditorx.photoeditor.*
import com.binish.sample.photoeditorx.adapter.TextEditsAdapter
import com.binish.sample.photoeditorx.fragments.BottomSheetStickerDialog
import com.binish.sample.photoeditorx.fragments.ImagePreviewFragment
import com.binish.sample.photoeditorx.utils.Utils
import com.binish.sample.photoeditorx.views.CustomEditText
import jp.wasabeef.blurry.Blurry
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_add_edit_text.*


class MainActivity : AppCompatActivity() {
    private lateinit var ttCommonBold: Typeface
    private var textSize = 32f
    private var strokeWidth = 0f
    private var strokeColor = 0
    private val textStyle = TextStyleBuilder()
    private lateinit var photoEditor: PhotoEditor
    private var isTextStyleVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Utils.changeStatusBarColor(this, false, R.color.color_black)
        ttCommonBold = Typeface.createFromAsset(assets, "fonts/editFonts/TTCommonBold.otf")


        //from camera
        val selectImg=intent.getStringExtra("selectImg")
        val takenPhoto = BitmapFactory.decodeFile(selectImg)

        //from gallery
        val byteArray = intent.getByteArrayExtra("imgUri")
        val bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteArray!!.size)

        if (bmp != null) {
            photoEditorView.source?.setImageBitmap(bmp)
            photoEditorView.source?.scaleType = ImageView.ScaleType.CENTER_CROP
        }

        if (takenPhoto != null) {
            photoEditorView.source?.setImageBitmap(takenPhoto)
            photoEditorView.source?.scaleType = ImageView.ScaleType.CENTER_CROP
        }

        setUpRotation()
        setUpTextAddition()
        setUpPhotoEditor()
        setUpSaveAndStickers()
    }

    private fun setUpSaveAndStickers() {
        imageButtonSave.setOnClickListener {
            //*Or you can use photoEditor.saveAsFile()*//

            photoEditor.saveAsBitmap(object : OnSaveBitmap {
                override fun onBitmapReady(saveBitmap: Bitmap?) {
                    Utils.startFragment(
                            ImagePreviewFragment.newInstance(saveBitmap),
                            supportFragmentManager, R.id.mainActivityContainer,
                            true,
                            replace = false,
                            addToBackStack = "ImagePreviewStack"
                    )

                    Log.d("image==", saveBitmap.toString())
                }

                override fun onFailure(e: Exception?) {
                    Toast.makeText(this@MainActivity, "Error saving image", Toast.LENGTH_SHORT)
                            .show()
                }
            })
        }

        imageButtonStickers.setOnClickListener {
            val bottomSheetStickerDialog = BottomSheetStickerDialog.newInstance(
                    Utils.takeScreenshot(mainActivityContainer),
                    object : BottomSheetStickerDialog.BottomMapDetailFragmentInteraction {
                        override fun onStickerClicker(bitmap: Bitmap) {
                            photoEditor.addImage(bitmap)
                        }

                        override fun onStickerTime(view: View) {
                            photoEditor.addDynamicSticker(view)
                        }
                    })
            bottomSheetStickerDialog.show(supportFragmentManager, "StickerDialog")
        }
    }

    private fun setUpImage(img:String) {

//        photoEditorView.source?.setImageBitmap(img)

//        photoEditorView.source?.setImageResource(R.drawable.default_image)
        photoEditorView.source?.scaleType = ImageView.ScaleType.CENTER_CROP
    }

    private fun setUpTextAddition() {
        textStyle.withTextFont(ttCommonBold)
        textStyle.withTextColor(ContextCompat.getColor(this, R.color.color_white))
        textStyle.withTextSize(textSize)
        textStyle.withTextAlign(TextView.TEXT_ALIGNMENT_CENTER)
        setUpEditView()

        photoEditorView.setOnClickListener {
            showOrHideEditView(true)
            setUpEdits(null)
        }

        imageButtonColorTextPicker.setOnClickListener {
            colorOrTextWork()
            isTextStyleVisible = !isTextStyleVisible
        }
    }

    private fun colorOrTextWork() {
        recyclerViewColorForEdit.visibility =
                if (isTextStyleVisible) View.VISIBLE else View.GONE
        recyclerViewStyleForEdit.visibility =
                if (isTextStyleVisible) View.GONE else View.VISIBLE
        if (isTextStyleVisible)
            imageButtonColorTextPicker.changeIcon(
                    R.drawable.ic_text_style,
                    R.drawable.camera_edit_color_button
            )
        else
            imageButtonColorTextPicker.changeIcon(
                    R.drawable.ic_color,
                    R.color.color_transparent
            )
    }

    private fun setUpPhotoEditor() {
        photoEditor = PhotoEditor.Builder(this, photoEditorView as PhotoEditorView)
                .setPinchTextScalable(true).build()
        photoEditor.setOnPhotoEditorListener(object : OnPhotoEditorListener {
            override fun onEditTextChangeListener(rootView: View?, text: String?, colorCode: Int) {
                showOrHideEditView(true)
                editViewForEdit.setText(text)
                setUpEdits(rootView)
            }

            override fun onAddViewListener(
                    view: View?,
                    viewType: EnumClass.ViewType?,
                    numberOfAddedViews: Int
            ) {

            }

            override fun onRemoveViewListener(
                    viewType: EnumClass.ViewType?,
                    numberOfAddedViews: Int
            ) {

            }

            override fun onStartViewChangeListener(viewType: EnumClass.ViewType?) {

            }

            override fun onStopViewChangeListener(
                    view: View?,
                    viewType: EnumClass.ViewType?,
                    currX: Float,
                    currY: Float,
                    rawX: Int,
                    rawY: Int
            ) {
                if (view != null)
                    viewDelete.onHideDeleteView(photoEditor, view, rawX, rawY)
            }

            override fun onMoveViewChangeListener(
                    view: View?,
                    isInProgress: Boolean,
                    rawX: Int,
                    rawY: Int
            ) {
                if (view != null)
                    viewDelete.onShowDeleteView(view, isInProgress, rawX, rawY)
            }

            override fun onViewClicked(view: View?) {

            }
        })
    }

    private fun setUpEdits(rootView: View?) {
        recyclerViewColorForEdit.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewColorForEdit.adapter = TextEditsAdapter(
                this,
                Utils.getColors(),
                textStyle,
                object : TextEditsAdapter.OnTextEditsInteractionAdapter() {
                    override fun onColorSelected(color: Int) {
                        val colorCode = ContextCompat.getColor(this@MainActivity, color)
                        if (toggleButtonStroke.isChecked) {
                            strokeColor = colorCode
                            textStyle.withStrokeWidthColor(
                                    StrokeProperties(
                                            if (strokeWidth == 0f) 0.01f else strokeWidth,
                                            colorCode
                                    )
                            )
                            editViewForEdit.setStroke(
                                    if (strokeWidth == 0f) 0.01f else strokeWidth,
                                    colorCode
                            )
                        } else {
                            textStyle.withTextColor(colorCode)
                            editViewForEdit.setTextColor(colorCode)
                        }
                        if (rootView != null)
                            photoEditor.editText(rootView, editViewForEdit.text.toString(), textStyle)
                    }
                })

        recyclerViewStyleForEdit.layoutManager =
                LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerViewStyleForEdit.adapter = TextEditsAdapter(
                this,
                Utils.getTextStyle(assets),
                textStyle,
                object : TextEditsAdapter.OnTextEditsInteractionAdapter() {
                    override fun onTextStyleSelected(typeface: Typeface) {
                        textStyle.withTextFont(typeface)
                        editViewForEdit.typeface = typeface
                        if (rootView != null)
                            photoEditor.editText(rootView, editViewForEdit.text.toString(), textStyle)
                    }
                }
        )

        textViewAlignForEdit.setOnClickListener {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                editViewForEdit.justificationMode = editViewForEdit.justificationMode
                textStyle.withTextJustify(editViewForEdit.justificationMode)
            }

            when (editViewForEdit.textAlignment) {
                EditText.TEXT_ALIGNMENT_TEXT_START -> {
                    textViewAlignForEdit.changeIcon(
                            R.drawable.ic_camera_text_align_center,
                            R.color.color_transparent
                    )
                    editViewForEdit.textAlignment = EditText.TEXT_ALIGNMENT_CENTER
                    textStyle.withTextAlign(EditText.TEXT_ALIGNMENT_CENTER)
                }
                EditText.TEXT_ALIGNMENT_CENTER -> {
                    textViewAlignForEdit.changeIcon(
                            R.drawable.ic_camera_text_align_end,
                            R.color.color_transparent
                    )
                    editViewForEdit.textAlignment = EditText.TEXT_ALIGNMENT_TEXT_END
                    textStyle.withTextAlign(EditText.TEXT_ALIGNMENT_TEXT_END)
                }
                EditText.TEXT_ALIGNMENT_TEXT_END -> {
                    textViewAlignForEdit.changeIcon(
                            R.drawable.ic_camera_text_align_start,
                            R.color.color_transparent
                    )
                    editViewForEdit.textAlignment = EditText.TEXT_ALIGNMENT_TEXT_START
                    textStyle.withTextAlign(EditText.TEXT_ALIGNMENT_TEXT_START)
                }
            }
        }

        toggleButtonStroke.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!isChecked)
                buttonView.setBackgroundResource(R.drawable.camera_edit_color_button)
            buttonView.backgroundTintList =
                    if (isChecked) ContextCompat.getColorStateList(this, R.color.color_white) else null
            buttonView.setTextColor(
                    ContextCompat.getColor(
                            this,
                            if (isChecked) R.color.color_black else R.color.color_white
                    )
            )
            imageButtonColorTextPicker.isEnabled = !isChecked
            if (isChecked) {
                isTextStyleVisible = true
                colorOrTextWork()
            }

            seekBarFontSizeForEdit.progress =
                    if (isChecked) (strokeWidth * 42f).toInt() else textStyle.textSize.toInt()
            seekBarFontSizeForEdit.max = if (isChecked) 100 else 50
        }

//        seekBarFontSizeForEdit.progress = if (toggleButtonStroke.isChecked) (strokeWidth * 42f).toInt() else (textStyle.textSize - 18).toInt()
        seekBarFontSizeForEdit.max = if (toggleButtonStroke.isChecked) 100 else 50
        seekBarFontSizeForEdit.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (toggleButtonStroke.isChecked) {
                    strokeWidth = progress / 42f
                    textStyle.withStrokeWidthColor(
                            StrokeProperties(
                                    if (strokeWidth == 0f) 0.01f else strokeWidth,
                                    strokeColor
                            )
                    )
                    editViewForEdit.setStroke(
                            if (strokeWidth == 0f) 0.01f else strokeWidth,
                            strokeColor
                    )
                } else {
                    textSize = progress + 18f
                    textStyle.withTextSize(textSize)
                    editViewForEdit.textSize = textSize
                }
                if (rootView != null)
                    photoEditor.editText(rootView, editViewForEdit.text.toString(), textStyle)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

            }

        })

        textViewDoneForEdit.setOnClickListener {
            postDoneWork(rootView)
        }
    }

    private fun postDoneWork(rootView: View?) {
        showOrHideEditView(false)
        if (rootView != null)
            photoEditor.editText(rootView, editViewForEdit.text.toString(), textStyle)
        else if (editViewForEdit.text.toString().isNotEmpty())
            photoEditor.addText(editViewForEdit.text.toString(), textStyle)
    }

    private fun showOrHideEditView(show: Boolean) {
        if (show) {
            Blurry.with(this).animate(10).radius(20).async().color(R.color.color_black_transparent)
                    .from(Utils.takeScreenshot(photoEditorView.source!!)).into(viewForEdit)
            editLayoutContainer.transitionToEnd()
            editLayoutContainer.post {
                editViewForEdit.focusAndShowKeyboard(object :
                        CustomEditText.CustomEditTextInteraction {
                    override fun onBackPressedDuringKeyboard() {
                        showOrHideEditView(false)
                    }
                })
            }
        } else {
            editViewForEdit.hideSoftKeyboard()
            editViewForEdit.postDelayed({
                editLayoutContainer.transitionToStart()
            }, 50)
        }
    }

    private fun setUpEditView() {
        editViewForEdit.setTextColor(textStyle.textColor)
        editViewForEdit.textSize = textStyle.textSize
        editViewForEdit.typeface = textStyle.textFont
    }

    private fun setUpRotation() {
        imageButtonRotateLeft.setOnClickListener {
            photoEditorView.rotate(PhotoEditorView.Rotation.ROTATE_270)
        }
        imageButtonRotateRight.setOnClickListener {
            photoEditorView.rotate(PhotoEditorView.Rotation.ROTATE_90)
        }
    }

    override fun onResume() {
        if (editLayoutContainer.currentState == R.id.motionEnd)
            showOrHideEditView(true)
        super.onResume()
    }

    override fun onBackPressed() {
        val intent = Intent(this, ImageSelectionActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}